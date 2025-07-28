package com.taskmanagement.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
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

import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.TaskNote;
import com.taskmanagement.entity.User;
import com.taskmanagement.enums.TaskPriority;
import com.taskmanagement.enums.TaskStatus;
import com.taskmanagement.service.FriendshipService;
import com.taskmanagement.service.TaskService;
import com.taskmanagement.service.UserService;

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
            
            // İlk not olarak oluşturan kişi bilgisini ekle
            String creationNote = String.format("Task created by %s %s", 
                user.getFirstName(), user.getLastName());
            TaskNote note = task.addNote(user, creationNote);
            note.setType(TaskNote.NoteType.CREATION);  // Not türünü belirt
            
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
            return "Task successfully created";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/my-tasks")
    public String listMyTasks(Model model,
                              @RequestParam(required = false) String statusFilter,
                              @RequestParam(required = false) String priorityFilter,
                              @RequestParam(required = false) String dateRangeFilter) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName());

//        List<Task> assignedTasks = taskService.getTasksByAssignee(currentUser);
        List<Task> createdTasks = taskService.getTasksByCreator(currentUser);

        // Varsayılan olarak PENDING seçili gelsin (PENDING_ALL yerine)
        if (statusFilter == null || statusFilter.isEmpty()) {
            statusFilter = "PENDING";
        }

        if ("PENDING".equals(statusFilter)) {
            // Önce bekleyen görevleri filtrele
//            assignedTasks = assignedTasks.stream()
//                    .filter(task -> task.getStatus() == TaskStatus.PENDING &&
//                            !task.getDeadline().isBefore(LocalDateTime.now()))
//                    .collect(Collectors.toList());
            createdTasks = createdTasks.stream()
                    .filter(task -> task.getStatus() == TaskStatus.PENDING &&
                            !task.getDeadline().isBefore(LocalDateTime.now()))
                    .collect(Collectors.toList());

            // Date Range filtresini uygula
            if (dateRangeFilter != null && !dateRangeFilter.isEmpty()) {
                LocalDateTime filterDate = null;

                switch (dateRangeFilter) {
                    case "TODAY":
                        filterDate = LocalDateTime.now().toLocalDate().atTime(23, 59, 59);
                        break;
                    case "THREE_DAYS":
                        filterDate = LocalDateTime.now().plusDays(3).toLocalDate().atTime(23, 59, 59);
                        break;
                    case "THIS_WEEK":
                        filterDate = LocalDateTime.now().plusWeeks(1).toLocalDate().atTime(23, 59, 59);
                        break;
                    case "NEXT_TWO_WEEKS":
                        filterDate = LocalDateTime.now().plusWeeks(2).toLocalDate().atTime(23, 59, 59);
                        break;
                    case "THIS_MONTH":
                        filterDate = LocalDateTime.now().plusMonths(1).toLocalDate().withDayOfMonth(1).minusDays(1).atTime(23, 59, 59);
                        break;
                }

                if (filterDate != null) {
                    final LocalDateTime finalFilterDate = filterDate;
//                    assignedTasks = assignedTasks.stream()
//                            .filter(task -> !task.getDeadline().isAfter(finalFilterDate))
//                            .collect(Collectors.toList());
                    createdTasks = createdTasks.stream()
                            .filter(task -> !task.getDeadline().isAfter(finalFilterDate))
                            .collect(Collectors.toList());
                }
            }
        } else if ("OVERDUE".equals(statusFilter)) {
            // Önce tüm süresi geçmiş görevleri filtrele
//            assignedTasks = assignedTasks.stream()
//                    .filter(task -> task.getStatus() == TaskStatus.PENDING &&
//                            task.getDeadline().isBefore(LocalDateTime.now()))
//                    .collect(Collectors.toList());
            createdTasks = createdTasks.stream()
                    .filter(task -> task.getStatus() == TaskStatus.PENDING &&
                            task.getDeadline().isBefore(LocalDateTime.now()))
                    .collect(Collectors.toList());

            // Date Range filtresini uygula
            if (dateRangeFilter != null && !dateRangeFilter.isEmpty()) {
                LocalDateTime filterDate = null;

                switch (dateRangeFilter) {
                    case "OVERDUE_TODAY":
                        filterDate = LocalDateTime.now().toLocalDate().atStartOfDay();
                        break;
                    case "OVERDUE_YESTERDAY":
                        filterDate = LocalDateTime.now().minusDays(1).toLocalDate().atStartOfDay();
                        break;
                    case "OVERDUE_THREE_DAYS":
                        filterDate = LocalDateTime.now().minusDays(3).toLocalDate().atStartOfDay();
                        break;
                    case "OVERDUE_WEEK":
                        filterDate = LocalDateTime.now().minusWeeks(1).toLocalDate().atStartOfDay();
                        break;
                    case "OVERDUE_MONTH":
                        filterDate = LocalDateTime.now().minusMonths(1).toLocalDate().atStartOfDay();
                        break;
                }

                if (filterDate != null) {
                    final LocalDateTime finalFilterDate = filterDate;
//                    assignedTasks = assignedTasks.stream()
//                            .filter(task -> task.getDeadline().isAfter(finalFilterDate))
//                            .collect(Collectors.toList());
                    createdTasks = createdTasks.stream()
                            .filter(task -> task.getDeadline().isAfter(finalFilterDate))
                            .collect(Collectors.toList());
                }
            }
        } else if (!statusFilter.isEmpty()) {
            // Diğer durumlar için (COMPLETED, CANCELLED)
            TaskStatus status = TaskStatus.valueOf(statusFilter);
//            assignedTasks = assignedTasks.stream()
//                    .filter(task -> task.getStatus() == status)
//                    .collect(Collectors.toList());
            createdTasks = createdTasks.stream()
                    .filter(task -> task.getStatus() == status)
                    .collect(Collectors.toList());
        }

        // Geriye dönük tarih filtresi için ortak mantık
        if (("OVERDUE".equals(statusFilter) || "COMPLETED".equals(statusFilter) || "CANCELLED".equals(statusFilter))
                && dateRangeFilter != null && !dateRangeFilter.isEmpty()) {
            LocalDateTime filterDate = null;

            switch (dateRangeFilter) {
                case "PAST_TODAY":
                    filterDate = LocalDateTime.now().toLocalDate().atStartOfDay();
                    break;
                case "PAST_YESTERDAY":
                    filterDate = LocalDateTime.now().minusDays(1).toLocalDate().atStartOfDay();
                    break;
                case "PAST_THREE_DAYS":
                    filterDate = LocalDateTime.now().minusDays(3).toLocalDate().atStartOfDay();
                    break;
                case "PAST_WEEK":
                    filterDate = LocalDateTime.now().minusWeeks(1).toLocalDate().atStartOfDay();
                    break;
                case "PAST_MONTH":
                    filterDate = LocalDateTime.now().minusMonths(1).toLocalDate().atStartOfDay();
                    break;
            }

            if (filterDate != null) {
                final LocalDateTime finalFilterDate = filterDate;
//                assignedTasks = assignedTasks.stream()
//                        .filter(task -> task.getDeadline().isAfter(finalFilterDate))
//                        .collect(Collectors.toList());
                createdTasks = createdTasks.stream()
                        .filter(task -> task.getDeadline().isAfter(finalFilterDate))
                        .collect(Collectors.toList());
            }
        }

        // Öncelik filtresi uygula (ALL seçeneğinde de çalışmalı)
        if (priorityFilter != null && !priorityFilter.isEmpty()) {
            TaskPriority priority = TaskPriority.valueOf(priorityFilter);
//            assignedTasks = assignedTasks.stream()
//                    .filter(task -> task.getPriority() == priority)
//                    .collect(Collectors.toList());
            createdTasks = createdTasks.stream()
                    .filter(task -> task.getPriority() == priority)
                    .collect(Collectors.toList());
        }

        // En sonda sırala:
        createdTasks = createdTasks.stream()
                .sorted(Comparator.comparing(Task::getDeadline))
                .collect(Collectors.toList());

