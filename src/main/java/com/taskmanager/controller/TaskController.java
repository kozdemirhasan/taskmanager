package com.taskmanager.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskNote;
import com.taskmanager.entity.User;
import com.taskmanager.enums.TaskPriority;
import com.taskmanager.enums.TaskStatus;
import com.taskmanager.service.FriendshipService;
import com.taskmanager.service.TaskService;
import com.taskmanager.service.UserService;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private FriendshipService friendshipService;

    @GetMapping("/create")
    public String showCreateTaskForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByEmail(auth.getName());
        
        model.addAttribute("priorities", TaskPriority.values());
        model.addAttribute("friends", friendshipService.getFriendList(user));
        return "task/create";
    }

    @PostMapping("/create")
    @ResponseBody
    public String createTask(@RequestBody Map<String, Object> taskData) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            
            Task task = new Task();
            task.setTitle((String) taskData.get("title"));
            task.setDescription((String) taskData.get("description"));
            task.setPriority(TaskPriority.valueOf((String) taskData.get("priority")));
            task.setDeadline(LocalDateTime.parse((String) taskData.get("deadline")));
            task.setCreator(user);
            task.setStatus(TaskStatus.PENDING);
            
            // Görevlileri ekle
            if (taskData.get("assigneeIds") != null) {
                Set<User> assignees = new HashSet<>();
                @SuppressWarnings("unchecked")
                List<String> assigneeIds = (List<String>) taskData.get("assigneeIds");
                for (String assigneeId : assigneeIds) {
                    User assignee = userService.findById(Long.parseLong(assigneeId));
                    if (assignee != null) {
                        assignees.add(assignee);
                    }
                }
                task.setAssignees(assignees);
            }
            
            taskService.createTask(task);
            return "Görev başarıyla oluşturuldu";
        } catch (Exception e) {
            e.printStackTrace();
            return "Hata: " + e.getMessage();
        }
    }

    @GetMapping("/my")
    public String listMyTasks(Model model, 
                             @RequestParam(required = false) String statusFilter, 
                             @RequestParam(required = false) String priorityFilter) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName());
        
        List<Task> assignedTasks = taskService.getTasksByAssignee(currentUser);
        List<Task> createdTasks = taskService.getTasksByCreator(currentUser);
        
        // Durum filtresi uygula
        if (statusFilter == null) {
            // Sayfa ilk açıldığında varsayılan olarak PENDING görevleri göster
            statusFilter = TaskStatus.PENDING.name();
            assignedTasks = assignedTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING)
                .collect(Collectors.toList());
            createdTasks = createdTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING)
                .collect(Collectors.toList());
        } else if ("OVERDUE".equals(statusFilter)) {
            // Süresi geçmiş görevleri filtrele
            LocalDateTime now = LocalDateTime.now();
            assignedTasks = assignedTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING && task.getDeadline().isBefore(now))
                .collect(Collectors.toList());
            createdTasks = createdTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING && task.getDeadline().isBefore(now))
                .collect(Collectors.toList());
        } else if (!statusFilter.isEmpty()) {
            // Belirli bir durum seçildiğinde o duruma göre filtrele
            TaskStatus status = TaskStatus.valueOf(statusFilter);
            assignedTasks = assignedTasks.stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
            createdTasks = createdTasks.stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
        }
        // Boş string geldiğinde (Tümü seçildiğinde) hiç filtreleme yapma
        
        // Öncelik filtresi uygula
        if (priorityFilter != null && !priorityFilter.isEmpty()) {
            TaskPriority priority = TaskPriority.valueOf(priorityFilter);
            assignedTasks = assignedTasks.stream()
                .filter(task -> task.getPriority() == priority)
                .collect(Collectors.toList());
            createdTasks = createdTasks.stream()
                .filter(task -> task.getPriority() == priority)
                .collect(Collectors.toList());
        }
        
        // Görevleri son teslim tarihine göre sırala
        Comparator<Task> deadlineComparator = (t1, t2) -> {
            if (t1.getDeadline() == null) return 1;
            if (t2.getDeadline() == null) return -1;
            return t1.getDeadline().compareTo(t2.getDeadline());
        };
        
        assignedTasks.sort(deadlineComparator);
        createdTasks.sort(deadlineComparator);
        
        model.addAttribute("assignedTasks", assignedTasks);
        model.addAttribute("createdTasks", createdTasks);
        model.addAttribute("priorities", TaskPriority.values());
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("currentStatusFilter", statusFilter);
        model.addAttribute("currentPriorityFilter", priorityFilter);
        model.addAttribute("currentUser", currentUser);
        return "task/list";
    }

    @PostMapping("/{id}/complete")
    @ResponseBody
    public String completeTask(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            Task task = taskService.getTaskById(id);
            
            if (!task.getAssignees().contains(user) && !task.getCreator().equals(user)) {
                return "Hata: Bu görevi tamamlama yetkiniz yok";
            }
            
            task.complete(user);
            taskService.updateTask(task);
            return "Görev başarıyla tamamlandı";
        } catch (Exception e) {
            return "Hata: " + e.getMessage();
        }
    }

    @PostMapping("/{id}/cancel")
    @ResponseBody
    public String cancelTask(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            Task task = taskService.getTaskById(id);
            
            if (!task.getCreator().equals(user)) {
                return "Hata: Bu görevi iptal etme yetkiniz yok";
            }
            
            task.setStatus(TaskStatus.CANCELLED);
            taskService.updateTask(task);
            return "Görev iptal edildi";
        } catch (Exception e) {
            return "Hata: " + e.getMessage();
        }
    }

    @PostMapping("/{id}/note")
    @ResponseBody
    public String addTaskNote(@PathVariable Long id, @RequestBody Map<String, String> noteData) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            Task task = taskService.getTaskById(id);
            
            if (!task.getAssignees().contains(user) && !task.getCreator().equals(user)) {
                return "Hata: Bu göreve açıklama ekleme yetkiniz yok";
            }
            
            task.addNote(user, noteData.get("note"));
            taskService.updateTask(task);
            return "Açıklama başarıyla eklendi";
        } catch (Exception e) {
            return "Hata: " + e.getMessage();
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditTaskForm(@PathVariable Long id, Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            Task task = taskService.getTaskById(id);
            
            if (!task.getCreator().equals(user)) {
                return "redirect:/tasks/my?error=unauthorized";
            }
            
            model.addAttribute("task", task);
            model.addAttribute("priorities", TaskPriority.values());
            model.addAttribute("friends", friendshipService.getFriendList(user));
            return "task/edit";
        } catch (Exception e) {
            return "redirect:/tasks/my?error=notfound";
        }
    }

    @PostMapping("/edit/{id}")
    @ResponseBody
    public String updateTask(@PathVariable Long id, @RequestBody Map<String, Object> taskData) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            Task task = taskService.getTaskById(id);
            
            if (!task.getCreator().equals(user)) {
                return "Hata: Bu görevi düzenleme yetkiniz yok";
            }
            
            task.setTitle((String) taskData.get("title"));
            task.setDescription((String) taskData.get("description"));
            task.setPriority(TaskPriority.valueOf((String) taskData.get("priority")));
            task.setDeadline(LocalDateTime.parse((String) taskData.get("deadline")));
            task.setStatus(TaskStatus.PENDING);
            task.setCompletedBy(null);
            task.setCompletedAt(null);
            
            // Görevlileri güncelle
            Set<User> assignees = new HashSet<>();
            if (taskData.get("assigneeIds") != null) {
                @SuppressWarnings("unchecked")
                List<String> assigneeIds = (List<String>) taskData.get("assigneeIds");
                for (String assigneeId : assigneeIds) {
                    User assignee = userService.findById(Long.parseLong(assigneeId));
                    if (assignee != null) {
                        assignees.add(assignee);
                    }
                }
            }
            task.setAssignees(assignees);
            
            taskService.updateTask(task);
            return "Görev başarıyla güncellendi";
        } catch (Exception e) {
            return "Hata: " + e.getMessage();
        }
    }

    @GetMapping("/{id}/history")
    @ResponseBody
    public Map<String, Object> getTaskHistory(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            Task task = taskService.getTaskById(id);
            
            // Kullanıcının görevi görüntüleme yetkisi var mı kontrol et
            if (!task.getCreator().equals(user) && !task.getAssignees().contains(user)) {
                throw new RuntimeException("Bu görevi görüntüleme yetkiniz yok");
            }
            
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("id", task.getId());
            taskMap.put("title", task.getTitle());
            taskMap.put("description", task.getDescription());
            taskMap.put("status", task.getStatus());
            taskMap.put("deadline", task.getDeadline());
            taskMap.put("createdAt", task.getCreatedAt());
            
            // Tamamlayan kişi bilgisini ekle
            if (task.getCompletedBy() != null) {
                Map<String, Object> completedByMap = new HashMap<>();
                completedByMap.put("firstName", task.getCompletedBy().getFirstName());
                completedByMap.put("lastName", task.getCompletedBy().getLastName());
                taskMap.put("completedBy", completedByMap);
                taskMap.put("completedAt", task.getCompletedAt());
            }
            
            // Notları ekle
            List<Map<String, Object>> notesMapList = new ArrayList<>();
            if (task.getNotes() != null) {
                for (TaskNote note : task.getNotes()) {
                    Map<String, Object> noteMap = new HashMap<>();
                    noteMap.put("note", note.getNote());
                    noteMap.put("createdAt", note.getCreatedAt());
                    
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("firstName", note.getUser().getFirstName());
                    userMap.put("lastName", note.getUser().getLastName());
                    noteMap.put("user", userMap);
                    
                    notesMapList.add(noteMap);
                }
            }
            
            // Görev durum değişikliklerini ekle
            List<Map<String, Object>> historyMapList = new ArrayList<>();
            
            // Tamamlanma bilgisini ekle
            if (task.getCompletedBy() != null) {
                Map<String, Object> completedMap = new HashMap<>();
                completedMap.put("type", "COMPLETED");
                completedMap.put("date", task.getCompletedAt());
                
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("firstName", task.getCompletedBy().getFirstName());
                userMap.put("lastName", task.getCompletedBy().getLastName());
                completedMap.put("user", userMap);
                
                historyMapList.add(completedMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("task", taskMap);
            response.put("notes", notesMapList);
            response.put("history", historyMapList);
            
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }

    @GetMapping("/tasks/overdue")
    public ResponseEntity<List<Task>> getOverdueTasks() {
        return ResponseEntity.ok(taskService.getOverdueTasks());
    }
} 