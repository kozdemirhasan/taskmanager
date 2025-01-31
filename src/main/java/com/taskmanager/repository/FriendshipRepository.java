package com.taskmanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.taskmanager.entity.Friendship;
import com.taskmanager.entity.User;
import com.taskmanager.enums.FriendshipStatus;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    @Query("SELECT DISTINCT f FROM Friendship f LEFT JOIN FETCH f.sender LEFT JOIN FETCH f.receiver WHERE f.receiver = :receiver AND f.status = :status")
    List<Friendship> findByReceiverAndStatus(@Param("receiver") User receiver, @Param("status") FriendshipStatus status);
    
    @Query("SELECT DISTINCT f FROM Friendship f LEFT JOIN FETCH f.sender LEFT JOIN FETCH f.receiver WHERE f.sender = :sender AND f.status = :status")
    List<Friendship> findBySenderAndStatus(@Param("sender") User sender, @Param("status") FriendshipStatus status);
    
    @Query("SELECT DISTINCT f FROM Friendship f LEFT JOIN FETCH f.sender LEFT JOIN FETCH f.receiver WHERE (f.sender = :user OR f.receiver = :user) AND f.status = :status")
    List<Friendship> findBySenderOrReceiverAndStatus(@Param("user") User user, @Param("status") FriendshipStatus status);
    
    boolean existsBySenderAndReceiverAndStatus(User sender, User receiver, FriendshipStatus status);
} 