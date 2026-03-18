package com.example.user.dto.response;

import java.time.LocalDate;
import java.util.Set;


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
    Set<RoleResponse> role;
    private StoreResponse store;
}
