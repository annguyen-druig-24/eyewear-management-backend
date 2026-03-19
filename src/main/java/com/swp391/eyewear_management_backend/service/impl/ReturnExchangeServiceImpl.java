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
import com.swp391.eyewear_management_backend.repository.PrescriptionOrderRepo;
import com.swp391.eyewear_management_backend.service.ImageUploadService;
import com.swp391.eyewear_management_backend.service.ReturnExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private OrderRepo orderRepository;

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private ReturnExchangeMapper returnExchangeMapper;

    @Autowired
    private ImageUploadService imageUploadService;

    @Autowired
    private PrescriptionOrderRepo prescriptionOrderRepository;

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
    public String createReturnExchange(ReturnExchangeRequest request, List<MultipartFile> itemImages, MultipartFile customerImageQr) {
        User currentUser = getCurrentUser();

        // 1. Kiểm tra Đơn hàng
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Đổi mã lỗi tương ứng ORDER_NOT_FOUND

        if (!"COMPLETED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new AppException(ErrorCode.RETURN_EXCHANGE_NOT_FIT);
        }

        // Kiểm tra quá 7 ngày kể từ ngày giao hàng dự kiến (Delivery_At)
        if (order.getShippingInfo() != null && order.getShippingInfo().getDeliveredAt() != null) {
            LocalDateTime delivery = order.getShippingInfo().getDeliveredAt();
            if (LocalDateTime.now().isAfter(delivery.plusDays(7))) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Cần đổi thành lỗi quá hạn đổi trả
            }
        }

        if ("RETURN".equalsIgnoreCase(request.getReturnType()) || "REFUND".equalsIgnoreCase(request.getReturnType())) {
            // Nếu là Trả hàng / Hoàn tiền thì 3 trường này BẮT BUỘC phải có
            if (request.getRefundMethod() == null || request.getRefundMethod().trim().isEmpty()) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }
            if (request.getRefundAccountNumber() == null || request.getRefundAccountNumber().trim().isEmpty()) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }
            if (request.getRefundAccountName() == null || request.getRefundAccountName().trim().isEmpty()) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }
        }

        // 2. Khởi tạo đối tượng Cha (Return_Exchange)
        ReturnExchange returnExchange = new ReturnExchange();
        returnExchange.setOrder(order);
        returnExchange.setUser(currentUser);
        returnExchange.setReturnCode(generateReturnCode(request.getReturnType()));
        returnExchange.setRequestDate(LocalDateTime.now());
        returnExchange.setRequestNote(request.getRequestNote());
        returnExchange.setReturnReason(request.getReturnReason());
        returnExchange.setReturnType(request.getReturnType());
        returnExchange.setRequestScope(request.getRequestScope());

        // Set thông tin hoàn tiền nếu có
        returnExchange.setRefundMethod(request.getRefundMethod());
        returnExchange.setRefundAccountNumber(request.getRefundAccountNumber());
        returnExchange.setRefundAccountName(request.getRefundAccountName());
        returnExchange.setStatus("PENDING");

        // 3. Chuẩn bị danh sách chi tiết đơn hàng để dò tìm tự động
        List<OrderDetail> normalDetails = order.getOrderDetails() != null ? order.getOrderDetails() : new ArrayList<>();
        List<PrescriptionOrderDetail> prescriptionDetails = new ArrayList<>();

        // Entity Order có @OneToOne PrescriptionOrder, lấy ra danh sách chi tiết kính thuốc nếu có
        if (order.getPrescriptionOrder() != null && order.getPrescriptionOrder().getPrescriptionOrderDetails() != null) {
            prescriptionDetails = order.getPrescriptionOrder().getPrescriptionOrderDetails();
        }

        // 4. Xử lý các Items con
        List<ReturnExchangeItem> items = new ArrayList<>();
        BigDecimal refundAmount = BigDecimal.ZERO;

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            int itemIndex = 0;
            for (ReturnExchangeItemRequest itemReq : request.getItems()) {
                Long targetId = itemReq.getOrderDetailId();

                // Tự động dò tìm ID này thuộc về loại nào trong đơn hàng hiện tại
                OrderDetail matchedNormal = normalDetails.stream()
                        .filter(nd -> nd.getOrderDetailID().equals(targetId))
                        .findFirst()
                        .orElse(null);

                PrescriptionOrderDetail matchedPrescription = prescriptionDetails.stream()
                        .filter(pd -> pd.getPrescriptionOrderDetailID().equals(targetId))
                        .findFirst()
                        .orElse(null);

                // Validation 1: Không tìm thấy ID này trong cả 2 danh sách
                if (matchedNormal == null && matchedPrescription == null) {
                    throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Báo lỗi item không hợp lệ
                }

                // Validation 2: Trùng ID giữa đơn thường và đơn thuốc (Xung đột dữ liệu hiếm gặp)
                if (matchedNormal != null && matchedPrescription != null) {
                    throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Báo lỗi xung đột dữ liệu
                }

                // Khởi tạo đối tượng Con
                ReturnExchangeItem returnItem = new ReturnExchangeItem();
                returnItem.setReturnExchange(returnExchange);
                returnItem.setItemReason(itemReq.getItemReason());
                returnItem.setNote(itemReq.getNote());

                // Phân nhánh gán dữ liệu
                if (matchedNormal != null) {
                    // ---> ĐÂY LÀ ĐƠN THƯỜNG
                    if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0 || itemReq.getQuantity() > matchedNormal.getQuantity()) {
                        throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
                    }

                    returnItem.setOrderDetail(matchedNormal);
                    returnItem.setItemSource("ORDER_DETAIL");
                    returnItem.setQuantity(itemReq.getQuantity());

                    // Cộng dồn tiền
                    if ("RETURN".equalsIgnoreCase(request.getReturnType()) || "REFUND".equalsIgnoreCase(request.getReturnType())) {
                        BigDecimal itemRefund = matchedNormal.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                        refundAmount = refundAmount.add(itemRefund);
                    }
                } else {
                    // ---> ĐÂY LÀ ĐƠN KÍNH THUỐC
                    returnItem.setPrescriptionOrderDetail(matchedPrescription);
                    returnItem.setItemSource("PRESCRIPTION_ORDER_DETAIL");
                    returnItem.setQuantity(1); // Mặc định kính thuốc tính là 1 bộ

                    // Cộng dồn tiền
                    if ("RETURN".equalsIgnoreCase(request.getReturnType()) || "REFUND".equalsIgnoreCase(request.getReturnType())) {
                        if (matchedPrescription.getSubTotal() != null) {
                            refundAmount = refundAmount.add(matchedPrescription.getSubTotal());
                        }
                    }
                }

                // Xử lý upload ảnh cho từng item dựa vào index
                if (itemImages != null && itemIndex < itemImages.size()) {
                    MultipartFile currentImage = itemImages.get(itemIndex);
                    if (currentImage != null && !currentImage.isEmpty()) {
                        try {
                            String itemImageUrl = imageUploadService.uploadImage(currentImage);
                            returnItem.setItemEvidenceUrl(itemImageUrl);
                        } catch (IOException e) {
                            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Đổi thành UPLOAD_IMAGE_FAILED
                        }
                    }
                }

                items.add(returnItem);
                itemIndex++;
            }
        }
        if ("ORDER".equalsIgnoreCase(request.getRequestScope())) {
            // Nếu trả nguyên toàn bộ đơn hàng (Scope là ORDER)
            refundAmount = order.getTotalAmount();
        }

        // Set lại danh sách Items con và Số tiền hoàn
        returnExchange.setReturnExchangeItems(items);
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            returnExchange.setRefundAmount(refundAmount);
        }

        // 5. Xử lý upload ảnh QR Code của khách hàng
        if (customerImageQr != null && !customerImageQr.isEmpty()) {
            try {
                String customerQrImageUrl = imageUploadService.uploadImage(customerImageQr);
                returnExchange.setCustomerAccountQr(customerQrImageUrl);
            } catch (IOException e) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Đổi thành UPLOAD_IMAGE_FAILED
            }
        }

        // 6. Lưu vào Database
        returnExchangeRepository.save(returnExchange);

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


    //@Override
