package com.example.order.service;

import com.example.order.dto.response.SellerRevenueStatResponse;
import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public boolean hasUserPurchasedProduct(String username, Long productId) {
        return orderRepository.existsByUserIdAndStatusAndItemsProductId(
                username,
                OrderStatus.DELIVERED,
                productId
        );
    }

    private Set<OrderStatus> completedStatuses() {
        return EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CONFIRMED);
    }

    private LocalDateTime atStart(LocalDate d) {
        return d.atStartOfDay();
    }

    private LocalDateTime atEnd(LocalDate d) {
        return d.plusDays(1).atStartOfDay().minusNanos(1);
    }

    // ===== DAILY =====
    public List<SellerRevenueStatResponse> getDailyRevenue(
            String storeId, LocalDate start, LocalDate end
    ) {
        List<Object[]> rows = orderItemRepository.getDailyStats(
                storeId, completedStatuses(), atStart(start), atEnd(end)
        );

        return rows.stream()
                .map(this::mapRowToDto)
                .collect(Collectors.toList());
    }

    // ===== MONTHLY =====
    public List<SellerRevenueStatResponse> getMonthlyRevenue(
            String storeId, LocalDate start, LocalDate end
    ) {
        List<Object[]> rows = orderItemRepository.getMonthlyStats(
                storeId, completedStatuses(), atStart(start), atEnd(end)
        );

        return rows.stream()
                .map(this::mapRowToDto)
                .collect(Collectors.toList());
    }

    // ===== YEARLY =====
    public List<SellerRevenueStatResponse> getYearlyRevenue(
            String storeId, LocalDate start, LocalDate end
    ) {
        List<Object[]> rows = orderItemRepository.getYearlyStats(
                storeId, completedStatuses(), atStart(start), atEnd(end)
        );

        return rows.stream()
                .map(this::mapRowToDto)
                .collect(Collectors.toList());
    }

    // ===== Hàm dùng chung để map 1 dòng Object[] -> DTO =====
    private SellerRevenueStatResponse mapRowToDto(Object[] row) {
        // index 0: DATE(o.orderDate) => có thể là java.sql.Date hoặc LocalDate tuỳ driver
        Object dateObj = row[0];
        LocalDate periodDate;
        if (dateObj instanceof LocalDate ld) {
            periodDate = ld;
        } else if (dateObj instanceof Date d) {
            periodDate = d.toLocalDate();
        } else {
            // fallback (hiếm khi cần)
            periodDate = LocalDate.parse(dateObj.toString());
        }

        Long orderCount = row[1] == null ? 0L : ((Number) row[1]).longValue();
        Long totalItems = row[2] == null ? 0L : ((Number) row[2]).longValue();
        BigDecimal totalRevenue = row[3] == null ? BigDecimal.ZERO : (BigDecimal) row[3];

        return new SellerRevenueStatResponse(periodDate, orderCount, totalItems, totalRevenue);
    }
}
