package com.example.minigame.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpendPointsResponse {
    private boolean success;
    private String message;
    private Integer remainingPoints;
}
