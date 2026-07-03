package com.achievement_system.achievement_tracking_application.controller;

import com.achievement_system.achievement_tracking_application.model.User;
import com.achievement_system.achievement_tracking_application.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserRepository repo;

    // SIGNUP ✅
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        user.setUsername(user.getUsername().toLowerCase().trim());
        user.setRole(user.getRole().toUpperCase().trim());

        User existing = repo.findByUsername(user.getUsername());
        if (existing != null) {
            return ResponseEntity.status(409).body("User already exists");
        }

        return ResponseEntity.ok(repo.save(user));
    }

    // LOGIN ✅
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {

        User existing = repo.findByUsername(user.getUsername().toLowerCase());

        if (existing != null &&
                existing.getPassword().equals(user.getPassword()) &&
                existing.getRole().equalsIgnoreCase(user.getRole())) {

            return ResponseEntity.ok(existing);
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }
}