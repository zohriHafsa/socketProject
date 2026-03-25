package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.dao.UserDAO;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    // ── REGISTER ───────────────────────────────
    public Response register(Request request) {
        String username  = request.getString("username");
        String email     = request.getString("email");
        String password  = request.getString("password");
        String firstName = request.getString("firstName");
        String lastName  = request.getString("lastName");
        String address   = request.getString("address");
        String phone     = request.getString("phone");

        if (username == null || email == null || password == null) {
            return Response.error("Champs obligatoires manquants (username, email, password).");
        }

        try {
            if (userDAO.getUserByUsername(username) != null)
                return Response.error("Ce nom d'utilisateur est déjà pris.");

            if (userDAO.getUserByEmail(email) != null)
                return Response.error("Cet email est déjà utilisé.");

            String hash = hashSHA256(password);
            int userId  = userDAO.createUser(username, email, hash,
                    firstName, lastName, address, phone);
            if (userId < 0)
                return Response.error("Échec de la création du compte.");

            ServerLogger.info("REGISTER OK : " + username + " (ID:" + userId + ")");

            Map<String, Object> result = new HashMap<>();
            result.put("userId",   userId);
            result.put("username", username);
            return Response.ok("Inscription réussie !", result);

        } catch (SQLException e) {
            ServerLogger.error("REGISTER SQL error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ── LOGIN ──────────────────────────────────
    @SuppressWarnings("unchecked")
    public Response login(Request request) {
        String username = request.getString("username");
        String password = request.getString("password");

        if (username == null || password == null)
            return Response.error("Username et password requis.");

        try {
            Map<String, Object> user = userDAO.getUserByUsername(username);
            if (user == null)
                return Response.error("Utilisateur introuvable.");

            if (!(boolean) user.get("isActive"))
                return Response.error("Ce compte est désactivé.");

            String storedHash = (String) user.get("passwordHash");
            if (!storedHash.equals(hashSHA256(password)))
                return Response.error("Mot de passe incorrect.");

            int    userId = (int)    user.get("userId");
            String role   = (String) user.get("role");
            String token  = SessionManager.createSession(userId, username, role);

            ServerLogger.info("LOGIN OK : " + username
                    + " (ID:" + userId + ", role=" + role + ")");

            Map<String, Object> result = new HashMap<>();
            result.put("token",     token);
            result.put("userId",    userId);
            result.put("username",  username);
            result.put("role",      role);
            result.put("firstName", user.get("firstName"));
            result.put("lastName",  user.get("lastName"));
            return Response.ok("Connexion réussie !", result);

        } catch (SQLException e) {
            ServerLogger.error("LOGIN SQL error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ── LOGOUT ─────────────────────────────────
    public Response logout(Request request) {
        String token = request.getToken();
        if (token == null)
            return Response.error("Aucun token fourni.");

        if (SessionManager.invalidate(token)) {
            ServerLogger.info("LOGOUT OK");
            return Response.ok("Déconnexion réussie.", null);
        }
        return Response.error("Session introuvable ou déjà expirée.");
    }

    // ── Hash ───────────────────────────────────
    private String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}