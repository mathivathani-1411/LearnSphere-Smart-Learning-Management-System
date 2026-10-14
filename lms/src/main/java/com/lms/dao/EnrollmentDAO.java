package com.lms.dao;

import com.lms.model.Enrollment;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for enrollment operations.
 */
public class EnrollmentDAO {

    /** Enroll a student in a course. Returns the new enrollment ID. */
    public int enroll(int studentId, int courseId) throws SQLException {
        String sql = "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    /** Check if a student is already enrolled. */
    public boolean isEnrolled(int studentId, int courseId) throws SQLException {
        String sql = "SELECT 1 FROM enrollments WHERE student_id=? AND course_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /** All enrollments for a specific student. */
    public List<Enrollment> findByStudent(int studentId) throws SQLException {
        String sql = "SELECT e.*, c.title AS course_title "
                + "FROM enrollments e JOIN courses c ON e.course_id = c.course_id "
                + "WHERE e.student_id = ? ORDER BY e.enrollment_date DESC";
        return queryList(sql, studentId);
    }

    /** All enrollments for a specific course (for instructor view). */
    public List<Enrollment> findByCourse(int courseId) throws SQLException {
        String sql = "SELECT e.*, u.name AS student_name, c.title AS course_title "
                + "FROM enrollments e JOIN users u ON e.student_id = u.user_id "
                + "JOIN courses c ON e.course_id = c.course_id "
                + "WHERE e.course_id = ? ORDER BY e.enrollment_date DESC";
        return queryList(sql, courseId);
    }

    /** Count students enrolled in a course. */
    public int countByCourse(int courseId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE course_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ---- helpers ----

    private List<Enrollment> queryList(String sql, Object... params) throws SQLException {
        List<Enrollment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Enrollment mapRow(ResultSet rs) throws SQLException {
        Enrollment e = new Enrollment();
        e.setEnrollmentId(rs.getInt("enrollment_id"));
        e.setStudentId(rs.getInt("student_id"));
        e.setCourseId(rs.getInt("course_id"));
        e.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("enrollment_date");
        if (ts != null) e.setEnrollmentDate(ts.toLocalDateTime());
        try { e.setCourseTitle(rs.getString("course_title")); } catch (SQLException ignored) {}
        try { e.setStudentName(rs.getString("student_name")); } catch (SQLException ignored) {}
        return e;
    }
}
