package com.chrionline.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public class PaymentDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    public void createPayment(String paymentId, String orderId, String method,
                              String status, double amount, String cardLastFour,
                              String transactionRef, String paidAt) throws SQLException {
        String sql = "INSERT INTO payments (payment_id, order_id, method, status, amount, "
                + "card_last_four, transaction_ref, paid_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, paymentId);
            ps.setString(2, orderId);
            ps.setString(3, method);
            ps.setString(4, status);
            ps.setDouble(5, amount);
            if (cardLastFour != null) {
                ps.setString(6, cardLastFour);
            } else {
                ps.setNull(6, Types.CHAR);
            }
            ps.setString(7, transactionRef);
            if (paidAt != null) {
                ps.setString(8, paidAt);
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }
            ps.executeUpdate();
        }
    }

    public void updatePayment(String orderId, String status,
                              String transactionRef, String paidAt) throws SQLException {
        String sql = "UPDATE payments SET status = ?, transaction_ref = ?, paid_at = ? WHERE order_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, transactionRef);
            if (paidAt != null) {
                ps.setString(3, paidAt);
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }
            ps.setString(4, orderId);
            ps.executeUpdate();
        }
    }

    public Map<String, Object> getPaymentByOrderId(String orderId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE order_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> p = new HashMap<>();
                p.put("paymentId", rs.getString("payment_id"));
                p.put("orderId", rs.getString("order_id"));
                p.put("method", rs.getString("method"));
                p.put("status", rs.getString("status"));
                p.put("amount", rs.getDouble("amount"));
                p.put("cardLastFour", rs.getString("card_last_four"));
                p.put("transactionRef", rs.getString("transaction_ref"));
                p.put("paidAt", rs.getString("paid_at"));
                return p;
            }
            return null;
        }
    }
}