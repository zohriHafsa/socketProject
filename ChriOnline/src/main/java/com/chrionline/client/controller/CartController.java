package com.chrionline.client.controller;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;

import java.util.HashMap;
import java.util.Map;

public class CartController {

    private final ServerConnection conn = ServerConnection.getInstance();

    public Response getCart(String token, int userId) {
        return conn.send(new Request(Action.GET_CART, token,
                Map.of("userId", userId)));
    }

    public Response add(String token, int userId, int productId, int quantity) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId",    userId);
        data.put("productId", productId);
        data.put("quantity",  quantity);
        return conn.send(new Request(Action.ADD_TO_CART, token, data));
    }

    public Response remove(String token, int userId, int productId) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId",    userId);
        data.put("productId", productId);
        return conn.send(new Request(Action.REMOVE_FROM_CART, token, data));
    }

    public Response clear(String token, int userId) {
        return conn.send(new Request(Action.CLEAR_CART, token,
                Map.of("userId", userId)));
    }
}