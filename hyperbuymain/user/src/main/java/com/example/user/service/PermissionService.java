package com.example.user.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.user.dto.request.PermissionRequest;
import com.example.user.dto.response.PermissionResponse;
import com.example.user.entity.Permission;
import com.example.user.mapper.PermissionMapper;
import com.example.user.repository.PermissionRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PermissionService {
    PermissionRepository permissionRespository;
    PermissionMapper permissionMapper;

    public PermissionResponse create(PermissionRequest request){
        Permission permision = permissionMapper.toPermission(request);
        permision = permissionRespository.save(permision);
        return permissionMapper.toPermissionResponse(permision);
    }

    public List<PermissionResponse> getAll(){
        var permision = permissionRespository.findAll();
        return permision.stream().map(permissionMapper::toPermissionResponse).toList();
    }

    public void delete (String permission){
        permissionRespository.deleteById(permission); 
    }
}
