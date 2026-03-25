package com.chrionline.server.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    public void createNotification(int userId, String message,
                                   String type, String protocol) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, message, type, protocol) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.setString(3, type);
            ps.setString(4, protocol);
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> getByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE user_id = ? "
                + "ORDER BY created_at DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapNotif(rs));
        }
        return list;
    }

    public List<Map<String, Object>> getUnreadByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = 0 "
                + "ORDER BY created_at DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapNotif(rs));
        }
        return list;
    }

    public boolean markAsRead(int notificationId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1 WHERE notification_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;
        }
    }

    public void markAllAsRead(int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public int getUnreadCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        }
    }

    private Map<String, Object> mapNotif(ResultSet rs) throws SQLException {
        Map<String, Object> n = new HashMap<>();
        n.put("notificationId", rs.getInt("notification_id"));
        n.put("userId",         rs.getInt("user_id"));
        n.put("message",        rs.getString("message"));
        n.put("type",           rs.getString("type"));
        n.put("isRead",         rs.getBoolean("is_read"));
        n.put("protocol",       rs.getString("protocol"));
        n.put("createdAt",      rs.getString("created_at"));
        return n;
    }
}