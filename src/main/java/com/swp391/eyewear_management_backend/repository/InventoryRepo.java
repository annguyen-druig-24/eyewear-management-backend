 package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepo extends JpaRepository<Inventory, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select i
        from Inventory i
        where i.product.productID = :productId
          and i.inventoryID = (
              select max(i2.inventoryID)
              from Inventory i2
              where i2.product.productID = :productId
          )
    """)
    Optional<Inventory> findLatestByProductIdForUpdate(@Param("productId") Long productId);
}
