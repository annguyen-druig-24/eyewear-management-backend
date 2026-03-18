package com.swp391.eyewear_management_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerCancelOrderResponse {
    private Long orderId;
    private String orderCode;
    private String orderStatus;
    private String cancelScenario;
    private Boolean refundRequired;
    private BigDecimal refundAmount;
    private Long returnExchangeId;
    private String returnExchangeCode;
    private String returnExchangeStatus;
}