//        model.addAttribute("assignedTasks", assignedTasks);
        model.addAttribute("createdTasks", createdTasks);
        model.addAttribute("priorities", TaskPriority.values());
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("currentStatusFilter", statusFilter);
        model.addAttribute("currentPriorityFilter", priorityFilter);
        model.addAttribute("currentDateRangeFilter", dateRangeFilter);
        model.addAttribute("currentUser", currentUser);
        return "task/my-tasks";
    }

    @GetMapping("/my-todos")
    public String listMyTodo(Model model,
                             @RequestParam(required = false) String statusFilter, 
                             @RequestParam(required = false) String priorityFilter,
                             @RequestParam(required = false) String dateRangeFilter) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName());
        
        List<Task> assignedTasks = taskService.getTasksByAssignee(currentUser);
//        List<Task> createdTasks = taskService.getTasksByCreator(currentUser);
        
        // Varsayılan olarak PENDING seçili gelsin (PENDING_ALL yerine)
        if (statusFilter == null || statusFilter.isEmpty()) {
            statusFilter = "PENDING";
        }

        if ("PENDING".equals(statusFilter)) {
            // Önce bekleyen görevleri filtrele
            assignedTasks = assignedTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING && 
                              !task.getDeadline().isBefore(LocalDateTime.now()))
                .collect(Collectors.toList());
