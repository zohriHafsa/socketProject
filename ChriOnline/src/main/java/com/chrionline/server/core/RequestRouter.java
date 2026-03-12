package com.chrionline.server.core;

import com.chrionline.server.service.CartService;
import com.chrionline.server.service.OrderService;
import com.chrionline.server.service.PaymentService;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

public class RequestRouter {

    private static final CartService cartService = new CartService();
    private static final OrderService orderService = new OrderService();
    private static final PaymentService paymentService = new PaymentService();

    public static Response route(String rawJson) {
        Request request;
        try {
            request = Request.fromJson(rawJson);
        } catch (Exception e) {
            ServerLogger.error("Invalid JSON: " + e.getMessage());
            return Response.error("Invalid JSON: " + e.getMessage());
        }

        if (request.getAction() == null) {
            ServerLogger.error("Missing action field");
            return Response.error("Missing action field.");
        }

        ServerLogger.info("Routing action: " + request.getAction());

        switch (request.getAction()) {

            case LOGIN:
            case REGISTER:
            case LOGOUT:
                return handleAuth(request);

            case GET_PRODUCTS:
            case GET_PRODUCT_DETAIL:
            case SEARCH_PRODUCTS:
                return handleProducts(request);

            case ADD_TO_CART:
                return cartService.addToCart(request);
            case REMOVE_FROM_CART:
                return cartService.removeFromCart(request);
            case GET_CART:
                return cartService.getCart(request);
            case CLEAR_CART:
                return cartService.clearCart(request);

            case PLACE_ORDER:
                return orderService.placeOrder(request);
            case GET_ORDER_HISTORY:
                return orderService.getOrderHistory(request);
            case GET_ORDER_STATUS:
                return orderService.getOrderStatus(request);

            case PROCESS_PAYMENT:
                return paymentService.processPayment(request);

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

    private static Response handleAuth(Request request) {
        return Response.ok("Auth placeholder - action: " + request.getAction(), null);
    }

    private static Response handleProducts(Request request) {
        return Response.ok("Products placeholder - action: " + request.getAction(), null);
    }

    private static Response handleAdmin(Request request) {
        return Response.ok("Admin placeholder - action: " + request.getAction(), null);
    }
}