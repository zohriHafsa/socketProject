package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.core.UDPNotifier;
import com.chrionline.server.dao.AdminDAO;
import com.chrionline.server.dao.NotificationDAO;
import com.chrionline.server.dao.OrderDAO;
import com.chrionline.server.dao.ProductDAO;
import com.chrionline.server.dao.UserDAO;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;


import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class AdminService {

    private final ProductDAO     productDAO     = new ProductDAO();
    private final UserDAO        userDAO        = new UserDAO();
    private final OrderDAO       orderDAO       = new OrderDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final AdminDAO       adminDAO       = new AdminDAO();

    // ═══════════ PRODUITS ══════════════════════

    public Response addProduct(Request request) {
        try {
            String name        = request.getString("name");
            String description = request.getString("description");
            double price       = request.getDouble("price");
            int    stock       = request.getInt("stockQuantity");
            int    categoryId  = request.getInt("categoryId");
            String imageUrl    = request.getString("imageUrl");

            if (name == null || price <= 0)
                return Response.error("Nom et prix (> 0) requis.");

            int id = productDAO.addProduct(name, description, price,
                    stock, categoryId, imageUrl);
            ServerLogger.info("ADMIN_ADD_PRODUCT : " + name + " (ID:" + id + ")");
            return Response.ok("Produit ajouté.", Map.of("productId", id, "name", name));

        } catch (SQLException e) {
            ServerLogger.error("ADMIN_ADD_PRODUCT error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    public Response editProduct(Request request) {
        try {
            int    id          = request.getInt("productId");
            String name        = request.getString("name");
            String description = request.getString("description");
            double price       = request.getDouble("price");
            int    stock       = request.getInt("stockQuantity");
            int    categoryId  = request.getInt("categoryId");
            String imageUrl    = request.getString("imageUrl");

            if (!productDAO.updateProduct(id, name, description, price,
                    stock, categoryId, imageUrl))
                return Response.error("Produit introuvable.");

            ServerLogger.info("ADMIN_EDIT_PRODUCT : ID=" + id);
            return Response.ok("Produit modifié.", Map.of("productId", id));

        } catch (SQLException e) {
            ServerLogger.error("ADMIN_EDIT_PRODUCT error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    public Response deleteProduct(Request request) {
        try {
            int id = request.getInt("productId");
            if (!productDAO.deleteProduct(id))
                return Response.error("Produit introuvable.");
            ServerLogger.info("ADMIN_DELETE_PRODUCT : ID=" + id);
            return Response.ok("Produit supprimé (désactivé).", null);
        } catch (SQLException e) {
            ServerLogger.error("ADMIN_DELETE_PRODUCT error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    // ═══════════ USERS ═════════════════════════

    public Response getUsers(Request request) {
        try {
            List<Map<String, Object>> users = userDAO.getAllUsers();
            users.forEach(u -> u.remove("passwordHash"));
            ServerLogger.info("ADMIN_GET_USERS : " + users.size() + " user(s)");
            return Response.ok("Liste des utilisateurs.", users);
        } catch (SQLException e) {
            ServerLogger.error("ADMIN_GET_USERS error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    public Response toggleUser(Request request) {
        try {
            int userId = request.getInt("userId");
            if (!userDAO.toggleUserActive(userId))
                return Response.error("Utilisateur introuvable.");
            ServerLogger.info("ADMIN_TOGGLE_USER : ID=" + userId);
            return Response.ok("Statut utilisateur modifié.", Map.of("userId", userId));
        } catch (SQLException e) {
            ServerLogger.error("ADMIN_TOGGLE_USER error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    // ═══════════ COMMANDES ═════════════════════

    public Response getAllOrders(Request request) {
        try {
            List<Map<String, Object>> orders = orderDAO.getAllOrders();
            ServerLogger.info("ADMIN_GET_ALL_ORDERS : " + orders.size());
            return Response.ok("Toutes les commandes.", orders);
        } catch (SQLException e) {
            ServerLogger.error("ADMIN_GET_ALL_ORDERS error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    public Response updateOrderStatus(Request request) {
        try {
            String orderId   = request.getString("orderId");
            String newStatus = request.getString("status");

            if (orderId == null || newStatus == null)
                return Response.error("orderId et status requis.");

            if (!isValidOrderStatus(newStatus))
                return Response.error("Statut invalide. Valeurs : "
                        + "EN_ATTENTE, VALIDEE, EXPEDIEE, LIVREE, ANNULEE");

            orderDAO.updateOrderStatus(orderId, newStatus);

            Map<String, Object> order = orderDAO.getOrderById(orderId);
            if (order != null) {
                int userId = ((Number) order.get("userId")).intValue();
                UDPNotifier.getInstance().notifyOrderStatusChange(
                        userId, orderId, newStatus);
                notificationDAO.createNotification(
                        userId,
                        "Commande " + orderId + " → " + newStatus,
                        "ORDER", "UDP");
            }

            ServerLogger.info("ADMIN_UPDATE_ORDER_STATUS : " + orderId + " → " + newStatus);
            return Response.ok("Statut commande mis à jour.",
                    Map.of("orderId", orderId, "status", newStatus));

        } catch (SQLException e) {
            ServerLogger.error("ADMIN_UPDATE_ORDER_STATUS error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    private boolean isValidOrderStatus(String s) {
        return "EN_ATTENTE".equals(s) || "VALIDEE".equals(s)
                || "EXPEDIEE".equals(s) || "LIVREE".equals(s)
                || "ANNULEE".equals(s);
    }

    // ═══════════ GESTION ADMINS (SUPER_ADMIN) ══

    public Response getAllAdmins(Request request) {
        try {
            List<Map<String, Object>> admins = adminDAO.getAllAdmins();
            ServerLogger.info("ADMIN_GET_ALL_ADMINS : " + admins.size() + " admin(s)");
            return Response.ok("Admins récupérés.", admins);
        } catch (SQLException e) {
            ServerLogger.error("ADMIN_GET_ALL_ADMINS error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    public Response createAdmin(Request request) {
        String username  = request.getString("username");
        String email     = request.getString("email");
        String firstName = request.getString("firstName");
        String lastName  = request.getString("lastName");

        if (username == null || email == null || firstName == null || lastName == null)
            return Response.error("Tous les champs sont obligatoires.");

        try {
            if (adminDAO.usernameExists(username))
                return Response.error("Ce nom d'utilisateur est déjà pris.");

            if (adminDAO.emailExists(email))
                return Response.error("Cet email est déjà utilisé.");

            String activationToken = java.util.UUID.randomUUID().toString();
            int    adminId = adminDAO.createAdmin(username, email, activationToken,
                    firstName, lastName);
            if (adminId < 0)
                return Response.error("Échec de la création.");

            ServerLogger.info("ADMIN CREATED : " + username + " (ID:" + adminId + ")");
            return Response.ok("Administrateur créé avec succès !", java.util.Map.of("activationToken", activationToken));

        } catch (SQLException e) {
            ServerLogger.error("ADMIN_CREATE_ADMIN error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    public Response toggleAdmin(Request request) {
        int     adminId  = request.getInt("adminId");
        String activeStr = request.getString("isActive");
        boolean activate = activeStr != null && 
                (activeStr.equals("1") || activeStr.equals("1.0") || Boolean.parseBoolean(activeStr));

        if (adminId <= 0)
            return Response.error("ID admin invalide.");

        try {
            boolean ok = adminDAO.setActive(adminId, activate);
            if (!ok) return Response.error("Admin introuvable.");

            String msg = activate ? "Admin activé." : "Admin désactivé.";
            ServerLogger.info("ADMIN_TOGGLE_ADMIN : ID=" + adminId + " active=" + activate);
            return Response.ok(msg, null);

        } catch (SQLException e) {
            ServerLogger.error("ADMIN_TOGGLE_ADMIN error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    // ═══════════ HASH ══════════════════════════

    // (Removed hashing logic since Admins no longer use passwords)
}