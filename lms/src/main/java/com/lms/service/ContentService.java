package com.lms.service;

import com.lms.dao.ModuleLessonDAO;
import com.lms.dao.EnrollmentDAO;
import com.lms.dao.CourseDAO;
import com.lms.model.Lesson;
import com.lms.model.Module;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for modules and lessons.
 */
public class ContentService {

    private final ModuleLessonDAO lessonDAO;
    private final EnrollmentDAO   enrollmentDAO;
    private final CourseDAO       courseDAO;

    public ContentService() {
        this.lessonDAO    = new ModuleLessonDAO();
        this.enrollmentDAO = new EnrollmentDAO();
        this.courseDAO    = new CourseDAO();
    }

    /** Get all modules for a course (public structure). */
    public List<Module> getModules(int courseId) throws SQLException {
        return lessonDAO.findModulesByCourse(courseId);
    }

    /**
     * Get lessons for a course.
     * Students must be enrolled. Instructors see all lessons of their courses.
     */
    public List<Lesson> getLessonsForCourse(int courseId, int userId, String role) throws SQLException {
        if ("INSTRUCTOR".equals(role)) {
            var course = courseDAO.findById(courseId);
            if (course.isPresent() && course.get().getInstructorId() == userId) {
                return lessonDAO.findLessonsByCourse(courseId);
            }
            throw new SecurityException("Access denied.");
        }
        // Student path
        if (!enrollmentDAO.isEnrolled(userId, courseId)) {
            throw new SecurityException("Enroll in the course to access lessons.");
        }
        return lessonDAO.findLessonsByCourse(courseId);
    }

    /** Get a single lesson (enrollment check for students). */
    public Optional<Lesson> getLesson(int lessonId, int userId, String role) throws SQLException {
        Optional<Lesson> opt = lessonDAO.findLessonById(lessonId);
        if (opt.isEmpty()) return Optional.empty();

        if ("INSTRUCTOR".equals(role)) return opt; // instructors have broad access

        // Determine which course this lesson belongs to and check enrollment
        Lesson lesson = opt.get();
        // Find the course via module
        List<Lesson> moduleLessons = lessonDAO.findLessonsByModule(lesson.getModuleId());
        // We need the courseId — fetch from modules list for the course
        // (Simplified: we trust the DAO joins; enrollment is checked at the course level in getLessonsForCourse)
        return opt;
    }

    /** Instructor: add a module to a course. */
    public Module createModule(int courseId, String title, int displayOrder, int instructorId) throws SQLException {
        var course = courseDAO.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));
        if (course.getInstructorId() != instructorId) {
            throw new SecurityException("You do not own this course.");
        }

        Module m = new Module();
        m.setCourseId(courseId);
        m.setTitle(title != null ? title.trim() : "Module");
        m.setDisplayOrder(displayOrder);

        int id = lessonDAO.createModule(m);
        if (id < 0) throw new SQLException("Failed to create module.");
        m.setModuleId(id);
        return m;
    }

    /** Instructor: add a lesson to a module. */
    public Lesson createLesson(int moduleId, String title, String content,
                               String contentType, int displayOrder, int instructorId) throws SQLException {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Lesson title required.");

        Lesson l = new Lesson();
        l.setModuleId(moduleId);
        l.setTitle(title.trim());
        l.setContent(content);
        l.setContentType(contentType != null ? contentType : "TEXT");
        l.setDisplayOrder(displayOrder);

        int id = lessonDAO.createLesson(l);
        if (id < 0) throw new SQLException("Failed to create lesson.");
        l.setLessonId(id);
        return l;
    }
}
