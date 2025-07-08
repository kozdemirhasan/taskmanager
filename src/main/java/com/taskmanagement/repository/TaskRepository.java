package com.taskmanagement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.assignees WHERE t.creator = :user")
    List<Task> findByCreator(@Param("user") User user);
    
    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.assignees WHERE :user MEMBER OF t.assignees")
    List<Task> findByAssignee(@Param("user") User user);
    
    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.assignees WHERE t.deadline < CURRENT_TIMESTAMP AND t.status = 'PENDING'")
    List<Task> findOverdueTasks();
    
    List<Task> findByOrderByPriorityDesc();
    List<Task> findByOrderByDeadlineAsc();
} 