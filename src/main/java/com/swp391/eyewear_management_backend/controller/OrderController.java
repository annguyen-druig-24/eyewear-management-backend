package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.request.CustomerCancelOrderRequest;
import com.swp391.eyewear_management_backend.dto.request.CreateOrderRequest;
import com.swp391.eyewear_management_backend.dto.response.*;
import com.swp391.eyewear_management_backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<CreateOrderResponse> create(@RequestBody @Valid CreateOrderRequest request) {
        return ApiResponse.<CreateOrderResponse>builder()
                .message("OK")
                .result(orderService.createOrder(request))
                .build();
    }

    @GetMapping("/history")
    public ApiResponse<List<CustomerOrderHistoryResponse>> getOrderHistory() {
        return ApiResponse.<List<CustomerOrderHistoryResponse>>builder()
                .message("OK")
                .result(orderService.getCustomerOrderHistory())
                .build();
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderStatusResponse> getStatus(@PathVariable Long orderId) {
        return ApiResponse.<OrderStatusResponse>builder()
                .message("OK")
                .result(orderService.getOrderStatus(orderId))
                .build();
    }

    @GetMapping("/{orderId}/detail")
    public ApiResponse<StaffOrderDetailResponse> getOrderDetailForCustomer(@PathVariable Long orderId) {
        return ApiResponse.<StaffOrderDetailResponse>builder()
                .message("OK")
                .result(orderService.getOrderDetailForCustomer(orderId))
                .build();
    }

    @PostMapping(value = "/{orderId}/cancel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CustomerCancelOrderResponse> cancelOrderByCustomer(
            @PathVariable Long orderId,
            @RequestPart("request") CustomerCancelOrderRequest request,
            @RequestPart(value = "customerAccountQrFile", required = false) MultipartFile customerAccountQrFile) {
        return ApiResponse.<CustomerCancelOrderResponse>builder()
                .message("OK")
                .result(orderService.cancelOrderByCustomer(orderId, request, customerAccountQrFile))
                .build();
    }
}
