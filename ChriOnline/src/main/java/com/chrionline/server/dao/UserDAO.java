package com.chrionline.server.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    public Map<String, Object> getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
            return null;
        }
    }

    public Map<String, Object> getUserByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
            return null;
        }
    }

    public Map<String, Object> getUserById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
            return null;
        }
    }

    public int createUser(String username, String email, String passwordHash,
                          String firstName, String lastName,
                          String address, String phone) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, "
                + "first_name, last_name, address, phone, role, is_active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'CLIENT', true)";
        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, firstName);
            ps.setString(5, lastName);
            ps.setString(6, address);
            ps.setString(7, phone);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
            return -1;
        }
    }

    public List<Map<String, Object>> getAllUsers() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY user_id";
        List<Map<String, Object>> users = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) users.add(mapUser(rs));
        }
        return users;
    }

    public boolean toggleUserActive(int userId) throws SQLException {
        String sql = "UPDATE users SET is_active = NOT is_active WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateUserRole(int userId, String role) throws SQLException {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private Map<String, Object> mapUser(ResultSet rs) throws SQLException {
        Map<String, Object> u = new HashMap<>();
        u.put("userId",       rs.getInt("user_id"));
        u.put("username",     rs.getString("username"));
        u.put("email",        rs.getString("email"));
        u.put("passwordHash", rs.getString("password_hash"));
        u.put("firstName",    rs.getString("first_name"));
        u.put("lastName",     rs.getString("last_name"));
        u.put("address",      rs.getString("address"));
        u.put("phone",        rs.getString("phone"));
        u.put("role",         rs.getString("role"));
        u.put("isActive",     rs.getBoolean("is_active"));
        u.put("isVerified",   rs.getBoolean("is_verified"));
        u.put("createdAt",    rs.getString("created_at"));
        u.put("updatedAt",    rs.getString("updated_at"));
        return u;
    }
    // Sauvegarder le code de vérification
    public void saveVerifyCode(int userId, String code) throws SQLException {
        String sql = "UPDATE users SET verify_code = ?, " +
                "verify_expires = DATE_ADD(NOW(), INTERVAL 10 MINUTE) " +
                "WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // Checking if user has an active, unexpired OTP for Spam Prevention
    public boolean hasActiveOtp(int userId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE user_id = ? AND verify_code IS NOT NULL AND verify_expires > NOW()";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    // Verify code for 2FA / Login
    public boolean verifyOtp(int userId, String code) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE user_id = ? AND verify_code = ? AND verify_expires > NOW()";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String update = "UPDATE users SET verify_code = NULL WHERE user_id = ?";
                try (PreparedStatement ps2 = conn().prepareStatement(update)) {
                    ps2.setInt(1, userId);
                    ps2.executeUpdate();
                }
                return true;
            }
            return false;
        }
    }

    // Vérifier le code pour l'inscription
    public boolean verifyCode(int userId, String code) throws SQLException {
        String sql = "SELECT verify_code FROM users " +
                "WHERE user_id = ? " +
                "AND verify_code = ? " +
                "AND verify_expires > NOW() " +
                "AND is_verified = 0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Marquer comme vérifié
                String update = "UPDATE users SET is_verified = 1, " +
                        "verify_code = NULL WHERE user_id = ?";
                try (PreparedStatement ps2 = conn().prepareStatement(update)) {
                    ps2.setInt(1, userId);
                    ps2.executeUpdate();
                }
                return true;
            }
            return false;
        }
    }

    public boolean updatePassword(int userId, String newHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}