package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.request.CustomerCancelOrderRequest;
import com.swp391.eyewear_management_backend.dto.request.CreateOrderRequest;
import com.swp391.eyewear_management_backend.dto.response.CustomerCancelOrderResponse;
import com.swp391.eyewear_management_backend.dto.response.CreateOrderResponse;
import com.swp391.eyewear_management_backend.dto.response.CustomerOrderHistoryResponse;
import com.swp391.eyewear_management_backend.dto.response.OrderStatusResponse;
import com.swp391.eyewear_management_backend.dto.response.StaffOrderDetailResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OrderService {
    CreateOrderResponse createOrder(CreateOrderRequest request);

    OrderStatusResponse getOrderStatus(Long orderId);

    List<CustomerOrderHistoryResponse> getCustomerOrderHistory();

    StaffOrderDetailResponse getOrderDetailForCustomer(Long orderId);

    CustomerCancelOrderResponse cancelOrderByCustomer(Long orderId, CustomerCancelOrderRequest request, MultipartFile customerAccountQrFile);
}
