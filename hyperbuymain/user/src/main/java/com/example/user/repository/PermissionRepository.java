package com.example.user.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.user.entity.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
     boolean existsByName(String name);
    Optional<Permission> findByName(String name);
}
