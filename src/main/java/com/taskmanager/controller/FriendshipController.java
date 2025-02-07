package com.taskmanager.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.taskmanager.entity.User;
import com.taskmanager.service.FriendshipService;
import com.taskmanager.service.UserService;

@Controller
@RequestMapping("/friends")
public class FriendshipController {

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String getFriendsPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByEmail(auth.getName());
        
        model.addAttribute("friends", friendshipService.getFriendList(user));
        model.addAttribute("pendingRequests", friendshipService.getPendingRequests(user));
        return "friends";
    }

    @PostMapping("/send-request")
    @ResponseBody
    public String sendFriendRequest(@RequestParam String email) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User sender = userService.findByEmail(auth.getName());
            
            friendshipService.sendFriendRequest(sender, email);
            return "Request sent successfully";
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/accept/{id}")
    @ResponseBody
    public String acceptRequest(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User receiver = userService.findByEmail(auth.getName());
            
            friendshipService.acceptFriendRequest(id, receiver);
            return "Request granted";
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/reject/{id}")
    @ResponseBody
    public String rejectRequest(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User receiver = userService.findByEmail(auth.getName());
            
            friendshipService.rejectFriendRequest(id, receiver);
            return "Request rejected";
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/remove")
    @ResponseBody
    public String removeFriend(@RequestParam String email) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName());
            
            friendshipService.removeFriend(currentUser, email);
            return "Friend removed successfully";
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }
} 