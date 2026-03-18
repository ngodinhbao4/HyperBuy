package com.example.voucher.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voucher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private Double discountValue; // % hoặc số tiền

    @Column(nullable = false, length = 10)
    private String discountType; // PERCENT / AMOUNT

    private Integer quantity = 0    ; // tổng số lượng phát hành

    private Integer used; // số lượng đã sử dụng

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    private Status status; // ACTIVE / EXPIRED / INACTIVE

    @PrePersist
    public void prePersist() {
        used = 0;
        if (status == null) status = Status.ACTIVE;
    }

    public enum Status {
        ACTIVE, INACTIVE, EXPIRED
    }

        @Column(nullable = false)
    private Integer pointCost;
}