//            createdTasks = createdTasks.stream()
//                .filter(task -> task.getStatus() == TaskStatus.PENDING &&
//                              !task.getDeadline().isBefore(LocalDateTime.now()))
//                .collect(Collectors.toList());

            // Date Range filtresini uygula
            if (dateRangeFilter != null && !dateRangeFilter.isEmpty()) {
                LocalDateTime filterDate = null;
                
                switch (dateRangeFilter) {
                    case "TODAY":
                        filterDate = LocalDateTime.now().toLocalDate().atTime(23, 59, 59);
                        break;
                    case "THREE_DAYS":
                        filterDate = LocalDateTime.now().plusDays(3).toLocalDate().atTime(23, 59, 59);
                        break;
                    case "THIS_WEEK":
                        filterDate = LocalDateTime.now().plusWeeks(1).toLocalDate().atTime(23, 59, 59);
                        break;
                    case "NEXT_TWO_WEEKS":
                        filterDate = LocalDateTime.now().plusWeeks(2).toLocalDate().atTime(23, 59, 59);
                        break;
                    case "THIS_MONTH":
                        filterDate = LocalDateTime.now().plusMonths(1).toLocalDate().withDayOfMonth(1).minusDays(1).atTime(23, 59, 59);
                        break;
                }

                if (filterDate != null) {
                    final LocalDateTime finalFilterDate = filterDate;
                    assignedTasks = assignedTasks.stream()
                        .filter(task -> !task.getDeadline().isAfter(finalFilterDate))
                        .collect(Collectors.toList());
//                    createdTasks = createdTasks.stream()
//                        .filter(task -> !task.getDeadline().isAfter(finalFilterDate))
//                        .collect(Collectors.toList());
                }
            }
        } else if ("OVERDUE".equals(statusFilter)) {
            // Önce tüm süresi geçmiş görevleri filtrele
            assignedTasks = assignedTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING && 
                              task.getDeadline().isBefore(LocalDateTime.now()))
                .collect(Collectors.toList());
