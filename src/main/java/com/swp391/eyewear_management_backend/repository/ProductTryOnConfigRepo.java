package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.ProductTryOnConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductTryOnConfigRepo extends JpaRepository<ProductTryOnConfig, Long> {
    Optional<ProductTryOnConfig> findByProduct_ProductID(Long productId);
}