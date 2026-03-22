package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.InventoryReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryReceiptRepository extends JpaRepository<InventoryReceipt, Long> {
    // Mặc định JpaRepository đã cung cấp findAll() để lấy danh sách

    // Spring sẽ tự động hiểu hàm này: Trả về true nếu mã đã tồn tại trong DB, false nếu chưa có
    boolean existsByReceiptCode(String receiptCode);
}
