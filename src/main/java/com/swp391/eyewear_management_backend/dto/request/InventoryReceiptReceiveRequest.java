package com.swp391.eyewear_management_backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class InventoryReceiptReceiveRequest {
    private Long inventoryReceiptId;
    private BigDecimal totalAmount;
    private List<InventoryReceiptReceiveDetailRequest> details;
}
