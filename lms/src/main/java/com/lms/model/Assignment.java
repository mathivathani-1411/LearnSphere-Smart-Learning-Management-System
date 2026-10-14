package com.lms.model;

import java.time.LocalDateTime;

/**
 * Assignment entity — belongs to a course, has a deadline.
 */
public class Assignment {
    private int assignmentId;
    private int courseId;
    private String courseTitle; // joined
    private String title;
    private String description;
    private LocalDateTime deadline;
    private int maxMarks;
    private boolean allowLate;
    private LocalDateTime createdDate;

    public Assignment() {}

    public Assignment(int assignmentId, int courseId, String title, String description,
                      LocalDateTime deadline, int maxMarks, boolean allowLate, LocalDateTime createdDate) {
        this.assignmentId = assignmentId;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.maxMarks = maxMarks;
        this.allowLate = allowLate;
        this.createdDate = createdDate;
    }

    public boolean isPastDeadline() {
        return LocalDateTime.now().isAfter(deadline);
    }

    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public int getMaxMarks() { return maxMarks; }
    public void setMaxMarks(int maxMarks) { this.maxMarks = maxMarks; }

    public boolean isAllowLate() { return allowLate; }
    public void setAllowLate(boolean allowLate) { this.allowLate = allowLate; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
