package com.swp391.eyewear_management_backend.service;

import com.swp391.eyewear_management_backend.dto.response.SupplierResponse;
import java.util.List;

public interface SupplierService {
    List<SupplierResponse> getAllSuppliers();
}