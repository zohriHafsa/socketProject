package com.chrionline.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UserDAO {

    // Gets a fresh DB connection from the singleton
    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // Find a user by their username (used in login)
    public Map<String, Object> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);  // "?" gets replaced by the actual username
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);  // found → convert to Map
            return null;  // not found
        }
    }

    // Find a user by email (used in register to check duplicates)
    public Map<String, Object> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
            return null;
        }
    }

    // Find a user by ID
    public Map<String, Object> findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
            return null;
        }
    }

    // Insert a new user into the DB, returns the new user_id
    public int createUser(String username, String email, String passwordHash,
                          String firstName, String lastName,
                          String address, String phone) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, " +
                "first_name, last_name, address, phone) VALUES (?,?,?,?,?,?,?)";
        // RETURN_GENERATED_KEYS tells JDBC to give us back the auto-increment ID
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, firstName);
            ps.setString(5, lastName);
            ps.setString(6, address);
            ps.setString(7, phone);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);  // return the new ID
            return -1;
        }
    }

    // Converts a DB row (ResultSet) into a Map<String, Object>
    // This is how Java represents a "User object" in this project
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
        return u;
    }
}
