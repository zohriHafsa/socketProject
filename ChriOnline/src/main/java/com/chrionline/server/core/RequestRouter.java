package com.chrionline.server.core;

import com.chrionline.server.dao.NotificationDAO;
import com.chrionline.server.security.AntiReplayFilter;
import com.chrionline.server.service.*;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class RequestRouter {
    private static final PaymentValidationService paymentValidationService =
            new PaymentValidationService();
    private static final AuthService     authService     = new AuthService();
    private static final ProductService  productService  = new ProductService();
    private static final CartService     cartService     = new CartService();
    private static final OrderService    orderService    = new OrderService();
    private static final PaymentService  paymentService  = new PaymentService();
    private static final AdminService    adminService    = new AdminService();
    private static final NotificationDAO notifDAO        = new NotificationDAO();

    public static Response route(String rawJson) {
        return route(rawJson, null);
    }

    public static Response route(String rawJson, InetAddress clientAddress) {
        Request request;
        try {
            request = Request.fromJson(rawJson);
        } catch (Exception e) {
            ServerLogger.error("Invalid JSON : " + e.getMessage());
            return Response.error("Invalid JSON : " + e.getMessage());
        }

        if (request.getAction() == null) {
            ServerLogger.error("Missing action field");
            return Response.error("Missing action field.");
        }

        // ── Anti-Replay Check (AJOUTER ICI) ──
        if (!AntiReplayFilter.getInstance().validate(request.getNonce(), request.getTimestamp())) {
            return Response.error("REPLAY ATTACK DETECTED - Request rejected");
        }

        ServerLogger.info("Routing action: " + request.getAction());

        // ── Token validation ──────────────────────────────
        // Actions publiques — pas besoin de token
        switch (request.getAction()) {
            case LOGIN:
            case REGISTER:
            case VERIFY_EMAIL:
            case VERIFY_2FA:
            case FORGOT_PASSWORD:
            case VERIFY_RESET_TOKEN:
            case RESET_PASSWORD:
            case GET_PRODUCTS:
            case GET_PRODUCT_DETAIL:
            case SEARCH_PRODUCTS:
            case GET_CATEGORIES:
            case ADMIN_REGISTER_KEY:
            case ADMIN_AUTH_INIT:
            case ADMIN_AUTH_RESPONSE:
                break;
            default:
                if (!SessionManager.isValid(request.getToken())) {
                    ServerLogger.warn("Token invalide ou expiré pour action "
                            + request.getAction());
                    return Response.error(
                            "Session invalide ou expirée. Veuillez vous reconnecter.");
                }
        }

        // ── Routing ───────────────────────────────────────
        switch (request.getAction()) {

            // Auth
            case LOGIN:        return authService.login(request, clientAddress);
            case REGISTER:     return authService.register(request);
            case LOGOUT:       return authService.logout(request);
            case VERIFY_EMAIL: return authService.verifyEmail(request);
            case VERIFY_2FA:   return authService.verify2FA(request);
            case FORGOT_PASSWORD: return authService.forgotPassword(request);
            case VERIFY_RESET_TOKEN:  return authService.verifyResetToken(request);
            case RESET_PASSWORD:  return authService.resetPassword(request);
            case ADMIN_AUTH_INIT: return authService.adminAuthInit(request, clientAddress);
            case ADMIN_AUTH_RESPONSE: return authService.adminAuthResponse(request, clientAddress);
            case VALIDATE_CARD:
                return handleValidateCard(request);

            case SEND_PAYMENT_2FA:
                return handleSendPayment2FA(request);

            case VERIFY_PAYMENT_2FA:
                return handleVerifyPayment2FA(request);

            // Products (public)
            case GET_PRODUCTS:       return productService.getProducts(request);
            case GET_PRODUCT_DETAIL: return productService.getProductDetail(request);
            case SEARCH_PRODUCTS:    return productService.searchProducts(request);
            case GET_CATEGORIES:     return productService.getCategories(request);

            // Cart
            case ADD_TO_CART:      return cartService.addToCart(request);
            case REMOVE_FROM_CART: return cartService.removeFromCart(request);
            case GET_CART:         return cartService.getCart(request);
            case CLEAR_CART:       return cartService.clearCart(request);

            // Orders
            case PLACE_ORDER:       return orderService.placeOrder(request);
            case GET_ORDER_HISTORY: return orderService.getOrderHistory(request);
            case GET_ORDER_STATUS:  return orderService.getOrderStatus(request);

            // Payment
            case PROCESS_PAYMENT: return paymentService.processPayment(request);

            // Notifications
            case GET_NOTIFICATIONS:           return handleGetNotifications(request);
            case MARK_NOTIFICATION_READ:      return handleMarkNotificationRead(request);
            case MARK_ALL_NOTIFICATIONS_READ: return handleMarkAllNotificationsRead(request);

            // Admin — produits
            case ADMIN_ADD_PRODUCT:    return adminService.addProduct(request);
            case ADMIN_EDIT_PRODUCT:   return adminService.editProduct(request);
            case ADMIN_DELETE_PRODUCT: return adminService.deleteProduct(request);

            // Admin — utilisateurs
            case ADMIN_GET_USERS:   return adminService.getUsers(request);
            case ADMIN_TOGGLE_USER: return adminService.toggleUser(request);

            // Admin — commandes
            case ADMIN_UPDATE_ORDER_STATUS: return adminService.updateOrderStatus(request);
            case ADMIN_GET_ALL_ORDERS:      return adminService.getAllOrders(request);

            // Admin — categories
            case ADMIN_ADD_CATEGORY:    return productService.addCategory(request);
            case ADMIN_EDIT_CATEGORY:   return productService.editCategory(request);
            case ADMIN_DELETE_CATEGORY: return productService.deleteCategory(request);

            // Admin — Initialize new admin with RSA details
            case ADMIN_REGISTER_KEY: return adminService.registerKey(request);

            // Admin — gestion admins (SUPER_ADMIN seulement)
            case ADMIN_GET_ALL_ADMINS: {
                if (!"SUPER_ADMIN".equals(SessionManager.getRole(request.getToken())))
                    return Response.error("Acces refuse — Super Admin requis.");
                return adminService.getAllAdmins(request);
            }
            case ADMIN_CREATE_ADMIN: {
                if (!"SUPER_ADMIN".equals(SessionManager.getRole(request.getToken())))
                    return Response.error("Acces refuse — Super Admin requis.");
                return adminService.createAdmin(request);
            }
            case ADMIN_TOGGLE_ADMIN: {
                if (!"SUPER_ADMIN".equals(SessionManager.getRole(request.getToken())))
                    return Response.error("Acces refuse — Super Admin requis.");
                return adminService.toggleAdmin(request);
            }

            // UDP
            case REGISTER_UDP: return handleRegisterUdp(request, clientAddress);

            default:
                return Response.error("Unknown action : " + request.getAction());
        }
    }

    private static Response handleGetNotifications(Request request) {
        int userId = request.getInt("userId");
        if (userId <= 0) return Response.error("userId requis.");
        try {
            List<Map<String, Object>> notifs = notifDAO.getByUserId(userId);
            return Response.ok("Notifications récupérées.", notifs);
        } catch (SQLException e) {
            ServerLogger.error("GET_NOTIFICATIONS error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    private static Response handleMarkNotificationRead(Request request) {
        int notifId = request.getInt("notificationId");
        if (notifId <= 0) return Response.error("notificationId requis.");
        try {
            boolean ok = notifDAO.markAsRead(notifId);
            return ok ? Response.ok("Notification lue.", null)
                    : Response.error("Notification introuvable.");
        } catch (SQLException e) {
            ServerLogger.error("MARK_NOTIFICATION_READ error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    private static Response handleMarkAllNotificationsRead(Request request) {
        int userId = request.getInt("userId");
        if (userId <= 0) return Response.error("userId requis.");
        try {
            notifDAO.markAllAsRead(userId);
            return Response.ok("Toutes marquées lues.", null);
        } catch (SQLException e) {
            ServerLogger.error("MARK_ALL_NOTIFICATIONS_READ error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    private static Response handleRegisterUdp(Request request,
                                              InetAddress clientAddress) {
        try {
            int userId  = request.getInt("userId");
            int udpPort = request.getInt("udpPort");
            if (userId <= 0 || udpPort <= 0)
                return Response.error("userId et udpPort requis.");
            InetAddress addr = clientAddress != null
                    ? clientAddress : InetAddress.getByName("127.0.0.1");
            UDPNotifier.getInstance().registerClient(userId, addr, udpPort);
            return Response.ok("UDP enregistré pour userId=" + userId
                    + " sur port " + udpPort, null);
        } catch (Exception e) {
            ServerLogger.error("REGISTER_UDP error : " + e.getMessage());
            return Response.error("Erreur UDP : " + e.getMessage());
        }
    }
    private static Response handleValidateCard(Request request) {
        int userId = SessionManager.getUserId(request.getToken());
        if (userId <= 0) return Response.error("Session invalide");

        String cardNumber = request.getString("cardNumber");
        String expiry     = request.getString("expiry");
        String cvv        = request.getString("cvv");

        return paymentValidationService.validateCard(userId, cardNumber, expiry, cvv);
    }

    private static Response handleSendPayment2FA(Request request) {
        int userId = SessionManager.getUserId(request.getToken());
        if (userId <= 0) return Response.error("Session invalide");

        double amount  = request.getDouble("amount");
        String orderId = request.getString("orderId");
        String email   = request.getString("email");

        return paymentValidationService.send2FACode(userId, amount, orderId, email);
    }

    private static Response handleVerifyPayment2FA(Request request) {
        int userId = SessionManager.getUserId(request.getToken());
        if (userId <= 0) return Response.error("Session invalide");

        String code = request.getString("code");

        return paymentValidationService.verify2FACode(userId, code);
    }
}