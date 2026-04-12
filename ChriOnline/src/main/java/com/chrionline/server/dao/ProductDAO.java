package com.chrionline.server.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ── Lecture ─────────────────────────────────

    public Map<String, Object> getProductById(int productId) throws SQLException {
        String sql = "SELECT p.*, c.name AS category_name FROM products p "
                + "LEFT JOIN categories c ON p.category_id = c.category_id "
                + "WHERE p.product_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapProduct(rs);
            return null;
        }
    }

    public List<Map<String, Object>> getAllProducts() throws SQLException {
        String sql = "SELECT p.*, c.name AS category_name FROM products p "
                + "LEFT JOIN categories c ON p.category_id = c.category_id "
                + "WHERE p.is_available = true ORDER BY p.product_id";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapProduct(rs));
        }
        return list;
    }

    public List<Map<String, Object>> getAllProductsAdmin() throws SQLException {
        String sql = "SELECT p.*, c.name AS category_name FROM products p "
                + "LEFT JOIN categories c ON p.category_id = c.category_id "
                + "ORDER BY p.product_id";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapProduct(rs));
        }
        return list;
    }

    public List<Map<String, Object>> searchProducts(String keyword) throws SQLException {
        String sql = "SELECT p.*, c.name AS category_name FROM products p "
                + "LEFT JOIN categories c ON p.category_id = c.category_id "
                + "WHERE p.is_available = true AND (p.name LIKE ? OR p.description LIKE ?) "
                + "ORDER BY p.product_id";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            String k = "%" + keyword + "%";
            ps.setString(1, k);
            ps.setString(2, k);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapProduct(rs));
        }
        return list;
    }

    public List<Map<String, Object>> searchProductsAdmin(String keyword) throws SQLException {
        String sql = "SELECT p.*, c.name AS category_name FROM products p "
                + "LEFT JOIN categories c ON p.category_id = c.category_id "
                + "WHERE (p.name LIKE ? OR p.description LIKE ?) "
                + "ORDER BY p.product_id";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            String k = "%" + keyword + "%";
            ps.setString(1, k);
            ps.setString(2, k);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapProduct(rs));
        }
        return list;
    }

    // ── Stock ──────────────────────────────────

    public boolean updateStock(int productId, int quantityToDeduct) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ? "
                + "WHERE product_id = ? AND stock_quantity >= ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, quantityToDeduct);
            ps.setInt(2, productId);
            ps.setInt(3, quantityToDeduct);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Admin CRUD ─────────────────────────────

    public int addProduct(String name, String description, double price,
            int stockQuantity, int categoryId,
            String imageUrl) throws SQLException {
        String sql = "INSERT INTO products (name, description, price, "
                + "stock_quantity, category_id, image_url, is_available) "
                + "VALUES (?, ?, ?, ?, ?, ?, true)";
        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, price);
            ps.setInt(4, stockQuantity);
            ps.setInt(5, categoryId);
            ps.setString(6, imageUrl);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
            return -1;
        }
    }

    public boolean updateProduct(int productId, String name, String description,
            double price, int stockQuantity,
            int categoryId, String imageUrl) throws SQLException {
        String sql = "UPDATE products SET name=?, description=?, price=?, "
                + "stock_quantity=?, category_id=?, image_url=? "
                + "WHERE product_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, price);
            ps.setInt(4, stockQuantity);
            ps.setInt(5, categoryId);
            ps.setString(6, imageUrl);
            ps.setInt(7, productId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Soft delete : désactive le produit. */
    public boolean deleteProduct(int productId) throws SQLException {
        String sql = "UPDATE products SET is_available = false WHERE product_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Mapping ────────────────────────────────

    private Map<String, Object> mapProduct(ResultSet rs) throws SQLException {
        Map<String, Object> p = new HashMap<>();
        p.put("productId",     rs.getInt("product_id"));
        p.put("name",          rs.getString("name"));
        p.put("description",   rs.getString("description"));
        p.put("price",         rs.getDouble("price"));
        p.put("stockQuantity", rs.getInt("stock_quantity"));
        p.put("categoryId",    rs.getInt("category_id"));
        p.put("categoryName",  rs.getString("category_name"));
        p.put("imageUrl",      rs.getString("image_url"));
        p.put("isAvailable",   rs.getBoolean("is_available"));
        return p;
    }
}