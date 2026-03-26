package com.chrionline.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    public void createOrder(String orderId, int userId, double totalAmount,
                            String shippingAddress) throws SQLException {
        String sql = "INSERT INTO orders (order_id, user_id, status, total_amount, shipping_address) "
                + "VALUES (?, ?, 'EN_ATTENTE', ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setInt(2, userId);
            ps.setDouble(3, totalAmount);
            ps.setString(4, shippingAddress);
            ps.executeUpdate();
        }
    }

    public void createOrderItem(String orderId, int productId, String productSnapshot,
                                int quantity, double unitPrice) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, product_snapshot, quantity, unit_price) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setInt(2, productId);
            ps.setString(3, productSnapshot);
            ps.setInt(4, quantity);
            ps.setDouble(5, unitPrice);
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> getAllOrders() throws SQLException {
        String sql = "SELECT o.*, u.username FROM orders o "
                + "JOIN users u ON o.user_id = u.user_id "
                + "ORDER BY o.created_at DESC";
        List<Map<String, Object>> orders = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> o = mapOrder(rs);
                o.put("username", rs.getString("username"));
                orders.add(o);
            }
        }
        return orders;
    }

    public void updateOrderStatus(String orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, orderId);
            ps.executeUpdate();
        }
    }

    public Map<String, Object> getOrderById(String orderId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE order_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapOrder(rs);
            return null;
        }
    }

    public List<Map<String, Object>> getOrdersByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC";
        List<Map<String, Object>> orders = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) orders.add(mapOrder(rs));
        }
        return orders;
    }

    public List<Map<String, Object>> getOrderItems(String orderId) throws SQLException {
        String sql = "SELECT oi.*, p.name AS product_name FROM order_items oi "
                + "LEFT JOIN products p ON oi.product_id = p.product_id WHERE oi.order_id = ?";
        List<Map<String, Object>> items = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("orderItemId", rs.getInt("order_item_id"));
                item.put("orderId", rs.getString("order_id"));
                item.put("productId", rs.getInt("product_id"));
                item.put("productName", rs.getString("product_name"));
                item.put("productSnapshot", rs.getString("product_snapshot"));
                item.put("quantity", rs.getInt("quantity"));
                item.put("unitPrice", rs.getDouble("unit_price"));
                item.put("subtotal", rs.getDouble("unit_price") * rs.getInt("quantity"));
                items.add(item);
            }
        }
        return items;
    }

    private Map<String, Object> mapOrder(ResultSet rs) throws SQLException {
        Map<String, Object> o = new HashMap<>();
        o.put("orderId", rs.getString("order_id"));
        o.put("userId", rs.getInt("user_id"));
        o.put("status", rs.getString("status"));
        o.put("totalAmount", rs.getDouble("total_amount"));
        o.put("shippingAddress", rs.getString("shipping_address"));
        o.put("createdAt", rs.getString("created_at"));
        o.put("updatedAt", rs.getString("updated_at"));
        return o;
    }
}