package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.config.OrderConstants;
import com.swp391.eyewear_management_backend.dto.request.StaffOrderSearchRequest;
import com.swp391.eyewear_management_backend.dto.response.OrderStatusGroupResponse;
import com.swp391.eyewear_management_backend.dto.response.OrderStatusOptionResponse;
import com.swp391.eyewear_management_backend.dto.response.StaffOrderListResponse;
import com.swp391.eyewear_management_backend.entity.Order;
import com.swp391.eyewear_management_backend.mapper.StaffOrderMapper;
import com.swp391.eyewear_management_backend.repository.OrderRepo;
import com.swp391.eyewear_management_backend.service.StaffOrderService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffOrderServiceImpl implements StaffOrderService {

    private final OrderRepo orderRepo;
    private final StaffOrderMapper staffOrderMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "orderDate", "orderCode", "orderType", "orderStatus", "totalAmount"
    );

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public Page<StaffOrderListResponse> searchOrdersForStaff(StaffOrderSearchRequest request) {
        Pageable pageable = buildPageable(request);
        Specification<Order> specification = buildSpecification(request, false);

        Page<Order> orderPage = orderRepo.findAll(specification, pageable);
        return orderPage.map(staffOrderMapper::toStaffOrderListResponse);
    }

    @Override
    public Page<StaffOrderListResponse> searchOrdersForOperationStaff(StaffOrderSearchRequest request) {
        Pageable pageable = buildPageable(request);
        Specification<Order> specification = buildSpecification(request, true);

        Page<Order> orderPage = orderRepo.findAll(specification, pageable);
        return orderPage.map(staffOrderMapper::toStaffOrderListResponse);
    }

    @Override
    public List<OrderStatusGroupResponse> getSalesStaffOrderStatuses() {
        return List.of(
                OrderStatusGroupResponse.builder()
                        .groupName("DIRECT/PRE ORDER")
                        .orderTypes(List.of(OrderConstants.ORDER_TYPE_DIRECT, OrderConstants.ORDER_TYPE_PRE))
                        .statuses(List.of(
                                statusOption(OrderConstants.ORDER_STATUS_CONFIRMED, "Đang chuẩn bị hàng"),
                                statusOption(OrderConstants.ORDER_STATUS_COMPLETED, "Hoàn thành"),
                                statusOption(OrderConstants.ORDER_STATUS_CANCELED, "Đã hủy"),
                                statusOption(OrderConstants.ORDER_STATUS_READY, "Đã chuyển cho đơn vị vận chuyển")
                        ))
                        .build(),
                OrderStatusGroupResponse.builder()
                        .groupName("PRESCRIPTION ORDER")
                        .orderTypes(List.of(OrderConstants.ORDER_TYPE_PRESCRIPTION))
                        .statuses(List.of(
                                statusOption(OrderConstants.ORDER_STATUS_CONFIRMED, "Đang chuẩn bị hàng"),
                                statusOption(OrderConstants.ORDER_STATUS_PROCESSING, "Đang gia công"),
                                statusOption(OrderConstants.ORDER_STATUS_COMPLETED, "Hoàn thành"),
                                statusOption(OrderConstants.ORDER_STATUS_CANCELED, "Đã hủy")
                        ))
                        .build()
        );
    }

    @Override
    public List<OrderStatusGroupResponse> getOperationStaffOrderStatuses() {
        return List.of(
                OrderStatusGroupResponse.builder()
                        .groupName("PRESCRIPTION ORDER")
                        .orderTypes(List.of(OrderConstants.ORDER_TYPE_PRESCRIPTION))
                        .statuses(List.of(
                                statusOption(OrderConstants.ORDER_STATUS_PROCESSING, "Đang gia công"),
                                statusOption(OrderConstants.ORDER_STATUS_READY, "Đã chuyển cho đơn vị vận chuyển"),
                                statusOption(OrderConstants.ORDER_STATUS_COMPLETED, "Hoàn thành"),
                                statusOption(OrderConstants.ORDER_STATUS_CANCELED, "Đã hủy")
                        ))
                        .build()
        );
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
                predicates.add(cb.equal(cb.upper(root.get("orderType")), normalizedType));
            }

            if (StringUtils.hasText(request.getOrderStatus())) {
                String normalizedStatus = normalizeStatus(request.getOrderStatus());
                predicates.add(cb.equal(cb.upper(root.get("orderStatus")), normalizedStatus));
            }

            if (operationStaffScope) {
                predicates.add(cb.equal(cb.upper(root.get("orderType")), OrderConstants.ORDER_TYPE_PRESCRIPTION));
                predicates.add(cb.equal(cb.upper(root.get("orderStatus")), OrderConstants.ORDER_STATUS_PROCESSING));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String normalizeStatus(String input) {
        String normalized = input.trim().toUpperCase();
        if ("CANCELLED".equals(normalized)) {
            return OrderConstants.ORDER_STATUS_CANCELED;
        }
        String alias = normalizeAlias(normalized);
        if ("DANG GIA CONG".equals(alias)) {
            return OrderConstants.ORDER_STATUS_PROCESSING;
        }
        if ("DANG CHUAN BI HANG".equals(alias)) {
            return OrderConstants.ORDER_STATUS_CONFIRMED;
        }
        if ("DA CHUYEN CHO DON VI VAN CHUYEN".equals(alias)) {
            return OrderConstants.ORDER_STATUS_READY;
        }
        if ("HOAN THANH".equals(alias)) {
            return OrderConstants.ORDER_STATUS_COMPLETED;
        }
        if ("DA HUY".equals(alias)) {
            return OrderConstants.ORDER_STATUS_CANCELED;
        }
        return normalized;
    }

    private String normalizeAlias(String input) {
        String withoutAccent = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccent
                .replace('Đ', 'D')
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private OrderStatusOptionResponse statusOption(String code, String displayName) {
        return OrderStatusOptionResponse.builder()
                .code(code)
                .displayName(displayName)
                .build();
    }
}
