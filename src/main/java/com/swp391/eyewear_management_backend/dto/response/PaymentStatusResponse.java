package com.swp391.eyewear_management_backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentStatusResponse {
    Long paymentId;
    String paymentPurpose;     // DEPOSIT/FULL/REMAINING
    String paymentMethod;      // VNPAY/MOMO/COD
    String status;             // PENDING/SUCCESS/FAILED/REFUNDED
    BigDecimal amount;
    LocalDateTime paymentDate;
}