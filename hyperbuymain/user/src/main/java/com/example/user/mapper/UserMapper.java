package com.example.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.example.user.dto.request.UserCreationRequest;
import com.example.user.dto.request.UserUpdateRequest;
import com.example.user.dto.response.UserResponse;
import com.example.user.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(UserCreationRequest request);

    // ❌ KHÔNG map store ở đây nữa
    @Mapping(target = "store", ignore = true)
    UserResponse toUserResponse(User user);

    @Mapping(target = "role", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
