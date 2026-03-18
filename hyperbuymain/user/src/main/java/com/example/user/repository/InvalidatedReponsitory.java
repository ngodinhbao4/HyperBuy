package com.example.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.user.entity.InvalidatedToken;

public interface InvalidatedReponsitory extends JpaRepository<InvalidatedToken, String> {

}
