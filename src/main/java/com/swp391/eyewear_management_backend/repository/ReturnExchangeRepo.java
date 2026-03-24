package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.dto.projection.ReturnExchangeOrderSummaryProjection;
import com.swp391.eyewear_management_backend.dto.projection.StaffReturnExchangeListProjection;
import com.swp391.eyewear_management_backend.entity.ReturnExchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    boolean existsByOrder_OrderIDAndReturnTypeAndRequestScopeAndStatusIn(
            Long orderId,
            String returnType,
            String requestScope,
            List<String> statuses);

    Optional<ReturnExchange> findTopByOrder_OrderIDAndReturnTypeIgnoreCaseAndRequestScopeIgnoreCaseOrderByRequestDateDesc(
            Long orderId,
            String returnType,
            String requestScope
    );

    List<ReturnExchange> findByOrder_OrderIDAndReturnTypeIgnoreCaseAndRequestScopeIgnoreCase(
            Long orderId,
            String returnType,
            String requestScope
    );

        List<ReturnExchange> findByOrder_OrderIDAndReturnTypeIgnoreCaseAndStatusIn(
            Long orderId,
            String returnType,
            List<String> statuses
        );

    @Query(value = """
        SELECT re.Return_Exchange_ID AS returnExchangeId,
               re.Return_Code AS returnCode,
               re.Order_ID AS orderId,
               o.Order_Code AS orderCode,
               o.Order_Date AS orderDate,
               o.Order_Status AS orderStatus,
               u.Name AS customerName,
               u.Phone AS customerPhone,
               u.Email AS customerEmail,
               re.Return_Type AS returnType,
               re.Request_Scope AS requestScope,
               re.Request_Date AS requestDate,
               re.Status AS returnExchangeStatus,
               re.Refund_Amount AS refundAmount,
               re.Refund_Method AS refundMethod,
               re.Refund_Account_Number AS refundAccountNumber,
               re.Refund_Account_Name AS refundAccountName,
               re.Request_Note AS requestNote,
               re.Reject_Reason AS rejectReason,
               re.Approved_Date AS approvedDate,
               re.Processed_Date AS processedDate
        FROM Return_Exchange re
        JOIN [Order] o ON o.Order_ID = re.Order_ID
        JOIN [User] u ON u.User_ID = re.User_ID
        WHERE NOT (
            (UPPER(re.Return_Type) = 'REFUND'
            AND UPPER(re.Request_Scope) = 'ORDER'
            AND re.Refund_Amount IS NOT NULL
            AND re.Refund_Amount > 0
            AND UPPER(o.Order_Status) = 'CANCELED')
            OR UPPER(re.Return_Type) = 'CANCEL_ORDER'
        )
        ORDER BY re.Request_Date DESC
    """, nativeQuery = true)
    List<StaffReturnExchangeListProjection> findStaffReturnExchangeSummaries();

    @Query(value = """
        SELECT re.Return_Exchange_ID AS returnExchangeId,
               re.Return_Code AS returnCode,
               re.Order_ID AS orderId,
               o.Order_Code AS orderCode,
               o.Order_Date AS orderDate,
               o.Order_Status AS orderStatus,
               u.Name AS customerName,
               u.Phone AS customerPhone,
               u.Email AS customerEmail,
               re.Return_Type AS returnType,
               re.Request_Scope AS requestScope,
               re.Request_Date AS requestDate,
               re.Status AS returnExchangeStatus,
               re.Refund_Amount AS refundAmount,
               re.Refund_Method AS refundMethod,
               re.Refund_Account_Number AS refundAccountNumber,
               re.Refund_Account_Name AS refundAccountName,
               re.Request_Note AS requestNote,
               re.Reject_Reason AS rejectReason,
               re.Approved_Date AS approvedDate,
               re.Processed_Date AS processedDate
        FROM Return_Exchange re
        JOIN [Order] o ON o.Order_ID = re.Order_ID
        JOIN [User] u ON u.User_ID = re.User_ID
        WHERE UPPER(re.Return_Type) = 'REFUND'
          AND UPPER(re.Request_Scope) = 'ORDER'
          AND re.Refund_Amount IS NOT NULL
          AND re.Refund_Amount > 0
          AND UPPER(o.Order_Status) = 'CANCELED'
        ORDER BY re.Request_Date DESC
    """, nativeQuery = true)
    List<StaffReturnExchangeListProjection> findCancelRefundRequestsForSalesStaff();

    @Query(value = """
        SELECT re.Order_ID AS orderId,
               re.Return_Exchange_ID AS returnExchangeId,
               re.Status AS status,
               re.Return_Type AS returnType
        FROM Return_Exchange re
        WHERE re.Order_ID IN (:orderIds)
          AND re.Request_Date = (
              SELECT MAX(re2.Request_Date)
              FROM Return_Exchange re2
              WHERE re2.Order_ID = re.Order_ID
          )
    """, nativeQuery = true)
    List<ReturnExchangeOrderSummaryProjection> findLatestSummariesByOrderIds(@Param("orderIds") List<Long> orderIds);

//    @Query("SELECT COUNT(re) > 0 FROM ReturnExchange re JOIN re.items i WHERE i.orderDetail.orderDetailID = :orderDetailId")
//    boolean existsByOrderDetailId(@org.springframework.data.repository.query.Param("orderDetailId") Long orderDetailId);

    Optional<ReturnExchange> findTopByReturnCodeStartingWithOrderByReturnCodeDesc(String prefix);

}
