package com.chrionline.client.controller;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;

import java.util.Map;

public class ProductController {

    private final ServerConnection conn = ServerConnection.getInstance();

    public Response getAll(String token) {
        return conn.send(new Request(Action.GET_PRODUCTS, token, null));
    }

    public Response getDetail(String token, int productId) {
        return conn.send(new Request(Action.GET_PRODUCT_DETAIL, token,
                Map.of("productId", productId)));
    }

    public Response search(String token, String keyword) {
        return conn.send(new Request(Action.SEARCH_PRODUCTS, token,
                Map.of("keyword", keyword)));
    }
}