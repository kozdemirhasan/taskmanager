package com.taskmanager.dto;

import lombok.Data;

@Data
public class UserRegistrationDto {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String password;
    private String confirmPassword;

    public boolean isPasswordsMatching() {
        return password != null && password.equals(confirmPassword);
    }
} 