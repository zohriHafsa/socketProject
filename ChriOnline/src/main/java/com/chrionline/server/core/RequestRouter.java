package com.chrionline.server.core;

import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

public class RequestRouter {
    public static Response route(String rawJson) {
        Request request;

        // 1. Parse JSON
        try {
            request = Request.fromJson(rawJson);
        } catch (Exception e) {
            return Response.error("Invalid JSON: " + e.getMessage());
        }

        // 2. Validate action
        if (request.getAction() == null) {
            return Response.error("Missing action field.");
        }

        // 3. Route to the right service
        switch (request.getAction()) {

            // Auth
            case LOGIN:
            case REGISTER:
            case LOGOUT:
                return handleAuth(request);

            // Products
            case GET_PRODUCTS:
            case GET_PRODUCT_DETAIL:
            case SEARCH_PRODUCTS:
                return handleProducts(request);

            // Cart
            case ADD_TO_CART:
            case REMOVE_FROM_CART:
            case GET_CART:
            case CLEAR_CART:
                return handleCart(request);

            // Orders
            case PLACE_ORDER:
            case GET_ORDER_HISTORY:
            case GET_ORDER_STATUS:
                return handleOrders(request);

            // Payment
            case PROCESS_PAYMENT:
                return handlePayment(request);

            // Admin
            case ADMIN_ADD_PRODUCT:
            case ADMIN_EDIT_PRODUCT:
            case ADMIN_DELETE_PRODUCT:
            case ADMIN_GET_USERS:
            case ADMIN_TOGGLE_USER:
            case ADMIN_UPDATE_ORDER_STATUS:
                return handleAdmin(request);

            default:
                return Response.error("Unknown action: " + request.getAction());
        }
    }

    // Placeholders (services will be wired in later)

    private static Response handleAuth(Request request) {
        // TODO: wire AuthService
        return Response.ok("Auth placeholder - action: " + request.getAction(), null);
    }

    private static Response handleProducts(Request request) {
        // TODO: wire ProductService
        return Response.ok("Products placeholder - action: " + request.getAction(), null);
    }

    private static Response handleCart(Request request) {
        // TODO: wire CartService
        return Response.ok("Cart placeholder - action: " + request.getAction(), null);
    }

    private static Response handleOrders(Request request) {
        // TODO: wire OrderService
        return Response.ok("Orders placeholder - action: " + request.getAction(), null);
    }

    private static Response handlePayment(Request request) {
        // TODO: wire PaymentService
        return Response.ok("Payment placeholder - action: " + request.getAction(), null);
    }

    private static Response handleAdmin(Request request) {
        // TODO: wire AdminService
        return Response.ok("Admin placeholder - action: " + request.getAction(), null);
    }
}
