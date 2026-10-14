package com.lms.dao;

import com.lms.model.Notification;
import com.lms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for notifications.
 */
public class NotificationDAO {

    public void create(int userId, String message, String link) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, message, link) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.setString(3, link);
            ps.executeUpdate();
        }
    }

    public List<Notification> findByUser(int userId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id=? ORDER BY created_date DESC LIMIT 50";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public int countUnread(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void markAllRead(int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read=1 WHERE user_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getInt("notification_id"));
        n.setUserId(rs.getInt("user_id"));
        n.setMessage(rs.getString("message"));
        n.setLink(rs.getString("link"));
        n.setRead(rs.getBoolean("is_read"));
        Timestamp ts = rs.getTimestamp("created_date");
        if (ts != null) n.setCreatedDate(ts.toLocalDateTime());
        return n;
    }
}
