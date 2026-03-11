package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.dto.projection.TopProductProjection;
import com.swp391.eyewear_management_backend.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface OrderDetailRepo extends JpaRepository<OrderDetail, Long> {
    
    @Query("""
        select distinct od
        from OrderDetail od
        join fetch od.product p
        left join fetch p.images pi
        where od.order.orderID = :orderId
    """)
    List<OrderDetail> findByOrderIdFetchProduct(@Param("orderId") Long orderId);


    // Lấy Top sản phẩm bán chạy nhất kèm Avatar Image
    @Query("SELECT p.productID AS id, p.productName AS name, p.price AS price, CAST(SUM(od.quantity) AS int) AS sold, " +
            "(SELECT pi.imageUrl FROM ProductImage pi WHERE pi.product.productID = p.productID AND pi.isAvatar = true) AS image " +
            "FROM OrderDetail od " +
            "JOIN od.product p " +
            "JOIN od.order o " +
            "WHERE o.orderStatus IN ('COMPLETED', 'PAID') " +
            "GROUP BY p.productID, p.productName, p.price " +
            "ORDER BY SUM(od.quantity) DESC")
    List<TopProductProjection> getTopSellingProducts(Pageable pageable);
}
