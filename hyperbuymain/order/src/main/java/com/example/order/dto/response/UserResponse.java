package com.example.order.dto.response;

import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String username;
    String name;
    String email;
    LocalDate dob;
    StoreResponse store;   // 🟢 QUAN TRỌNG: thêm field này
}
