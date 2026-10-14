package com.lms.service;

import com.lms.dao.EnrollmentDAO;
import com.lms.dao.ModuleLessonDAO;
import com.lms.dao.ProgressDAO;
import com.lms.model.Progress;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for lesson access and progress tracking.
 */
public class ProgressService {

    private final ModuleLessonDAO lessonDAO;
    private final EnrollmentDAO   enrollmentDAO;
    private final ProgressDAO     progressDAO;

    public ProgressService() {
        this.lessonDAO     = new ModuleLessonDAO();
        this.enrollmentDAO = new EnrollmentDAO();
        this.progressDAO   = new ProgressDAO();
    }

    /**
     * Mark a lesson as completed for a student and recalculate progress.
     * The student must be enrolled in the course the lesson belongs to.
     */
    public Progress completeLesson(int studentId, int lessonId, int courseId) throws SQLException {
        // Enrolment guard
        if (!enrollmentDAO.isEnrolled(studentId, courseId)) {
            throw new SecurityException("Not enrolled in this course.");
        }

        lessonDAO.markComplete(studentId, lessonId);
        return recalculate(studentId, courseId);
    }

    /** Recalculate and persist the student's progress for a given course. */
    public Progress recalculate(int studentId, int courseId) throws SQLException {
        int total     = lessonDAO.countLessonsInCourse(courseId);
        int completed = lessonDAO.countCompletedLessons(studentId, courseId);
        double pct    = total > 0 ? Math.round((completed * 100.0 / total) * 100.0) / 100.0 : 0.0;

        Progress p = new Progress();
        p.setStudentId(studentId);
        p.setCourseId(courseId);
        p.setCompletedLessons(completed);
        p.setTotalLessons(total);
        p.setPercentage(pct);

        progressDAO.upsert(p);
        return p;
    }

    /** All progress records for a student (my-progress page). */
    public List<Progress> getStudentProgress(int studentId) throws SQLException {
        return progressDAO.findByStudent(studentId);
    }

    /** Progress for a specific course. */
    public Progress getCourseProgress(int studentId, int courseId) throws SQLException {
        return progressDAO.findByStudentAndCourse(studentId, courseId)
                .orElseGet(() -> {
                    Progress p = new Progress();
                    p.setStudentId(studentId);
                    p.setCourseId(courseId);
                    return p;
                });
    }

    /** List of lesson IDs completed by a student in a course. */
    public List<Integer> getCompletedLessonIds(int studentId, int courseId) throws SQLException {
        return lessonDAO.findCompletedLessonIds(studentId, courseId);
    }
}
