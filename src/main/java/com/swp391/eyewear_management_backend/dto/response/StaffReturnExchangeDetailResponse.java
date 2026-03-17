package com.swp391.eyewear_management_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffReturnExchangeDetailResponse {
    private Long orderId;
    private String orderCode;
    private String orderStatus;
    private String orderType;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private String shippingStatus;
    private BigDecimal shippingFee;
    private LocalDateTime expectedDeliveryAt;
    private LocalDateTime deliveredAt;
    private Long remainingTimeValid;
    private Boolean isPastExpectedDeliveryAt;
    private Boolean hasPrescriptionItem;
    private Boolean requiresFinalPayment;
    private List<String> availableActions;

    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private List<StaffOrderItemResponse> orderDetail;
    private List<StaffPrescriptionOrderItemResponse> prescriptionOrderDetail;

    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
    private String recipientAddress;
    private String note;

    private Long returnExchangeId;
    private String returnCode;
    private LocalDateTime requestDate;
    private String returnExchangeStatus;
    private String returnType;
    private String requestScope;
    private String requestNote;
    private String returnReason;
    private BigDecimal refundAmount;
    private String refundMethod;
    private String refundAccountNumber;
    private String customerAccountQr;
    private String refundAccountName;
    private String refundReferenceCode;
    private String staffRefundEvidenceUrl;
    private LocalDateTime approvedDate;
    private String approvedByName;
    private LocalDateTime processedDate;
    private String processedByName;
    private String rejectReason;
    private List<ReturnExchangeItemResponse> returnExchangeItems;
}

