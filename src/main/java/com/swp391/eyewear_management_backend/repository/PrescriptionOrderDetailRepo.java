package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.PrescriptionOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionOrderDetailRepo extends JpaRepository<PrescriptionOrderDetail, Long> {
}
