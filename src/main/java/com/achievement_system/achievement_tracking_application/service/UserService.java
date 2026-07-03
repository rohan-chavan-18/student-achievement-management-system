package com.achievement_system.achievement_tracking_application.service;

import com.achievement_system.achievement_tracking_application.model.User;
import com.achievement_system.achievement_tracking_application.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    public User register(User user) {
        user.setRole(user.getRole().toUpperCase()); // 🔥 CRITICAL FIX
        return repo.save(user);
    }

    public User login(String username, String password) {
        User user = repo.findByUsername(username);

        if (user != null && user.getPassword().equals(password)) {
            return user; // return full user (with role)
        }
        return null;
    }
}