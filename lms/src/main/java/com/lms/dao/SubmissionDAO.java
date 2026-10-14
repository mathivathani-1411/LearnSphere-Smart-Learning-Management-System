package com.lms.dao;

import com.lms.model.Submission;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for submissions.
 */
public class SubmissionDAO {

    public int create(Submission s) throws SQLException {
        String sql = "INSERT INTO submissions (assignment_id, student_id, file_path, original_name, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getAssignmentId());
            ps.setInt(2, s.getStudentId());
            ps.setString(3, s.getFilePath());
            ps.setString(4, s.getOriginalName());
            ps.setString(5, s.getStatus() != null ? s.getStatus() : "SUBMITTED");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public Optional<Submission> findById(int id) throws SQLException {
        String sql = "SELECT s.*, a.title AS assignment_title, u.name AS student_name, "
                + "g.score, g.feedback "
                + "FROM submissions s JOIN assignments a ON s.assignment_id = a.assignment_id "
                + "JOIN users u ON s.student_id = u.user_id "
                + "LEFT JOIN grades g ON g.submission_id = s.submission_id "
                + "WHERE s.submission_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /** Check whether a student already submitted for this assignment. */
    public Optional<Submission> findByAssignmentAndStudent(int assignmentId, int studentId) throws SQLException {
        String sql = "SELECT s.*, a.title AS assignment_title, u.name AS student_name, g.score, g.feedback "
                + "FROM submissions s JOIN assignments a ON s.assignment_id = a.assignment_id "
                + "JOIN users u ON s.student_id = u.user_id "
                + "LEFT JOIN grades g ON g.submission_id = s.submission_id "
                + "WHERE s.assignment_id=? AND s.student_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /** All submissions for an assignment (instructor view). */
    public List<Submission> findByAssignment(int assignmentId) throws SQLException {
        String sql = "SELECT s.*, a.title AS assignment_title, u.name AS student_name, g.score, g.feedback "
                + "FROM submissions s JOIN assignments a ON s.assignment_id = a.assignment_id "
                + "JOIN users u ON s.student_id = u.user_id "
                + "LEFT JOIN grades g ON g.submission_id = s.submission_id "
                + "WHERE s.assignment_id=? ORDER BY s.submission_date DESC";
        return queryList(sql, assignmentId);
    }

    /** All submissions made by a student. */
    public List<Submission> findByStudent(int studentId) throws SQLException {
        String sql = "SELECT s.*, a.title AS assignment_title, u.name AS student_name, g.score, g.feedback "
                + "FROM submissions s JOIN assignments a ON s.assignment_id = a.assignment_id "
                + "JOIN users u ON s.student_id = u.user_id "
                + "LEFT JOIN grades g ON g.submission_id = s.submission_id "
                + "WHERE s.student_id=? ORDER BY s.submission_date DESC";
        return queryList(sql, studentId);
    }

    public boolean updateStatus(int submissionId, String status) throws SQLException {
        String sql = "UPDATE submissions SET status=? WHERE submission_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, submissionId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---- helpers ----

    private List<Submission> queryList(String sql, Object... params) throws SQLException {
        List<Submission> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Submission mapRow(ResultSet rs) throws SQLException {
        Submission s = new Submission();
        s.setSubmissionId(rs.getInt("submission_id"));
        s.setAssignmentId(rs.getInt("assignment_id"));
        s.setStudentId(rs.getInt("student_id"));
        s.setFilePath(rs.getString("file_path"));
        s.setOriginalName(rs.getString("original_name"));
        s.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("submission_date");
        if (ts != null) s.setSubmissionDate(ts.toLocalDateTime());
        try { s.setAssignmentTitle(rs.getString("assignment_title")); } catch (SQLException ignored) {}
        try { s.setStudentName(rs.getString("student_name")); } catch (SQLException ignored) {}
        try {
            double score = rs.getDouble("score");
            if (!rs.wasNull()) s.setScore(score);
        } catch (SQLException ignored) {}
        try { s.setFeedback(rs.getString("feedback")); } catch (SQLException ignored) {}
        return s;
    }
}
