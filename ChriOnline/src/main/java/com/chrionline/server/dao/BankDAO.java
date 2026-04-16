package com.chrionline.server.dao;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class BankDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    /**
     * Récupère les infos d'une banque via le BIN (6 premiers chiffres de la carte).
     * @return Map avec bankName, bankCode, logoFilename, ou null si non trouvé
     */
    public Map<String, Object> getBankByBIN(String bin) throws SQLException {
        if (bin == null || bin.length() < 6) return null;

        String binPrefix = bin.substring(0, 6);

        String sql = "SELECT bank_name, bank_code, logo_filename, is_active " +
                "FROM moroccan_banks WHERE bin_prefix = ? AND is_active = 1";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, binPrefix);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Map<String, Object> bank = new HashMap<>();
                bank.put("bankName",     rs.getString("bank_name"));
                bank.put("bankCode",     rs.getString("bank_code"));
                bank.put("logoFilename", rs.getString("logo_filename"));
                bank.put("isActive",     rs.getBoolean("is_active"));
                return bank;
            }
        }

        return null;
    }

    /**
     * Valide le code banque d'un RIB marocain (format : 024 780 0001234567890 12).
     * @return true si le code banque existe
     */
    public boolean isValidBankCode(String bankCode) throws SQLException {
        String sql = "SELECT 1 FROM moroccan_banks WHERE bank_code = ? AND is_active = 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, bankCode);
            return ps.executeQuery().next();
        }
    }

    /**
     * Enregistre une tentative de paiement échouée.
     */
    public void recordFailedAttempt(int userId, String cardNumber, String reason)
            throws SQLException {
        String sql = "INSERT INTO payment_attempts (user_id, card_number, failure_reason) " +
                "VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, maskCardNumber(cardNumber));
            ps.setString(3, reason);
            ps.executeUpdate();
        }
    }

    /**
     * Compte les tentatives échouées dans les 30 dernières minutes.
     */
    public int getRecentFailedAttempts(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payment_attempts " +
                "WHERE user_id = ? AND attempted_at > DATE_SUB(NOW(), INTERVAL 30 MINUTE)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Sauvegarde un code 2FA pour paiement.
     */
    public void save2FACode(int userId, String code, double amount, String orderId)
            throws SQLException {
        String sql = "INSERT INTO payment_2fa_codes (user_id, code, amount, order_id, expires_at) " +
                "VALUES (?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL 5 MINUTE))";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ps.setDouble(3, amount);
            ps.setString(4, orderId);
            ps.executeUpdate();
        }
    }

    /**
     * Vérifie et consomme un code 2FA.
     */
    public boolean verify2FACode(int userId, String code) throws SQLException {
        String sql = "SELECT code_id FROM payment_2fa_codes " +
                "WHERE user_id = ? AND code = ? AND is_used = 0 AND expires_at > NOW()";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int codeId = rs.getInt("code_id");

                // Marquer comme utilisé
                String update = "UPDATE payment_2fa_codes SET is_used = 1 WHERE code_id = ?";
                try (PreparedStatement ps2 = conn().prepareStatement(update)) {
                    ps2.setInt(1, codeId);
                    ps2.executeUpdate();
                }

                return true;
            }
        }
        return false;
    }

    /**
     * Masque un numéro de carte : 1234567890123456 → **** **** **** 3456
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        String cleaned = cardNumber.replaceAll("\\s+", "");
        return "**** **** **** " + cleaned.substring(cleaned.length() - 4);
    }
}