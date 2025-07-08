package com.taskmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taskmanagement.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
} 