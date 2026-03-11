package com.swp391.eyewear_management_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOrderListResponse {
    private Long orderId;
    private String orderCode;
    private String customerName;
    private String orderType;
    private String orderStatus;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private Long returnExchangeId;
    private String returnType;
}