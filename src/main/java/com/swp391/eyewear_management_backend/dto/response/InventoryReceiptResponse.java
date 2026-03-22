package com.swp391.eyewear_management_backend.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryReceiptResponse {
    private Long inventoryReceiptId;
    private String receiptCode;

    // Thay vì chỉ trả ID, ta trả luôn tên để Frontend dễ hiển thị lên bảng
    private Long supplierId;
    private String supplierName;

    private Integer createdById;
    private String createdByName;

    private LocalDateTime orderDate;
    private LocalDateTime receivedDate;
    private String status;
    private BigDecimal totalAmount;
    private String note;
}
