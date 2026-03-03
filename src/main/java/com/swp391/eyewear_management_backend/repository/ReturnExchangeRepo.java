package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.ReturnExchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnExchangeRepo extends JpaRepository<ReturnExchange, Long> {
    
    /**
     * Tìm return/exchange theo return code
     */
    Optional<ReturnExchange> findByReturnCode(String returnCode);
    
    /**
     * Lấy tất cả return/exchange của một user
     */
    List<ReturnExchange> findByUser_UserId(Long userId);
    
    /**
     * Lấy tất cả return/exchange theo status
     */
    List<ReturnExchange> findByStatus(String status);
    
    /**
     * Kiểm tra xem Order Detail đã có return/exchange chưa
     */
    boolean existsByOrderDetail_OrderDetailID(Long orderDetailId);
}
