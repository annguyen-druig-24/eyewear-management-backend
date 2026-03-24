package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.Brand;
import com.swp391.eyewear_management_backend.entity.BrandSupplier;
import com.swp391.eyewear_management_backend.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Nhớ import đúng package chứa entity BrandSupplier của bạn nhé

@Repository
public interface BrandSupplierRepo extends JpaRepository<BrandSupplier, Long> {
    boolean existsByBrandAndSupplier(Brand brand, Supplier supplier);
}
