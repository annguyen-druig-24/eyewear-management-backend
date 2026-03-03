package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.request.CreateOrderRequest;
import com.swp391.eyewear_management_backend.dto.response.CreateOrderResponse;
import com.swp391.eyewear_management_backend.dto.response.OrderStatusResponse;

public interface OrderService {
    CreateOrderResponse createOrder(CreateOrderRequest request);

    OrderStatusResponse getOrderStatus(Long orderId);
}
