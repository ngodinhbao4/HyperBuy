package com.hyperbuy.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "recommendation")
public class Recommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recid;

    private Long userId;
    private Long productId;
    private Double score;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
