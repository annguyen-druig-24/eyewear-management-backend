package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepo extends JpaRepository<OrderDetail, Long> {
    
    /**
     * Lấy tất cả OrderDetail của một Order
     */
    List<OrderDetail> findByOrder_OrderID(Long orderId);
}
