package com.chrionline.server.dao;

import java.sql.*;
import java.util.*;

public class AdminDAO {
    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ── Login ─────────────────────────────────────────────
    public Map<String, Object> getAdminByUsername(String username)
            throws SQLException {
        String sql = "SELECT * FROM admins WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapAdmin(rs);
            return null;
        }
    }

    // ── Liste tous les admins ─────────────────────────────
    public List<Map<String, Object>> getAllAdmins() throws SQLException {
        String sql = "SELECT * FROM admins ORDER BY created_at DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapAdmin(rs));
        }
        return list;
    }

    // ── Créer un nouvel admin ─────────────────────────────
    public int createAdmin(String username, String email, String activationToken,
                           String firstName, String lastName)
            throws SQLException {
        String sql = "INSERT INTO admins " +
                "(username, email, activation_token, status, first_name, last_name) " +
                "VALUES (?, ?, ?, 'PENDING', ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, activationToken);
            ps.setString(4, firstName);
            ps.setString(5, lastName);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
            return -1;
        }
    }

    // ── Activer / Désactiver ──────────────────────────────
    public boolean setActive(int adminId, boolean active)
            throws SQLException {
        String sql = "UPDATE admins SET is_active = ? WHERE admin_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, adminId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Vérifier si username existe déjà ─────────────────
    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT admin_id FROM admins WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeQuery().next();
        }
    }

    // ── Vérifier si email existe déjà ────────────────────
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT admin_id FROM admins WHERE email = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        }
    }

    // ── Mapping ResultSet → Map ───────────────────────────
    private Map<String, Object> mapAdmin(ResultSet rs) throws SQLException {
        Map<String, Object> m = new HashMap<>();
        m.put("adminId",      rs.getInt("admin_id"));
        m.put("username",     rs.getString("username"));
        m.put("email",        rs.getString("email"));
        m.put("publicKey",    rs.getString("public_key"));
        m.put("activationToken", rs.getString("activation_token"));
        m.put("status",       rs.getString("status"));
        m.put("firstName",    rs.getString("first_name"));
        m.put("lastName",     rs.getString("last_name"));
        m.put("isActive",     rs.getBoolean("is_active"));
        m.put("isSuper",      rs.getBoolean("is_super"));
        m.put("createdAt",    rs.getString("created_at"));
        return m;
    }
}
