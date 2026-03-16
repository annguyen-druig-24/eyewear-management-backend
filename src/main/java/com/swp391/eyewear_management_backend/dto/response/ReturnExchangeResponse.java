package com.swp391.eyewear_management_backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnExchangeResponse {
    Long returnExchangeId;
    Long orderId;
    Long userId;
    String returnCode;
    LocalDateTime requestDate;
    String requestNote;
    String returnReason;
    String customerEvidenceUrl;
    String returnType;
    String requestScope;
    BigDecimal refundAmount;
    String refundMethod;
    String refundAccountNumber;
    String refundAccountName;
    String refundReferenceCode;
    String staffRefundEvidenceUrl;
    String status;
    Long approvedById;
    LocalDateTime approvedDate;
    Long processedById;
    LocalDateTime processedDate;
    String rejectReason;

    // Danh sách các sản phẩm đổi trả
    List<ReturnExchangeItemResponse> items;
}