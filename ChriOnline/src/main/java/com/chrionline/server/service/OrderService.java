package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.dao.CartDAO;
import com.chrionline.server.dao.OrderDAO;
import com.chrionline.server.dao.PaymentDAO;
import com.chrionline.server.dao.ProductDAO;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OrderService {

    private final OrderDAO orderDAO = new OrderDAO();
    private final CartDAO cartDAO = new CartDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final Gson gson = new Gson();

    public Response placeOrder(Request request) {
        try {
            int userId = request.getInt("userId");
            String shippingAddress = request.getString("shippingAddress");
            String paymentMethod = request.getString("paymentMethod");
            String cardLastFour = request.getString("cardLastFour");

            if (shippingAddress == null || shippingAddress.isEmpty())
                return Response.error("Shipping address is required");
            if (paymentMethod == null || paymentMethod.isEmpty())
                return Response.error("Payment method is required");

            int cartId = cartDAO.getCartIdByUserId(userId);
            if (cartId == -1) return Response.error("Cart is empty");

            List<Map<String, Object>> cartItems = cartDAO.getCartItems(cartId);
            if (cartItems.isEmpty()) return Response.error("Cart is empty");

            for (Map<String, Object> item : cartItems) {
                int productId = (int) item.get("productId");
                int qty = (int) item.get("quantity");
                Map<String, Object> product = productDAO.getProductById(productId);
                if (product == null)
                    return Response.error("Product not found: " + productId);
                if ((int) product.get("stockQuantity") < qty)
                    return Response.error("Insufficient stock for: " + product.get("name"));
            }

            String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            double totalAmount = cartItems.stream()
                    .mapToDouble(i -> (double) i.get("subtotal")).sum();

            orderDAO.createOrder(orderId, userId, totalAmount, shippingAddress);

            for (Map<String, Object> item : cartItems) {
                int productId = (int) item.get("productId");
                int qty = (int) item.get("quantity");
                double unitPrice = (double) item.get("unitPrice");
                String snapshot = gson.toJson(Map.of(
                        "name", item.get("productName"),
                        "price", unitPrice));
                orderDAO.createOrderItem(orderId, productId, snapshot, qty, unitPrice);
                productDAO.updateStock(productId, qty);
            }

            String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String transactionRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String paymentStatus = simulatePayment();
            String paidAt = paymentStatus.equals("ACCEPTE")
                    ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : null;

            paymentDAO.createPayment(paymentId, orderId, paymentMethod,
                    paymentStatus, totalAmount, cardLastFour, transactionRef, paidAt);

            if (paymentStatus.equals("ACCEPTE")) {
                orderDAO.updateOrderStatus(orderId, "VALIDEE");
            }

            cartDAO.clearCart(cartId);

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", orderId);
            result.put("totalAmount", totalAmount);
            result.put("orderStatus", paymentStatus.equals("ACCEPTE") ? "VALIDEE" : "EN_ATTENTE");
            result.put("paymentId", paymentId);
            result.put("paymentStatus", paymentStatus);
            result.put("transactionRef", transactionRef);

            ServerLogger.info("PLACE_ORDER userId=" + userId + " orderId=" + orderId
                    + " total=" + totalAmount + " payment=" + paymentStatus);
            return Response.ok("Order placed successfully", result);
        } catch (Exception e) {
            ServerLogger.error("placeOrder: " + e.getMessage());
            return Response.error("Failed to place order: " + e.getMessage());
        }
    }

    public Response getOrderHistory(Request request) {
        try {
            int userId = request.getInt("userId");
            List<Map<String, Object>> orders = orderDAO.getOrdersByUserId(userId);
            for (Map<String, Object> order : orders) {
                List<Map<String, Object>> items =
                        orderDAO.getOrderItems((String) order.get("orderId"));
                order.put("items", items);
            }
            return Response.ok(orders);
        } catch (Exception e) {
            ServerLogger.error("getOrderHistory: " + e.getMessage());
            return Response.error("Failed to get order history: " + e.getMessage());
        }
    }

    public Response getOrderStatus(Request request) {
        try {
            String orderId = request.getString("orderId");
            Map<String, Object> order = orderDAO.getOrderById(orderId);
            if (order == null) return Response.error("Order not found");

            order.put("items", orderDAO.getOrderItems(orderId));
            order.put("payment", paymentDAO.getPaymentByOrderId(orderId));
            return Response.ok(order);
        } catch (Exception e) {
            ServerLogger.error("getOrderStatus: " + e.getMessage());
            return Response.error("Failed to get order status: " + e.getMessage());
        }
    }

    private String simulatePayment() {
        return Math.random() < 0.9 ? "ACCEPTE" : "REFUSE";
    }
}