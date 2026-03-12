package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.request.ReturnExchangeRequest;
import com.swp391.eyewear_management_backend.dto.response.ReturnExchangeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReturnExchangeService {
    
    /**
     * Tạo yêu cầu đổi trả mới
     * @param request Thông tin đổi trả
     * @param imageFile File ảnh đính kèm
     * @return ReturnExchangeResponse
     */
    ReturnExchangeResponse createReturnExchange(ReturnExchangeRequest request, MultipartFile imageFile);
    
    /**
     * Lấy chi tiết một yêu cầu đổi trả
     * @param returnExchangeId ID của return/exchange
     * @return ReturnExchangeResponse
     */
    ReturnExchangeResponse getReturnExchangeById(Long returnExchangeId);
    
    /**
     * Lấy yêu cầu đổi trả theo return code
     * @param returnCode Mã đổi trả
     * @return ReturnExchangeResponse
     */
    ReturnExchangeResponse getReturnExchangeByCode(String returnCode);
    
    /**
     * Lấy tất cả yêu cầu đổi trả của user hiện tại
     * @return Danh sách ReturnExchangeResponse
     */
    List<ReturnExchangeResponse> getMyReturnExchanges();
    
    /**
     * Lấy tất cả yêu cầu đổi trả (cho admin)
     * @return Danh sách ReturnExchangeResponse
     */
    List<ReturnExchangeResponse> getAllReturnExchanges();
    
    /**
     * Lấy tất cả yêu cầu đổi trả theo trạng thái
     * @param status Trạng thái
     * @return Danh sách ReturnExchangeResponse
     */
    List<ReturnExchangeResponse> getReturnExchangesByStatus(String status);
    
    /**
     * Duyệt yêu cầu đổi trả
     * @param returnExchangeId ID của return/exchange
     * @return ReturnExchangeResponse
     */
    ReturnExchangeResponse approveReturnExchange(Long returnExchangeId);
    
    /**
     * Từ chối yêu cầu đổi trả
     * @param returnExchangeId ID của return/exchange
     * @param rejectReason Lý do từ chối
     * @return ReturnExchangeResponse
     */
    ReturnExchangeResponse rejectReturnExchange(Long returnExchangeId, String rejectReason);
    
//    /**
//     * Cập nhật thông tin đổi trả
//     * @param returnExchangeId ID của return/exchange
//     * @param request Thông tin cần cập nhật
//     * @return ReturnExchangeResponse
//     */
//    ReturnExchangeResponse updateReturnExchange(Long returnExchangeId, ReturnExchangeRequest request);
//
    /**
     * Xóa yêu cầu đổi trả
     * @param returnExchangeId ID của return/exchange
     */
    void deleteReturnExchange(Long returnExchangeId);
}