//            createdTasks = createdTasks.stream()
//                .filter(task -> task.getStatus() == TaskStatus.PENDING &&
//                              task.getDeadline().isBefore(LocalDateTime.now()))
//                .collect(Collectors.toList());

            // Date Range filtresini uygula
            if (dateRangeFilter != null && !dateRangeFilter.isEmpty()) {
                LocalDateTime filterDate = null;
                
                switch (dateRangeFilter) {
                    case "OVERDUE_TODAY":
                        filterDate = LocalDateTime.now().toLocalDate().atStartOfDay();
                        break;
                    case "OVERDUE_YESTERDAY":
                        filterDate = LocalDateTime.now().minusDays(1).toLocalDate().atStartOfDay();
                        break;
                    case "OVERDUE_THREE_DAYS":
                        filterDate = LocalDateTime.now().minusDays(3).toLocalDate().atStartOfDay();
                        break;
                    case "OVERDUE_WEEK":
                        filterDate = LocalDateTime.now().minusWeeks(1).toLocalDate().atStartOfDay();
                        break;
                    case "OVERDUE_MONTH":
                        filterDate = LocalDateTime.now().minusMonths(1).toLocalDate().atStartOfDay();
                        break;
                }

                if (filterDate != null) {
                    final LocalDateTime finalFilterDate = filterDate;
                    assignedTasks = assignedTasks.stream()
                        .filter(task -> task.getDeadline().isAfter(finalFilterDate))
                        .collect(Collectors.toList());
//                    createdTasks = createdTasks.stream()
//                        .filter(task -> task.getDeadline().isAfter(finalFilterDate))
//                        .collect(Collectors.toList());
                }
            }
        } else if (!statusFilter.isEmpty()) {
            // Diğer durumlar için (COMPLETED, CANCELLED)
            TaskStatus status = TaskStatus.valueOf(statusFilter);
            assignedTasks = assignedTasks.stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
//            createdTasks = createdTasks.stream()
//                .filter(task -> task.getStatus() == status)
//                .collect(Collectors.toList());
        }
        
        // Geriye dönük tarih filtresi için ortak mantık
        if (("OVERDUE".equals(statusFilter) || "COMPLETED".equals(statusFilter) || "CANCELLED".equals(statusFilter)) 
                && dateRangeFilter != null && !dateRangeFilter.isEmpty()) {
            LocalDateTime filterDate = null;
            
            switch (dateRangeFilter) {
                case "PAST_TODAY":
                    filterDate = LocalDateTime.now().toLocalDate().atStartOfDay();
                    break;
                case "PAST_YESTERDAY":
                    filterDate = LocalDateTime.now().minusDays(1).toLocalDate().atStartOfDay();
                    break;
                case "PAST_THREE_DAYS":
                    filterDate = LocalDateTime.now().minusDays(3).toLocalDate().atStartOfDay();
                    break;
                case "PAST_WEEK":
                    filterDate = LocalDateTime.now().minusWeeks(1).toLocalDate().atStartOfDay();
                    break;
                case "PAST_MONTH":
                    filterDate = LocalDateTime.now().minusMonths(1).toLocalDate().atStartOfDay();
                    break;
            }

            if (filterDate != null) {
                final LocalDateTime finalFilterDate = filterDate;
                assignedTasks = assignedTasks.stream()
                    .filter(task -> task.getDeadline().isAfter(finalFilterDate))
                    .collect(Collectors.toList());
//                createdTasks = createdTasks.stream()
//                    .filter(task -> task.getDeadline().isAfter(finalFilterDate))
//                    .collect(Collectors.toList());
            }
        }
        
        // Öncelik filtresi uygula (ALL seçeneğinde de çalışmalı)
        if (priorityFilter != null && !priorityFilter.isEmpty()) {
            TaskPriority priority = TaskPriority.valueOf(priorityFilter);
            assignedTasks = assignedTasks.stream()
                .filter(task -> task.getPriority() == priority)
                .collect(Collectors.toList());
//            createdTasks = createdTasks.stream()
//                .filter(task -> task.getPriority() == priority)
//                .collect(Collectors.toList());
        }

        // En sonda sırala:
        assignedTasks = assignedTasks.stream()
                .sorted(Comparator.comparing(Task::getDeadline))
                .collect(Collectors.toList());
        
        model.addAttribute("assignedTasks", assignedTasks);
//        model.addAttribute("createdTasks", createdTasks);
        model.addAttribute("priorities", TaskPriority.values());
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("currentStatusFilter", statusFilter);
        model.addAttribute("currentPriorityFilter", priorityFilter);
        model.addAttribute("currentDateRangeFilter", dateRangeFilter);
        model.addAttribute("currentUser", currentUser);
        return "task/my-todos";
    }

    @PostMapping("/{id}/complete")
    @ResponseBody
    public String completeTask(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.findByEmail(auth.getName());
            Task task = taskService.getTaskById(id);
            
            if (!task.getAssignees().contains(user) && !task.getCreator().equals(user)) {
                return "Error: You are not authorised to complete this task";
            }
            
            task.complete(user);
            taskService.updateTask(task);
            return "Task successfully completed";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
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
                return "Error: You are not authorised to cancel this task";
            }
            
            task.setStatus(TaskStatus.CANCELLED);
            taskService.updateTask(task);
            return "Task cancelled";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
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
                return "Error: You are not authorised to annotate this task";
            }
            
            task.addNote(user, noteData.get("note"));
            taskService.updateTask(task);
            return "Description successfully added";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
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
                return "Error: You are not authorised to edit this task";
            }
            
            task.setTitle((String) taskData.get("title"));
            task.setDescription((String) taskData.get("description"));
            task.setPriority(TaskPriority.valueOf((String) taskData.get("priority")));
            
            // Tarih formatını düzelt
            String deadlineStr = (String) taskData.get("deadline");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime deadline = LocalDateTime.parse(deadlineStr, formatter);
            task.setDeadline(deadline);
            
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
            return "Task successfully updated";
        } catch (DateTimeParseException e) {
            return "Error: Invalid date format. Please use yyyy-MM-dd HH:mm format";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
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
                throw new RuntimeException("You are not authorised to display this task");
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
                completedByMap.put("id", task.getCompletedBy().getId()); // Add user id for frontend alignment
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
                    userMap.put("id", note.getUser().getId()); // Add user id for frontend alignment
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
                userMap.put("id", task.getCompletedBy().getId()); // Add user id for frontend alignment
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