package com.achievement_system.achievement_tracking_application.repository;

import com.achievement_system.achievement_tracking_application.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

    User findByUsername(String username);

    User findByUsernameAndPasswordAndRole(String username, String password, String role);
}