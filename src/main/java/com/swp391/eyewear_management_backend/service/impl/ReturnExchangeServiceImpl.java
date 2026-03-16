package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.request.ReturnExchangeItemRequest;
import com.swp391.eyewear_management_backend.dto.request.ReturnExchangeRequest;
import com.swp391.eyewear_management_backend.dto.response.ReturnExchangeResponse;
import com.swp391.eyewear_management_backend.entity.*;
import com.swp391.eyewear_management_backend.exception.AppException;
import com.swp391.eyewear_management_backend.exception.ErrorCode;
import com.swp391.eyewear_management_backend.mapper.ReturnExchangeMapper;
import com.swp391.eyewear_management_backend.repository.OrderDetailRepo;
import com.swp391.eyewear_management_backend.repository.OrderRepo;
import com.swp391.eyewear_management_backend.repository.ReturnExchangeRepo;
import com.swp391.eyewear_management_backend.repository.UserRepo;
import com.swp391.eyewear_management_backend.service.ImageUploadService;
import com.swp391.eyewear_management_backend.service.ReturnExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private OrderRepo orderRepository;


    @Autowired
    private ReturnExchangeMapper returnExchangeMapper;

    @Autowired
    private ImageUploadService imageUploadService;

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

    private String generateReturnCode(String returnType) {
        // 1. Xác định tiền tố
        String typePrefix = "RX";
        if (returnType != null) {
            switch (returnType.trim().toUpperCase()) {
                case "RETURN": typePrefix = "RT"; break;
                case "EXCHANGE": typePrefix = "EX"; break;
                case "WARRANTY": typePrefix = "WA"; break;
                case "REFUND": typePrefix = "RF"; break;
            }
        }

        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String prefix = typePrefix + datePart;

        // 2. Tìm mã mới nhất trong DB
        Optional<ReturnExchange> lastReturnOpt = returnExchangeRepository
                .findTopByReturnCodeStartingWithOrderByReturnCodeDesc(prefix);

        int sequence = 1;
        if (lastReturnOpt.isPresent()) {
            String lastCode = lastReturnOpt.get().getReturnCode();
            String sequenceStr = lastCode.substring(lastCode.length() - 4);
            sequence = Integer.parseInt(sequenceStr) + 1;
        }

        // 3. Vòng lặp CHỐNG TRÙNG LẶP (Cực kỳ quan trọng khi có nhiều đơn)
        String finalCode;
        do {
            // Sinh ra mã với sequence hiện tại (VD: ...0002)
            finalCode = prefix + String.format("%04d", sequence);

            // Tăng sequence lên trước 1 đơn vị phòng trường hợp vòng lặp quay lại (VD: lên 3)
            sequence++;

            // Kiểm tra trong DB xem finalCode này đã có thằng nào nhanh tay chộp mất chưa?
            // Nếu có rồi (isPresent) -> Vòng lặp quay lại thử với số tiếp theo.
        } while (returnExchangeRepository.findByReturnCode(finalCode).isPresent());

        return finalCode;
    }

    @Override
    public String createReturnExchange(ReturnExchangeRequest request, MultipartFile imageFile) {
        User currentUser = getCurrentUser();

        // 1. Kiểm tra Đơn hàng
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Đổi mã lỗi tương ứng ORDER_NOT_FOUND

        if (!order.getOrderStatus().equals("COMPLETED")) {
            throw new AppException(ErrorCode.RETURN_EXCHANGE_NOT_FIT);
        }

        if ("RETURN".equalsIgnoreCase(request.getReturnType()) || "REFUND".equalsIgnoreCase(request.getReturnType())) {

            // Nếu là Trả hàng / Hoàn tiền thì 3 trường này BẮT BUỘC phải có
            if (request.getRefundMethod() == null || request.getRefundMethod().trim().isEmpty()) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Thay bằng mã lỗi: Vui lòng chọn phương thức hoàn tiền
            }
            if (request.getRefundAccountNumber() == null || request.getRefundAccountNumber().trim().isEmpty()) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Thay bằng mã lỗi: Vui lòng nhập số tài khoản
            }
            if (request.getRefundAccountName() == null || request.getRefundAccountName().trim().isEmpty()) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Thay bằng mã lỗi: Vui lòng nhập tên chủ tài khoản
            }
        }

        // 2. Upload hình ảnh evidence từ khách hàng
        String customerEvidenceUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                customerEvidenceUrl = imageUploadService.uploadImage(imageFile);
            } catch (IOException e) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Đổi thành UPLOAD_IMAGE_FAILED
            }
        }

        // 3. Khởi tạo đối tượng Cha (Return_Exchange)
        ReturnExchange returnExchange = new ReturnExchange();
        returnExchange.setOrder(order);
        returnExchange.setUser(currentUser);
        returnExchange.setReturnCode(generateReturnCode(request.getReturnType()));
        returnExchange.setRequestDate(LocalDateTime.now());
        returnExchange.setRequestNote(request.getRequestNote());
        returnExchange.setReturnReason(request.getReturnReason());
        returnExchange.setReturnType(request.getReturnType()); // REFUND, RETURN, WARRANTY
        returnExchange.setRequestScope(request.getRequestScope()); // ORDER hoặc ITEM

        // Set thông tin hoàn tiền nếu có
        returnExchange.setRefundMethod(request.getRefundMethod());
        returnExchange.setRefundAccountNumber(request.getRefundAccountNumber());
        returnExchange.setRefundAccountName(request.getRefundAccountName());

        returnExchange.setCustomerEvidenceUrl(customerEvidenceUrl);
        returnExchange.setStatus("PENDING"); // Mặc định chờ duyệt

        // 4. Xử lý các Items con (nếu Request_Scope là ITEM và có truyền danh sách item lên)
        List<ReturnExchangeItem> items = new ArrayList<>();
        BigDecimal refundAmount = BigDecimal.ZERO;

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (ReturnExchangeItemRequest itemReq : request.getItems()) {
                OrderDetail orderDetail = orderDetailRepository.findById(itemReq.getOrderDetailId())
                        .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

                // Đảm bảo Item thuộc về đúng Đơn hàng
                if (!orderDetail.getOrder().getOrderID().equals(order.getOrderID())) {
                    throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
                }

                // Kiểm tra số lượng hợp lệ
                if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0 || itemReq.getQuantity() > orderDetail.getQuantity()) {
                    throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
                }

                // Cộng dồn tiền nếu thuộc type trả hàng/hoàn tiền
                if (request.getReturnType().equalsIgnoreCase("RETURN") || request.getReturnType().equalsIgnoreCase("REFUND")) {
                    BigDecimal itemRefund = orderDetail.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                    refundAmount = refundAmount.add(itemRefund);
                }

                // Tạo đối tượng Con
                ReturnExchangeItem returnItem = new ReturnExchangeItem();
                returnItem.setReturnExchange(returnExchange); // Quan trọng: Liên kết 2 chiều
                returnItem.setOrderDetail(orderDetail);
                returnItem.setQuantity(itemReq.getQuantity());
                returnItem.setItemReason(itemReq.getItemReason());
                returnItem.setNote(itemReq.getNote());

                items.add(returnItem);
            }
        }
        // Nếu Request_Scope là ORDER (Hoàn/trả toàn bộ đơn), tự động tính tổng tiền từ Order
        else if ("ORDER".equalsIgnoreCase(request.getRequestScope())) {
            refundAmount = order.getTotalAmount();
        }

        // Set lại danh sách Items con và Số tiền hoàn
        returnExchange.setItems(items);
        if(refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            returnExchange.setRefundAmount(refundAmount);
        }

        // 5. Lưu vào Database
        ReturnExchange savedReturnExchange = returnExchangeRepository.save(returnExchange);

       // TRẢ VỀ CHUỖI THÔNG BÁO THÀNH CÔNG BẰNG TIẾNG ANH
        return "Return exchange request created successfully";
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

