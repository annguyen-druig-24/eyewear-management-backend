package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.request.CreateOrderRequest;
import com.swp391.eyewear_management_backend.dto.response.ApiResponse;
import com.swp391.eyewear_management_backend.dto.response.CreateOrderResponse;
import com.swp391.eyewear_management_backend.dto.response.OrderStatusResponse;
import com.swp391.eyewear_management_backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping()
    public ApiResponse<CreateOrderResponse> create(@RequestBody @Valid CreateOrderRequest request) {
        return ApiResponse.<CreateOrderResponse>builder()
                .message("OK")
                .result(orderService.createOrder(request))
                .build();
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderStatusResponse> getStatus(@PathVariable Long orderId) {
        return ApiResponse.<OrderStatusResponse>builder()
                .message("OK")
                .result(orderService.getOrderStatus(orderId))
                .build();
    }
}