package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.request.StaffOrderSearchRequest;
import com.swp391.eyewear_management_backend.dto.response.ApiResponse;
import com.swp391.eyewear_management_backend.dto.response.StaffOrderDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.OrderStatusGroupResponse;
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
@RequestMapping(value = "/api/staff/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class StaffOrderController {

    private final StaffOrderService staffOrderService;

    //Hàm này dùng để show dữ liệu cho trang OrderList của SALES STAFF
    @GetMapping
//    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ApiResponse<List<StaffOrderListResponse>> getOrders() {
        List<StaffOrderListResponse> result = staffOrderService.getOrdersForStaff();
        return ApiResponse.<List<StaffOrderListResponse>>builder()
                .message("OK")
                .result(result)
                .build();
    }

    //Hàm này dùng để show dữ liệu và nhận các field để search/filter dữ liệu cho trang OrderList của SALES STAFF (CHƯA DÙNG)
    @PostMapping("/search")
//    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ApiResponse<Page<StaffOrderListResponse>> searchOrders(
            @RequestBody(required = false) @Valid StaffOrderSearchRequest request
    ) {
        StaffOrderSearchRequest safeRequest = request == null ? StaffOrderSearchRequest.builder().build() : request;

        Page<StaffOrderListResponse> result = staffOrderService.searchOrdersForStaff(safeRequest);

        return ApiResponse.<Page<StaffOrderListResponse>>builder()
                .message("OK")
                .result(result)
                .build();
    }

    //Hàm này dùng khi drop down show dữ liệu orderStatus cho trang OrderList của SALES STAFF
    @GetMapping("/status-options")
//    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ApiResponse<List<OrderStatusGroupResponse>> getStatusOptions() {
        List<OrderStatusGroupResponse> result = staffOrderService.getSalesStaffOrderStatuses();
        return ApiResponse.<List<OrderStatusGroupResponse>>builder()
                .message("OK")
                .result(result)
                .build();
    }

    //Hàm này dùng để show dữ liệu cho trang OrderDetail của SALES STAFF, nhận vào orderId
    @GetMapping("/{orderId}")
    public ApiResponse<StaffOrderDetailResponse> getOrderDetail(@PathVariable Long orderId) {
        StaffOrderDetailResponse result = staffOrderService.getOrderDetailForSalesStaff(orderId);
        return ApiResponse.<StaffOrderDetailResponse>builder()
                .message("OK")
                .result(result)
                .build();
    }

    @PutMapping("/{orderId}/confirm")
    public ApiResponse<StaffOrderDetailResponse> confirmOrder(@PathVariable Long orderId) {
        StaffOrderDetailResponse result = staffOrderService.confirmOrderForSalesStaff(orderId);
        return ApiResponse.<StaffOrderDetailResponse>builder()
                .message("OK")
                .result(result)
                .build();
    }
}
