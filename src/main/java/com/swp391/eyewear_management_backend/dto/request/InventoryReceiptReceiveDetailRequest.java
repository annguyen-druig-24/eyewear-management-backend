package com.swp391.eyewear_management_backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryReceiptReceiveDetailRequest {
    private Long productId;
    private Long receiptDetailId;
    private Integer receivedQuantity;
    private BigDecimal unitCost;
    private BigDecimal totalPrice;
    private String note;
}
