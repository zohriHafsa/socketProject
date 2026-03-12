package com.chrionline.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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
}