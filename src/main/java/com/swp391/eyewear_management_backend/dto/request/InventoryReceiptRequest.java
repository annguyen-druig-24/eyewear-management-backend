package com.swp391.eyewear_management_backend.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class InventoryReceiptRequest {
    private Long supplierId;
    private String note;
    private List<ReceiptDetailRequest> details; // Danh sách sản phẩm nhập
}
