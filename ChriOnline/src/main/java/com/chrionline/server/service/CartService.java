package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.dao.CartDAO;
import com.chrionline.server.dao.ProductDAO;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartService {

    private final CartDAO    cartDAO    = new CartDAO();
    private final ProductDAO productDAO = new ProductDAO();

    public Response addToCart(Request request) {
        int userId    = request.getInt("userId");
        int productId = request.getInt("productId");
        int quantity  = request.getInt("quantity");
        if (quantity <= 0) quantity = 1;

        try {
            Map<String, Object> product = productDAO.getProductById(productId);
            if (product == null)
                return Response.error("Produit introuvable (ID=" + productId + ").");
            if (!(boolean) product.get("isAvailable"))
                return Response.error("Ce produit n'est plus disponible.");

            int stock = ((Number) product.get("stockQuantity")).intValue();
            if (stock < quantity)
                return Response.error("Stock insuffisant (disponible : " + stock + ").");

            int cartId = cartDAO.getCartIdByUserId(userId);
            if (cartId < 0) cartId = cartDAO.createCart(userId);

            double unitPrice = ((Number) product.get("price")).doubleValue();
            cartDAO.addItem(cartId, productId, quantity, unitPrice);

            ServerLogger.info("ADD_TO_CART userId=" + userId
                    + " productId=" + productId + " qty=" + quantity);

            Map<String, Object> result = new HashMap<>();
            result.put("productId",   productId);
            result.put("productName", product.get("name"));
            result.put("quantity",    quantity);
            return Response.ok("Produit ajouté au panier.", result);

        } catch (SQLException e) {
            ServerLogger.error("ADD_TO_CART error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    public Response removeFromCart(Request request) {
        int userId    = request.getInt("userId");
        int productId = request.getInt("productId");

        try {
            int cartId = cartDAO.getCartIdByUserId(userId);
            if (cartId < 0) return Response.error("Panier introuvable.");

            if (!cartDAO.removeItem(cartId, productId))
                return Response.error("Produit non trouvé dans le panier.");

            ServerLogger.info("REMOVE_FROM_CART userId=" + userId
                    + " productId=" + productId);
            return Response.ok("Produit retiré du panier.", null);

        } catch (SQLException e) {
            ServerLogger.error("REMOVE_FROM_CART error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Response getCart(Request request) {
        int userId = request.getInt("userId");

        try {
            int cartId = cartDAO.getCartIdByUserId(userId);
            if (cartId < 0) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("cartId",    -1);
                empty.put("items",     new ArrayList<>());
                empty.put("total",     0.0);
                empty.put("itemCount", 0);
                return Response.ok("Panier vide.", empty);
            }

            List<Map<String, Object>> items = cartDAO.getCartItems(cartId);

            double total = items.stream()
                    .mapToDouble(i -> ((Number) i.get("subtotal")).doubleValue())
                    .sum();
            int itemCount = items.stream()
                    .mapToInt(i -> ((Number) i.get("quantity")).intValue())
                    .sum();

            Map<String, Object> result = new HashMap<>();
            result.put("cartId",    cartId);
            result.put("items",     items);
            result.put("total",     total);
            result.put("itemCount", itemCount);

            ServerLogger.info("GET_CART userId=" + userId + " items=" + items.size());
            return Response.ok("Panier récupéré.", result);

        } catch (SQLException e) {
            ServerLogger.error("GET_CART error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    public Response clearCart(Request request) {
        int userId = request.getInt("userId");

        try {
            int cartId = cartDAO.getCartIdByUserId(userId);
            if (cartId < 0) return Response.error("Panier introuvable.");

            cartDAO.clearCart(cartId);
            ServerLogger.info("CLEAR_CART userId=" + userId);
            return Response.ok("Panier vidé.", null);

        } catch (SQLException e) {
            ServerLogger.error("CLEAR_CART error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }
}