package com.chrionline.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    public Map<String, Object> getProductById(int productId) throws SQLException {
        String sql = "SELECT * FROM products WHERE product_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> p = new HashMap<>();
                p.put("productId", rs.getInt("product_id"));
                p.put("name", rs.getString("name"));
                p.put("description", rs.getString("description"));
                p.put("price", rs.getDouble("price"));
                p.put("stockQuantity", rs.getInt("stock_quantity"));
                p.put("categoryId", rs.getInt("category_id"));
                p.put("imageUrl", rs.getString("image_url"));
                p.put("isAvailable", rs.getBoolean("is_available"));
                return p;
            }
            return null;
        }
    }

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

    // Returns all available products with their category name
    public List<Map<String, Object>> getAllProducts() throws SQLException {
        String sql = "SELECT p.*, c.name AS category_name " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE p.is_available = true " +
                "ORDER BY p.product_id";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapProduct(rs));  // convert each row
        }
        return list;
    }

    // Searches products by name or description using LIKE
    public List<Map<String, Object>> searchProducts(String keyword) throws SQLException {
        String sql = "SELECT p.*, c.name AS category_name " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE p.is_available = true " +
                "AND (p.name LIKE ? OR p.description LIKE ?) " +
                "ORDER BY p.product_id";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";  // %phone% matches "smartphone", etc.
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapProduct(rs));
        }
        return list;
    }

    // Converts a DB row to a Map — used by all 3 product methods
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