package com.swp391.eyewear_management_backend.service.impl;

import com.swp391.eyewear_management_backend.dto.response.SupplierResponse;
import com.swp391.eyewear_management_backend.entity.Supplier;
import com.swp391.eyewear_management_backend.repository.SupplierRepository;
import com.swp391.eyewear_management_backend.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    public List<SupplierResponse> getAllSuppliers() {
        List<Supplier> suppliers = supplierRepository.findAll();

        // Map từ Entity sang DTO
        return suppliers.stream().map(supplier -> SupplierResponse.builder()
                .id(supplier.getSupplierID())
                .name(supplier.getSupplierName()) // Thay đổi getter cho khớp với Entity của bạn
                .phone(supplier.getSupplierPhone())
                .email(null)
                .address(supplier.getSupplierAddress())
                .build()
        ).collect(Collectors.toList());
    }
}