package com.lms.model;

import java.time.LocalDateTime;

/**
 * Course entity — created and managed by an Instructor.
 */
public class Course {
    private int courseId;
    private String title;
    private String description;
    private int instructorId;
    private String instructorName; // joined field for display
    private String status;         // DRAFT | PUBLISHED | ARCHIVED
    private LocalDateTime createdDate;

    public Course() {}

    public Course(int courseId, String title, String description, int instructorId, String status, LocalDateTime createdDate) {
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.instructorId = instructorId;
        this.status = status;
        this.createdDate = createdDate;
    }

    // ---- Getters & Setters ----
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getInstructorId() { return instructorId; }
    public void setInstructorId(int instructorId) { this.instructorId = instructorId; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public boolean isPublished() { return "PUBLISHED".equals(status); }
}
