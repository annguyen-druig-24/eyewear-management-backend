package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.request.ReturnExchangeDecisionRequest;
import com.swp391.eyewear_management_backend.dto.request.StaffCompleteRefundRequest;
import com.swp391.eyewear_management_backend.dto.request.StaffOrderSearchRequest;
import com.swp391.eyewear_management_backend.dto.response.*;
import com.swp391.eyewear_management_backend.service.StaffOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(value = "/api/staff", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class StaffOrderController {

    private final StaffOrderService staffOrderService;

    //Hàm này dùng để show dữ liệu cho trang OrderList của SALES STAFF
    @GetMapping("/orders")
//    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ApiResponse<List<StaffOrderListResponse>> getOrders() {
        List<StaffOrderListResponse> result = staffOrderService.getOrdersForStaff();
        return ApiResponse.<List<StaffOrderListResponse>>builder()
                .message("OK")
                .result(result)
                .build();
    }

    //Hàm này dùng để show dữ liệu và nhận các field để search/filter dữ liệu cho trang OrderList của SALES STAFF (CHƯA DÙNG)
    @PostMapping("/orders/search")
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
    @GetMapping("/orders/status-options")
//    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ApiResponse<List<OrderStatusGroupResponse>> getStatusOptions() {
        List<OrderStatusGroupResponse> result = staffOrderService.getSalesStaffOrderStatuses();
        return ApiResponse.<List<OrderStatusGroupResponse>>builder()
                .message("OK")
                .result(result)
                .build();
    }

    //Hàm này dùng để show dữ liệu cho trang OrderDetail của SALES STAFF, nhận vào orderId
    @GetMapping("/orders/{orderId}")
    public ApiResponse<StaffOrderDetailResponse> getOrderDetail(@PathVariable Long orderId) {
        StaffOrderDetailResponse result = staffOrderService.getOrderDetailForSalesStaff(orderId);
        return ApiResponse.<StaffOrderDetailResponse>builder()
                .message("OK")
                .result(result)
                .build();
    }

    @PutMapping("/orders/{orderId}/confirm")
    public ApiResponse<StaffOrderDetailResponse> confirmOrder(@PathVariable Long orderId) {
        StaffOrderDetailResponse result = staffOrderService.confirmOrderForSalesStaff(orderId);
        return ApiResponse.<StaffOrderDetailResponse>builder()
                .message("OK")
                .result(result)
                .build();
    }

    //Hàm này dùng để lấy danh sách đơn hàng có yêu cầu đổi trả cho SALES STAFF
    @GetMapping("/return-exchange")
//    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ApiResponse<List<StaffReturnExchangeListResponse>> getReturnExchangeOrders() {
        List<StaffReturnExchangeListResponse> result = staffOrderService.getReturnExchangeOrders();
        return ApiResponse.<List<StaffReturnExchangeListResponse>>builder()
                .message("OK")
                .result(result)
                .build();
    }

    /**
     * Lấy chi tiết yêu cầu đổi trả theo ID bao gồm cả thông tin của Order
     */
    @GetMapping("/return-exchange/{returnExchangeId}")
    public ResponseEntity<ApiResponse<StaffReturnExchangeDetailResponse>> getReturnExchange(
            @PathVariable Long returnExchangeId) {
        StaffReturnExchangeDetailResponse response = staffOrderService.getReturnExchangeDetailForSalesStaff(returnExchangeId);
        return ResponseEntity.ok(ApiResponse.<StaffReturnExchangeDetailResponse>builder()
                .code(1000)
                .message("Return exchange retrieved successfully")
                .result(response)
                .build());
    }

    /**
     * Lấy chi tiết yêu cầu đổi trả theo ID chỉ bao gồm cả thông tin của ReturnExchange
     */
    @GetMapping("/return-exchange/{returnExchangeId}/raw")
    public ResponseEntity<ApiResponse<ReturnExchangeResponse>> getReturnExchangeRaw(
            @PathVariable Long returnExchangeId) {
        ReturnExchangeResponse response = staffOrderService.getReturnExchangeById(returnExchangeId);
        return ResponseEntity.ok(ApiResponse.<ReturnExchangeResponse>builder()
                .code(1000)
                .message("Return exchange retrieved successfully")
                .result(response)
                .build());
    }

    //Hàm này dùng để cập nhật Return_Exchange.Status = "APPROVED" hoặc "REJECTED" khi request đang PENDING
    @PutMapping("/return-exchange/{returnExchangeId}/status")
    public ResponseEntity<ApiResponse<ReturnExchangeResponse>> updateReturnExchangeStatus(
            @PathVariable Long returnExchangeId,
            @RequestBody @Valid ReturnExchangeDecisionRequest request) {
        ReturnExchangeResponse response = staffOrderService.updateReturnExchangeStatusForSalesStaff(returnExchangeId, request);
        return ResponseEntity.ok(ApiResponse.<ReturnExchangeResponse>builder()
                .code(1000)
                .message("Return exchange status updated successfully")
                .result(response)
                .build());
    }

    /**
     * Hoàn tất quá trình hoàn tiền thủ công cho yêu cầu đổi trả
     */
    @PutMapping(value = "/return-exchange/{returnExchangeId}/complete-refund", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReturnExchangeResponse>> completeRefund(
            @PathVariable Long returnExchangeId,
            @RequestPart("request") @Valid StaffCompleteRefundRequest request,
            @RequestPart("staffEvidenceFile") MultipartFile staffEvidenceFile) {
        ReturnExchangeResponse response = staffOrderService.completeRefundForSalesStaff(returnExchangeId, request, staffEvidenceFile);
        return ResponseEntity.ok(ApiResponse.<ReturnExchangeResponse>builder()
                .code(1000)
                .message("Refund completed successfully")
                .result(response)
                .build());
    }
}
