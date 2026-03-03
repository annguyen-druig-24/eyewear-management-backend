package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepo extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByOrderOrderID(Long orderId);
}
