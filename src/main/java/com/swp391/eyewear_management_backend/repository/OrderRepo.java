package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderRepo extends JpaRepository<Order, Long> {

    @Query("""
        select distinct o
        from Order o
        join fetch o.user u
        left join fetch o.invoice i
        left join fetch o.payments p
        left join fetch o.shippingInfo s
        where o.orderID = :orderId
    """)
    Optional<Order> findByIdFetchStatus(Long orderId);
}
