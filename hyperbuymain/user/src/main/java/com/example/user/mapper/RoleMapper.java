package com.example.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.user.dto.request.RoleRequest;
import com.example.user.dto.response.RoleResponse;
import com.example.user.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permission", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
