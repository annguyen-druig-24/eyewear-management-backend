package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionRepo extends JpaRepository<Promotion, Long> {

    @Query("""
      select p from Promotion p
      where p.isActive = true
        and p.startDate <= :now
        and p.endDate >= :now
        and (p.usageLimit is null or p.usedCount < p.usageLimit)
    """)
    List<Promotion> findAvailableNow(LocalDateTime now);

    @Modifying
    @Query("""
        update Promotion p
        set p.usedCount = p.usedCount + 1
        where p.promotionID = :promotionId
    """)
    void incrementUsedCount(Long promotionId);
}
