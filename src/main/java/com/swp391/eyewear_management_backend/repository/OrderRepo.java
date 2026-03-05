package com.swp391.eyewear_management_backend.repository;

import com.swp391.eyewear_management_backend.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;

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
}