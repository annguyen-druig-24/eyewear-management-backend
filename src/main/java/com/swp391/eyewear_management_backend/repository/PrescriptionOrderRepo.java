package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.PrescriptionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionOrderRepo extends JpaRepository<PrescriptionOrder, Long> {
    Optional<PrescriptionOrder> findByOrder_OrderID(Long orderID);
}
