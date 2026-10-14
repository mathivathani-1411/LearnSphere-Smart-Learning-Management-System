package com.lms.model;

import java.time.LocalDateTime;

/**
 * Base User entity — shared fields for Student and Instructor.
 */
public class User {
    private int userId;
    private String name;
    private String email;
    private String passwordHash;
    private String role; // "STUDENT" or "INSTRUCTOR"
    private LocalDateTime createdDate;

    public User() {}

    public User(int userId, String name, String email, String passwordHash, String role, LocalDateTime createdDate) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdDate = createdDate;
    }

    // ---- Getters & Setters ----
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public boolean isStudent()    { return "STUDENT".equals(role); }
    public boolean isInstructor() { return "INSTRUCTOR".equals(role); }

    @Override
    public String toString() {
        return "User{userId=" + userId + ", name='" + name + "', email='" + email + "', role='" + role + "'}";
    }
}
