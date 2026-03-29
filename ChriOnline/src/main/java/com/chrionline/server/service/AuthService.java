package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.dao.AdminDAO;
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

    private final UserDAO  userDAO  = new UserDAO();
    private final AdminDAO adminDAO = new AdminDAO();

    // ── REGISTER ───────────────────────────────────────────
    public Response register(Request request) {
        String username  = request.getString("username");
        String email     = request.getString("email");
        String password  = request.getString("password");
        String firstName = request.getString("firstName");
        String lastName  = request.getString("lastName");
        String address   = request.getString("address");
        String phone     = request.getString("phone");

        if (username == null || email == null || password == null)
            return Response.error("Champs obligatoires manquants (username, email, password).");

        if (password.length() < 8)
            return Response.error("Le mot de passe doit contenir au moins 8 caracteres.");

        boolean hasUpperCase = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) { hasUpperCase = true; break; }
        }
        if (!hasUpperCase)
            return Response.error("Le mot de passe doit contenir au moins une majuscule.");

        try {
            if (userDAO.getUserByUsername(username) != null)
                return Response.error("Ce nom d'utilisateur est deja pris.");

            if (userDAO.getUserByEmail(email) != null)
                return Response.error("Cet email est deja utilise.");

            String hash   = hashSHA256(password);
            int    userId = userDAO.createUser(username, email, hash,
                    firstName, lastName, address, phone);
            if (userId < 0)
                return Response.error("Echec de la creation du compte.");

            // ── Générer et envoyer le code de vérification ──
            String code = EmailService.generateCode();
            userDAO.saveVerifyCode(userId, code);

            boolean sent = EmailService.sendVerificationCode(email, code);
            if (!sent)
                return Response.error("Compte cree mais email non envoye. Contactez le support.");

            ServerLogger.info("REGISTER OK + EMAIL SENT : " + username + " (ID:" + userId + ")");

            Map<String, Object> result = new HashMap<>();
            result.put("userId",   userId);
            result.put("username", username);
            result.put("email",    email);
            return Response.ok("Code de verification envoye !", result);

        } catch (SQLException e) {
            ServerLogger.error("REGISTER SQL error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ── LOGIN ──────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Response login(Request request) {
        String username = request.getString("username");
        String password = request.getString("password");

        if (username == null || password == null)
            return Response.error("Username et password requis.");

        String hash = hashSHA256(password);

        try {
            // 1. Cherche dans ADMINS d'abord
            Map<String, Object> admin = adminDAO.getAdminByUsername(username);

            if (admin != null) {
                if (!hash.equals(admin.get("passwordHash")))
                    return Response.error("Mot de passe incorrect.");

                if (!(boolean) admin.get("isActive"))
                    return Response.error("Ce compte admin est desactive.");

                int    adminId = (int) admin.get("adminId");
                String role    = (boolean) admin.get("isSuper")
                        ? "SUPER_ADMIN" : "ADMIN";
                String token   = SessionManager.createSession(adminId, username, role);

                ServerLogger.info("LOGIN ADMIN OK : " + username
                        + " (ID:" + adminId + ", role=" + role + ")");

                Map<String, Object> result = new HashMap<>();
                result.put("token",     token);
                result.put("userId",    adminId);
                result.put("username",  username);
                result.put("role",      role);
                result.put("firstName", admin.get("firstName"));
                result.put("lastName",  admin.get("lastName"));
                return Response.ok("Connexion reussie !", result);
            }

            // 2. Cherche dans USERS
            Map<String, Object> user = userDAO.getUserByUsername(username);

            if (user == null)
                return Response.error("Utilisateur introuvable.");

            if (!(boolean) user.get("isActive"))
                return Response.error("Ce compte est desactive.");

            if (!hash.equals(user.get("passwordHash")))
                return Response.error("Mot de passe incorrect.");

            // ── Vérification email obligatoire ──────────────
            Object verified = user.get("isVerified");
            boolean isVerified = verified != null &&
                    (Boolean.TRUE.equals(verified) ||
                            "1".equals(verified.toString()) ||
                            "true".equalsIgnoreCase(verified.toString()));

            if (!isVerified)
                return Response.error(
                        "Veuillez verifier votre email avant de vous connecter.");

            int    userId = (int)   user.get("userId");
            String role   = "CLIENT";
            String token  = SessionManager.createSession(userId, username, role);

            ServerLogger.info("LOGIN CLIENT OK : " + username
                    + " (ID:" + userId + ", role=" + role + ")");

            Map<String, Object> result = new HashMap<>();
            result.put("token",     token);
            result.put("userId",    userId);
            result.put("username",  username);
            result.put("role",      role);
            result.put("firstName", user.get("firstName"));
            result.put("lastName",  user.get("lastName"));
            return Response.ok("Connexion reussie !", result);

        } catch (SQLException e) {
            ServerLogger.error("LOGIN SQL error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ── VERIFY EMAIL ───────────────────────────────────────
    public Response verifyEmail(Request request) {
        int    userId = request.getInt("userId");
        String code   = request.getString("code");

        if (userId <= 0 || code == null)
            return Response.error("userId et code requis.");

        try {
            boolean ok = userDAO.verifyCode(userId, code);
            if (ok) {
                ServerLogger.info("EMAIL VERIFIED : userId=" + userId);
                return Response.ok(
                        "Email verifie ! Vous pouvez maintenant vous connecter.", null);
            }
            return Response.error("Code incorrect ou expire.");
        } catch (SQLException e) {
            ServerLogger.error("VERIFY_EMAIL error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    // ── LOGOUT ─────────────────────────────────────────────
    public Response logout(Request request) {
        String token = request.getToken();
        if (token == null)
            return Response.error("Aucun token fourni.");

        if (SessionManager.invalidate(token)) {
            ServerLogger.info("LOGOUT OK");
            return Response.ok("Deconnexion reussie.", null);
        }
        return Response.error("Session introuvable ou deja expiree.");
    }

    // ── Hash SHA-256 ───────────────────────────────────────
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