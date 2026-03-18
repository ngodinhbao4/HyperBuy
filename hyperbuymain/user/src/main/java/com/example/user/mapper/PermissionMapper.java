package com.example.user.mapper;

import org.mapstruct.Mapper;
import com.example.user.dto.request.PermissionRequest;
import com.example.user.dto.response.PermissionResponse;
import com.example.user.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
