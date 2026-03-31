package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepo extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByOrderOrderIDAndTransactionTypeIgnoreCase(Long orderId, String transactionType);
}
