package com.swp391.eyewear_management_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/*
    * Class này dùng để trả về chuỗi JSON tổng cho trang OrderDetail khi nhận vào orderId
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOrderDetailResponse {
    //Thông tin đơn hàng
    private Long orderId;
    private String orderCode;
    private String orderStatus;
    private String orderType;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private String shippingStatus;
    private Boolean hasPrescriptionItem;
    private Boolean requiresFinalPayment;
    private List<String> availableActions;

    //Thông tin người đặt hàng
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    //Thông tin sản phẩm
    private List<StaffOrderItemResponse> orderDetail;
    private List<StaffPrescriptionOrderItemResponse> prescriptionOrderDetail;

    //Thông tin người nhận
    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
    private String recipientAddress;
    private String note;
}
