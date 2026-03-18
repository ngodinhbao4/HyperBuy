package com.hyperbuy.recommendation.repository;

import com.hyperbuy.recommendation.model.ProductScore;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RawDataRepository extends Repository<Object, Long> {

    @Query(value = """
        SELECT 
            p.productid AS productId,
            (
                (COALESCE(order_freq, 0) * 0.5) +
                (COALESCE(avg_rating, 0) * 0.3) +
                (COALESCE(view_count, 0) * 0.2)
            ) AS score
        FROM product p
        LEFT JOIN (
            SELECT oi.productid, COUNT(*) AS order_freq
            FROM orderitem oi
            JOIN orders o ON o.orderid = oi.orderid
            WHERE o.userid = :userId
            GROUP BY oi.productid
        ) AS order_stats ON order_stats.productid = p.productid
        LEFT JOIN (
            SELECT r.productid, AVG(rating) AS avg_rating
            FROM review r
            GROUP BY r.productid
        ) AS review_stats ON review_stats.productid = p.productid
        LEFT JOIN (
            SELECT ua.productid, COUNT(*) AS view_count
            FROM useractivity ua
            WHERE ua.userid = :userId AND ua.activitytype = 'VIEW'
            GROUP BY ua.productid
        ) AS view_stats ON view_stats.productid = p.productid
        ORDER BY score DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> calculateProductScores(@Param("userId") Long userId);
}
