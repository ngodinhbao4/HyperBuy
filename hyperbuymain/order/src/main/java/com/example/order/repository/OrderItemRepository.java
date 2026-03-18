package com.example.order.repository;

import com.example.order.entity.OrderItem;
import com.example.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
        SELECT 
            FUNCTION('DATE', o.orderDate),
            COUNT(DISTINCT o.id),
            SUM(oi.quantity),
            SUM(oi.subtotal)
        FROM OrderItem oi
        JOIN oi.order o
        WHERE oi.storeId = :storeId
        AND o.status IN :statuses
        AND o.orderDate BETWEEN :start AND :end
        GROUP BY FUNCTION('DATE', o.orderDate)
        ORDER BY FUNCTION('DATE', o.orderDate)
    """)
    List<Object[]> getDailyStats(
            @Param("storeId") String storeId,
            @Param("statuses") Set<OrderStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


        // ===== MONTHLY =====
        @Query("""
            SELECT 
                FUNCTION('DATE', MIN(o.orderDate)), 
                COUNT(DISTINCT o.id),                
                SUM(oi.quantity),                   
                SUM(oi.subtotal)                     
            FROM OrderItem oi
            JOIN oi.order o
            WHERE oi.storeId = :storeId
            AND o.status IN :statuses
            AND o.orderDate BETWEEN :start AND :end
            GROUP BY FUNCTION('YEAR', o.orderDate), FUNCTION('MONTH', o.orderDate)
            ORDER BY FUNCTION('YEAR', o.orderDate), FUNCTION('MONTH', o.orderDate)
        """)
        List<Object[]> getMonthlyStats(
                @Param("storeId") String storeId,
                @Param("statuses") Set<OrderStatus> statuses,
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end
        );

    // ===== YEARLY =====
    @Query("""
        SELECT 
            FUNCTION('DATE', MIN(o.orderDate)),  
            COUNT(DISTINCT o.id),                
            SUM(oi.quantity),                    
            SUM(oi.subtotal)                     
        FROM OrderItem oi
        JOIN oi.order o
        WHERE oi.storeId = :storeId
          AND o.status IN :statuses
          AND o.orderDate BETWEEN :start AND :end
        GROUP BY FUNCTION('YEAR', o.orderDate)
        ORDER BY FUNCTION('YEAR', o.orderDate)
    """)
    List<Object[]> getYearlyStats(
            @Param("storeId") String storeId,
            @Param("statuses") Set<OrderStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
