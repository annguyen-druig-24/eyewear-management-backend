package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p
        from Payment p
        left join fetch p.order o
        where p.paymentID = :paymentId
    """)
    Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);
}
