package com.example.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SellerRevenueStatResponse {

    private LocalDate periodDate;
    private Long orderCount;
    private Long totalItems;
    private BigDecimal totalRevenue;

    public SellerRevenueStatResponse() {
    }

    public SellerRevenueStatResponse(LocalDate periodDate, Long orderCount, Long totalItems, BigDecimal totalRevenue) {
        this.periodDate = periodDate;
        this.orderCount = orderCount;
        this.totalItems = totalItems;
        this.totalRevenue = totalRevenue;
    }

    public LocalDate getPeriodDate() {
        return periodDate;
    }

    public void setPeriodDate(LocalDate periodDate) {
        this.periodDate = periodDate;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public Long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Long totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}
