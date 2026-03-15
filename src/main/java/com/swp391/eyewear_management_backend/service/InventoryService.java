package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.response.ProductInventoryResponse;

import java.util.List;

public interface InventoryService {
    List<ProductInventoryResponse> getAllProductsWithInventoryQuantity();
}
