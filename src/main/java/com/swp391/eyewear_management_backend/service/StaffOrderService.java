package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.request.StaffOrderSearchRequest;
import com.swp391.eyewear_management_backend.dto.response.OrderStatusGroupResponse;
import com.swp391.eyewear_management_backend.dto.response.StaffOrderDetailResponse;
import com.swp391.eyewear_management_backend.dto.response.StaffOrderListResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface StaffOrderService {
    List<StaffOrderListResponse> getOrdersForStaff();
    Page<StaffOrderListResponse> searchOrdersForStaff(StaffOrderSearchRequest request);
    Page<StaffOrderListResponse> searchOrdersForOperationStaff(StaffOrderSearchRequest request);
    StaffOrderDetailResponse getOrderDetailForSalesStaff(Long orderId);
    List<OrderStatusGroupResponse> getSalesStaffOrderStatuses();
    List<OrderStatusGroupResponse> getOperationStaffOrderStatuses();
}
