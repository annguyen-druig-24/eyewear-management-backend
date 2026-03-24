package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.request.BrandDto;
import com.swp391.eyewear_management_backend.dto.request.CreateSupplierBrandRequest;
import com.swp391.eyewear_management_backend.dto.response.SupplierResponse;
import java.util.List;

public interface SupplierService {
    List<SupplierResponse> getAllSuppliers();

    void createSupplierWithBrands(CreateSupplierBrandRequest request);

    // Thêm hàm mới này
    void addBrandsToExistingSupplier(Long supplierId, List<BrandDto> brands);
}