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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

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

    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {

        // 1) current user (fix NPE getName)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            // đổi ErrorCode theo project bạn nếu khác
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String username = auth.getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Long userId = user.getUserId();

        // 2) normalize ids
        List<Long> ids = request.getCartItemIds() == null ? List.of() : request.getCartItemIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) throw new AppException(ErrorCode.INVALID_REQUEST);

        // 3) load cart items belong to user (để insert detail + delete)
        List<CartItem> cartItems = cartItemRepo.findByUserIdAndIdsFetchAll(userId, ids);
        if (cartItems.size() != ids.size()) throw new AppException(ErrorCode.INVALID_REQUEST);

        // 3.1) load prescription map
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
        order.setOrderDate(LocalDateTime.now());

        order.setSubTotal(preview.getSubTotal());
        order.setTaxAmount(BigDecimal.ZERO);
        order.setDiscountAmount(preview.getDiscountAmount());
        order.setShippingFee(preview.getShippingFee());

        order.setOrderType(preview.getOrderType());
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

        // 8) INSERT Order_Detail + Prescription_Order(+Detail)
        Map<Long, CheckoutLineItemResponse> lineMap = new HashMap<>();
        for (CheckoutLineItemResponse li : preview.getItems()) {
            lineMap.put(li.getCartItemId(), li);
        }

        // 8.1) create prescription header if needed
        boolean hasPrescription = preview.getItems().stream().anyMatch(li -> "PRESCRIPTION".equals(li.getItemType()));
        PrescriptionOrder savedRxOrder = null;

        if (hasPrescription) {
            PrescriptionOrder rxOrder = new PrescriptionOrder();
            rxOrder.setOrder(savedOrder);
            rxOrder.setUser(user);
            rxOrder.setPrescriptionDate(LocalDateTime.now());
            rxOrder.setNote(request.getNote());
            savedRxOrder = prescriptionOrderRepo.save(rxOrder);
        }

        // 8.2) loop items
        List<OrderDetail> normalDetails = new ArrayList<>();
        List<PrescriptionOrderDetail> rxDetails = new ArrayList<>();

        for (CartItem ci : cartItems) {
            CheckoutLineItemResponse li = lineMap.get(ci.getCartItemId());
            if (li == null) continue;

            if (!"PRESCRIPTION".equals(li.getItemType())) {
                // === NORMAL (DIRECT / PRE_ORDER) -> Order_Detail
                Product product = resolveOrderDetailProduct(ci);
                if (product == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }

                OrderDetail od = new OrderDetail();
                od.setOrder(savedOrder);
                od.setProduct(product);
                od.setUnitPrice(li.getUnitPrice());
                od.setQuantity(li.getQuantity() == null ? 1 : li.getQuantity());
                od.setNote(request.getNote());

                normalDetails.add(od);
            } else {
                // === PRESCRIPTION -> Prescription_Order_Detail
                if (savedRxOrder == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }

                CartItemPrescription rx = rxMap.get(ci.getCartItemId());

                int qty = li.getQuantity() == null ? 1 : li.getQuantity();
                BigDecimal lineDiscount = li.getLineDiscount() == null ? BigDecimal.ZERO : li.getLineDiscount();
                BigDecimal perUnitDiscount = (qty <= 0) ? BigDecimal.ZERO : lineDiscount.divide(BigDecimal.valueOf(qty), 0, BigDecimal.ROUND_HALF_UP);

                // ✅ Vì Prescription_Order_Detail của bạn KHÔNG có Quantity
                // => insert N dòng tương ứng qty (MVP)
                for (int i = 0; i < qty; i++) {
                    PrescriptionOrderDetail pod = new PrescriptionOrderDetail();
                    pod.setPrescriptionOrder(savedRxOrder);
                    pod.setFrame(ci.getFrame());
                    pod.setLens(ci.getLens());

                    if (rx != null) {
                        pod.setRightEyeSph(toBd(rx.getRightEyeSph()));
                        pod.setRightEyeCyl(toBd(rx.getRightEyeCyl()));
                        pod.setRightEyeAxis(rx.getRightEyeAxis());

                        pod.setLeftEyeSph(toBd(rx.getLeftEyeSph()));
                        pod.setLeftEyeCyl(toBd(rx.getLeftEyeCyl()));
                        pod.setLeftEyeAxis(rx.getLeftEyeAxis());
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
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setTotalAmount(preview.getTotalAmount());
        invoice.setStatus("UNPAID");
        invoiceRepo.save(invoice);

        // 11) INSERT Payment record(s)
        Payment createdPayment = null;

        if (plan.createDepositPayment) {
            Payment dep = Payment.builder()
                    .order(savedOrder)
                    .paymentPurpose("DEPOSIT")
                    .createdAt(LocalDateTime.now())
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
                    .createdAt(LocalDateTime.now())
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
                    .createdAt(LocalDateTime.now())
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

        // 13) Load managed cart items rồi xóa bằng JPA remove (cascade/orphanRemoval sẽ chạy)
        var managedCartItems = cartItemRepo.findByCartItemIdIn(ids);
        if (managedCartItems.size() != ids.size()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        cartItemRepo.deleteAll(managedCartItems);

        // 14) nếu cần online redirect => tạo paymentUrl (hiện tại stub)
        String paymentUrl = null;
        boolean redirect = false;
        Long paymentId = null;

        if (createdPayment != null && !"COD".equalsIgnoreCase(createdPayment.getPaymentMethod())) {
            redirect = true;
            paymentId = createdPayment.getPaymentID();
            long payosAmount = createdPayment.getAmount().longValue();

            paymentUrl = paymentService.createPayOSPaymentUrl(
                    paymentId,
                    payosAmount,
                    savedOrder.getOrderCode()
            );
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
        return "ORD-" + LocalDateTime.now().toLocalDate() + "-" +
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

        String method = req.getPaymentMethod(); // COD/VNPAY/MOMO
        String payStrategy = req.getPayStrategy(); // FULL/DEPOSIT

        if (!depositRequired) {
            return PaymentPlan.full(method, preview.getTotalAmount());
        }

        BigDecimal dep = preview.getDepositAmount();
        BigDecimal rem = preview.getRemainingAmount();

        if ("COD".equalsIgnoreCase(method)) {
            // COD main but deposit must be online
            if (req.getDepositPaymentMethod() == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
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

        // tránh sai số double: dùng String để tạo BigDecimal
        return new BigDecimal(String.valueOf(v)).setScale(2, RoundingMode.HALF_UP);
    }
}