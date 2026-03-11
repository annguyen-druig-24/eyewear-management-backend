package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.dto.projection.OrderStatusProjection;
import com.swp391.eyewear_management_backend.dto.projection.RevenueChartProjection;
import com.swp391.eyewear_management_backend.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepo extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    /*
        * JpaSpecificationExecutor<T> là một interface của Spring Data JPA giúp repository có thể chạy query động dựa trên Specification<T> (dùng JPA Criteria API)
        * Giải bài toán filter/search động (optional nhiều điều kiện), user có thể nhập 1 trong 4 hoặc nhiều điều kiện: orderCode, orderDate, orderType, orderStatus
        * Nên dùng khi: Filter thay đổi theo UI (thêm/bớt tiêu chí thường xuyên), Cần kết hợp filter + sort + paging
     */

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

    Optional<Order> findByOrderCode(String orderCode);

    /*
        * @EntityGraph là cơ chế của JPA (JPA 2.1) để chỉ định “fetch plan” cho một query cụ thể: tức là lần query này hãy load thêm các quan hệ nào.
        * Công dụng chính: giảm N+1 query
        * Công dụng: Query list orders kèm user ngay từ đầu, Map customerName không phát sinh thêm query
     */
    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Order> findAll(@Nullable Specification<Order> spec, Pageable pageable);

    /**
     * Lấy tất cả orders có return/exchange request
     */
    @Query("""
        SELECT DISTINCT o
        FROM Order o
        JOIN FETCH o.user u
        JOIN o.orderDetails od
        WHERE od.returnExchange IS NOT NULL
    """)
    List<Order> findAllOrdersWithReturnExchange();



    // =========================================================================
    // 2. CÁC METHOD MỚI THÊM VÀO CHO TÍNH NĂNG DASHBOARD
    // =========================================================================

    /**
     * Lấy doanh thu trong một khoảng thời gian (Chỉ tính đơn đã hoàn thành hoặc đã thanh toán)
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus IN ('COMPLETED', 'PAID') AND o.orderDate >= :startDate AND o.orderDate <= :endDate")
    BigDecimal calculateRevenueBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Đếm số lượng đơn hàng theo trạng thái cụ thể (Ví dụ: PENDING)
     */
    int countByOrderStatus(String orderStatus);

    /**
     * Đếm số lượng đơn hàng theo trạng thái trong một khoảng thời gian (Ví dụ: Hoàn thành trong tháng)
     */
    int countByOrderStatusAndOrderDateBetween(String orderStatus, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Thống kê số lượng đơn hàng theo từng trạng thái cho Biểu đồ tròn (Pie Chart)
     */
    // 1. Thêm startDate và endDate cho Chart trạng thái
    @Query("SELECT o.orderStatus AS status, CAST(COUNT(o) AS int) AS count " +
            "FROM Order o " +
            "WHERE o.orderDate >= :startDate AND o.orderDate <= :endDate " +
            "GROUP BY o.orderStatus")
    List<OrderStatusProjection> getOrderStatusChart(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 2. Thêm endDate cho Chart doanh thu
    @Query(value = "SELECT FORMAT(Order_Date, 'dd/MM') AS label, COALESCE(SUM(Total_Amount), 0) AS revenue " +
            "FROM [Order] " +
            "WHERE Order_Status IN ('COMPLETED', 'PAID') " +
            "AND Order_Date >= :startDate AND Order_Date <= :endDate " +
            "GROUP BY FORMAT(Order_Date, 'dd/MM'), CAST(Order_Date AS date) " +
            "ORDER BY CAST(Order_Date AS date)", nativeQuery = true)
    List<RevenueChartProjection> getRevenueChartNative(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);


}