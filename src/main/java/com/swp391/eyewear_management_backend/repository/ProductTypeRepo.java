package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductTypeRepo extends JpaRepository<ProductType, Long> {
    Optional<ProductType> findByTypeName(String typeName);
}
