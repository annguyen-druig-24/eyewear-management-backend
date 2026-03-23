package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.request.InventoryReceiptRequest;
import com.swp391.eyewear_management_backend.dto.request.InventoryReceiptReceiveRequest;
import com.swp391.eyewear_management_backend.dto.response.InventoryReceiptConformResponse;
import com.swp391.eyewear_management_backend.dto.response.InventoryReceiptResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductOfSupplierResponse;
import com.swp391.eyewear_management_backend.entity.Product;
import com.swp391.eyewear_management_backend.service.InventoryReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-receipts")
@RequiredArgsConstructor
@CrossOrigin("*") // Mở CORS nếu frontend chạy khác port (ví dụ React cổng 3000)
public class InventoryReceiptController {

    private final InventoryReceiptService receiptService;

    // 1. Lấy và show các mặt hàng của một supplier khi nhập ID
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    @GetMapping("/products/search")
    public ResponseEntity<List<ProductOfSupplierResponse>> getProductsBySupplierId(@RequestParam Long supplierId) {
        List<ProductOfSupplierResponse> products = receiptService.getProductsBySupplierId(supplierId);
        return ResponseEntity.ok(products);
    }

    // 2. Thêm/ tạo đơn hàng nhập kho
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<InventoryReceiptResponse> createReceipt(@RequestBody InventoryReceiptRequest request) {
        InventoryReceiptResponse response = receiptService.createInventoryReceipt(request);
        return ResponseEntity.ok(response);
    }

    // 3. Lấy các đơn hàng đã được đặt và trạng thái của nó
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<List<InventoryReceiptResponse>> getAllReceipts() {
        List<InventoryReceiptResponse> receipts = receiptService.getAllReceipts();
        return ResponseEntity.ok(receipts);
    }

    // 4. Lấy chi tiết đơn hàng nhập kho theo ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<InventoryReceiptConformResponse> getReceiptById(@PathVariable Long id) {
        InventoryReceiptConformResponse response = receiptService.getReceiptById(id);
        return ResponseEntity.ok(response);
    }

    // 5. Receive inventory receipt by actual quantity/amount and update stock
    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAnyAuthority('ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<InventoryReceiptConformResponse> receiveReceipt(
            @PathVariable Long id,
            @RequestBody InventoryReceiptReceiveRequest request) {
        InventoryReceiptConformResponse response = receiptService.receiveReceipt(id, request);
        return ResponseEntity.ok(response);
    }
}