package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepo extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProduct_ProductID(Long productId);
    
    List<ProductImage> findByProduct_ProductIDAndIsAvatar(Long productId, Boolean isAvatar);
}
