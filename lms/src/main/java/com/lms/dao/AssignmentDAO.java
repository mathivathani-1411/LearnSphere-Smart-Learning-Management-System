package com.lms.dao;

import com.lms.model.Assignment;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for assignments.
 */
public class AssignmentDAO {

    public int create(Assignment a) throws SQLException {
        String sql = "INSERT INTO assignments (course_id, title, description, deadline, max_marks, allow_late) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getCourseId());
            ps.setString(2, a.getTitle());
            ps.setString(3, a.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(a.getDeadline()));
            ps.setInt(5, a.getMaxMarks());
            ps.setBoolean(6, a.isAllowLate());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public Optional<Assignment> findById(int id) throws SQLException {
        String sql = "SELECT a.*, c.title AS course_title "
                + "FROM assignments a JOIN courses c ON a.course_id = c.course_id "
                + "WHERE a.assignment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Assignment> findByCourse(int courseId) throws SQLException {
        String sql = "SELECT a.*, c.title AS course_title "
                + "FROM assignments a JOIN courses c ON a.course_id = c.course_id "
                + "WHERE a.course_id = ? ORDER BY a.deadline ASC";
        return queryList(sql, courseId);
    }

    /** All assignments for the courses a student is enrolled in. */
    public List<Assignment> findForStudent(int studentId) throws SQLException {
        String sql = "SELECT a.*, c.title AS course_title "
                + "FROM assignments a JOIN courses c ON a.course_id = c.course_id "
                + "JOIN enrollments e ON e.course_id = c.course_id "
                + "WHERE e.student_id = ? ORDER BY a.deadline ASC";
        return queryList(sql, studentId);
    }

    public boolean update(Assignment a) throws SQLException {
        String sql = "UPDATE assignments SET title=?, description=?, deadline=?, max_marks=?, allow_late=? "
                + "WHERE assignment_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getTitle());
            ps.setString(2, a.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(a.getDeadline()));
            ps.setInt(4, a.getMaxMarks());
            ps.setBoolean(5, a.isAllowLate());
            ps.setInt(6, a.getAssignmentId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM assignments WHERE assignment_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ---- helpers ----

    private List<Assignment> queryList(String sql, Object... params) throws SQLException {
        List<Assignment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Assignment mapRow(ResultSet rs) throws SQLException {
        Assignment a = new Assignment();
        a.setAssignmentId(rs.getInt("assignment_id"));
        a.setCourseId(rs.getInt("course_id"));
        a.setTitle(rs.getString("title"));
        a.setDescription(rs.getString("description"));
        Timestamp dl = rs.getTimestamp("deadline");
        if (dl != null) a.setDeadline(dl.toLocalDateTime());
        a.setMaxMarks(rs.getInt("max_marks"));
        a.setAllowLate(rs.getBoolean("allow_late"));
        Timestamp cr = rs.getTimestamp("created_date");
        if (cr != null) a.setCreatedDate(cr.toLocalDateTime());
        try { a.setCourseTitle(rs.getString("course_title")); } catch (SQLException ignored) {}
        return a;
    }
}
