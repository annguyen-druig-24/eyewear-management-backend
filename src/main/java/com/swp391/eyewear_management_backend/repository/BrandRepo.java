package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandRepo extends JpaRepository<Brand, Long> {
    Optional<Brand> findByBrandName(String brandName); // Lưu ý tên field trong entity của bạn
}
