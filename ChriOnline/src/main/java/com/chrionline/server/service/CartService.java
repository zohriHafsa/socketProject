package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.dao.CartDAO;
import com.chrionline.server.dao.ProductDAO;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartService {

    private final CartDAO cartDAO = new CartDAO();
    private final ProductDAO productDAO = new ProductDAO();

    public Response getCart(Request request) {
        try {
            int userId = request.getInt("userId");
            int cartId = cartDAO.getCartIdByUserId(userId);
            if (cartId == -1) {
                return Response.ok("Cart is empty",
                        Map.of("items", List.of(), "total", 0.0, "itemCount", 0));
            }
            List<Map<String, Object>> items = cartDAO.getCartItems(cartId);
            double total = items.stream().mapToDouble(i -> (double) i.get("subtotal")).sum();
            Map<String, Object> data = new HashMap<>();
            data.put("cartId", cartId);
            data.put("items", items);
            data.put("total", total);
            data.put("itemCount", items.size());
            return Response.ok(data);
        } catch (Exception e) {
            ServerLogger.error("getCart: " + e.getMessage());
            return Response.error("Failed to get cart: " + e.getMessage());
        }
    }

    public Response addToCart(Request request) {
        try {
            int userId = request.getInt("userId");
            int productId = request.getInt("productId");
            int quantity = request.getInt("quantity");
            if (quantity <= 0) quantity = 1;

            Map<String, Object> product = productDAO.getProductById(productId);
            if (product == null) return Response.error("Product not found");
            if (!(boolean) product.get("isAvailable")) return Response.error("Product not available");
            if ((int) product.get("stockQuantity") < quantity) return Response.error("Insufficient stock");

            int cartId = cartDAO.getCartIdByUserId(userId);
            if (cartId == -1) cartId = cartDAO.createCart(userId);

            double unitPrice = (double) product.get("price");
            cartDAO.addItem(cartId, productId, quantity, unitPrice);
            ServerLogger.info("ADD_TO_CART userId=" + userId + " productId=" + productId + " qty=" + quantity);
            return Response.ok("Product added to cart", null);
        } catch (Exception e) {
            ServerLogger.error("addToCart: " + e.getMessage());
            return Response.error("Failed to add to cart: " + e.getMessage());
        }
    }

    public Response removeFromCart(Request request) {
        try {
            int userId = request.getInt("userId");
            int productId = request.getInt("productId");

            int cartId = cartDAO.getCartIdByUserId(userId);
            if (cartId == -1) return Response.error("Cart not found");

            boolean removed = cartDAO.removeItem(cartId, productId);
            if (!removed) return Response.error("Product not in cart");
            ServerLogger.info("REMOVE_FROM_CART userId=" + userId + " productId=" + productId);
            return Response.ok("Product removed from cart", null);
        } catch (Exception e) {
            ServerLogger.error("removeFromCart: " + e.getMessage());
            return Response.error("Failed to remove from cart: " + e.getMessage());
        }
    }

    public Response clearCart(Request request) {
        try {
            int userId = request.getInt("userId");
            int cartId = cartDAO.getCartIdByUserId(userId);
            if (cartId == -1) return Response.error("Cart not found");

            cartDAO.clearCart(cartId);
            ServerLogger.info("CLEAR_CART userId=" + userId);
            return Response.ok("Cart cleared", null);
        } catch (Exception e) {
            ServerLogger.error("clearCart: " + e.getMessage());
            return Response.error("Failed to clear cart: " + e.getMessage());
        }
    }
}