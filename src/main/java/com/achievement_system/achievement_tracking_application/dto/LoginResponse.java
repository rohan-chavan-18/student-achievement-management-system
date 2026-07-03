package com.achievement_system.achievement_tracking_application.dto;

public class LoginResponse {
    private String username;
    private String role;

    // constructor
    public LoginResponse(String username, String role) {
        this.username = username;
        this.role = role;
    }

    // getters
    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}