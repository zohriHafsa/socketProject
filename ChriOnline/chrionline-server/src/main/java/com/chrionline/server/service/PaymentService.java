package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.core.UDPNotifier;
import com.chrionline.server.dao.NotificationDAO;
import com.chrionline.server.dao.OrderDAO;
import com.chrionline.server.dao.PaymentDAO;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PaymentService {

    private final PaymentDAO      paymentDAO      = new PaymentDAO();
    private final OrderDAO        orderDAO        = new OrderDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    public Response processPayment(Request request) {
        String orderId       = request.getString("orderId");
        String paymentMethod = request.getString("paymentMethod");
        String cardLastFour  = request.getString("cardLastFour");

        if (orderId == null)
            return Response.error("ID commande requis.");
        if (!isValidMethod(paymentMethod))
            return Response.error("Méthode invalide. Utilisez : "
                    + "CARTE_BANCAIRE, PAIEMENT_FICTIF, VIREMENT");

        try {
            // 1 ── Vérifier la commande ────────────
            Map<String, Object> order = orderDAO.getOrderById(orderId);
            if (order == null)
                return Response.error("Commande introuvable (" + orderId + ").");

            double amount = ((Number) order.get("totalAmount")).doubleValue();
            int    userId = ((Number) order.get("userId")).intValue();

            // 2 ── Paiement existant ? ─────────────
            Map<String, Object> existing = paymentDAO.getPaymentByOrderId(orderId);
            if (existing != null && "ACCEPTE".equals(existing.get("status")))
                return Response.error("Cette commande est déjà payée.");

            // 3 ── Simuler le paiement (90 %) ──────
            boolean success  = Math.random() < 0.9;
            String  status   = success ? "ACCEPTE" : "REFUSE";
            String  txnRef   = success
                    ? "TXN-" + UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase() : null;
            String  paidAt   = success ? LocalDateTime.now().toString() : null;

            // 4 ── Créer ou mettre à jour ──────────
            if (existing != null) {
                paymentDAO.updatePayment(orderId, status, txnRef, paidAt);
            } else {
                String paymentId = "PAY-" + UUID.randomUUID().toString()
                        .substring(0, 8).toUpperCase();
                paymentDAO.createPayment(paymentId, orderId, paymentMethod,
                        status, amount, cardLastFour, txnRef, paidAt);
            }

            // 5 ── Mettre à jour la commande ───────
            if (success) {
                orderDAO.updateOrderStatus(orderId, "VALIDEE");
                UDPNotifier.getInstance()
                        .notifyOrderStatusChange(userId, orderId, "VALIDEE");
                notificationDAO.createNotification(userId,
                        "Paiement accepté pour commande " + orderId,
                        "PAYMENT", "UDP");
            }

            // 6 ── Réponse ─────────────────────────
            Map<String, Object> result = new HashMap<>();
            result.put("orderId",        orderId);
            result.put("paymentStatus",  status);
            result.put("transactionRef", txnRef);
            result.put("amount",         amount);

            ServerLogger.info("PROCESS_PAYMENT " + orderId + " → " + status);
            return Response.ok(
                    success ? "Paiement accepté !" : "Paiement refusé. Réessayez.",
                    result);

        } catch (SQLException e) {
            ServerLogger.error("PROCESS_PAYMENT error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    private boolean isValidMethod(String m) {
        return "CARTE_BANCAIRE".equals(m)
                || "PAIEMENT_FICTIF".equals(m)
                || "VIREMENT".equals(m);
    }
}