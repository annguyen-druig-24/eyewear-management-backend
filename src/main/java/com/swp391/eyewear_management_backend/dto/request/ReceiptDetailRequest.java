package com.swp391.eyewear_management_backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReceiptDetailRequest {
    private Long productId;
    private Integer quantity; // Số lượng nhập vào
    private BigDecimal unitCost; // Đơn giá nhập
    private BigDecimal totalPrice;
    private BigDecimal vatRate;
    private String note;
}
