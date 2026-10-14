package com.lms.dao;

import com.lms.model.Grade;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.Optional;

/**
 * DAO for grades. Each submission can only have one grade (UNIQUE KEY).
 */
public class GradeDAO {

    public int create(Grade grade) throws SQLException {
        String sql = "INSERT INTO grades (submission_id, score, feedback, graded_by) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, grade.getSubmissionId());
            ps.setDouble(2, grade.getScore());
            ps.setString(3, grade.getFeedback());
            ps.setInt(4, grade.getGradedBy());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public Optional<Grade> findBySubmission(int submissionId) throws SQLException {
        String sql = "SELECT * FROM grades WHERE submission_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public boolean update(Grade grade) throws SQLException {
        String sql = "UPDATE grades SET score=?, feedback=?, graded_by=?, graded_date=NOW() "
                + "WHERE submission_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, grade.getScore());
            ps.setString(2, grade.getFeedback());
            ps.setInt(3, grade.getGradedBy());
            ps.setInt(4, grade.getSubmissionId());
            return ps.executeUpdate() > 0;
        }
    }

    private Grade mapRow(ResultSet rs) throws SQLException {
        Grade g = new Grade();
        g.setGradeId(rs.getInt("grade_id"));
        g.setSubmissionId(rs.getInt("submission_id"));
        g.setScore(rs.getDouble("score"));
        g.setFeedback(rs.getString("feedback"));
        g.setGradedBy(rs.getInt("graded_by"));
        Timestamp ts = rs.getTimestamp("graded_date");
        if (ts != null) g.setGradedDate(ts.toLocalDateTime());
        return g;
    }
}
