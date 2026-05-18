package com.chrionline.server.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    public int getCartIdByUserId(int userId) throws SQLException {
        String sql = "SELECT cart_id FROM carts WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("cart_id");
            return -1;
        }
    }

    public int createCart(int userId) throws SQLException {
        String sql = "INSERT INTO carts (user_id) VALUES (?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
            return -1;
        }
    }

    public List<Map<String, Object>> getCartItems(int cartId) throws SQLException {
        String sql = "SELECT ci.cart_item_id, ci.cart_id, ci.product_id, ci.quantity, ci.unit_price, "
                + "p.name, p.description, p.price, p.stock_quantity, p.image_url "
                + "FROM cart_items ci JOIN products p ON ci.product_id = p.product_id "
                + "WHERE ci.cart_id = ?";
        List<Map<String, Object>> items = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("cartItemId", rs.getInt("cart_item_id"));
                item.put("cartId", rs.getInt("cart_id"));
                item.put("productId", rs.getInt("product_id"));
                item.put("productName", rs.getString("name"));
                item.put("productDescription", rs.getString("description"));
                item.put("productPrice", rs.getDouble("price"));
                item.put("stockQuantity", rs.getInt("stock_quantity"));
                item.put("imageUrl", rs.getString("image_url"));
                item.put("quantity", rs.getInt("quantity"));
                item.put("unitPrice", rs.getDouble("unit_price"));
                item.put("subtotal", rs.getDouble("unit_price") * rs.getInt("quantity"));
                items.add(item);
            }
        }
        return items;
    }

    public boolean itemExists(int cartId, int productId) throws SQLException {
        String sql = "SELECT cart_item_id FROM cart_items WHERE cart_id = ? AND product_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.setInt(2, productId);
            return ps.executeQuery().next();
        }
    }

    public void addItem(int cartId, int productId, int quantity, double unitPrice) throws SQLException {
        if (itemExists(cartId, productId)) {
            String sql = "UPDATE cart_items SET quantity = quantity + ?, unit_price = ? "
                    + "WHERE cart_id = ? AND product_id = ?";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt(1, quantity);
                ps.setDouble(2, unitPrice);
                ps.setInt(3, cartId);
                ps.setInt(4, productId);
                ps.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO cart_items (cart_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt(1, cartId);
                ps.setInt(2, productId);
                ps.setInt(3, quantity);
                ps.setDouble(4, unitPrice);
                ps.executeUpdate();
            }
        }
    }

    public boolean removeItem(int cartId, int productId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        }
    }

    public void clearCart(int cartId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.executeUpdate();
        }
    }
}