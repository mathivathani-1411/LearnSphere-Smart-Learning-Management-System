package com.lms.dao;

import com.lms.model.Course;
import com.lms.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the courses table.
 */
public class CourseDAO {

    private static final Logger log = LoggerFactory.getLogger(CourseDAO.class);

    /** Insert a new course and return its generated ID. */
    public int create(Course course) throws SQLException {
        String sql = "INSERT INTO courses (title, description, instructor_id, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, course.getTitle());
            ps.setString(2, course.getDescription());
            ps.setInt(3, course.getInstructorId());
            ps.setString(4, course.getStatus() != null ? course.getStatus() : "DRAFT");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    /** Find all published courses (for the catalogue). */
    public List<Course> findAllPublished() throws SQLException {
        String sql = "SELECT c.*, u.name AS instructor_name "
                + "FROM courses c JOIN users u ON c.instructor_id = u.user_id "
                + "WHERE c.status = 'PUBLISHED' ORDER BY c.created_date DESC";
        return queryList(sql);
    }

    /** Find all courses owned by a specific instructor. */
    public List<Course> findByInstructor(int instructorId) throws SQLException {
        String sql = "SELECT c.*, u.name AS instructor_name "
                + "FROM courses c JOIN users u ON c.instructor_id = u.user_id "
                + "WHERE c.instructor_id = ? ORDER BY c.created_date DESC";
        return queryList(sql, instructorId);
    }

    /** Find a course by its ID. */
    public Optional<Course> findById(int courseId) throws SQLException {
        String sql = "SELECT c.*, u.name AS instructor_name "
                + "FROM courses c JOIN users u ON c.instructor_id = u.user_id "
                + "WHERE c.course_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /** Update course fields. */
    public boolean update(Course course) throws SQLException {
        String sql = "UPDATE courses SET title=?, description=?, status=? WHERE course_id=? AND instructor_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, course.getTitle());
            ps.setString(2, course.getDescription());
            ps.setString(3, course.getStatus());
            ps.setInt(4, course.getCourseId());
            ps.setInt(5, course.getInstructorId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Delete a course. Only the owning instructor can do this (checked here as extra safety). */
    public boolean delete(int courseId, int instructorId) throws SQLException {
        String sql = "DELETE FROM courses WHERE course_id=? AND instructor_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, instructorId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---- helpers ----

    private List<Course> queryList(String sql, Object... params) throws SQLException {
        List<Course> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Course mapRow(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setCourseId(rs.getInt("course_id"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setInstructorId(rs.getInt("instructor_id"));
        c.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_date");
        if (ts != null) c.setCreatedDate(ts.toLocalDateTime());
        try { c.setInstructorName(rs.getString("instructor_name")); } catch (SQLException ignored) {}
        return c;
    }
}
