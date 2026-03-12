package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.dao.OrderDAO;
import com.chrionline.server.dao.PaymentDAO;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PaymentService {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    public Response processPayment(Request request) {
        try {
            String orderId = request.getString("orderId");
            String method = request.getString("paymentMethod");
            String cardLastFour = request.getString("cardLastFour");

            if (orderId == null) return Response.error("Order ID is required");

            Map<String, Object> order = orderDAO.getOrderById(orderId);
            if (order == null) return Response.error("Order not found");

            Map<String, Object> existing = paymentDAO.getPaymentByOrderId(orderId);

            String transactionRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String status = Math.random() < 0.9 ? "ACCEPTE" : "REFUSE";
            String paidAt = status.equals("ACCEPTE")
                    ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : null;

            if (existing != null) {
                if ("ACCEPTE".equals(existing.get("status")))
                    return Response.error("Order already paid");
                paymentDAO.updatePayment(orderId, status, transactionRef, paidAt);
            } else {
                if (method == null) return Response.error("Payment method is required");
                double amount = (double) order.get("totalAmount");
                String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                paymentDAO.createPayment(paymentId, orderId, method, status,
                        amount, cardLastFour, transactionRef, paidAt);
            }

            if (status.equals("ACCEPTE")) {
                orderDAO.updateOrderStatus(orderId, "VALIDEE");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", orderId);
            result.put("paymentStatus", status);
            result.put("transactionRef", transactionRef);

            ServerLogger.info("PROCESS_PAYMENT orderId=" + orderId + " status=" + status);
            return Response.ok("Payment processed", result);
        } catch (Exception e) {
            ServerLogger.error("processPayment: " + e.getMessage());
            return Response.error("Failed to process payment: " + e.getMessage());
        }
    }
}