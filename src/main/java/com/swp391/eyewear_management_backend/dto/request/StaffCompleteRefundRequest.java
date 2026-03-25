package com.swp391.eyewear_management_backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCompleteRefundRequest {

//    @NotNull(message = "Refund amount is required")
//    @DecimalMin(value = "0.0", inclusive = false, message = "Refund amount must be greater than 0")
    @DecimalMin(value = "0.0", inclusive = true, message = "Refund amount must be greater than or equal to 0")
    private BigDecimal refundAmount;

    private String refundReferenceCode;

    private String processedNote;
}
