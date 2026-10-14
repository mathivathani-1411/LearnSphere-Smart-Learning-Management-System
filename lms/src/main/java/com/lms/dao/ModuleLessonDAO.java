package com.lms.dao;

import com.lms.model.Lesson;
import com.lms.model.Module;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for modules and lessons.
 */
public class ModuleLessonDAO {

    // -------- MODULES --------

    public int createModule(Module m) throws SQLException {
        String sql = "INSERT INTO modules (course_id, title, display_order) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getCourseId());
            ps.setString(2, m.getTitle());
            ps.setInt(3, m.getDisplayOrder());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public List<Module> findModulesByCourse(int courseId) throws SQLException {
        List<Module> list = new ArrayList<>();
        String sql = "SELECT * FROM modules WHERE course_id=? ORDER BY display_order";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapModule(rs));
            }
        }
        return list;
    }

    public boolean deleteModule(int moduleId) throws SQLException {
        String sql = "DELETE FROM modules WHERE module_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, moduleId);
            return ps.executeUpdate() > 0;
        }
    }

    // -------- LESSONS --------

    public int createLesson(Lesson l) throws SQLException {
        String sql = "INSERT INTO lessons (module_id, title, content, content_url, content_type, display_order) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, l.getModuleId());
            ps.setString(2, l.getTitle());
            ps.setString(3, l.getContent());
            ps.setString(4, l.getContentUrl());
            ps.setString(5, l.getContentType() != null ? l.getContentType() : "TEXT");
            ps.setInt(6, l.getDisplayOrder());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public Optional<Lesson> findLessonById(int lessonId) throws SQLException {
        String sql = "SELECT * FROM lessons WHERE lesson_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lessonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapLesson(rs));
            }
        }
        return Optional.empty();
    }

    public List<Lesson> findLessonsByModule(int moduleId) throws SQLException {
        List<Lesson> list = new ArrayList<>();
        String sql = "SELECT * FROM lessons WHERE module_id=? ORDER BY display_order";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, moduleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapLesson(rs));
            }
        }
        return list;
    }

    /** All lessons for a course (across all modules). */
    public List<Lesson> findLessonsByCourse(int courseId) throws SQLException {
        List<Lesson> list = new ArrayList<>();
        String sql = "SELECT l.* FROM lessons l "
                + "JOIN modules m ON l.module_id = m.module_id "
                + "WHERE m.course_id=? ORDER BY m.display_order, l.display_order";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapLesson(rs));
            }
        }
        return list;
    }

    /** Count all lessons in a course. */
    public int countLessonsInCourse(int courseId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM lessons l JOIN modules m ON l.module_id=m.module_id WHERE m.course_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean deleteLesson(int lessonId) throws SQLException {
        String sql = "DELETE FROM lessons WHERE lesson_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lessonId);
            return ps.executeUpdate() > 0;
        }
    }

    // -------- LESSON COMPLETION --------

    public boolean markComplete(int studentId, int lessonId) throws SQLException {
        String sql = "INSERT IGNORE INTO lesson_completions (student_id, lesson_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, lessonId);
            return ps.executeUpdate() > 0;
        }
    }

    public int countCompletedLessons(int studentId, int courseId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM lesson_completions lc "
                + "JOIN lessons l ON lc.lesson_id = l.lesson_id "
                + "JOIN modules m ON l.module_id = m.module_id "
                + "WHERE lc.student_id=? AND m.course_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Integer> findCompletedLessonIds(int studentId, int courseId) throws SQLException {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT lc.lesson_id FROM lesson_completions lc "
                + "JOIN lessons l ON lc.lesson_id = l.lesson_id "
                + "JOIN modules m ON l.module_id = m.module_id "
                + "WHERE lc.student_id=? AND m.course_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getInt(1));
            }
        }
        return list;
    }

    // ---- private helpers ----

    private Module mapModule(ResultSet rs) throws SQLException {
        Module m = new Module();
        m.setModuleId(rs.getInt("module_id"));
        m.setCourseId(rs.getInt("course_id"));
        m.setTitle(rs.getString("title"));
        m.setDisplayOrder(rs.getInt("display_order"));
        return m;
    }

    private Lesson mapLesson(ResultSet rs) throws SQLException {
        Lesson l = new Lesson();
        l.setLessonId(rs.getInt("lesson_id"));
        l.setModuleId(rs.getInt("module_id"));
        l.setTitle(rs.getString("title"));
        l.setContent(rs.getString("content"));
        l.setContentUrl(rs.getString("content_url"));
        l.setContentType(rs.getString("content_type"));
        l.setDisplayOrder(rs.getInt("display_order"));
        return l;
    }
}
