package com.example.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.user.entity.User;


@Repository
public interface UserRepository extends JpaRepository<User, String>{
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
    List<User> findByIsBannedTrue();
}
