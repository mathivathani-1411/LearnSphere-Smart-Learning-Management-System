package com.lms.model;

import java.time.LocalDateTime;

/**
 * Grade entity — records score and feedback for a submission.
 */
public class Grade {
    private int gradeId;
    private int submissionId;
    private double score;
    private String feedback;
    private LocalDateTime gradedDate;
    private int gradedBy;

    public Grade() {}

    public Grade(int gradeId, int submissionId, double score, String feedback,
                 LocalDateTime gradedDate, int gradedBy) {
        this.gradeId = gradeId;
        this.submissionId = submissionId;
        this.score = score;
        this.feedback = feedback;
        this.gradedDate = gradedDate;
        this.gradedBy = gradedBy;
    }

    public int getGradeId() { return gradeId; }
    public void setGradeId(int gradeId) { this.gradeId = gradeId; }

    public int getSubmissionId() { return submissionId; }
    public void setSubmissionId(int submissionId) { this.submissionId = submissionId; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public LocalDateTime getGradedDate() { return gradedDate; }
    public void setGradedDate(LocalDateTime gradedDate) { this.gradedDate = gradedDate; }

    public int getGradedBy() { return gradedBy; }
    public void setGradedBy(int gradedBy) { this.gradedBy = gradedBy; }
}
