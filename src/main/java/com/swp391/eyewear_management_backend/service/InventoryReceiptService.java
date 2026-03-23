package com.swp391.eyewear_management_backend.service;



import com.swp391.eyewear_management_backend.dto.request.InventoryReceiptRequest;
import com.swp391.eyewear_management_backend.dto.request.InventoryReceiptReceiveRequest;
import com.swp391.eyewear_management_backend.dto.response.InventoryReceiptConformResponse;
import com.swp391.eyewear_management_backend.dto.response.InventoryReceiptResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductOfSupplierResponse;
import com.swp391.eyewear_management_backend.entity.Product;

import java.util.List;

public interface InventoryReceiptService {
    List<ProductOfSupplierResponse> getProductsBySupplierId(Long supplierId);
    InventoryReceiptResponse createInventoryReceipt(InventoryReceiptRequest request);
    List<InventoryReceiptResponse> getAllReceipts();

    InventoryReceiptConformResponse getReceiptById(Long id);
    InventoryReceiptConformResponse receiveReceipt(Long id, InventoryReceiptReceiveRequest request);
}