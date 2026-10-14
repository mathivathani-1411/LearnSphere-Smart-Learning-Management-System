package com.lms.service;

import com.lms.dao.CourseDAO;
import com.lms.dao.EnrollmentDAO;
import com.lms.dao.NotificationDAO;
import com.lms.model.Course;
import com.lms.util.ValidationUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for course management.
 */
public class CourseService {

    private final CourseDAO     courseDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final NotificationDAO notificationDAO;

    public CourseService() {
        this.courseDAO        = new CourseDAO();
        this.enrollmentDAO    = new EnrollmentDAO();
        this.notificationDAO  = new NotificationDAO();
    }

    /** For testing with injected DAOs. */
    public CourseService(CourseDAO courseDAO, EnrollmentDAO enrollmentDAO, NotificationDAO notificationDAO) {
        this.courseDAO = courseDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.notificationDAO = notificationDAO;
    }

    /** Return all published courses (public catalogue). */
    public List<Course> getPublishedCourses() throws SQLException {
        return courseDAO.findAllPublished();
    }

    /** Return all courses for an instructor. */
    public List<Course> getInstructorCourses(int instructorId) throws SQLException {
        return courseDAO.findByInstructor(instructorId);
    }

    /** Get a course by ID, verifying visibility for the requesting user. */
    public Optional<Course> getCourse(int courseId, int requesterId, String role) throws SQLException {
        Optional<Course> opt = courseDAO.findById(courseId);
        if (opt.isEmpty()) return Optional.empty();
        Course c = opt.get();

        // Instructors can see their own courses regardless of status
        if ("INSTRUCTOR".equals(role) && c.getInstructorId() == requesterId) return opt;
        // Students and guests only see published courses
        if (c.isPublished()) return opt;
        return Optional.empty();
    }

    /** Create a new course. */
    public Course createCourse(String title, String description, int instructorId) throws SQLException {
        title       = ValidationUtil.sanitise(title);
        description = ValidationUtil.sanitise(description);

        if (title == null || title.isBlank() || title.length() > 200) {
            throw new IllegalArgumentException("Course title must be 1–200 characters.");
        }

        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.setInstructorId(instructorId);
        course.setStatus("DRAFT");

        int id = courseDAO.create(course);
        if (id < 0) throw new SQLException("Failed to create course.");
        course.setCourseId(id);
        return course;
    }

    /** Update course details (only the owner can do this). */
    public Course updateCourse(int courseId, String title, String description, String status,
                               int requesterId) throws SQLException {
        Optional<Course> opt = courseDAO.findById(courseId);
        if (opt.isEmpty()) throw new IllegalArgumentException("Course not found.");

        Course course = opt.get();
        if (course.getInstructorId() != requesterId) {
            throw new SecurityException("Access denied: you do not own this course.");
        }

        title       = ValidationUtil.sanitise(title);
        description = ValidationUtil.sanitise(description);
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required.");
        if (status != null && !List.of("DRAFT","PUBLISHED","ARCHIVED").contains(status)) {
            throw new IllegalArgumentException("Invalid status.");
        }

        course.setTitle(title);
        course.setDescription(description);
        if (status != null) course.setStatus(status);

        courseDAO.update(course);
        return course;
    }

    /** Delete a course (owner only). */
    public void deleteCourse(int courseId, int requesterId) throws SQLException {
        Optional<Course> opt = courseDAO.findById(courseId);
        if (opt.isEmpty()) throw new IllegalArgumentException("Course not found.");
        if (opt.get().getInstructorId() != requesterId) {
            throw new SecurityException("Access denied.");
        }
        courseDAO.delete(courseId, requesterId);
    }

    /** Enroll a student, preventing duplicates. */
    public void enroll(int studentId, int courseId) throws SQLException {
        Optional<Course> opt = courseDAO.findById(courseId);
        if (opt.isEmpty() || !opt.get().isPublished()) {
            throw new IllegalArgumentException("Course not found or not published.");
        }
        if (enrollmentDAO.isEnrolled(studentId, courseId)) {
            throw new IllegalStateException("Already enrolled in this course.");
        }
        enrollmentDAO.enroll(studentId, courseId);
        // Fire notification
        try {
            notificationDAO.create(studentId,
                    "You have been enrolled in \"" + opt.get().getTitle() + "\".",
                    "/student/my-courses.html");
        } catch (SQLException e) { /* non-critical — log only */ }
    }
}
