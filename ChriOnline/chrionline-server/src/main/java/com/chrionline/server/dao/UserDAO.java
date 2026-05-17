package com.chrionline.server.dao;

import com.chrionline.server.security.EncryptionHelper;

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
        // username is NOT encrypted — it's a public identifier used for login
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
            return null;
        }
    }

    public Map<String, Object> getUserByEmail(String email) throws SQLException {
        // ✅ CHANGED: search by email_hash instead of plaintext email
        String sql = "SELECT * FROM users WHERE email_hash = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, EncryptionHelper.hashEmail(email)); // hash for search
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
        // ✅ CHANGED: added email_hash column, encrypt sensitive fields
        String sql = "INSERT INTO users (username, email, email_hash, password_hash, "
                + "first_name, last_name, address, phone, role, is_active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'CLIENT', true)";
        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, EncryptionHelper.encrypt(email));         // ✅ encrypted
            ps.setString(3, EncryptionHelper.hashEmail(email));       // ✅ hash for search
            ps.setString(4, passwordHash);                             // ✅ NOT encrypted (Argon2 hash)
            ps.setString(5, EncryptionHelper.encrypt(firstName));     // ✅ encrypted
            ps.setString(6, EncryptionHelper.encrypt(lastName));      // ✅ encrypted
            ps.setString(7, EncryptionHelper.encrypt(address));       // ✅ encrypted
            ps.setString(8, EncryptionHelper.encrypt(phone));         // ✅ encrypted
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
            // mapUser() handles decryption automatically
            while (rs.next()) users.add(mapUser(rs));
        }
        return users;
    }

    public boolean toggleUserActive(int userId) throws SQLException {
        // No sensitive fields touched — unchanged
        String sql = "UPDATE users SET is_active = NOT is_active WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateUserRole(int userId, String role) throws SQLException {
        // No sensitive fields touched — unchanged
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ✅ CHANGED: mapUser now decrypts sensitive fields on every read
    private Map<String, Object> mapUser(ResultSet rs) throws SQLException {
        Map<String, Object> u = new HashMap<>();
        u.put("userId",       rs.getInt("user_id"));
        u.put("username",     rs.getString("username"));
        u.put("email",        EncryptionHelper.decrypt(rs.getString("email")));       // ✅ decrypted
        u.put("passwordHash", rs.getString("password_hash"));                          // ✅ NOT decrypted
        u.put("firstName",    EncryptionHelper.decrypt(rs.getString("first_name")));  // ✅ decrypted
        u.put("lastName",     EncryptionHelper.decrypt(rs.getString("last_name")));   // ✅ decrypted
        u.put("address",      EncryptionHelper.decrypt(rs.getString("address")));     // ✅ decrypted
        u.put("phone",        EncryptionHelper.decrypt(rs.getString("phone")));       // ✅ decrypted
        u.put("role",         rs.getString("role"));
        u.put("isActive",     rs.getBoolean("is_active"));
        u.put("isVerified",   rs.getBoolean("is_verified"));
        u.put("createdAt",    rs.getString("created_at"));
        u.put("updatedAt",    rs.getString("updated_at"));
        u.put("failedAttempts", rs.getInt("failed_attempts"));
        u.put("lockedUntil",    rs.getTimestamp("locked_until"));
        return u;
    }

    // ── All methods below are UNCHANGED ──────────────────────────

    public void saveVerifyCode(int userId, String code) throws SQLException {
        String sql = """
        UPDATE users SET verify_code = ?, verify_expires = DATE_ADD(NOW(), INTERVAL 30 MINUTE)
        WHERE user_id = ?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public boolean hasActiveOtp(int userId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE user_id = ? AND verify_code IS NOT NULL AND verify_expires > NOW()";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    public boolean verifyOtp(int userId, String code) throws SQLException {
        String sql = """
        SELECT 1 FROM users
        WHERE user_id = ? AND verify_code = ? AND verify_expires > NOW()
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            boolean valid = ps.executeQuery().next();
            if (valid) {
                PreparedStatement clear = conn().prepareStatement(
                        "UPDATE users SET verify_code = NULL, verify_expires = NULL WHERE user_id = ?");
                clear.setInt(1, userId);
                clear.executeUpdate();
            }
            return valid;
        }
    }

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

    public void saveResetToken(int userId, String token) throws SQLException {
        String deleteSql = "DELETE FROM password_reset_tokens WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(deleteSql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        String insertSql = """
            INSERT INTO password_reset_tokens (token, user_id, expires_at)
            VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 30 MINUTE))
            """;
        try (PreparedStatement ps = conn().prepareStatement(insertSql)) {
            ps.setString(1, token);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public int validateResetToken(String token) throws SQLException {
        String sql = """
            SELECT user_id FROM password_reset_tokens
            WHERE token = ?
              AND used = 0
              AND expires_at > NOW()
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("user_id");
        }
        return -1;
    }

    public void markTokenUsed(String token) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used = 1 WHERE token = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    public void updatePassword(int userId, String newHash) throws SQLException {
        // password_hash is Argon2 — NOT encrypted with AES, unchanged
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void recordFailedAttempt(int userId) throws SQLException {
        String increment = "UPDATE users SET failed_attempts = failed_attempts + 1 WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(increment)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        String getCount = "SELECT failed_attempts FROM users WHERE user_id = ?";
        int attempts = 0;
        try (PreparedStatement ps = conn().prepareStatement(getCount)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) attempts = rs.getInt("failed_attempts");
        }
        if (attempts >= 3) {
            String lockDuration;
            if      (attempts == 3) lockDuration = "15 MINUTE";
            else if (attempts == 4) lockDuration = "30 MINUTE";
            else                    lockDuration = "60 MINUTE";
            String lock = "UPDATE users SET is_active = 0, " +
                    "locked_until = DATE_ADD(NOW(), INTERVAL " + lockDuration + ") " +
                    "WHERE user_id = ?";
            try (PreparedStatement ps = conn().prepareStatement(lock)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
        }
    }

    public void resetFailedAttempts(int userId) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = 0, " +
                "locked_until = NULL, is_active = 1 WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public boolean updateProfile(int userId, String firstName, String lastName,
                                 String address, String phone) throws SQLException {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, address = ?, phone = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, EncryptionHelper.encrypt(firstName));
            ps.setString(2, EncryptionHelper.encrypt(lastName));
            ps.setString(3, EncryptionHelper.encrypt(address));
            ps.setString(4, EncryptionHelper.encrypt(phone));
            ps.setInt(5, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean checkAndUnlockIfExpired(int userId) throws SQLException {
        String sql = "SELECT locked_until, " +
                "locked_until < NOW() AS is_expired " +
                "FROM users WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                java.sql.Timestamp lockedUntil = rs.getTimestamp("locked_until");
                if (lockedUntil == null) return true;
                boolean isExpired = rs.getBoolean("is_expired");
                if (isExpired) {
                    String unlock = "UPDATE users SET is_active = 1, " +
                            "failed_attempts = 0, locked_until = NULL " +
                            "WHERE user_id = ? AND locked_until < NOW()";
                    try (PreparedStatement ps2 = conn().prepareStatement(unlock)) {
                        ps2.setInt(1, userId);
                        ps2.executeUpdate();
                    }
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}