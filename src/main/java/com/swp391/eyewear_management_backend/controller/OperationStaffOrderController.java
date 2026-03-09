package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.request.StaffOrderSearchRequest;
import com.swp391.eyewear_management_backend.dto.response.ApiResponse;
import com.swp391.eyewear_management_backend.dto.response.OrderStatusGroupResponse;
import com.swp391.eyewear_management_backend.dto.response.StaffOrderDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.StaffOrderListResponse;
import com.swp391.eyewear_management_backend.service.StaffOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/operation-staff/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OperationStaffOrderController {

    private final StaffOrderService staffOrderService;

    @PostMapping("/search")
//    @PreAuthorize("hasAnyAuthority('ROLE_OPERATIONS STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ApiResponse<Page<StaffOrderListResponse>> searchOrders(
            @RequestBody(required = false) @Valid StaffOrderSearchRequest request
    ) {
        StaffOrderSearchRequest safeRequest = request == null ? StaffOrderSearchRequest.builder().build() : request;
        Page<StaffOrderListResponse> result = staffOrderService.searchOrdersForOperationStaff(safeRequest);

        return ApiResponse.<Page<StaffOrderListResponse>>builder()
                .message("OK")
                .result(result)
                .build();
    }

    @GetMapping("/status-options")
//    @PreAuthorize("hasAnyAuthority('ROLE_OPERATIONS STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ApiResponse<List<OrderStatusGroupResponse>> getStatusOptions() {
        List<OrderStatusGroupResponse> result = staffOrderService.getOperationStaffOrderStatuses();
        return ApiResponse.<List<OrderStatusGroupResponse>>builder()
                .message("OK")
                .result(result)
                .build();
    }

    @GetMapping("/{orderId}")
    public ApiResponse<StaffOrderDetailResponse> getOrderDetail(@PathVariable Long orderId) {
        StaffOrderDetailResponse result = staffOrderService.getOrderDetailForOperationStaff(orderId);
        return ApiResponse.<StaffOrderDetailResponse>builder()
                .message("OK")
                .result(result)
                .build();
    }
}
