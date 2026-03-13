package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.request.CheckoutPreviewRequest;
import com.swp391.eyewear_management_backend.dto.request.CreateOrderRequest;
import com.swp391.eyewear_management_backend.dto.request.ShippingAddressRequest;
import com.swp391.eyewear_management_backend.dto.response.*;
import com.swp391.eyewear_management_backend.entity.*;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import com.swp391.eyewear_management_backend.repository.*;
import com.swp391.eyewear_management_backend.service.CheckoutService;
import com.swp391.eyewear_management_backend.service.OrderService;
import com.swp391.eyewear_management_backend.service.PaymentGatewayService;
import com.swp391.eyewear_management_backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final ZoneId APP_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserRepo userRepo;
    private final CartItemRepo cartItemRepo;
    private final CartItemPrescriptionRepo cartItemPrescriptionRepo;

    private final OrderRepo orderRepo;
    private final OrderDetailRepo orderDetailRepo;
    private final PrescriptionOrderRepo prescriptionOrderRepo;
    private final PrescriptionOrderDetailRepo prescriptionOrderDetailRepo;

    private final ShippingInfoRepo shippingInfoRepo;
    private final PaymentRepo paymentRepo;
    private final InvoiceRepo invoiceRepo;

    private final PromotionRepo promotionRepo;

    private final CheckoutService checkoutService; // reuse preview calculation
    private final PaymentService paymentService; // stub for now
    private final PaymentGatewayService paymentGatewayService;
    private final CheckoutCartTrackingService checkoutCartTrackingService;

    /*
        Hàm này làm 6 nhóm việc chính trong 1 transaction:
        - Xác thực user hiện tại
        - Lấy và kiểm tra cart items của user
        - Tính toán tiền/ship/discount (re-use checkoutService.preview)
        - Tạo dữ liệu DB: Order, OrderDetail / PrescriptionOrderDetail, ShippingInfo, Invoice, Payment
        - Update promotion used_count + xóa cart items
        - Nếu online payment → tạo paymentUrl trả về cho FE redirect
     */
    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {

        // 1) Xác thực user hiện tại
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String username = auth.getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Long userId = user.getUserId();

        // 2) Kiểm tra xem cart items có chứa items nào ko? --> Nếu ko thì throw exception
        List<Long> ids = request.getCartItemIds() == null ? List.of() : request.getCartItemIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) throw new AppException(ErrorCode.CART_ITEM_ID_REQUIRED);

        // 3) Lấy và kiểm tra cart items của user (để insert detail + delete)
        /*
            Mục tiêu:
            - Bảo mật: user A không thể tạo order từ cartItem của user B.
            - Data integrity: phải đủ items thì order mới đúng.
         */
        List<CartItem> cartItems = cartItemRepo.findByUserIdAndIdsFetchAll(userId, ids);
        if (cartItems.size() != ids.size()) throw new AppException(ErrorCode.CART_ITEM_INVALID_FOR_CHECKOUT);  // Kiểm tra số lượng items trong cart của user có bằng với số lượng trong cart đang được chọn hay ko

        // 3.1) load prescription map
        /*
            Tác dụng:
            - Với cartItem kiểu PRESCRIPTION, bạn cần thông số mắt (SPH/CYL/AXIS).
            - Tạo rxMap để tra nhanh theo cartItemId khi loop items.
            Mục tiêu:
            - Tránh mỗi vòng lặp lại query DB → tối ưu.
            - Tách dữ liệu “kính thuốc” (prescription) khỏi CartItem.
         */
        Map<Long, CartItemPrescription> rxMap = new HashMap<>();
        List<CartItemPrescription> rxList = cartItemPrescriptionRepo.findByCartItem_CartItemIdIn(ids);
        for (CartItemPrescription rx : rxList) {
            if (rx.getCartItem() != null && rx.getCartItem().getCartItemId() != null) {
                rxMap.put(rx.getCartItem().getCartItemId(), rx);
            }
        }

        // 4) build address: nếu request không gửi => dùng default của user
        ShippingAddressRequest address = resolveAddress(request.getAddress(), user);

        // 5) chạy preview nội bộ để lấy tất cả số tiền chuẩn
        CheckoutPreviewRequest previewReq = new CheckoutPreviewRequest();
        previewReq.setCartItemIds(ids);
        previewReq.setPromotionId(request.getPromotionId());
        previewReq.setPaymentMethod(request.getPaymentMethod());
        previewReq.setAddress(address);

        CheckoutPreviewResponse preview = checkoutService.preview(previewReq);

        // FIX: nếu user gửi promotionId nhưng preview không áp được => báo lỗi rõ ràng
        if (request.getPromotionId() != null && preview.getAppliedPromotionId() == null) {
            throw new AppException(ErrorCode.PROMOTION_NOT_APPLICABLE); // bạn thêm ErrorCode này
        }

        // 6) validate payment strategy theo preview.depositRequired
        PaymentPlan plan = buildPaymentPlan(preview, request);

        // 7) INSERT Order
        Order order = new Order();
        order.setUser(user);
        order.setOrderCode(generateOrderCode());
        order.setOrderDate(now());

        order.setSubTotal(preview.getSubTotal());
        order.setTaxAmount(BigDecimal.ZERO);
        order.setDiscountAmount(preview.getDiscountAmount());
        order.setShippingFee(preview.getShippingFee());

        String orderType = determineOrderTypeByBusinessRule(preview.getItems(), cartItems, rxMap);
        order.setOrderType(orderType);
        order.setOrderStatus("PENDING");

        // FIX: không dùng setPromotionId (Order không có field promotionId)
        if (preview.getAppliedPromotionId() != null) {
            Promotion promotion = promotionRepo.findById(preview.getAppliedPromotionId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
            order.setPromotion(promotion);
        } else {
            order.setPromotion(null);
        }

        Order savedOrder = orderRepo.save(order);
        checkoutCartTrackingService.recordCheckoutCartItemIds(savedOrder, user, ids);

        // 8) INSERT Order_Detail + Prescription_Order(+Detail)
        Map<Long, CheckoutLineItemResponse> lineMap = new HashMap<>();
        for (CheckoutLineItemResponse li : preview.getItems()) {
            lineMap.put(li.getCartItemId(), li);
        }

        // 8.1) create prescription header if needed
        boolean hasPrescription = cartItems.stream()
                .anyMatch(ci -> isPrescriptionItemByBusinessRule(ci, rxMap.get(ci.getCartItemId())));
        PrescriptionOrder savedRxOrder = null;

        if (hasPrescription) {
            PrescriptionOrder rxOrder = new PrescriptionOrder();
            rxOrder.setOrder(savedOrder);
            rxOrder.setUser(user);
            rxOrder.setPrescriptionDate(now());
            savedRxOrder = prescriptionOrderRepo.save(rxOrder);
        }

        // 8.2) loop items
        List<OrderDetail> normalDetails = new ArrayList<>();
        List<PrescriptionOrderDetail> rxDetails = new ArrayList<>();

        for (CartItem ci : cartItems) {
            CheckoutLineItemResponse li = lineMap.get(ci.getCartItemId());
            if (li == null) continue;

            CartItemPrescription rx = rxMap.get(ci.getCartItemId());
            boolean isPrescriptionItem = isPrescriptionItemByBusinessRule(ci, rx);
            if (!isPrescriptionItem) {
                normalDetails.add(buildNormalOrderDetail(savedOrder, ci, li));
            } else if (ci.getContactLens() != null) {
                normalDetails.add(buildNormalOrderDetail(savedOrder, ci, li));
            } else {
                // === PRESCRIPTION -> Prescription_Order_Detail
                if (savedRxOrder == null) {
                    throw new AppException(ErrorCode.CART_ITEM_INVALID_FOR_CHECKOUT);
                }

                int qty = li.getQuantity() == null ? 1 : li.getQuantity();
                BigDecimal lineDiscount = li.getLineDiscount() == null ? BigDecimal.ZERO : li.getLineDiscount();
                BigDecimal perUnitDiscount = (qty <= 0) ? BigDecimal.ZERO : lineDiscount.divide(BigDecimal.valueOf(qty), 0, BigDecimal.ROUND_HALF_UP);

                // Vì Prescription_Order_Detail KHÔNG có Quantity
                // => insert N dòng tương ứng qty (MVP)
                for (int i = 0; i < qty; i++) {
                    PrescriptionOrderDetail pod = new PrescriptionOrderDetail();
                    pod.setPrescriptionOrder(savedRxOrder);
                    pod.setFrame(ci.getFrame());
                    pod.setLens(ci.getLens());

                    if (rx != null) {
                        pod.setRightEyeSph(rx.getRightEyeSph());
                        pod.setRightEyeCyl(rx.getRightEyeCyl());
                        pod.setRightEyeAxis(rx.getRightEyeAxis());
                        pod.setRightEyeAdd(rx.getRightEyeAdd());

                        pod.setLeftEyeSph(rx.getLeftEyeSph());
                        pod.setLeftEyeCyl(rx.getLeftEyeCyl());
                        pod.setLeftEyeAxis(rx.getLeftEyeAxis());
                        pod.setLeftEyeAdd(rx.getLeftEyeAdd());

                        pod.setPd(rx.getPd());
                        pod.setPdRight(rx.getPdRight());
                        pod.setPdLeft(rx.getPdLeft());
                    }

                    // Sub_Total = unitPrice - perUnitDiscount (net per unit)
                    BigDecimal net = li.getUnitPrice().subtract(perUnitDiscount);
                    if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;
                    pod.setSubTotal(net);

                    rxDetails.add(pod);
                }
            }
        }

        if (!normalDetails.isEmpty()) {
            orderDetailRepo.saveAll(normalDetails);
        }
        if (!rxDetails.isEmpty()) {
            prescriptionOrderDetailRepo.saveAll(rxDetails);
        }

        // 9) INSERT Shipping_Info
        ShippingInfo ship = new ShippingInfo();
        ship.setOrder(savedOrder);
        ship.setRecipientName(request.getRecipientName());
        ship.setRecipientPhone(request.getRecipientPhone());
        ship.setRecipientEmail(request.getRecipientEmail());
        ship.setNote(request.getNote());

        ship.setRecipientAddress(buildFullAddress(address, user));
        ship.setProvinceCode(address != null ? address.getProvinceCode() : null);
        ship.setProvinceName(address != null ? address.getProvinceName() : null);
        ship.setDistrictCode(address != null ? address.getDistrictCode() : null);
        ship.setDistrictName(address != null ? address.getDistrictName() : null);
        ship.setWardCode(address != null ? address.getWardCode() : null);
        ship.setWardName(address != null ? address.getWardName() : null);

        ship.setShippingMethod("GHN");
        ship.setShippingFee(preview.getShippingFee());
        ship.setShippingStatus("PENDING");
        ship.setExpectedDeliveryAt(preview.getExpectedDeliveryAt());

        shippingInfoRepo.save(ship);

        // 10) INSERT Invoice
        Invoice invoice = new Invoice();
        invoice.setOrder(savedOrder);
        invoice.setIssueDate(now());
        invoice.setTotalAmount(preview.getTotalAmount());
        invoice.setStatus("UNPAID");
        invoiceRepo.save(invoice);

        // 11) INSERT Payment record(s)
        Payment createdPayment = null;

        if (plan.createDepositPayment) {
            Payment dep = Payment.builder()
                    .order(savedOrder)
                    .paymentPurpose("DEPOSIT")
                    .createdAt(now())
                    .paymentDate(null)
                    .paymentMethod(plan.depositMethod)
                    .amount(plan.depositAmount)
                    .status("PENDING")
                    .build();
            paymentRepo.save(dep);

            createdPayment = dep; // payment cần redirect

            Payment rem = Payment.builder()
                    .order(savedOrder)
                    .paymentPurpose("REMAINING")
                    .createdAt(now())
                    .paymentDate(null)
                    .paymentMethod("COD")
                    .amount(plan.remainingAmount)
                    .status("PENDING")
                    .build();
            paymentRepo.save(rem);

        } else {
            Payment full = Payment.builder()
                    .order(savedOrder)
                    .paymentPurpose("FULL")
                    .createdAt(now())
                    .paymentDate(null)
                    .paymentMethod(plan.fullMethod)
                    .amount(plan.fullAmount)
                    .status("PENDING")
                    .build();
            paymentRepo.save(full);

            // nếu online thì đây là payment cần redirect
            if (!"COD".equalsIgnoreCase(plan.fullMethod)) {
                createdPayment = full;
            }
        }

        // 12) update Promotion.Used_Count + 1
        if (preview.getAppliedPromotionId() != null) {
            promotionRepo.incrementUsedCount(preview.getAppliedPromotionId());
        }

        boolean hasOnlinePendingPayment = createdPayment != null
                && createdPayment.getPaymentMethod() != null
                && !"COD".equalsIgnoreCase(createdPayment.getPaymentMethod());
        if (!hasOnlinePendingPayment) {
            cartItemRepo.deleteAll(cartItems);
        }

        // 14) nếu cần online redirect => tạo paymentUrl theo paymentMethod
        String paymentUrl = null;
        boolean redirect = false;
        Long paymentId = null;

        if (createdPayment != null) {
            String method = createdPayment.getPaymentMethod();
            if (method != null && !"COD".equalsIgnoreCase(method)) {
                paymentId = createdPayment.getPaymentID();
                try {
                    BigDecimal payAmount = createdPayment.getAmount();
                    paymentUrl = paymentGatewayService.createPaymentUrl(
                            method,
                            savedOrder.getOrderID(),
                            paymentId,
                            payAmount
                    );
                } catch (RuntimeException ex) {
                    paymentUrl = null;
                }
                redirect = paymentUrl != null;
            }
        }

        return CreateOrderResponse.builder()
                .orderId(savedOrder.getOrderID())
                .orderCode(savedOrder.getOrderCode())
                .orderStatus(savedOrder.getOrderStatus())

                .subTotal(preview.getSubTotal())
                .discountAmount(preview.getDiscountAmount())
                .shippingFee(preview.getShippingFee())
                .expectedDeliveryAt(preview.getExpectedDeliveryAt())
                .totalAmount(preview.getTotalAmount())

                .depositRequired(preview.isDepositRequired())
                .depositAmount(preview.getDepositAmount())
                .remainingAmount(preview.getRemainingAmount())

                .appliedPromotionId(preview.getAppliedPromotionId())

                .paymentRedirectRequired(redirect)
                .paymentUrl(paymentUrl)
                .paymentId(paymentId)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStatusResponse getOrderStatus(Long orderId) {

        // 1) current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String username = auth.getName();
        User currentUser = userRepo.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2) load order (fetch invoice + payments + shippingInfo)
        Order order = orderRepo.findByIdFetchStatus(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND)); // bạn thêm ErrorCode nếu chưa có

        // 3) authorize: owner hoặc ADMIN
        boolean isOwner = order.getUser() != null
                && Objects.equals(order.getUser().getUserId(), currentUser.getUserId());

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority())
                        || "ADMIN".equalsIgnoreCase(a.getAuthority()));

        if (!isOwner && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED); // bạn thêm ErrorCode nếu chưa có
        }

        // 4) map payments
        List<PaymentStatusResponse> payments = (order.getPayments() == null) ? List.of()
                : order.getPayments().stream()
                .map(p -> PaymentStatusResponse.builder()
                        .paymentId(p.getPaymentID())
                        .paymentPurpose(p.getPaymentPurpose())
                        .paymentMethod(p.getPaymentMethod())
                        .status(p.getStatus())
                        .amount(p.getAmount())
                        .paymentDate(p.getPaymentDate())
                        .build())
                // sort: DEPOSIT trước, rồi FULL/REMAINING tuỳ bạn
                .sorted(Comparator.comparing(PaymentStatusResponse::getPaymentPurpose, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        String invoiceStatus = (order.getInvoice() != null) ? order.getInvoice().getStatus() : null;

        LocalDateTime expectedDeliveryAt = (order.getShippingInfo() != null)
                ? order.getShippingInfo().getExpectedDeliveryAt()
                : null;

        return OrderStatusResponse.builder()
                .orderId(order.getOrderID())
                .orderCode(order.getOrderCode())
                .orderStatus(order.getOrderStatus())
                .orderType(order.getOrderType())

                .subTotal(order.getSubTotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount()) // computed column DB

                .invoiceStatus(invoiceStatus)
                .expectedDeliveryAt(expectedDeliveryAt)

                .payments(payments)
                .build();
    }

    private Product resolveOrderDetailProduct(CartItem ci) {
        if (ci.getContactLens() != null) return ci.getContactLens().getProduct();
        if (ci.getFrame() != null && ci.getLens() == null) return ci.getFrame().getProduct();
        if (ci.getLens() != null && ci.getFrame() == null) return ci.getLens().getProduct();
        return null; // frame+lens => prescription, không vào Order_Detail
    }

    private OrderDetail buildNormalOrderDetail(Order order, CartItem ci, CheckoutLineItemResponse li) {
        Product product = resolveOrderDetailProduct(ci);
        if (product == null) {
            throw new AppException(ErrorCode.CART_ITEM_INVALID_FOR_CHECKOUT);
        }
        OrderDetail od = new OrderDetail();
        od.setOrder(order);
        od.setProduct(product);
        od.setUnitPrice(li.getUnitPrice());
        od.setQuantity(li.getQuantity() == null ? 1 : li.getQuantity());
        return od;
    }

    private ShippingAddressRequest resolveAddress(ShippingAddressRequest reqAddr, User user) {
        if (reqAddr != null && reqAddr.getDistrictCode() != null && reqAddr.getWardCode() != null) {
            return reqAddr;
        }

        // dùng default codes từ user
        if (user.getDistrictCode() == null || user.getWardCode() == null) {
            // chưa có default codes => preview sẽ shipFee=0, FE buộc user chọn địa chỉ mới
            return null;
        }

        ShippingAddressRequest a = new ShippingAddressRequest();
        a.setStreet(null);
        a.setProvinceCode(user.getProvinceCode());
        a.setProvinceName(user.getProvinceName());
        a.setDistrictCode(user.getDistrictCode());
        a.setDistrictName(user.getDistrictName());
        a.setWardCode(user.getWardCode());
        a.setWardName(user.getWardName());
        return a;
    }

    private String buildFullAddress(ShippingAddressRequest address, User user) {
        if (address == null) return user.getAddress();

        String street = address.getStreet() != null ? address.getStreet().trim() : null;
        if (street == null || street.isEmpty()) return user.getAddress();

        return street + ", " + address.getWardName() + ", " + address.getDistrictName() + ", " + address.getProvinceName();
    }

    private String generateOrderCode() {
        return "ORD-" + now().toLocalDate() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Convert rule UI payment => plan tạo Payment records.
     * - Nếu không cần cọc:
     * COD => 1 record FULL COD PENDING
     * VNPAY/MOMO => 1 record FULL online PENDING (redirect)
     * - Nếu cần cọc:
     * paymentMethod=COD => DEPOSIT online + REMAINING COD
     * paymentMethod=VNPAY/MOMO:
     * payStrategy=FULL => FULL online
     * payStrategy=DEPOSIT => DEPOSIT online + REMAINING COD
     */
    private PaymentPlan buildPaymentPlan(CheckoutPreviewResponse preview, CreateOrderRequest req) {
        boolean depositRequired = preview.isDepositRequired();

        String method = req.getPaymentMethod(); // COD/VNPAY/MOMO/PAYOS
        String payStrategy = req.getPayStrategy(); // FULL/DEPOSIT

        if (!depositRequired) {
            return PaymentPlan.full(method, preview.getTotalAmount());
        }

        BigDecimal dep = preview.getDepositAmount();
        BigDecimal rem = preview.getRemainingAmount();

        if ("COD".equalsIgnoreCase(method)) {
            // COD main but deposit must be online
            if (req.getDepositPaymentMethod() == null) {
                throw new AppException(ErrorCode.DEPOSIT_PAYMENT_METHOD_REQUIRED);
            }
            return PaymentPlan.deposit(req.getDepositPaymentMethod(), dep, rem);
        }

        // online main
        if ("FULL".equalsIgnoreCase(payStrategy)) {
            return PaymentPlan.full(method, preview.getTotalAmount());
        }

        // default: DEPOSIT
        return PaymentPlan.deposit(method, dep, rem);
    }

    private static class PaymentPlan {
        boolean createDepositPayment;
        String depositMethod;
        BigDecimal depositAmount;
        BigDecimal remainingAmount;

        String fullMethod;
        BigDecimal fullAmount;

        static PaymentPlan deposit(String depositMethod, BigDecimal depositAmount, BigDecimal remainingAmount) {
            PaymentPlan p = new PaymentPlan();
            p.createDepositPayment = true;
            p.depositMethod = depositMethod;
            p.depositAmount = depositAmount;
            p.remainingAmount = remainingAmount;
            return p;
        }

        static PaymentPlan full(String fullMethod, BigDecimal fullAmount) {
            PaymentPlan p = new PaymentPlan();
            p.createDepositPayment = false;
            p.fullMethod = fullMethod;
            p.fullAmount = fullAmount;
            return p;
        }
    }

    private BigDecimal toBd(Double v) {
        if (v == null) return null;
        return new BigDecimal(String.valueOf(v)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toPdBd(Double v) {
        if (v == null) return null;
        return new BigDecimal(String.valueOf(v)).setScale(1, RoundingMode.HALF_UP);
    }

    private boolean hasPdData(CartItemPrescription rx) {
        return rx.getPd() != null || (rx.getPdRight() != null && rx.getPdLeft() != null);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(APP_ZONE_ID);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrderHistoryResponse> getCustomerOrderHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String username = auth.getName();
        User currentUser = userRepo.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Order> orders = orderRepo.findAllByUserIdWithShippingInfo(currentUser.getUserId());

        return orders.stream()
                .map(order -> CustomerOrderHistoryResponse.builder()
                        .orderId(order.getOrderID())
                        .orderCode(order.getOrderCode())
                        .orderType(order.getOrderType())
                        .orderStatus(order.getOrderStatus())
                        .orderDate(order.getOrderDate())
                        .totalAmount(order.getTotalAmount())
                        .shippingStatus(order.getShippingInfo() != null ? order.getShippingInfo().getShippingStatus() : null)
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffOrderDetailResponse getOrderDetailForCustomer(Long orderId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String username = auth.getName();
        User currentUser = userRepo.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Order order = orderRepo.findByIdFetchStatus(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        boolean isOwner = order.getUser() != null
                && Objects.equals(order.getUser().getUserId(), currentUser.getUserId());

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority())
                        || "ADMIN".equalsIgnoreCase(a.getAuthority()));

        if (!isOwner && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Long orderEntityId = order.getOrderID();
        ShippingInfo shippingInfo = order.getShippingInfo();
        String shippingStatus = shippingInfo != null ? shippingInfo.getShippingStatus() : null;
        BigDecimal shippingFee = shippingInfo != null ? shippingInfo.getShippingFee() : null;
        LocalDateTime expectedDeliveryAt = shippingInfo != null ? shippingInfo.getExpectedDeliveryAt() : null;
        Boolean isPastExpectedDeliveryAt = expectedDeliveryAt != null && LocalDateTime.now(APP_ZONE_ID).isAfter(expectedDeliveryAt);

        List<StaffOrderItemResponse> orderItems = orderDetailRepo.findByOrderIdFetchProduct(orderEntityId).stream()
                .map(this::toOrderItemResponse)
                .toList();

        List<StaffPrescriptionOrderItemResponse> prescriptionItems = mapPrescriptionItems(orderEntityId);
        boolean hasPrescriptionItem = !prescriptionItems.isEmpty();
        boolean requiresFinalPayment = isRequiresFinalPayment(order);

        return StaffOrderDetailResponse.builder()
                .orderId(order.getOrderID())
                .orderCode(order.getOrderCode())
                .orderStatus(order.getOrderStatus())
                .orderType(order.getOrderType())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .shippingStatus(shippingStatus)
                .shippingFee(shippingFee)
                .expectedDeliveryAt(expectedDeliveryAt)
                .isPastExpectedDeliveryAt(isPastExpectedDeliveryAt)
                .hasPrescriptionItem(hasPrescriptionItem)
                .requiresFinalPayment(requiresFinalPayment)
                .availableActions(List.of())
                .customerName(order.getUser() != null ? order.getUser().getName() : null)
                .customerPhone(order.getUser() != null ? order.getUser().getPhone() : null)
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .orderDetail(orderItems)
                .prescriptionOrderDetail(prescriptionItems)
                .recipientName(shippingInfo != null ? shippingInfo.getRecipientName() : null)
                .recipientPhone(shippingInfo != null ? shippingInfo.getRecipientPhone() : null)
                .recipientEmail(shippingInfo != null ? shippingInfo.getRecipientEmail() : null)
                .recipientAddress(shippingInfo != null ? shippingInfo.getRecipientAddress() : null)
                .note(shippingInfo != null ? shippingInfo.getNote() : null)
                .build();
    }

    private StaffOrderItemResponse toOrderItemResponse(OrderDetail detail) {
        Product product = detail.getProduct();
        Integer quantity = detail.getQuantity() == null ? 0 : detail.getQuantity();
        BigDecimal unitPrice = detail.getUnitPrice() == null ? BigDecimal.ZERO : detail.getUnitPrice();
        return StaffOrderItemResponse.builder()
                .productId(product != null ? product.getProductID() : null)
                .productName(product != null ? product.getProductName() : null)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                .imageUrl(pickPrimaryImage(product))
                .build();
    }

    private List<StaffPrescriptionOrderItemResponse> mapPrescriptionItems(Long orderId) {
        PrescriptionOrder prescriptionOrder = prescriptionOrderRepo.findByOrder_OrderID(orderId).orElse(null);
        if (prescriptionOrder == null || prescriptionOrder.getPrescriptionOrderDetails() == null) {
            return List.of();
        }

        Map<PrescriptionGroupKey, RxAggregate> aggregates = new LinkedHashMap<>();
        for (PrescriptionOrderDetail detail : prescriptionOrder.getPrescriptionOrderDetails()) {
            String rightSph = bdToText(detail.getRightEyeSph());
            String rightCyl = bdToText(detail.getRightEyeCyl());
            String rightAxis = detail.getRightEyeAxis() == null ? null : String.valueOf(detail.getRightEyeAxis());
            String rightAdd = detail.getRightEyeAdd() == null ? null : String.valueOf(detail.getRightEyeAdd());
            String rightPD = bdToText(detail.getPdRight());
            String leftSph = bdToText(detail.getLeftEyeSph());
            String leftCyl = bdToText(detail.getLeftEyeCyl());
            String leftAxis = detail.getLeftEyeAxis() == null ? null : String.valueOf(detail.getLeftEyeAxis());
            String leftAdd = detail.getLeftEyeAdd() == null ? null : String.valueOf(detail.getLeftEyeAdd());
            String leftPD = bdToText(detail.getPdLeft());

            Long frameId = detail.getFrame() != null ? detail.getFrame().getFrameID() : null;
            Long lensId = detail.getLens() != null ? detail.getLens().getLensID() : null;
            Product frameProduct = detail.getFrame() != null ? detail.getFrame().getProduct() : null;
            Product lensProduct = detail.getLens() != null ? detail.getLens().getProduct() : null;
            String frameName = frameProduct != null ? frameProduct.getProductName() : null;
            String lensName = lensProduct != null ? lensProduct.getProductName() : null;

            BigDecimal framePrice = productPrice(frameProduct);
            BigDecimal lensPrice = productPrice(lensProduct);
            BigDecimal lineTotal = detail.getSubTotal() == null ? BigDecimal.ZERO : detail.getSubTotal();

            PrescriptionGroupKey key = new PrescriptionGroupKey(
                    frameId,
                    lensId,
                    rightSph,
                    rightCyl,
                    rightAxis,
                    rightAdd,
                    rightPD,
                    leftSph,
                    leftCyl,
                    leftAxis,
                    leftAdd,
                    leftPD,
                    bdToText(lineTotal)
            );

            RxAggregate aggregate = aggregates.computeIfAbsent(key, k -> new RxAggregate(
                    StaffPrescriptionOrderItemResponse.builder()
                            .frameId(frameId)
                            .frameName(frameName)
                            .framePrice(framePrice)
                            .frameImg(pickPrimaryImage(frameProduct))
                            .lensId(lensId)
                            .lensName(lensName)
                            .lensPrice(lensPrice)
                            .lensImg(pickPrimaryImage(lensProduct))
                            .contactLensId(null)
                            .contactLensName(null)
                            .contactLensPrice(BigDecimal.ZERO)
                            .contactLensImg(null)
                            .rightEyeSph(rightSph)
                            .rightEyeCyl(rightCyl)
                            .rightEyeAxis(rightAxis)
                            .rightEyeAdd(rightAdd)
                            .rightPD(rightPD)
                            .leftEyeSph(leftSph)
                            .leftEyeCyl(leftCyl)
                            .leftEyeAxis(leftAxis)
                            .leftEyeAdd(leftAdd)
                            .leftPD(leftPD)
                            .quantity(0)
                            .totalPrice(BigDecimal.ZERO)
                            .build()
            ));

            aggregate.response.setQuantity(aggregate.response.getQuantity() + 1);
            aggregate.response.setTotalPrice(aggregate.response.getTotalPrice().add(lineTotal));
        }

        return aggregates.values().stream().map(a -> a.response).toList();
    }

    private BigDecimal productPrice(Product product) {
        return product != null && product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
    }

    private String pickPrimaryImage(Product product) {
        if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
                .filter(Objects::nonNull)
                .filter(i -> Boolean.TRUE.equals(i.getAvatar()))
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElseGet(() -> product.getImages().stream()
                        .filter(Objects::nonNull)
                        .map(ProductImage::getImageUrl)
                        .filter(url -> url != null && !url.trim().isEmpty())
                        .findFirst()
                        .orElse(null));
    }

    private String bdToText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String bdToText(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private boolean isRequiresFinalPayment(Order order) {
        if (order == null) {
            return false;
        }
        Invoice invoice = order.getInvoice();
        if (invoice != null && isStatus(invoice.getStatus(), "PARTIALLY_PAID")) {
            return true;
        }
        return isStatus(order.getOrderStatus(), "PARTIALLY_PAID");
    }

    private boolean isStatus(String value, String expected) {
        return value != null && expected != null && value.trim().equalsIgnoreCase(expected);
    }

    private static class RxAggregate {
        private final StaffPrescriptionOrderItemResponse response;

        private RxAggregate(StaffPrescriptionOrderItemResponse response) {
            this.response = response;
        }
    }

    private static class PrescriptionGroupKey {
        private final Long frameId;
        private final Long lensId;
        private final String rightEyeSph;
        private final String rightEyeCyl;
        private final String rightEyeAxis;
        private final String rightEyeAdd;
        private final String rightPD;
        private final String leftEyeSph;
        private final String leftEyeCyl;
        private final String leftEyeAxis;
        private final String leftEyeAdd;
        private final String leftPD;
        private final String lineSubTotal;

        private PrescriptionGroupKey(Long frameId, Long lensId, String rightEyeSph, String rightEyeCyl,
                                     String rightEyeAxis, String rightEyeAdd, String rightPD,
                                     String leftEyeSph, String leftEyeCyl, String leftEyeAxis,
                                     String leftEyeAdd, String leftPD, String lineSubTotal) {
            this.frameId = frameId;
            this.lensId = lensId;
            this.rightEyeSph = rightEyeSph;
            this.rightEyeCyl = rightEyeCyl;
            this.rightEyeAxis = rightEyeAxis;
            this.rightEyeAdd = rightEyeAdd;
            this.rightPD = rightPD;
            this.leftEyeSph = leftEyeSph;
            this.leftEyeCyl = leftEyeCyl;
            this.leftEyeAxis = leftEyeAxis;
            this.leftEyeAdd = leftEyeAdd;
            this.leftPD = leftPD;
            this.lineSubTotal = lineSubTotal;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PrescriptionGroupKey that = (PrescriptionGroupKey) o;
            return Objects.equals(frameId, that.frameId)
                    && Objects.equals(lensId, that.lensId)
                    && Objects.equals(rightEyeSph, that.rightEyeSph)
                    && Objects.equals(rightEyeCyl, that.rightEyeCyl)
                    && Objects.equals(rightEyeAxis, that.rightEyeAxis)
                    && Objects.equals(rightEyeAdd, that.rightEyeAdd)
                    && Objects.equals(rightPD, that.rightPD)
                    && Objects.equals(leftEyeSph, that.leftEyeSph)
                    && Objects.equals(leftEyeCyl, that.leftEyeCyl)
                    && Objects.equals(leftEyeAxis, that.leftEyeAxis)
                    && Objects.equals(leftEyeAdd, that.leftEyeAdd)
                    && Objects.equals(leftPD, that.leftPD)
                    && Objects.equals(lineSubTotal, that.lineSubTotal);
        }

        @Override
        public int hashCode() {
            return Objects.hash(frameId, lensId, rightEyeSph, rightEyeCyl, rightEyeAxis, rightEyeAdd, rightPD,
                    leftEyeSph, leftEyeCyl, leftEyeAxis, leftEyeAdd, leftPD, lineSubTotal);
        }
    }

    private boolean isPdRequired(CartItem ci) {
        return ci.getFrame() != null;
    }

    private boolean isPrescriptionItemByBusinessRule(CartItem ci, CartItemPrescription rx) {
        if (ci.getFrame() != null && ci.getLens() != null) return true;
        if (ci.getLens() != null && ci.getFrame() == null) return rx != null && hasPrescriptionData(rx);
        if (ci.getContactLens() != null) return rx != null && hasPrescriptionData(rx);
        return false;
    }

    private String determineOrderTypeByBusinessRule(List<CheckoutLineItemResponse> previewItems,
                                                    List<CartItem> cartItems,
                                                    Map<Long, CartItemPrescription> rxMap) {
        Map<Long, CheckoutLineItemResponse> previewMap = new HashMap<>();
        for (CheckoutLineItemResponse li : previewItems) {
            previewMap.put(li.getCartItemId(), li);
        }
        boolean hasDirect = false;
        boolean hasPre = false;
        boolean hasPrescription = false;
        for (CartItem ci : cartItems) {
            CheckoutLineItemResponse li = previewMap.get(ci.getCartItemId());
            boolean isPrescription = isPrescriptionItemByBusinessRule(ci, rxMap.get(ci.getCartItemId()));
            boolean isPre = li != null && "PRE_ORDER".equals(li.getItemType());
            if (isPrescription) {
                hasPrescription = true;
            } else if (isPre) {
                hasPre = true;
            } else {
                hasDirect = true;
            }
        }
        int types = 0;
        if (hasDirect) types++;
        if (hasPre) types++;
        if (hasPrescription) types++;
        if (types > 1) return "MIX_ORDER";
        if (hasPrescription) return "PRESCRIPTION_ORDER";
        if (hasPre) return "PRE_ORDER";
        return "DIRECT_ORDER";
    }

    private boolean hasPrescriptionData(CartItemPrescription rx) {
        return rx.getRightEyeSph() != null
                || rx.getRightEyeCyl() != null
                || rx.getRightEyeAxis() != null
                || rx.getRightEyeAdd() != null
                || rx.getLeftEyeSph() != null
                || rx.getLeftEyeCyl() != null
                || rx.getLeftEyeAxis() != null
                || rx.getLeftEyeAdd() != null
                || rx.getPd() != null
                || rx.getPdRight() != null
                || rx.getPdLeft() != null;
    }
}
