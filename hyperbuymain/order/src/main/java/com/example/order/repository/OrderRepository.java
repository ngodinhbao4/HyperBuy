package com.example.order.repository; 

import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(String userId); // Đảm bảo userId là String
    boolean existsByUserIdAndStatusAndItemsProductId(
            String userId,
            OrderStatus status,
            Long productId
    );


        @Query("""
        SELECT DISTINCT o
        FROM Order o
        JOIN o.items i
        WHERE i.storeId = :storeId
          AND (:status IS NULL OR o.status = :status)
        ORDER BY o.orderDate DESC
    """)
    List<Order> findByStoreIdAndStatus(
            @Param("storeId") String storeId,
            @Param("status") OrderStatus status
    );

    
}