//    @Override
//    public ReturnExchangeResponse updateReturnExchange(Long returnExchangeId, ReturnExchangeRequest request) {
//        ReturnExchange returnExchange = returnExchangeRepository.findById(returnExchangeId)
//                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Thay bằng lỗi phù hợp
//
//        // Chỉ có thể cập nhật nếu status là PENDING
//        if (!returnExchange.getStatus().equals("PENDING")) {
//            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Thay bằng lỗi phù hợp
//        }
//
//        // Cập nhật các trường cho phép
//        if (request.getQuantity() != null) {
//            returnExchange.setQuantity(request.getQuantity());
//        }
//        if (request.getReturnReason() != null) {
//            returnExchange.setReturnReason(request.getReturnReason());
//        }
//        if (request.getProductCondition() != null) {
//            returnExchange.setProductCondition(request.getProductCondition());
//        }
//        if (request.getRefundAmount() != null) {
//            returnExchange.setRefundAmount(request.getRefundAmount());
//        }
//        if (request.getRefundMethod() != null) {
//            returnExchange.setRefundMethod(request.getRefundMethod());
//        }
//        if (request.getRefundAccountNumber() != null) {
//            returnExchange.setRefundAccountNumber(request.getRefundAccountNumber());
//        }
//        if (request.getImageUrl() != null) {
//            returnExchange.setImageUrl(request.getImageUrl());
//        }
//
//        ReturnExchange updatedReturnExchange = returnExchangeRepository.save(returnExchange);
//        return returnExchangeMapper.toReturnExchangeResponse(updatedReturnExchange);
//    }

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
