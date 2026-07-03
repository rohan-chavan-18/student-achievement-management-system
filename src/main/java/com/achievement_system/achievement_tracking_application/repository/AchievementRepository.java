package com.achievement_system.achievement_tracking_application.repository;

import com.achievement_system.achievement_tracking_application.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Integer> {

    List<Achievement> findByUsername(String username);
}