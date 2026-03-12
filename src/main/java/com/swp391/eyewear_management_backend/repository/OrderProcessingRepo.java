package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.OrderProcessing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderProcessingRepo extends JpaRepository<OrderProcessing, Long> {
    Optional<OrderProcessing> findFirstByOrderOrderIDAndNoteStartingWithOrderByOrderProcessingIDDesc(Long orderId, String notePrefix);
}
