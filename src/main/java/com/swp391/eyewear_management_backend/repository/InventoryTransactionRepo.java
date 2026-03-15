package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepo extends JpaRepository<InventoryTransaction, Long> {
}
