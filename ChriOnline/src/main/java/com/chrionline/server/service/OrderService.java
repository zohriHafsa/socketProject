package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.core.UDPNotifier;
import com.chrionline.server.dao.*;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.google.gson.Gson;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class OrderService {

    private final OrderDAO        orderDAO        = new OrderDAO();
    private final CartDAO         cartDAO         = new CartDAO();
    private final ProductDAO      productDAO      = new ProductDAO();
    private final PaymentDAO      paymentDAO      = new PaymentDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    // ═══════════ PLACE ORDER ═══════════════════

    public Response placeOrder(Request request) {
        int    userId          = request.getInt("userId");
        String shippingAddress = request.getString("shippingAddress");
        String paymentMethod   = request.getString("paymentMethod");
        String cardLastFour    = request.getString("cardLastFour");

        if (shippingAddress == null || shippingAddress.isEmpty())
            return Response.error("Adresse de livraison requise.");
        if (!isValidPaymentMethod(paymentMethod))
            return Response.error("Méthode invalide. Utilisez : "
                    + "CARTE_BANCAIRE, PAIEMENT_FICTIF, VIREMENT");

        try {
            // 1 ── Panier ──────────────────────────
            int cartId = cartDAO.getCartIdByUserId(userId);
            if (cartId < 0) return Response.error("Panier introuvable.");

            List<Map<String, Object>> cartItems = cartDAO.getCartItems(cartId);
            if (cartItems.isEmpty()) return Response.error("Le panier est vide.");

            // 2 ── Total ───────────────────────────
            double totalAmount = cartItems.stream()
                    .mapToDouble(i -> ((Number) i.get("subtotal")).doubleValue())
                    .sum();

            // 3 ── Créer la commande ───────────────
            String orderId = "ORD-" + UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase();

            orderDAO.createOrder(orderId, userId, totalAmount, shippingAddress);

            // 4 ── Créer les articles + décrémenter stock
            for (Map<String, Object> item : cartItems) {
                int    pid       = ((Number) item.get("productId")).intValue();
                int    qty       = ((Number) item.get("quantity")).intValue();
                double unitPrice = ((Number) item.get("unitPrice")).doubleValue();
                String pName     = (String)  item.get("productName");
                double pPrice    = ((Number) item.get("productPrice")).doubleValue();

                String snapshot = new Gson().toJson(Map.of(
                        "name", pName != null ? pName : "?",
                        "price", pPrice));

                orderDAO.createOrderItem(orderId, pid, snapshot, qty, unitPrice);

                if (!productDAO.updateStock(pid, qty))
                    ServerLogger.warn("Stock insuffisant produit ID=" + pid);
            }

            // 5 ── Paiement simulé (90 % succès) ──
            String  paymentId      = "PAY-" + UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase();
            boolean paymentSuccess = Math.random() < 0.9;
            String  paymentStatus  = paymentSuccess ? "ACCEPTE" : "REFUSE";
            String  txnRef         = paymentSuccess
                    ? "TXN-" + UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase() : null;
            String  paidAt         = paymentSuccess
                    ? LocalDateTime.now().toString() : null;

            paymentDAO.createPayment(paymentId, orderId, paymentMethod,
                    paymentStatus, totalAmount, cardLastFour, txnRef, paidAt);

            // 6 ── Statut commande ─────────────────
            String orderStatus = paymentSuccess ? "VALIDEE" : "EN_ATTENTE";
            if (paymentSuccess)
                orderDAO.updateOrderStatus(orderId, orderStatus);

            // 7 ── Vider le panier ─────────────────
            cartDAO.clearCart(cartId);

            // 8 ── Notification UDP + DB ────────────
            String notifMsg = paymentSuccess
                    ? "Commande " + orderId + " confirmée ! Total : " + totalAmount + " €"
                    : "Commande " + orderId + " en attente de paiement.";

            notificationDAO.createNotification(userId, notifMsg, "ORDER", "UDP");

            if (paymentSuccess)
                UDPNotifier.getInstance()
                        .notifyOrderConfirmation(userId, orderId, totalAmount);

            // 9 ── Réponse ─────────────────────────
            Map<String, Object> result = new HashMap<>();
            result.put("orderId",        orderId);
            result.put("totalAmount",    totalAmount);
            result.put("orderStatus",    orderStatus);
            result.put("paymentId",      paymentId);
            result.put("paymentStatus",  paymentStatus);
            result.put("transactionRef", txnRef);

            ServerLogger.info("PLACE_ORDER " + orderId + " userId=" + userId
                    + " total=" + totalAmount + " payment=" + paymentStatus);
            return Response.ok("Commande créée avec succès !", result);

        } catch (SQLException e) {
            ServerLogger.error("PLACE_ORDER error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ═══════════ ORDER HISTORY ═════════════════

    public Response getOrderHistory(Request request) {
        int userId = request.getInt("userId");
        try {
            List<Map<String, Object>> orders = orderDAO.getOrdersByUserId(userId);
            ServerLogger.info("GET_ORDER_HISTORY userId=" + userId
                    + " count=" + orders.size());
            return Response.ok("Historique des commandes.", orders);
        } catch (SQLException e) {
            ServerLogger.error("GET_ORDER_HISTORY error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ═══════════ ORDER STATUS (avec items + payment) ═══

    public Response getOrderStatus(Request request) {
        String orderId = request.getString("orderId");
        if (orderId == null || orderId.isEmpty())
            return Response.error("ID de commande requis.");

        try {
            Map<String, Object> order = orderDAO.getOrderById(orderId);
            if (order == null)
                return Response.error("Commande introuvable (" + orderId + ").");

            // ── Ajouter les articles ─────────────
            List<Map<String, Object>> items = orderDAO.getOrderItems(orderId);
            order.put("items", items);

            // ── Ajouter le paiement ──────────────
            Map<String, Object> payment = paymentDAO.getPaymentByOrderId(orderId);
            if (payment != null) order.put("payment", payment);

            ServerLogger.info("GET_ORDER_STATUS " + orderId);
            return Response.ok("Détails de la commande.", order);

        } catch (SQLException e) {
            ServerLogger.error("GET_ORDER_STATUS error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ═══════════ UTILS ═════════════════════════

    private boolean isValidPaymentMethod(String m) {
        return "CARTE_BANCAIRE".equals(m)
                || "PAIEMENT_FICTIF".equals(m)
                || "VIREMENT".equals(m);
    }
}