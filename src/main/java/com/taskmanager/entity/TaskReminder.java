package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.taskmanager.enums.ReminderType;

@Entity
@Data
@Table(name = "task_reminders")
public class TaskReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private Task task;
    
    @ManyToOne
    private User user;
    
    private LocalDateTime reminderTime;
    
    @Enumerated(EnumType.STRING)
    private ReminderType reminderType;
} 