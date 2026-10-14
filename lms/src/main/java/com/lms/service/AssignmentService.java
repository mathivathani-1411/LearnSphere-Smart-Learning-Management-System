package com.lms.service;

import com.lms.dao.*;
import com.lms.model.*;
import com.lms.util.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for assignments, submissions, and grading.
 */
public class AssignmentService {

    private final AssignmentDAO   assignmentDAO;
    private final SubmissionDAO   submissionDAO;
    private final GradeDAO        gradeDAO;
    private final EnrollmentDAO   enrollmentDAO;
    private final CourseDAO       courseDAO;
    private final NotificationDAO notificationDAO;

    public AssignmentService() {
        this.assignmentDAO   = new AssignmentDAO();
        this.submissionDAO   = new SubmissionDAO();
        this.gradeDAO        = new GradeDAO();
        this.enrollmentDAO   = new EnrollmentDAO();
        this.courseDAO       = new CourseDAO();
        this.notificationDAO = new NotificationDAO();
    }

    public AssignmentService(AssignmentDAO assignmentDAO, SubmissionDAO submissionDAO,
                             GradeDAO gradeDAO, EnrollmentDAO enrollmentDAO,
                             CourseDAO courseDAO, NotificationDAO notificationDAO) {
        this.assignmentDAO   = assignmentDAO;
        this.submissionDAO   = submissionDAO;
        this.gradeDAO        = gradeDAO;
        this.enrollmentDAO   = enrollmentDAO;
        this.courseDAO       = courseDAO;
        this.notificationDAO = notificationDAO;
    }

    // ----- Assignments -----

    public Assignment createAssignment(int courseId, String title, String description,
                                       LocalDateTime deadline, int maxMarks,
                                       boolean allowLate, int instructorId) throws SQLException {
        // Verify instructor owns the course
        Optional<Course> courseOpt = courseDAO.findById(courseId);
        if (courseOpt.isEmpty()) throw new IllegalArgumentException("Course not found.");
        if (courseOpt.get().getInstructorId() != instructorId) {
            throw new SecurityException("You do not own this course.");
        }

        title = ValidationUtil.sanitise(title);
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required.");
        if (deadline == null || deadline.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Deadline must be in the future.");
        }
        if (maxMarks <= 0 || maxMarks > 1000) {
            throw new IllegalArgumentException("Max marks must be 1–1000.");
        }

        Assignment a = new Assignment();
        a.setCourseId(courseId);
        a.setTitle(title);
        a.setDescription(ValidationUtil.sanitise(description));
        a.setDeadline(deadline);
        a.setMaxMarks(maxMarks);
        a.setAllowLate(allowLate);

        int id = assignmentDAO.create(a);
        if (id < 0) throw new SQLException("Failed to create assignment.");
        a.setAssignmentId(id);
        return a;
    }

    public Optional<Assignment> getAssignment(int id) throws SQLException {
        return assignmentDAO.findById(id);
    }

    public List<Assignment> getAssignmentsForStudent(int studentId) throws SQLException {
        return assignmentDAO.findForStudent(studentId);
    }

    public List<Assignment> getAssignmentsByCourse(int courseId) throws SQLException {
        return assignmentDAO.findByCourse(courseId);
    }

    // ----- Submissions -----

