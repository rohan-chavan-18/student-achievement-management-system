package com.achievement_system.achievement_tracking_application.model;

import jakarta.persistence.*;

@Entity
@Table(name = "achievements")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String username;
    private String title;
    private String category;
    private String year;
    private String semester;
    private String fileName;
    private String eventLevel;
    private String skills;
    private String approved;

    private String status; // Winner / Runner / Participation
    private int score;     // NEW

    public int getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getStatus() { return status; }

    // 🔥 AUTO SCORE LOGIC
    public void setStatus(String status) {
        this.status = status;

        String s = status.toLowerCase();
        if (s.contains("winner")) this.score = 3;
        else if (s.contains("runner")) this.score = 2;
        else this.score = 1;
    }

    public int getScore() { return score; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getEventLevel() { return eventLevel; }
    public void setEventLevel(String eventLevel) { this.eventLevel = eventLevel; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getApproved() { return approved; }
    public void setApproved(String approved) { this.approved = approved; }
}