package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.request.StaffOrderSearchRequest;
import com.swp391.eyewear_management_backend.dto.response.OrderStatusGroupResponse;
import com.swp391.eyewear_management_backend.dto.response.StaffOrderListResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface StaffOrderService {
    Page<StaffOrderListResponse> searchOrdersForStaff(StaffOrderSearchRequest request);
    Page<StaffOrderListResponse> searchOrdersForOperationStaff(StaffOrderSearchRequest request);
    List<OrderStatusGroupResponse> getSalesStaffOrderStatuses();
    List<OrderStatusGroupResponse> getOperationStaffOrderStatuses();
}