    /**
     * Submit a file for an assignment.
     * Rules enforced:
     *  - Student must be enrolled in the course.
     *  - Only one submission per student per assignment (UNIQUE constraint + check).
     *  - Late submissions rejected unless allowLate is true.
     */
    public Submission submit(int assignmentId, int studentId,
                             String filePath, String originalName) throws SQLException {
        Assignment a = assignmentDAO.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found."));

        // Enrollment check
        if (!enrollmentDAO.isEnrolled(studentId, a.getCourseId())) {
            throw new SecurityException("You are not enrolled in this course.");
        }

        // Deadline check
        boolean isLate = a.isPastDeadline();
        if (isLate && !a.isAllowLate()) {
            throw new IllegalStateException("Submission deadline has passed.");
        }

        // Duplicate check
        Optional<Submission> existing = submissionDAO.findByAssignmentAndStudent(assignmentId, studentId);
        if (existing.isPresent()) {
            throw new IllegalStateException("You have already submitted this assignment.");
        }

        Submission s = new Submission();
        s.setAssignmentId(assignmentId);
        s.setStudentId(studentId);
        s.setFilePath(filePath);
        s.setOriginalName(originalName);
        s.setStatus(isLate ? "LATE" : "SUBMITTED");

        int id = submissionDAO.create(s);
        if (id < 0) throw new SQLException("Failed to save submission.");
        s.setSubmissionId(id);

        // Notify instructor
        try {
            Course course = courseDAO.findById(a.getCourseId()).orElse(null);
            if (course != null) {
                notificationDAO.create(course.getInstructorId(),
                        "Student #" + studentId + " submitted \"" + a.getTitle() + "\".",
                        "/instructor/submissions.html?asgId=" + assignmentId);
            }
        } catch (SQLException ignored) {}

        return s;
    }

    /** Retrieve a submission after verifying authorization. */
    public Submission getSubmission(int submissionId, int requesterId, String role) throws SQLException {
        Submission sub = submissionDAO.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found."));
        if ("STUDENT".equals(role)) {
            if (sub.getStudentId() != requesterId) {
                throw new SecurityException("Access denied: You do not own this submission.");
            }
        } else if ("INSTRUCTOR".equals(role)) {
            Assignment a = assignmentDAO.findById(sub.getAssignmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Assignment not found."));
            Course c = courseDAO.findById(a.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("Course not found."));
            if (c.getInstructorId() != requesterId) {
                throw new SecurityException("Access denied: You do not teach this course.");
            }
        }
        return sub;
    }

    public List<Submission> getSubmissionsForAssignment(int assignmentId, int instructorId) throws SQLException {
        // Verify ownership
        Assignment a = assignmentDAO.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found."));
        Course c = courseDAO.findById(a.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));
        if (c.getInstructorId() != instructorId) {
            throw new SecurityException("Access denied.");
        }
        return submissionDAO.findByAssignment(assignmentId);
    }

    public List<Submission> getMySubmissions(int studentId) throws SQLException {
        return submissionDAO.findByStudent(studentId);
    }

    // ----- Grading -----

    /**
     * Grade or re-grade a submission.
     * Only the owning instructor may grade.
     * Score must be between 0 and maxMarks.
     */
    public Grade grade(int submissionId, double score, String feedback, int instructorId) throws SQLException {
        Submission sub = submissionDAO.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found."));
        Assignment a = assignmentDAO.findById(sub.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found."));
        Course c = courseDAO.findById(a.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));

        if (c.getInstructorId() != instructorId) {
            throw new SecurityException("Only the course instructor can grade this submission.");
        }
        if (score < 0 || score > a.getMaxMarks()) {
            throw new IllegalArgumentException("Score must be between 0 and " + a.getMaxMarks() + ".");
        }

        Optional<Grade> existingGrade = gradeDAO.findBySubmission(submissionId);
        Grade grade;
        if (existingGrade.isPresent()) {
            grade = existingGrade.get();
            grade.setScore(score);
            grade.setFeedback(ValidationUtil.sanitise(feedback));
            grade.setGradedBy(instructorId);
            gradeDAO.update(grade);
        } else {
            grade = new Grade();
            grade.setSubmissionId(submissionId);
            grade.setScore(score);
            grade.setFeedback(ValidationUtil.sanitise(feedback));
            grade.setGradedBy(instructorId);
            int id = gradeDAO.create(grade);
            grade.setGradeId(id);
        }

        // Update submission status
        submissionDAO.updateStatus(submissionId, "GRADED");

        // Notify student
        try {
            notificationDAO.create(sub.getStudentId(),
                    "Your submission for \"" + a.getTitle() + "\" has been graded: " + score + "/" + a.getMaxMarks() + ".",
                    "/student/grades.html");
        } catch (SQLException ignored) {}

        return grade;
    }
}
