package com.swp391.eyewear_management_backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InventoryReceiptDetailResponse {
    private Long receiptDetailId;
    private Long productId;
    private String productName;
    private Integer orderedQuantity;
    private Integer receivedQuantity;
    private Integer rejectedQuantity;
    private BigDecimal unitCost;
    private BigDecimal vatRate;
    private BigDecimal totalPrice;
    private String productImage; // Lấy ảnh avatar của sản phẩm
}