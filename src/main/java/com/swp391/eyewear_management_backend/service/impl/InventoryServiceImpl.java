package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.response.ProductInventoryResponse;
import com.swp391.eyewear_management_backend.repository.ProductRepo;
import com.swp391.eyewear_management_backend.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private ProductRepo productRepo;

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_OPERATIONS STAFF', 'ROLE_SALES STAFF', 'ROLE_ADMIN','ROLE_MANAGER')")
    public List<ProductInventoryResponse> getAllProductsWithLatestInventoryQuantity() {
        return productRepo.findAllProductsWithLatestInventoryQuantity();
    }
}