//    public ReturnExchangeResponse updateReturnExchange(Long returnExchangeId, ReturnExchangeRequest request) {
//        ReturnExchange returnExchange = returnExchangeRepository.findById(returnExchangeId)
//                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Thay bằng lỗi phù hợp
//
//        // Chỉ có thể cập nhật nếu status là PENDING
//        if (!returnExchange.getStatus().equals("PENDING")) {
//            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Thay bằng lỗi phù hợp
//        }
//
//        if (request.getRequestNote() != null) {
//            returnExchange.setRequestNote(trimToNull(request.getRequestNote()));
//        }
//        if (request.getReturnReason() != null) {
//            returnExchange.setReturnReason(request.getReturnReason());
//        }
////        if (request.getCustomerEvidenceUrl() != null) {
////            returnExchange.setCustomerEvidenceUrl(trimToNull(request.getCustomerEvidenceUrl()));
////        }
////        if (request.getRefundAmount() != null) {
////            returnExchange.setRefundAmount(request.getRefundAmount());
////        }
//        if (request.getRefundMethod() != null) {
//            returnExchange.setRefundMethod(trimToNull(request.getRefundMethod()));
//        }
//        if (request.getRefundAccountNumber() != null) {
//            returnExchange.setRefundAccountNumber(trimToNull(request.getRefundAccountNumber()));
//        }
//        if (request.getRefundAccountName() != null) {
//            returnExchange.setRefundAccountName(trimToNull(request.getRefundAccountName()));
//        }
////        if (request.getRefundReferenceCode() != null) {
////            returnExchange.setRefundReferenceCode(trimToNull(request.getRefundReferenceCode()));
////        }
////        if (request.getStaffRefundEvidenceUrl() != null) {
////            returnExchange.setStaffRefundEvidenceUrl(trimToNull(request.getStaffRefundEvidenceUrl()));
////        }
//        if (request.getItems() != null) {
//            returnExchange.getReturnExchangeItems().clear();
//            returnExchange.getReturnExchangeItems()
//                    .addAll(buildReturnExchangeItems(request, returnExchange.getOrder(), returnExchange, returnExchange.getRequestScope()));
//        }
//
//        ReturnExchange updatedReturnExchange = returnExchangeRepository.save(returnExchange);
//        return returnExchangeMapper.toReturnExchangeResponse(updatedReturnExchange);
//    }

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

