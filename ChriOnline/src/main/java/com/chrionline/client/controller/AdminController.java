package com.chrionline.client.controller;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;

import java.util.HashMap;
import java.util.Map;

public class AdminController {

    private final ServerConnection conn = ServerConnection.getInstance();

    // ── Produits ────────────────────────────────

    public Response addProduct(String token, String name, String description,
                               double price, int stock, int categoryId,
                               String imageUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("name",          name);
        data.put("description",   description);
        data.put("price",         price);
        data.put("stockQuantity", stock);
        data.put("categoryId",    categoryId);
        data.put("imageUrl",      imageUrl);
        return conn.send(new Request(Action.ADMIN_ADD_PRODUCT, token, data));
    }

    public Response editProduct(String token, int productId, String name,
                                String description, double price, int stock,
                                int categoryId, String imageUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId",     productId);
        data.put("name",          name);
        data.put("description",   description);
        data.put("price",         price);
        data.put("stockQuantity", stock);
        data.put("categoryId",    categoryId);
        data.put("imageUrl",      imageUrl);
        return conn.send(new Request(Action.ADMIN_EDIT_PRODUCT, token, data));
    }

    public Response deleteProduct(String token, int productId) {
        return conn.send(new Request(Action.ADMIN_DELETE_PRODUCT, token,
                Map.of("productId", productId)));
    }

    // ── Users ───────────────────────────────────

    public Response getUsers(String token) {
        return conn.send(new Request(Action.ADMIN_GET_USERS, token, null));
    }

    public Response toggleUser(String token, int userId) {
        return conn.send(new Request(Action.ADMIN_TOGGLE_USER, token,
                Map.of("userId", userId)));
    }

    // ── Commandes ───────────────────────────────

    public Response getAllOrders(String token) {
        return conn.send(new Request(Action.ADMIN_GET_ALL_ORDERS, token, null));
    }

    public Response updateOrderStatus(String token, String orderId, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("status",  status);
        return conn.send(new Request(Action.ADMIN_UPDATE_ORDER_STATUS, token, data));
    }
}