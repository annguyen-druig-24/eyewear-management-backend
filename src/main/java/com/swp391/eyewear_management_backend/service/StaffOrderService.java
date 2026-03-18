package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.request.ReturnExchangeDecisionRequest;
import com.swp391.eyewear_management_backend.dto.request.StaffCompleteRefundRequest;
import com.swp391.eyewear_management_backend.dto.request.StaffOrderSearchRequest;
import com.swp391.eyewear_management_backend.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StaffOrderService {
    List<StaffOrderListResponse> getOrdersForStaff();
    Page<StaffOrderListResponse> searchOrdersForStaff(StaffOrderSearchRequest request);
    Page<StaffOrderListResponse> searchOrdersForOperationStaff(StaffOrderSearchRequest request);
    StaffOrderDetailResponse getOrderDetailForSalesStaff(Long orderId);
    StaffOrderDetailResponse getOrderDetailForOperationStaff(Long orderId);
    StaffOrderDetailResponse confirmOrderForSalesStaff(Long orderId);
    StaffOrderDetailResponse updateOrderForOperationStaff(Long orderId, String action);
    List<OrderStatusGroupResponse> getSalesStaffOrderStatuses();
    List<OrderStatusGroupResponse> getOperationStaffOrderStatuses();
    List<StaffReturnExchangeListResponse> getReturnExchangeOrders();
    List<StaffReturnExchangeListResponse> getCancelRefundRequestsForSalesStaff();
    StaffReturnExchangeDetailResponse getCancelRefundRequestDetailForSalesStaff(Long returnExchangeId);
    StaffReturnExchangeDetailResponse getReturnExchangeDetailForSalesStaff(Long returnExchangeId);
    ReturnExchangeResponse getReturnExchangeById(Long returnExchangeId);
    ReturnExchangeResponse updateCancelRefundRequestStatusForSalesStaff(Long returnExchangeId, ReturnExchangeDecisionRequest request);
    ReturnExchangeResponse updateReturnExchangeStatusForSalesStaff(Long returnExchangeId, ReturnExchangeDecisionRequest request);
    ReturnExchangeResponse completeCancelRefundRequestForSalesStaff(Long returnExchangeId, StaffCompleteRefundRequest request, MultipartFile staffEvidenceFile);
    ReturnExchangeResponse completeRefundForSalesStaff(Long returnExchangeId, StaffCompleteRefundRequest request, MultipartFile staffEvidenceFile);
}
