package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.request.ReturnExchangeRequest;
import com.swp391.eyewear_management_backend.dto.response.ReturnExchangeResponse;
import com.swp391.eyewear_management_backend.entity.*;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import com.swp391.eyewear_management_backend.mapper.ReturnExchangeMapper;
import com.swp391.eyewear_management_backend.repository.OrderDetailRepo;
import com.swp391.eyewear_management_backend.repository.ReturnExchangeRepo;
import com.swp391.eyewear_management_backend.repository.UserRepo;
import com.swp391.eyewear_management_backend.service.ReturnExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReturnExchangeServiceImpl implements ReturnExchangeService {

    @Autowired
    private ReturnExchangeRepo returnExchangeRepository;

    @Autowired
    private OrderDetailRepo orderDetailRepository;

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private ReturnExchangeMapper returnExchangeMapper;

    /**
     * Lấy user hiện tại từ security context
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    /**
     * Tạo mã đổi trả tự động
     */
    private String generateReturnCode() {
        String code;
        do {
            code = "RX" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        } while (returnExchangeRepository.findByReturnCode(code).isPresent());
        return code;
    }

    @Override
    public ReturnExchangeResponse createReturnExchange(ReturnExchangeRequest request) {
        // Lấy user hiện tại
        User currentUser = getCurrentUser();

        // Kiểm tra OrderDetail có tồn tại không
        OrderDetail orderDetail = orderDetailRepository.findById(request.getOrderDetailId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Thay bằng lỗi phù hợp

        // Kiểm tra OrderDetail đã có return/exchange chưa
        if (returnExchangeRepository.existsByOrderDetail_OrderDetailID(request.getOrderDetailId())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Thay bằng lỗi phù hợp
        }

        // Tạo ReturnExchange mới
        ReturnExchange returnExchange = new ReturnExchange();
        returnExchange.setOrderDetail(orderDetail);
        returnExchange.setUser(currentUser);
        returnExchange.setReturnCode(generateReturnCode());
        returnExchange.setRequestDate(LocalDateTime.now());
        returnExchange.setQuantity(request.getQuantity());
        returnExchange.setReturnReason(request.getReturnReason());
        returnExchange.setProductCondition(request.getProductCondition());
        returnExchange.setRefundAmount(request.getRefundAmount());
        returnExchange.setRefundMethod(request.getRefundMethod());
        returnExchange.setRefundAccountNumber(request.getRefundAccountNumber());
        returnExchange.setStatus("PENDING"); // Trạng thái mặc định
        returnExchange.setImageUrl(request.getImageUrl());

        ReturnExchange savedReturnExchange = returnExchangeRepository.save(returnExchange);
        return returnExchangeMapper.toReturnExchangeResponse(savedReturnExchange);
    }

    @Override
    public ReturnExchangeResponse getReturnExchangeById(Long returnExchangeId) {
        ReturnExchange returnExchange = returnExchangeRepository.findById(returnExchangeId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Thay bằng lỗi phù hợp
        return returnExchangeMapper.toReturnExchangeResponse(returnExchange);
    }

    @Override
    public ReturnExchangeResponse getReturnExchangeByCode(String returnCode) {
        ReturnExchange returnExchange = returnExchangeRepository.findByReturnCode(returnCode)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Thay bằng lỗi phù hợp
        return returnExchangeMapper.toReturnExchangeResponse(returnExchange);
    }

    @Override
    public List<ReturnExchangeResponse> getMyReturnExchanges() {
        User currentUser = getCurrentUser();
        List<ReturnExchange> returnExchanges = returnExchangeRepository.findByUser_UserId(currentUser.getUserId());
        return returnExchanges.stream()
                .map(returnExchangeMapper::toReturnExchangeResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReturnExchangeResponse> getAllReturnExchanges() {
        List<ReturnExchange> returnExchanges = returnExchangeRepository.findAll();
        return returnExchanges.stream()
                .map(returnExchangeMapper::toReturnExchangeResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReturnExchangeResponse> getReturnExchangesByStatus(String status) {
        List<ReturnExchange> returnExchanges = returnExchangeRepository.findByStatus(status);
        return returnExchanges.stream()
                .map(returnExchangeMapper::toReturnExchangeResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReturnExchangeResponse approveReturnExchange(Long returnExchangeId) {
        User approver = getCurrentUser();
        ReturnExchange returnExchange = returnExchangeRepository.findById(returnExchangeId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Thay bằng lỗi phù hợp

        returnExchange.setApprovedBy(approver);
        returnExchange.setApprovedDate(LocalDateTime.now());
        returnExchange.setStatus("APPROVED");

        ReturnExchange updatedReturnExchange = returnExchangeRepository.save(returnExchange);
        return returnExchangeMapper.toReturnExchangeResponse(updatedReturnExchange);
    }

    @Override
    public ReturnExchangeResponse rejectReturnExchange(Long returnExchangeId, String rejectReason) {
        User approver = getCurrentUser();
        ReturnExchange returnExchange = returnExchangeRepository.findById(returnExchangeId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Thay bằng lỗi phù hợp

        returnExchange.setApprovedBy(approver);
        returnExchange.setApprovedDate(LocalDateTime.now());
        returnExchange.setStatus("REJECTED");
        returnExchange.setRejectReason(rejectReason);

        ReturnExchange updatedReturnExchange = returnExchangeRepository.save(returnExchange);
        return returnExchangeMapper.toReturnExchangeResponse(updatedReturnExchange);
    }

    @Override
    public ReturnExchangeResponse updateReturnExchange(Long returnExchangeId, ReturnExchangeRequest request) {
        ReturnExchange returnExchange = returnExchangeRepository.findById(returnExchangeId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Thay bằng lỗi phù hợp

        // Chỉ có thể cập nhật nếu status là PENDING
        if (!returnExchange.getStatus().equals("PENDING")) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Thay bằng lỗi phù hợp
        }

        // Cập nhật các trường cho phép
        if (request.getQuantity() != null) {
            returnExchange.setQuantity(request.getQuantity());
        }
        if (request.getReturnReason() != null) {
            returnExchange.setReturnReason(request.getReturnReason());
        }
        if (request.getProductCondition() != null) {
            returnExchange.setProductCondition(request.getProductCondition());
        }
        if (request.getRefundAmount() != null) {
            returnExchange.setRefundAmount(request.getRefundAmount());
        }
        if (request.getRefundMethod() != null) {
            returnExchange.setRefundMethod(request.getRefundMethod());
        }
        if (request.getRefundAccountNumber() != null) {
            returnExchange.setRefundAccountNumber(request.getRefundAccountNumber());
        }
        if (request.getImageUrl() != null) {
            returnExchange.setImageUrl(request.getImageUrl());
        }

        ReturnExchange updatedReturnExchange = returnExchangeRepository.save(returnExchange);
        return returnExchangeMapper.toReturnExchangeResponse(updatedReturnExchange);
    }

    @Override
    public void deleteReturnExchange(Long returnExchangeId) {
        ReturnExchange returnExchange = returnExchangeRepository.findById(returnExchangeId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Thay bằng lỗi phù hợp

        // Chỉ có thể xóa nếu status là PENDING
        if (!returnExchange.getStatus().equals("PENDING")) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Thay bằng lỗi phù hợp
        }

        returnExchangeRepository.delete(returnExchange);
    }
}
