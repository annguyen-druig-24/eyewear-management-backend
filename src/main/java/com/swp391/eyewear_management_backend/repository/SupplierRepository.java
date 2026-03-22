package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    // JpaRepository đã cung cấp sẵn hàm findById(Long id)
    // Bạn không cần viết thêm gì ở đây trừ khi có câu truy vấn đặc biệt
}