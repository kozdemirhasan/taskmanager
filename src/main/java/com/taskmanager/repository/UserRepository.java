package com.taskmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taskmanager.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
} 