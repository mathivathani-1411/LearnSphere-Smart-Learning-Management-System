package com.lms.model;

import java.time.LocalDateTime;

/**
 * Enrollment entity — records which student enrolled in which course.
 */
public class Enrollment {
    private int enrollmentId;
    private int studentId;
    private String studentName;   // joined
    private int courseId;
    private String courseTitle;   // joined
    private LocalDateTime enrollmentDate;
    private String status;        // ACTIVE | COMPLETED | DROPPED

    public Enrollment() {}

    public Enrollment(int enrollmentId, int studentId, int courseId,
                      LocalDateTime enrollmentDate, String status) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.enrollmentDate = enrollmentDate;
        this.status = status;
    }

    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public LocalDateTime getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDateTime enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
