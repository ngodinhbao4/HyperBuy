package com.example.user.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.user.dto.request.RoleRequest;
import com.example.user.dto.response.RoleResponse;
import com.example.user.entity.Role;
import com.example.user.exception.AppException;
import com.example.user.exception.ErrorCode;
import com.example.user.mapper.RoleMapper;
import com.example.user.repository.PermissionRepository;
import com.example.user.repository.RoleRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;
    
    public RoleResponse create(RoleRequest request){
        var role = roleMapper.toRole(request);
        
        var permission =  permissionRepository.findAllById(request.getPermission());
        role.setPermission(new HashSet<>(permission));

        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    public List<RoleResponse> getAll(){
        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toRoleResponse)
                .toList();
    }

    public void delete(String role){
        roleRepository.deleteById(role);
    }

    public Role findByName(String name) {
    return roleRepository.findByName(name)
            .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
}
}
