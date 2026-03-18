package com.example.product.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RatingRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer ratingValue;

    private String comment;
}
