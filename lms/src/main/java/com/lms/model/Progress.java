package com.lms.model;

import java.time.LocalDateTime;

/**
 * Progress entity — tracks a student's lesson completion for a course.
 */
public class Progress {
    private int progressId;
    private int studentId;
    private int courseId;
    private String courseTitle; // joined
    private int completedLessons;
    private int totalLessons;
    private double percentage;
    private LocalDateTime lastUpdated;

    public Progress() {}

    public int getProgressId() { return progressId; }
    public void setProgressId(int progressId) { this.progressId = progressId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public int getCompletedLessons() { return completedLessons; }
    public void setCompletedLessons(int completedLessons) { this.completedLessons = completedLessons; }

    public int getTotalLessons() { return totalLessons; }
    public void setTotalLessons(int totalLessons) { this.totalLessons = totalLessons; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
