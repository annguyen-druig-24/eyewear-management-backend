package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.config.OrderConstants;
import com.swp391.eyewear_management_backend.config.ghn.GhnProperties;
import com.swp391.eyewear_management_backend.dto.request.StaffOrderSearchRequest;
import com.swp391.eyewear_management_backend.dto.response.*;
import com.swp391.eyewear_management_backend.entity.*;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import com.swp391.eyewear_management_backend.integration.ghn.GhnShippingClient;
import com.swp391.eyewear_management_backend.mapper.ReturnExchangeMapper;
import com.swp391.eyewear_management_backend.mapper.StaffOrderMapper;
import com.swp391.eyewear_management_backend.repository.OrderDetailRepo;
import com.swp391.eyewear_management_backend.repository.OrderRepo;
import com.swp391.eyewear_management_backend.repository.PrescriptionOrderRepo;
import com.swp391.eyewear_management_backend.repository.ReturnExchangeRepo;
import com.swp391.eyewear_management_backend.service.StaffOrderService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffOrderServiceImpl implements StaffOrderService {
    private static final ZoneId APP_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OrderRepo orderRepo;
    private final OrderDetailRepo orderDetailRepo;
    private final PrescriptionOrderRepo prescriptionOrderRepo;
    private final StaffOrderMapper staffOrderMapper;
    private final ReturnExchangeRepo returnExchangeRepo;
    private final ReturnExchangeMapper returnExchangeMapper;
    private final GhnShippingClient ghnShippingClient;
    private final GhnProperties ghnProperties;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "orderDate", "orderCode", "orderType", "orderStatus", "totalAmount"
    );
    private static final Set<String> SALES_ALLOWED_ORDER_TYPES = Set.of(
            OrderConstants.ORDER_TYPE_DIRECT,
            OrderConstants.ORDER_TYPE_PRE,
            OrderConstants.ORDER_TYPE_PRESCRIPTION,
            OrderConstants.ORDER_TYPE_MIX
    );
    private static final Set<String> OPERATION_ALLOWED_ORDER_TYPES = Set.of(
            OrderConstants.ORDER_TYPE_PRESCRIPTION,
            OrderConstants.ORDER_TYPE_MIX
    );
    private static final Set<String> SALES_ALLOWED_ORDER_STATUSES = Set.of(
            OrderConstants.ORDER_STATUS_PENDING,
            OrderConstants.ORDER_STATUS_CONFIRMED,
            OrderConstants.ORDER_STATUS_PARTIALLY_PAID,
            OrderConstants.ORDER_STATUS_PAID,
            OrderConstants.ORDER_STATUS_PROCESSING,
            OrderConstants.ORDER_STATUS_READY,
            OrderConstants.ORDER_STATUS_COMPLETED,
            OrderConstants.ORDER_STATUS_CANCELED
    );
    private static final Set<String> OPERATION_ALLOWED_ORDER_STATUSES = Set.of(
            OrderConstants.ORDER_STATUS_CONFIRMED,
            OrderConstants.ORDER_STATUS_PROCESSING,
            OrderConstants.ORDER_STATUS_READY,
            OrderConstants.ORDER_STATUS_COMPLETED,
            OrderConstants.ORDER_STATUS_CANCELED
    );
    private static final Set<String> SALES_CONFIRMABLE_STATUSES = Set.of(
            OrderConstants.ORDER_STATUS_PENDING,
            OrderConstants.ORDER_STATUS_PARTIALLY_PAID,
            OrderConstants.ORDER_STATUS_PAID
    );

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public List<StaffOrderListResponse> getOrdersForStaff() {
        StaffOrderSearchRequest request = StaffOrderSearchRequest.builder().build();
        Specification<Order> specification = buildSpecification(request, false);
        List<Order> orders = orderRepo.findAll(specification, Sort.by("orderDate").descending());
        return orders.stream().map(staffOrderMapper::toStaffOrderListResponse).toList();
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public List<StaffOrderListResponse> getReturnExchangeOrders() {
        List<Order> orders = orderRepo.findAllOrdersWithReturnExchange();
        orders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()));

        List<StaffOrderListResponse> responses = new ArrayList<>();
        for (Order entityOrder : orders) {
            StaffOrderListResponse response = staffOrderMapper.toStaffOrderListResponse(entityOrder);
            ReturnExchange returnExchange = entityOrder.getOrderDetails().stream()
                    .map(OrderDetail::getReturnExchange)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            if (returnExchange != null) {
                response.setReturnExchangeId(returnExchange.getReturnExchangeID());
                response.setReturnType(resolveReturnType(returnExchange));
            }
            responses.add(response);
        }
        return responses;
    }

    private String resolveReturnType(ReturnExchange returnExchange) {
        if (returnExchange.getRefundAmount() != null || StringUtils.hasText(returnExchange.getRefundMethod())) {
            return "RETURN";
        }
        return "EXCHANGE";
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public Page<StaffOrderListResponse> searchOrdersForStaff(StaffOrderSearchRequest request) {
        Pageable pageable = buildPageable(request);
        Specification<Order> specification = buildSpecification(request, false);

        Page<Order> orderPage = orderRepo.findAll(specification, pageable);
        return orderPage.map(staffOrderMapper::toStaffOrderListResponse);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_OPERATIONS STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public Page<StaffOrderListResponse> searchOrdersForOperationStaff(StaffOrderSearchRequest request) {
        Pageable pageable = buildPageable(request);
        Specification<Order> specification = buildSpecification(request, true);

        Page<Order> orderPage = orderRepo.findAll(specification, pageable);
        return orderPage.map(staffOrderMapper::toStaffOrderListResponse);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public StaffOrderDetailResponse getOrderDetailForSalesStaff(Long orderId) {
        return getOrderDetailForSalesStaffInternal(orderId);
    }

    private StaffOrderDetailResponse getOrderDetailForSalesStaffInternal(Long orderId) {
        Order order = orderRepo.findByIdFetchStatus(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return buildOrderDetailResponse(order, false);
    }

    //Hàm này dùng để cập nhật Order_Status cho trang OrderDetail của SALES STAFF
    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public StaffOrderDetailResponse confirmOrderForSalesStaff(Long orderId) {
        Order order = orderRepo.findByIdFetchStatus(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        String currentStatus = order.getOrderStatus() == null
                ? ""
                : order.getOrderStatus().trim().toUpperCase(Locale.ROOT);
        if (!SALES_CONFIRMABLE_STATUSES.contains(currentStatus)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        order.setOrderStatus(OrderConstants.ORDER_STATUS_CONFIRMED);
        return buildOrderDetailResponse(order, false);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_OPERATIONS STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public StaffOrderDetailResponse getOrderDetailForOperationStaff(Long orderId) {
        Order order = orderRepo.findByIdFetchStatus(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return buildOrderDetailResponse(order, true);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_OPERATIONS STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public StaffOrderDetailResponse updateOrderForOperationStaff(Long orderId, String action) {
        Order order = orderRepo.findByIdFetchStatus(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        ShippingInfo shippingInfo = requireShippingInfo(order);
        boolean hasPrescriptionItem = hasPrescriptionItem(orderId);
        boolean requiresFinalPayment = isRequiresFinalPayment(order);

        String orderStatus = normalize(order.getOrderStatus());
        String shippingStatus = normalize(shippingInfo.getShippingStatus());
        if (isReadOnlyStatus(orderStatus, shippingStatus)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String normalizedAction = normalize(action);
        switch (normalizedAction) {
            case OrderConstants.OPERATION_ACTION_START_PROCESSING -> {
                if (!isStatus(orderStatus, OrderConstants.ORDER_STATUS_CONFIRMED) || !hasPrescriptionItem) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                order.setOrderStatus(OrderConstants.ORDER_STATUS_PROCESSING);   //Chỉ cập nhật Order_Status = PROCESSING khi Order_Status = CONFIRMED và hasPrescription = true
            }
            case OrderConstants.OPERATION_ACTION_START_PACKING -> { //dành cho đơn thường
                if (!isStatus(orderStatus, OrderConstants.ORDER_STATUS_CONFIRMED)
                        || hasPrescriptionItem
                        || !isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_PENDING)) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                shippingInfo.setShippingStatus(OrderConstants.SHIPPING_STATUS_PACKING); //Chỉ cập nhật Shipping_Status = PACKING khi Order_Status = CONFIRMED và hasPrescription = false và Shipping_Status = PENDING
            }
            case OrderConstants.OPERATION_ACTION_MOVE_TO_PACKING -> {   //dành cho đơn Prescription
                if (!isStatus(orderStatus, OrderConstants.ORDER_STATUS_PROCESSING)
                        || !hasPrescriptionItem
                        || !isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_PENDING)) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                shippingInfo.setShippingStatus(OrderConstants.SHIPPING_STATUS_PACKING); //Chỉ cập nhật Shipping_Status = PACKING khi Order_Status = PROCESSING và hasPrescription = true
            }
            case OrderConstants.OPERATION_ACTION_HANDOVER_TO_GHN -> {   //Chuyển giao cho bên GHN
                if (!isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_PACKING)) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                order.setOrderStatus(OrderConstants.ORDER_STATUS_READY);    //Chỉ cập nhật Order_Status = READY khi Shipping_Status = PACKING
                shippingInfo.setShippingStatus(OrderConstants.SHIPPING_STATUS_SHIPPING);    //Chỉ cập nhật Shipping_Status = SHIPPING khi Shipping_Status = PACKING
            }
            case OrderConstants.OPERATION_ACTION_MARK_DELIVERED -> {
                if (!isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_SHIPPING)) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                validateReachedExpectedDeliveryAt(shippingInfo);
                shippingInfo.setShippingStatus(OrderConstants.SHIPPING_STATUS_DELIVERED);
                settlePaymentsAndInvoiceOnDelivered(order);
            }
            case OrderConstants.OPERATION_ACTION_MARK_FAILED -> {
                if (!isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_SHIPPING)) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                shippingInfo.setShippingStatus(OrderConstants.SHIPPING_STATUS_FAILED);
            }
            case OrderConstants.OPERATION_ACTION_MARK_RETURNED -> {
                if (!isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_SHIPPING)
                        && !isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_DELIVERED)) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                shippingInfo.setShippingStatus(OrderConstants.SHIPPING_STATUS_RETURNED);
            }
            case OrderConstants.OPERATION_ACTION_COMPLETE_ORDER -> {    //Dành cho đơn trả full
                if (!isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_DELIVERED)
                ) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }
                validateReachedExpectedDeliveryAt(shippingInfo);
                settlePaymentsAndInvoiceOnDelivered(order);
                order.setOrderStatus(OrderConstants.ORDER_STATUS_COMPLETED);    //Chỉ cập nhật Order_Status = COMPLETED khi Shipping_Status = DELIVERED và Order_Status != PARTIALLY_PAID
            }
            default -> throw new AppException(ErrorCode.INVALID_REQUEST);   //Các case khác
        }

        return buildOrderDetailResponse(order, true);
    }

    private StaffOrderDetailResponse buildOrderDetailResponse(Order order, boolean operationStaffView) {
        Long orderId = order.getOrderID();
        ShippingInfo shippingInfo = order.getShippingInfo();
        String shippingStatus = shippingInfo != null ? shippingInfo.getShippingStatus() : null;
        BigDecimal shippingFee = shippingInfo != null ? shippingInfo.getShippingFee() : null;
        LocalDateTime expectedDeliveryAt = shippingInfo != null ? shippingInfo.getExpectedDeliveryAt() : null;
        Boolean isPastExpectedDeliveryAt = expectedDeliveryAt != null && LocalDateTime.now(APP_ZONE_ID).isAfter(expectedDeliveryAt);

        List<StaffOrderItemResponse> orderItems = orderDetailRepo.findByOrderIdFetchProduct(orderId).stream()
                .map(this::toOrderItemResponse)
                .toList();

        List<StaffPrescriptionOrderItemResponse> prescriptionItems = mapPrescriptionItems(orderId);
        boolean hasPrescriptionItem = !prescriptionItems.isEmpty();
        boolean requiresFinalPayment = isRequiresFinalPayment(order);
        List<String> availableActions = operationStaffView
                ? resolveOperationActions(order.getOrderStatus(), shippingStatus, hasPrescriptionItem, requiresFinalPayment)
                : resolveSalesActions(order.getOrderStatus());

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
                .availableActions(availableActions)
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

    private List<String> resolveSalesActions(String orderStatus) {
        String currentStatus = orderStatus == null ? "" : orderStatus.trim().toUpperCase(Locale.ROOT);
        if (SALES_CONFIRMABLE_STATUSES.contains(currentStatus)) {
            return List.of("CONFIRM_ORDER");
        }
        return List.of();
    }

    private List<String> resolveOperationActions(String orderStatus,
                                                 String shippingStatus,
                                                 boolean hasPrescriptionItem,
                                                 boolean requiresFinalPayment) {
        if (isReadOnlyStatus(orderStatus, shippingStatus)) {
            return List.of();
        }

        List<String> actions = new ArrayList<>();
        if (isStatus(orderStatus, OrderConstants.ORDER_STATUS_CONFIRMED)) {
            if (hasPrescriptionItem) {
                actions.add(OrderConstants.OPERATION_ACTION_START_PROCESSING);
            } else if (isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_PENDING)) {
                actions.add(OrderConstants.OPERATION_ACTION_START_PACKING);
            }
        }

        if (isStatus(orderStatus, OrderConstants.ORDER_STATUS_PROCESSING)
                && hasPrescriptionItem
                && isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_PENDING)) {
            actions.add(OrderConstants.OPERATION_ACTION_MOVE_TO_PACKING);
        }

        if (isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_PACKING)) {
            actions.add(OrderConstants.OPERATION_ACTION_HANDOVER_TO_GHN);
        }

        if (isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_SHIPPING)) {
            actions.add(OrderConstants.OPERATION_ACTION_MARK_DELIVERED);
            actions.add(OrderConstants.OPERATION_ACTION_MARK_FAILED);
            actions.add(OrderConstants.OPERATION_ACTION_MARK_RETURNED);
        }

        if (isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_DELIVERED)) {
            actions.add(OrderConstants.OPERATION_ACTION_COMPLETE_ORDER);
        }

        return actions;
    }

    private ShippingInfo requireShippingInfo(Order order) {
        ShippingInfo shippingInfo = order.getShippingInfo();
        if (shippingInfo == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return shippingInfo;
    }

    private boolean hasPrescriptionItem(Long orderId) {
        return prescriptionOrderRepo.findByOrder_OrderID(orderId)
                .map(po -> po.getPrescriptionOrderDetails() != null && !po.getPrescriptionOrderDetails().isEmpty())
                .orElse(false);
    }

    //Hàm này kiểm tra xem Invoice.Status = PARTIALLY_PAID hoặc Order.Order_Status = PARTIALLY_PAID --> Thỏa trả về true
    private boolean isRequiresFinalPayment(Order order) {
        if (order == null) {
            return false;
        }
        Invoice invoice = order.getInvoice();
        if (invoice != null && isStatus(invoice.getStatus(), OrderConstants.INVOICE_STATUS_PARTIALLY_PAID)) {
            return true;
        }
        return isStatus(order.getOrderStatus(), OrderConstants.ORDER_STATUS_PARTIALLY_PAID);
    }

    //Hàm này cập kiểm tra và cập nhật Payment.Status = SUCCESS khi remainingPayment != null và Payment.Status = PENDING
    private void markRemainingPaymentSuccess(Order order) {
        Payment remainingPayment = findPayment(order, OrderConstants.PAYMENT_PURPOSE_REMAINING);
        if (remainingPayment == null || !isStatus(remainingPayment.getStatus(), OrderConstants.PAYMENT_STATUS_PENDING)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        remainingPayment.setStatus(OrderConstants.PAYMENT_STATUS_SUCCESS);
        remainingPayment.setPaymentDate(LocalDateTime.now(APP_ZONE_ID));
    }

    //Hàm này kiểm tra và cập nhật Payment.Status = SUCCESS khi Payment.Payment_Purpose = FULL và Payment.Status = PENDING
    private void markFullCodPaymentSuccessIfPending(Order order) {
        Payment fullPayment = findPayment(order, OrderConstants.PAYMENT_PURPOSE_FULL);
        if (fullPayment == null) {
            return;
        }
        if (!isStatus(fullPayment.getPaymentMethod(), OrderConstants.PAYMENT_METHOD_COD)) {
            return;
        }
        if (isStatus(fullPayment.getStatus(), OrderConstants.PAYMENT_STATUS_PENDING)) {
            fullPayment.setStatus(OrderConstants.PAYMENT_STATUS_SUCCESS);
            fullPayment.setPaymentDate(LocalDateTime.now(APP_ZONE_ID));
        }
    }

    private Payment findPayment(Order order, String paymentPurpose) {
        if (order == null || order.getPayments() == null || order.getPayments().isEmpty()) {
            return null;
        }
        return order.getPayments().stream()
                .filter(Objects::nonNull)
                .filter(p -> isStatus(p.getPaymentPurpose(), paymentPurpose))
                .findFirst()
                .orElse(null);
    }

//    private void markInvoicePaidIfExists(Order order) {
//        if (order != null && order.getInvoice() != null) {
//            order.getInvoice().setStatus(OrderConstants.INVOICE_STATUS_PAID);
//        }
//    }

    /*
        Hàm này dùng để:
        - Kiểm tra dữ liệu giao hàng
        - Chọn service GHN
        - build request body
        - gọi API tạo đơn GHN
        - kiểm tra response có thành công không
     */
//    private void createGhnOrder(Order order, ShippingInfo shippingInfo) {
//        Integer toDistrictId = shippingInfo.getDistrictCode();
//        String toWardCode = shippingInfo.getWardCode();
//        String toAddress = shippingInfo.getRecipientAddress();
//        String toName = shippingInfo.getRecipientName();
//        String toPhone = shippingInfo.getRecipientPhone();
//        if (toDistrictId == null || !StringUtils.hasText(toWardCode)
//                || !StringUtils.hasText(toAddress)
//                || !StringUtils.hasText(toName)
//                || !StringUtils.hasText(toPhone)) {
//            throw new AppException(ErrorCode.INVALID_REQUEST);
//        }
//
//        int serviceId = chooseServiceId(toDistrictId);
//        System.out.println("GHN serviceId = " + serviceId);
//        Map<String, Object> body = new HashMap<>();
//        body.put("to_name", toName);
//        body.put("to_phone", toPhone);
//        body.put("to_address", toAddress);
//        body.put("to_ward_code", toWardCode);
//        body.put("to_district_id", toDistrictId);
//        body.put("service_id", serviceId);
//        body.put("payment_type_id", ghnProperties.getPaymentTypeId());
//        int codAmount = calculateCodAmountForGhn(order);
//        validateCodAmount(codAmount);
//        body.put("required_note", resolveRequiredNote(codAmount));
//        body.put("cod_amount", codAmount);
//        body.put("content", order.getOrderCode());
//        body.put("client_order_code", order.getOrderCode());
//        body.put("length", ghnProperties.getPkg().getLength());
//        body.put("width", ghnProperties.getPkg().getWidth());
//        body.put("height", ghnProperties.getPkg().getHeight());
//        body.put("weight", Math.max(ghnProperties.getPkg().getWeight().getDef(), 1));
//        body.put("items", buildGhnItems(order));
//
//        Map<String, Object> response;
//        try {
//            response = ghnShippingClient.createOrder(body);
//        } catch (HttpClientErrorException ex) {
//            throw new AppException(ErrorCode.GHN_CREATE_ORDER_FAILED, formatGhnHttpError(ex));
//        }
//        Object code = response != null ? response.get("code") : null;
//        if (code == null || !"200".equals(String.valueOf(code))) {
//            throw new AppException(ErrorCode.GHN_CREATE_ORDER_FAILED, formatGhnUnexpectedResponse(response));
//        }
//        System.out.println("GHN baseUrl = " + ghnProperties.getBaseUrl());
//        System.out.println("GHN shopId = " + ghnProperties.getShopId());
//        System.out.println("GHN token = " + ghnProperties.getToken());
//        System.out.println("GHN toDistrictId = " + toDistrictId);
//        System.out.println("GHN toWardCode = " + toWardCode);
//    }

//    private void syncFromGhn(Order order, ShippingInfo shippingInfo) {
//        Map<String, Object> body = new HashMap<>();
//        body.put("client_order_code", order.getOrderCode());
//        Map<String, Object> response;
//        try {
//            response = ghnShippingClient.detailByClientCode(body);
//        } catch (HttpClientErrorException ex) {
//            throw new AppException(ErrorCode.GHN_SYNC_FAILED, formatGhnHttpError(ex));
//        }
//        Map<String, Object> data = response == null ? null : (Map<String, Object>) response.get("data");
//        String ghnStatus = extractGhnStatus(data);
//        String mappedShippingStatus = mapGhnStatusToShippingStatus(ghnStatus);
//        if (!StringUtils.hasText(mappedShippingStatus)) {
//            throw new AppException(ErrorCode.GHN_SYNC_FAILED, "GHN response missing/unknown status");
//        }
//        shippingInfo.setShippingStatus(mappedShippingStatus);
//        applyGhnShippingResult(order, mappedShippingStatus);
//    }

//    private void applyGhnShippingResult(Order order, String shippingStatus) {
//        if (isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_SHIPPING)) {
//            if (!isStatus(order.getOrderStatus(), OrderConstants.ORDER_STATUS_COMPLETED)
//                    && !isStatus(order.getOrderStatus(), OrderConstants.ORDER_STATUS_CANCELED)) {
//                order.setOrderStatus(OrderConstants.ORDER_STATUS_READY);
//            }
//            return;
//        }
//        if (isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_DELIVERED)) {
//            boolean requiresFinalPayment = isRequiresFinalPayment(order);
//            if (requiresFinalPayment) {
//                markRemainingPaymentSuccess(order);
//                order.setOrderStatus(OrderConstants.ORDER_STATUS_PAID);
//            } else {
//                markFullCodPaymentSuccessIfPending(order);
//            }
//            markInvoicePaidIfExists(order);
//            order.setOrderStatus(OrderConstants.ORDER_STATUS_COMPLETED);
//            return;
//        }
//        if (isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_FAILED)
//                || isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_RETURNED)
//                || isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_CANCELED)) {
//            if (!isStatus(order.getOrderStatus(), OrderConstants.ORDER_STATUS_COMPLETED)) {
//                order.setOrderStatus(OrderConstants.ORDER_STATUS_CANCELED);
//            }
//        }
//    }
//
//    private String extractGhnStatus(Map<String, Object> data) {
//        if (data == null || data.isEmpty()) {
//            return null;
//        }
//        Object directStatus = data.get("status");
//        if (directStatus != null) {
//            return String.valueOf(directStatus);
//        }
//        Object statusName = data.get("status_name");
//        if (statusName != null) {
//            return String.valueOf(statusName);
//        }
//        Object currentStatus = data.get("current_status");
//        if (currentStatus != null) {
//            return String.valueOf(currentStatus);
//        }
//        return null;
//    }
//
//    private String mapGhnStatusToShippingStatus(String ghnStatus) {
//        if (!StringUtils.hasText(ghnStatus)) {
//            return null;
//        }
//        String normalized = ghnStatus.trim().toLowerCase(Locale.ROOT);
//        if (isAnyOf(normalized, "delivered")) {
//            return OrderConstants.SHIPPING_STATUS_DELIVERED;
//        }
//        if (isAnyOf(normalized, "return", "waiting_to_return", "return_transporting", "return_sorting")) {
//            return OrderConstants.SHIPPING_STATUS_RETURNED;
//        }
//        if (isAnyOf(normalized, "cancel")) {
//            return OrderConstants.SHIPPING_STATUS_CANCELED;
//        }
//        if (isAnyOf(normalized, "delivery_fail", "exception", "damage", "lost")) {
//            return OrderConstants.SHIPPING_STATUS_FAILED;
//        }
//        if (isAnyOf(normalized,
//                "ready_to_pick", "picking", "money_collect_picking",
//                "picked", "storing", "transporting", "sorting",
//                "delivering", "money_collect_delivering")) {
//            return OrderConstants.SHIPPING_STATUS_SHIPPING;
//        }
//        return null;
//    }
//
//    private int chooseServiceId(int toDistrictId) {
//        List<Map<String, Object>> services;
//        try {
//            services = ghnShippingClient.availableServices(toDistrictId);
//        } catch (HttpClientErrorException ex) {
//            throw new AppException(ErrorCode.GHN_CREATE_ORDER_FAILED, formatGhnHttpError(ex));
//        }
//        if (services == null || services.isEmpty()) {
//            throw new AppException(ErrorCode.INVALID_REQUEST);
//        }
//        Integer preferredServiceTypeId = ghnProperties.getPreferredServiceTypeId();
//        for (Map<String, Object> service : services) {
//            Number typeId = (Number) service.get("service_type_id");
//            if (typeId != null && preferredServiceTypeId != null && typeId.intValue() == preferredServiceTypeId) {
//                return ((Number) service.get("service_id")).intValue();
//            }
//        }
//        return ((Number) services.get(0).get("service_id")).intValue();
//    }

//    private String formatGhnHttpError(HttpClientErrorException ex) {
//        if (ex == null) {
//            return "GHN request failed";
//        }
//        String body = ex.getResponseBodyAsString();
//        if (!StringUtils.hasText(body)) {
//            return "GHN " + ex.getStatusCode().value() + " " + ex.getStatusText();
//        }
//        String normalized = body.replace("\n", " ").replace("\r", " ").trim();
//        if (normalized.length() > 500) {
//            normalized = normalized.substring(0, 500);
//        }
//        return "GHN " + ex.getStatusCode().value() + ": " + normalized;
//    }
//
//    private String formatGhnUnexpectedResponse(Map<String, Object> response) {
//        if (response == null) {
//            return "GHN response is null";
//        }
//        Object code = response.get("code");
//        Object message = response.get("message");
//        return "GHN unexpected response (code=" + code + ", message=" + message + ")";
//    }
//
//    private List<Map<String, Object>> buildGhnItems(Order order) {
//        List<Map<String, Object>> items = new ArrayList<>();
//        if (order == null || order.getOrderDetails() == null) {
//            return items;
//        }
//        for (OrderDetail detail : order.getOrderDetails()) {
//            Map<String, Object> item = new HashMap<>();
//            Product product = detail.getProduct();
//            item.put("name", product != null ? product.getProductName() : "ITEM");
//            item.put("quantity", detail.getQuantity() == null ? 1 : Math.max(detail.getQuantity(), 1));
//            item.put("price", detail.getUnitPrice() == null ? 0 : detail.getUnitPrice().intValue());
//            item.put("length", ghnProperties.getPkg().getLength());
//            item.put("width", ghnProperties.getPkg().getWidth());
//            item.put("height", ghnProperties.getPkg().getHeight());
//            item.put("weight", Math.max(ghnProperties.getPkg().getWeight().getDef(), 1));
//            items.add(item);
//        }
//        if (items.isEmpty()) {
//            Map<String, Object> item = new HashMap<>();
//            item.put("name", order.getOrderCode());
//            item.put("quantity", 1);
//            item.put("price", 0);
//            item.put("length", ghnProperties.getPkg().getLength());
//            item.put("width", ghnProperties.getPkg().getWidth());
//            item.put("height", ghnProperties.getPkg().getHeight());
//            item.put("weight", Math.max(ghnProperties.getPkg().getWeight().getDef(), 1));
//            items.add(item);
//        }
//        return items;
//    }
//
//    private int calculateCodAmountForGhn(Order order) {
//        if (order == null || order.getPayments() == null || order.getPayments().isEmpty()) {
//            return 0;
//        }
//        BigDecimal totalCodPending = order.getPayments().stream()
//                .filter(Objects::nonNull)
//                .filter(p -> isStatus(p.getPaymentMethod(), OrderConstants.PAYMENT_METHOD_COD))
//                .filter(p -> isStatus(p.getStatus(), OrderConstants.PAYMENT_STATUS_PENDING))
//                .map(Payment::getAmount)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//        if (totalCodPending.compareTo(BigDecimal.ZERO) <= 0) {
//            return 0;
//        }
//        return totalCodPending.intValue();
//    }
//
//    private String resolveRequiredNote(int codAmount) {
//        String configured = ghnProperties.getRequiredNote();
//        if (StringUtils.hasText(configured)) {
//            String normalized = configured.trim().toUpperCase(Locale.ROOT);
//            if (isAnyOf(normalized, "CHOTHUHANG", "CHOXEMHANGKHONGTHU", "KHONGCHOXEMHANG")) {
//                return normalized;
//            }
//        }
//        if (codAmount > 0) {
//            return "CHOTHUHANG";
//        }
//        return "KHONGCHOXEMHANG";
//    }
//
//    private void validateCodAmount(int codAmount) {
//        Integer codMaxAmount = ghnProperties.getCodMaxAmount();
//        int max = codMaxAmount == null || codMaxAmount <= 0 ? 300000 : codMaxAmount;
//        if (codAmount > max) {
//            throw new AppException(ErrorCode.GHN_COD_OVER_LIMIT);
//        }
//    }

//    private boolean isAnyOf(String value, String... candidates) {
//        if (!StringUtils.hasText(value) || candidates == null) {
//            return false;
//        }
//        for (String candidate : candidates) {
//            if (candidate != null && candidate.equalsIgnoreCase(value)) {
//                return true;
//            }
//        }
//        return false;
//    }

    private void validateReachedExpectedDeliveryAt(ShippingInfo shippingInfo) {
        if (shippingInfo == null) {
            return;
        }
        LocalDateTime expectedDeliveryAt = shippingInfo.getExpectedDeliveryAt();
        if (expectedDeliveryAt == null) {
            return;
        }
        LocalDate expectedDeliveryDate = expectedDeliveryAt.toLocalDate();
        LocalDate today = LocalDate.now(APP_ZONE_ID);
        if (today.isBefore(expectedDeliveryDate)) {
            throw new AppException(ErrorCode.ORDER_NOT_REACHED_EXPECTED_DELIVERY);
        }
    }

    private void settlePaymentsAndInvoiceOnDelivered(Order order) {
        if (order == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE_ID);

        if (order.getPayments() != null) {
            for (Payment payment : order.getPayments()) {
                if (payment == null) {
                    continue;
                }
                if (isStatus(payment.getStatus(), OrderConstants.PAYMENT_STATUS_PENDING)) {
                    payment.setStatus(OrderConstants.PAYMENT_STATUS_SUCCESS);
                    payment.setPaymentDate(now);
                }
            }
        }

        Invoice invoice = order.getInvoice();
        if (invoice != null) {
            String invoiceStatus = normalize(invoice.getStatus());
            if (isStatus(invoiceStatus, OrderConstants.INVOICE_STATUS_UNPAID)
                    || isStatus(invoiceStatus, OrderConstants.INVOICE_STATUS_PARTIALLY_PAID)) {
                invoice.setStatus(OrderConstants.INVOICE_STATUS_PAID);
            }
        }
    }

    private boolean isReadOnlyStatus(String orderStatus, String shippingStatus) {
        return isStatus(orderStatus, OrderConstants.ORDER_STATUS_CANCELED)
                || isStatus(orderStatus, OrderConstants.ORDER_STATUS_COMPLETED)
                || isStatus(shippingStatus, OrderConstants.SHIPPING_STATUS_CANCELED);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isStatus(String value, String expected) {
        return expected != null && expected.equalsIgnoreCase(normalize(value));
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public List<OrderStatusGroupResponse> getSalesStaffOrderStatuses() {
        return List.of(
                OrderStatusGroupResponse.builder()
                        .groupName("ORDER WORKFLOW")
                        .orderTypes(List.of(
                                OrderConstants.ORDER_TYPE_DIRECT,
                                OrderConstants.ORDER_TYPE_PRE,
                                OrderConstants.ORDER_TYPE_PRESCRIPTION,
                                OrderConstants.ORDER_TYPE_MIX
                        ))
                        .statuses(List.of(
                                statusOption(OrderConstants.ORDER_STATUS_PENDING, "Đang chờ"),
                                statusOption(OrderConstants.ORDER_STATUS_CONFIRMED, "Đã xác nhận và đang chuẩn bị hàng"),
                                statusOption(OrderConstants.ORDER_STATUS_PARTIALLY_PAID, "Đã trả cọc 1 phần"),
                                statusOption(OrderConstants.ORDER_STATUS_PAID, "Đã trả"),
                                statusOption(OrderConstants.ORDER_STATUS_PROCESSING, "Đang gia công"),
                                statusOption(OrderConstants.ORDER_STATUS_READY, "Đã chuyển cho đơn vị vận chuyển"),
                                statusOption(OrderConstants.ORDER_STATUS_COMPLETED, "Hoàn thành"),
                                statusOption(OrderConstants.ORDER_STATUS_CANCELED, "Đã hủy")
                        ))
                        .build()
        );
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_OPERATIONS STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public List<OrderStatusGroupResponse> getOperationStaffOrderStatuses() {
        return List.of(
                OrderStatusGroupResponse.builder()
                        .groupName("PRESCRIPTION WORKFLOW")
                        .orderTypes(List.of(OrderConstants.ORDER_TYPE_PRESCRIPTION, OrderConstants.ORDER_TYPE_MIX))
                        .statuses(List.of(
                                statusOption(OrderConstants.ORDER_STATUS_CONFIRMED, "Đã xác nhận và đang chuẩn bị hàng"),
                                statusOption(OrderConstants.ORDER_STATUS_PROCESSING, "Đang gia công"),
                                statusOption(OrderConstants.ORDER_STATUS_READY, "Đã chuyển cho đơn vị vận chuyển"),
                                statusOption(OrderConstants.ORDER_STATUS_COMPLETED, "Hoàn thành"),
                                statusOption(OrderConstants.ORDER_STATUS_CANCELED, "Đã hủy")
                        ))
                        .build()
        );
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
                    rightAdd,
                    rightAxis,
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

    private String pickPrimaryImage(Product... products) {
        if (products == null) {
            return null;
        }
        for (Product product : products) {
            String image = pickPrimaryImage(product);
            if (image != null) {
                return image;
            }
        }
        return null;
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
                        .filter(StringUtils::hasText)
                        .findFirst()
                        .orElse(null));
    }

    private String bdToText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String bdToText(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private Pageable buildPageable(StaffOrderSearchRequest request) {
        int page = request.getPage() == null ? 0 : Math.max(request.getPage(), 0);
        int size = request.getSize() == null ? 10 : Math.min(Math.max(request.getSize(), 1), 100);

        String sortBy = request.getSortBy();
        if (!StringUtils.hasText(sortBy) || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "orderDate";
        }

        String sortDir = request.getSortDir();
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return PageRequest.of(page, size, sort);
    }

    private Specification<Order> buildSpecification(StaffOrderSearchRequest request, boolean operationStaffScope) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Set<String> allowedOrderTypes = operationStaffScope
                    ? OPERATION_ALLOWED_ORDER_TYPES
                    : SALES_ALLOWED_ORDER_TYPES;
            Set<String> allowedOrderStatuses = operationStaffScope
                    ? OPERATION_ALLOWED_ORDER_STATUSES
                    : SALES_ALLOWED_ORDER_STATUSES;

            if (StringUtils.hasText(request.getOrderCode())) {
                String keyword = "%" + request.getOrderCode().trim().toUpperCase() + "%";
                predicates.add(cb.like(cb.upper(root.get("orderCode")), keyword));
            }

            if (request.getOrderDate() != null) {
                LocalDateTime start = request.getOrderDate().atStartOfDay();
                LocalDateTime end = request.getOrderDate().plusDays(1).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), start));
                predicates.add(cb.lessThan(root.get("orderDate"), end));
            }

            if (StringUtils.hasText(request.getOrderType())) {
                String normalizedType = request.getOrderType().trim().toUpperCase();
                if (!allowedOrderTypes.contains(normalizedType)) {
                    return cb.disjunction();
                }
                predicates.add(cb.equal(cb.upper(root.get("orderType")), normalizedType));
            }

            boolean hasOrderStatusFilter = StringUtils.hasText(request.getOrderStatus());
            if (hasOrderStatusFilter) {
                String normalizedStatus = request.getOrderStatus().trim().toUpperCase(Locale.ROOT);
                if (!allowedOrderStatuses.contains(normalizedStatus)) {
                    return cb.disjunction();
                }
                predicates.add(cb.equal(cb.upper(root.get("orderStatus")), normalizedStatus));
            }

            /*
                * Giới hạn phạm vi dữ liệu được phép xem
                * Ví dụ role chỉ được thấy PRESCRIPTION, MIX thì dù không truyền filter, query vẫn chỉ trả về 2 loại này
                * Dòng 1 chặn theo orderType
                * Dòng 2 chặn theo orderStatus
                * equal(...): filter “người dùng yêu cầu gì thì lọc đúng cái đó”.
                * in(...): filter “hệ thống cho phép gì thì chỉ được thấy cái đó”.
             */
            predicates.add(cb.upper(root.get("orderType")).in(allowedOrderTypes));
            predicates.add(cb.upper(root.get("orderStatus")).in(allowedOrderStatuses));

            if (operationStaffScope) {
                predicates.add(buildHasPrescriptionPredicate(root, query, cb));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /*
        * Hàm này dùng để từ 1 dòng dữ liệu trong table Order và kiểm tra xem đơn hàng đó có bao gồm: PRESCRIPTION_ORDER hay không?
     */
    private Predicate buildHasPrescriptionPredicate(jakarta.persistence.criteria.Root<Order> root,
                                                    jakarta.persistence.criteria.CriteriaQuery<?> query,
                                                    jakarta.persistence.criteria.CriteriaBuilder cb) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<PrescriptionOrder> prescriptionRoot = subquery.from(PrescriptionOrder.class);
        subquery.select(prescriptionRoot.get("prescriptionOrderID"));
        subquery.where(cb.equal(prescriptionRoot.get("order"), root));
        return cb.exists(subquery);
    }

    private OrderStatusOptionResponse statusOption(String code, String displayName) {
        return OrderStatusOptionResponse.builder()
                .code(code)
                .displayName(displayName)
                .build();
    }

    private static class RxAggregate {
        private final StaffPrescriptionOrderItemResponse response;

        private RxAggregate(StaffPrescriptionOrderItemResponse response) {
            this.response = response;
        }
    }

    private record PrescriptionGroupKey(
            Long frameId,
            Long lensId,
            String rightEyeSph,
            String rightEyeCyl,
            String rightEyeAxis,
            String rightEyeAdd,
            String rightPD,
            String leftEyeSph,
            String leftEyeCyl,
            String leftEyeAxis,
            String leftEyeAdd,
            String leftPD,
            String lineSubTotal
    ) {
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public StaffReturnExchangeDetailResponse getReturnExchangeDetailForSalesStaff(Long returnExchangeId) {
        ReturnExchange returnExchange = returnExchangeRepo.findById(returnExchangeId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_EXCHANGE_NOT_FOUND));

        OrderDetail orderDetail = returnExchange.getOrderDetail();
        Long orderId = orderDetail != null && orderDetail.getOrder() != null
                ? orderDetail.getOrder().getOrderID()
                : null;
        if (orderId == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        StaffOrderDetailResponse orderDetailResponse = getOrderDetailForSalesStaff(orderId);

        return StaffReturnExchangeDetailResponse.builder()
                .orderId(orderDetailResponse.getOrderId())
                .orderCode(orderDetailResponse.getOrderCode())
                .orderStatus(orderDetailResponse.getOrderStatus())
                .orderType(orderDetailResponse.getOrderType())
                .orderDate(orderDetailResponse.getOrderDate())
                .totalAmount(orderDetailResponse.getTotalAmount())
                .shippingStatus(orderDetailResponse.getShippingStatus())
                .shippingFee(orderDetailResponse.getShippingFee())
                .expectedDeliveryAt(orderDetailResponse.getExpectedDeliveryAt())
                .isPastExpectedDeliveryAt(orderDetailResponse.getIsPastExpectedDeliveryAt())
                .hasPrescriptionItem(orderDetailResponse.getHasPrescriptionItem())
                .requiresFinalPayment(orderDetailResponse.getRequiresFinalPayment())
                .availableActions(orderDetailResponse.getAvailableActions())
                .customerName(orderDetailResponse.getCustomerName())
                .customerPhone(orderDetailResponse.getCustomerPhone())
                .customerEmail(orderDetailResponse.getCustomerEmail())
                .orderDetail(orderDetailResponse.getOrderDetail())
                .prescriptionOrderDetail(orderDetailResponse.getPrescriptionOrderDetail())
                .recipientName(orderDetailResponse.getRecipientName())
                .recipientPhone(orderDetailResponse.getRecipientPhone())
                .recipientEmail(orderDetailResponse.getRecipientEmail())
                .recipientAddress(orderDetailResponse.getRecipientAddress())
                .note(orderDetailResponse.getNote())
                .returnExchangeId(returnExchange.getReturnExchangeID())
                .returnOrderDetailId(orderDetail.getOrderDetailID())
                .returnCode(returnExchange.getReturnCode())
                .requestDate(returnExchange.getRequestDate())
                .returnExchangeStatus(returnExchange.getStatus())
                .returnQuantity(returnExchange.getQuantity())
                .returnReason(returnExchange.getReturnReason())
                .returnImgUrl(returnExchange.getImageUrl())
                .productCondition(returnExchange.getProductCondition())
                .refundAmount(returnExchange.getRefundAmount())
                .refundMethod(returnExchange.getRefundMethod())
                .refundAccountNumber(returnExchange.getRefundAccountNumber())
                .approvedDate(returnExchange.getApprovedDate())
                .rejectReason(returnExchange.getRejectReason())
                .build();
    }

    @Override
    public ReturnExchangeResponse getReturnExchangeById(Long returnExchangeId) {
        ReturnExchange returnExchange = returnExchangeRepo.findById(returnExchangeId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_EXCHANGE_NOT_FOUND));
        return returnExchangeMapper.toReturnExchangeResponse(returnExchange);
    }
}
