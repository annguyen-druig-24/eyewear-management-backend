package com.swp391.eyewear_management_backend.controller;

import com.swp391.eyewear_management_backend.dto.response.ApiResponse;
import com.swp391.eyewear_management_backend.dto.response.ProductInventoryResponse;
import com.swp391.eyewear_management_backend.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:3000")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/products")
    @PreAuthorize("hasAnyAuthority('ROLE_OPERATIONS STAFF','ROLE_SALES STAFF','ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<ProductInventoryResponse>>> getAllProductsWithInventoryQuantity() {
        return ResponseEntity.ok(ApiResponse.<List<ProductInventoryResponse>>builder()
                .message("OK")
                .result(inventoryService.getAllProductsWithInventoryQuantity())
                .build());
    }
}