//    private Order resolveOrder(ReturnExchangeRequest request) {
//        if (request == null) {
//            throw new AppException(ErrorCode.INVALID_REQUEST);
//        }
//        if (request.getOrderId() != null) {
//            return orderRepository.findById(request.getOrderId())
//                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
//        }
//        if (request.getOrderDetailId() != null) {
//            OrderDetail orderDetail = orderDetailRepository.findById(request.getOrderDetailId())
//                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
//            return orderDetail.getOrder();
//        }
//        throw new AppException(ErrorCode.INVALID_REQUEST);
//    }

    private String normalizeReturnType(String returnType) {
        String normalized = trimToNull(returnType);
        if (normalized == null) {
            return "REFUND";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeRequestScope(String requestScope, String returnType) {
        String normalized = trimToNull(requestScope);
        if (normalized == null) {
            return "REFUND".equals(returnType) ? "ORDER" : "ITEM";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

//    private List<ReturnExchangeItem> buildReturnExchangeItems(
//            ReturnExchangeRequest request,
//            Order order,
//            ReturnExchange returnExchange,
//            String requestScope
//    ) {
//        if (!"ITEM".equals(requestScope)) {
//            return List.of();
//        }
//        List<ReturnExchangeItemRequest> items = request.getItems();
//        if (items == null || items.isEmpty()) {
//            if (request.getOrderDetailId() == null) {
//                throw new AppException(ErrorCode.INVALID_REQUEST);
//            }
//            items = List.of(ReturnExchangeItemRequest.builder()
//                    .orderDetailId(request.getOrderDetailId())
//                    .quantity(1)
//                    .build());
//        }
//
//        List<ReturnExchangeItem> result = new ArrayList<>();
//        for (ReturnExchangeItemRequest itemRequest : items) {
//            if (itemRequest.getOrderDetailId() == null || itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
//                throw new AppException(ErrorCode.INVALID_REQUEST);
//            }
//            OrderDetail orderDetail = orderDetailRepository.findById(itemRequest.getOrderDetailId())
//                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
//            if (orderDetail.getOrder() == null || !order.getOrderID().equals(orderDetail.getOrder().getOrderID())) {
//                throw new AppException(ErrorCode.INVALID_REQUEST);
//            }
//            ReturnExchangeItem item = new ReturnExchangeItem();
//            item.setReturnExchange(returnExchange);
//            item.setOrderDetail(orderDetail);
//            item.setQuantity(itemRequest.getQuantity());
//            item.setItemReason(trimToNull(itemRequest.getItemReason()));
//            item.setNote(trimToNull(itemRequest.getNote()));
//            result.add(item);
//        }
//        return result;
//    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
