package com.lms.dao;

import com.lms.model.Progress;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for the progress table.
 */
public class ProgressDAO {

    public void upsert(Progress p) throws SQLException {
        String sql = "INSERT INTO progress (student_id, course_id, completed_lessons, total_lessons, percentage) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE completed_lessons=VALUES(completed_lessons), "
                + "total_lessons=VALUES(total_lessons), percentage=VALUES(percentage)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getStudentId());
            ps.setInt(2, p.getCourseId());
            ps.setInt(3, p.getCompletedLessons());
            ps.setInt(4, p.getTotalLessons());
            ps.setDouble(5, p.getPercentage());
            ps.executeUpdate();
        }
    }

    public Optional<Progress> findByStudentAndCourse(int studentId, int courseId) throws SQLException {
        String sql = "SELECT p.*, c.title AS course_title "
                + "FROM progress p JOIN courses c ON p.course_id = c.course_id "
                + "WHERE p.student_id=? AND p.course_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Progress> findByStudent(int studentId) throws SQLException {
        List<Progress> list = new ArrayList<>();
        String sql = "SELECT p.*, c.title AS course_title "
                + "FROM progress p JOIN courses c ON p.course_id = c.course_id "
                + "WHERE p.student_id=? ORDER BY p.percentage DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Progress mapRow(ResultSet rs) throws SQLException {
        Progress p = new Progress();
        p.setProgressId(rs.getInt("progress_id"));
        p.setStudentId(rs.getInt("student_id"));
        p.setCourseId(rs.getInt("course_id"));
        p.setCompletedLessons(rs.getInt("completed_lessons"));
        p.setTotalLessons(rs.getInt("total_lessons"));
        p.setPercentage(rs.getDouble("percentage"));
        Timestamp ts = rs.getTimestamp("last_updated");
        if (ts != null) p.setLastUpdated(ts.toLocalDateTime());
        try { p.setCourseTitle(rs.getString("course_title")); } catch (SQLException ignored) {}
        return p;
    }
}
