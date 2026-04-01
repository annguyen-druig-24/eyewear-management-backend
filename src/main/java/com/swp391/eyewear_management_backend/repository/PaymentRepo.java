package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;


/*
    - Query khóa bản ghi (`PESSIMISTIC_WRITE`) để callback idempotent/an toàn cạnh tranh.
*/

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p
        from Payment p
        where p.order.orderID = :orderId
          and upper(p.paymentPurpose) = upper(:paymentPurpose)
          and upper(p.status) = upper(:status)
    """)
    List<Payment> findByOrderIdAndPurposeAndStatusForUpdate(
            @Param("orderId") Long orderId,
            @Param("paymentPurpose") String paymentPurpose,
            @Param("status") String status
    );
}
