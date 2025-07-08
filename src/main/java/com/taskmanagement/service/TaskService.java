package com.taskmanagement.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.repository.TaskRepository;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    public List<Task> getTasksByCreator(User user) {
        return taskRepository.findByCreator(user);
    }

    public List<Task> getTasksByAssignee(User user) {
        return taskRepository.findByAssignee(user);
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Task not found"));
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    public Task updateTask(Task task) {
        return taskRepository.save(task);
    }

    public Task updateTaskAssignees(Long taskId, Set<User> assignees) {
        Task task = getTaskById(taskId);
        task.setAssignees(assignees);
        return taskRepository.save(task);
    }

    public List<Task> getOverdueTasks() {
        return taskRepository.findOverdueTasks();
    }
} 