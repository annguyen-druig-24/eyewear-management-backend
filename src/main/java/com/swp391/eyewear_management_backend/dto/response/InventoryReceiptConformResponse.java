package com.swp391.eyewear_management_backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InventoryReceiptConformResponse {
    private Long inventoryReceiptId;
    private String receiptCode;

    // 1. Thông tin Nhà cung cấp (Supplier)
    private Long supplierId;
    private String supplierName;
    private String supplierPhone;
    private String supplierAddress;

    // 2. Thông tin Người tạo đơn (User)
    private Long createdById;
    private String createdByName;

    // 3. Thông tin Phiếu nhập
    private LocalDateTime orderDate;
    private LocalDateTime receivedDate;
    private String status;
    private BigDecimal totalAmount;
    private String note;

    // 4. Danh sách các sản phẩm trong phiếu nhập
    private List<InventoryReceiptDetailResponse> details;
}