package com.chrionline.client.controller;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;

import java.util.HashMap;
import java.util.Map;

public class OrderController {

    private final ServerConnection conn = ServerConnection.getInstance();

    public Response placeOrder(String token, int userId,
                               String shippingAddress, String paymentMethod,
                               String cardLastFour) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId",          userId);
        data.put("shippingAddress", shippingAddress);
        data.put("paymentMethod",   paymentMethod);
        if (cardLastFour != null && !cardLastFour.isEmpty()) {
            data.put("cardLastFour", cardLastFour);
        }
        return conn.send(new Request(Action.PLACE_ORDER, token, data));
    }

    public Response getHistory(String token, int userId) {
        return conn.send(new Request(Action.GET_ORDER_HISTORY, token,
                Map.of("userId", userId)));
    }

    public Response getStatus(String token, String orderId) {
        return conn.send(new Request(Action.GET_ORDER_STATUS, token,
                Map.of("orderId", orderId)));
    }
}