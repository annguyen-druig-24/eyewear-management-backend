package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.config.OrderConstants;
import com.swp391.eyewear_management_backend.dto.request.StaffOrderSearchRequest;
import com.swp391.eyewear_management_backend.dto.response.OrderStatusGroupResponse;
import com.swp391.eyewear_management_backend.dto.response.OrderStatusOptionResponse;
import com.swp391.eyewear_management_backend.dto.response.StaffOrderDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.StaffOrderItemResponse;
import com.swp391.eyewear_management_backend.dto.response.StaffOrderListResponse;
import com.swp391.eyewear_management_backend.dto.response.StaffPrescriptionOrderItemResponse;
import com.swp391.eyewear_management_backend.entity.*;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import com.swp391.eyewear_management_backend.mapper.StaffOrderMapper;
import com.swp391.eyewear_management_backend.repository.OrderDetailRepo;
import com.swp391.eyewear_management_backend.repository.OrderRepo;
import com.swp391.eyewear_management_backend.repository.PrescriptionOrderRepo;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private final OrderRepo orderRepo;
    private final OrderDetailRepo orderDetailRepo;
    private final PrescriptionOrderRepo prescriptionOrderRepo;
    private final StaffOrderMapper staffOrderMapper;

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

    private StaffOrderDetailResponse buildOrderDetailResponse(Order order, boolean operationStaffView) {
        Long orderId = order.getOrderID();
        ShippingInfo shippingInfo = order.getShippingInfo();
        String shippingStatus = shippingInfo != null ? shippingInfo.getShippingStatus() : null;

        List<StaffOrderItemResponse> orderItems = orderDetailRepo.findByOrderIdFetchProduct(orderId).stream()
                .map(this::toOrderItemResponse)
                .toList();

        List<StaffPrescriptionOrderItemResponse> prescriptionItems = mapPrescriptionItems(orderId);
        boolean hasPrescriptionItem = !prescriptionItems.isEmpty();
        List<String> availableActions = operationStaffView
                ? resolveOperationActions(order.getOrderStatus(), shippingStatus, hasPrescriptionItem)
                : resolveSalesActions(order.getOrderStatus());

        return StaffOrderDetailResponse.builder()
                .orderId(order.getOrderID())
                .orderCode(order.getOrderCode())
                .orderStatus(order.getOrderStatus())
                .orderType(order.getOrderType())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .shippingStatus(shippingStatus)
                .hasPrescriptionItem(hasPrescriptionItem)
                .requiresFinalPayment(isStatus(order.getOrderStatus(), OrderConstants.ORDER_STATUS_PARTIALLY_PAID))
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

    private List<String> resolveOperationActions(String orderStatus, String shippingStatus, boolean hasPrescriptionItem) {
        if (isStatus(orderStatus, OrderConstants.ORDER_STATUS_CANCELED)
                || isStatus(orderStatus, OrderConstants.ORDER_STATUS_COMPLETED)
                || isStatus(shippingStatus, OrderConstants.ORDER_STATUS_CANCELED)) {
            return List.of();
        }

        List<String> actions = new ArrayList<>();
        if (isStatus(orderStatus, OrderConstants.ORDER_STATUS_CONFIRMED)) {
            if (hasPrescriptionItem) {
                actions.add("START_PROCESSING");
            } else if (isStatus(shippingStatus, OrderConstants.ORDER_STATUS_PENDING)) {
                actions.add("START_PACKING");
            }
        }

        if (isStatus(orderStatus, OrderConstants.ORDER_STATUS_PROCESSING) && hasPrescriptionItem) {
            actions.add("MOVE_TO_PACKING");
        }

        if (isStatus(shippingStatus, "PACKING")) {
            actions.add("HANDOVER_TO_GHN");
        }

        if (isStatus(shippingStatus, "SHIPPING")) {
            actions.add("SYNC_GHN");
        }

        if (isStatus(shippingStatus, "DELIVERED")) {
            if (isStatus(orderStatus, OrderConstants.ORDER_STATUS_PARTIALLY_PAID)) {
                actions.add("CONFIRM_FULL_PAYMENT");
            }
            if (!isStatus(orderStatus, OrderConstants.ORDER_STATUS_PARTIALLY_PAID)) {
                actions.add("COMPLETE_ORDER");
            }
        }

        return actions;
    }

    private boolean isStatus(String value, String expected) {
        return value != null && expected != null && expected.equalsIgnoreCase(value);
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
                            .rightEyeAdd(leftAdd)
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
}
