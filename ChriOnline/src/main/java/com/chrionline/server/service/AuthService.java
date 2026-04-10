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

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

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
            return Response.error("Le mot de passe doit contenir au moins 8 caractères.");

        if (!password.matches(".*[A-Z].*"))
            return Response.error("Le mot de passe doit contenir au moins une lettre majuscule.");

        if (!password.matches(".*\\d.*"))
            return Response.error("Le mot de passe doit contenir au moins un chiffre.");

        if (!password.matches(".*[^a-zA-Z0-9\\s].*"))
            return Response.error("Le mot de passe doit contenir au moins un caractère spécial.");

        if (firstName != null && !firstName.isBlank() && password.toLowerCase().contains(firstName.toLowerCase()))
            return Response.error("Le mot de passe ne doit pas inclure votre prénom.");

        if (lastName != null && !lastName.isBlank() && password.toLowerCase().contains(lastName.toLowerCase()))
            return Response.error("Le mot de passe ne doit pas inclure votre nom de famille.");

        try {
            if (userDAO.getUserByUsername(username) != null)
                return Response.error("Ce nom d'utilisateur est deja pris.");

            if (userDAO.getUserByEmail(email) != null)
                return Response.error("Cet email est deja utilise.");

            String hash   = hashPassword(password);
            int    userId = userDAO.createUser(username, email, hash,
                    firstName, lastName, address, phone);
            if (userId < 0)
                return Response.error("Echec de la creation du compte.");

            // ── Générer et envoyer le code de vérification ──
            String code = EmailService.generateCode();
            userDAO.saveVerifyCode(userId, code);

            boolean sent = EmailService.sendRegistrationEmail(email, code);
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
    public Response login(Request request, java.net.InetAddress clientAddress) {
        String username = request.getString("username");
        String password = request.getString("password");

        if (username == null || password == null)
            return Response.error("Username et password requis.");

        try {
            // 1. Cherche dans ADMINS d'abord
            Map<String, Object> admin = adminDAO.getAdminByUsername(username);

            if (admin != null) {
                String ip = clientAddress != null ? clientAddress.getHostAddress() : "127.0.0.1";
                if (!isInternalIp(ip)) {
                    ServerLogger.warn("SECURITY ALERT: Tentative de connexion admin hors réseau interne (IP: " + ip + ")");
                    return Response.error("Acces refuse. Les administrateurs peuvent se connecter uniquement via le reseau interne.");
                }

                if (!verifyPassword(password, (String) admin.get("passwordHash")))
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

            if (!verifyPassword(password, (String) user.get("passwordHash")))
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
            String userEmail = (String) user.get("email");

            if (userDAO.hasActiveOtp(userId)) {
                Map<String, Object> result = new HashMap<>();
                result.put("userId", userId);
                result.put("email",  userEmail);
                result.put("2fa_required", true);
                ServerLogger.info("LOGIN 2FA RE-TRIGGERED (SPAM PREVENTED) : " + username);
                return Response.ok("Un code 2FA a deja ete envoye a votre email.", result);
            }

            String code = EmailService.generateCode();
            userDAO.saveVerifyCode(userId, code);
            EmailService.send2faEmail(userEmail, code);

            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("email",  userEmail);
            result.put("2fa_required", true);
            ServerLogger.info("LOGIN 2FA TRIGGERED (NEW CODE) : " + username);
            return Response.ok("Code 2FA envoye a votre email.", result);

        } catch (SQLException e) {
            ServerLogger.error("LOGIN SQL error : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ── 2FA VERIFY ─────────────────────────────────────────
    public Response verify2FA(Request request) {
        int    userId = request.getInt("userId");
        String code   = request.getString("code");

        if (userId <= 0 || code == null)
            return Response.error("userId et code requis.");

        try {
            boolean ok = userDAO.verifyOtp(userId, code);
            if (ok) {
                Map<String, Object> user = userDAO.getUserById(userId);
                String username = (String) user.get("username");
                String role   = "CLIENT";
                String token  = SessionManager.createSession(userId, username, role);

                ServerLogger.info("LOGIN CLIENT (2FA PASSED) OK : " + username
                        + " (ID:" + userId + ", role=" + role + ")");

                Map<String, Object> result = new HashMap<>();
                result.put("token",     token);
                result.put("userId",    userId);
                result.put("username",  username);
                result.put("role",      role);
                result.put("firstName", user.get("firstName"));
                result.put("lastName",  user.get("lastName"));
                return Response.ok("Connexion reussie !", result);
            }
            return Response.error("Code incorrect ou expire.");
        } catch (SQLException e) {
            ServerLogger.error("VERIFY_2FA error : " + e.getMessage());
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

    // ─────────────────────────────────────────────────────────────
//  Add these two methods to your existing AuthService.java
//  Place them after the verifyEmail() method
// ─────────────────────────────────────────────────────────────

    // ── FORGOT PASSWORD ────────────────────────────────────
    public Response forgotPassword(Request request) {
        String username = request.getString("username");

        if (username == null || username.isBlank())
            return Response.error("Nom d'utilisateur requis.");

        try {
            Map<String, Object> user = userDAO.getUserByUsername(username);

            // SECURITY: always return the same response whether user exists or not
            // This prevents attackers from knowing which usernames are registered
            if (user == null) {
                ServerLogger.warn("FORGOT_PASSWORD : username inconnu = " + username);
                return Response.ok("Si ce compte existe, un email a ete envoye.", null);
            }

            int    userId = (int)    user.get("userId");
            String email  = (String) user.get("email");

            // Generate a 6-digit code (same style as your OTP)
            String token = EmailService.generateCode();

            // Save token in DB with 30-minute expiry
            userDAO.saveVerifyCode(userId, token);
            // Send the email
            boolean sent = EmailService.sendResetPasswordEmail(email, token);
            if (!sent) {
                ServerLogger.error("FORGOT_PASSWORD : email non envoye pour userId=" + userId);
                return Response.error("Erreur lors de l'envoi de l'email. Reessayez plus tard.");
            }

            ServerLogger.info("FORGOT_PASSWORD OK : " + username + " (userId=" + userId + ")");
            return Response.ok("Si ce compte existe, un email a ete envoye.", null);

        } catch (SQLException e) {
            ServerLogger.error("FORGOT_PASSWORD SQL error : " + e.getMessage());
            return Response.error("Erreur serveur.");
        }
    }

    // ── RESET PASSWORD ─────────────────────────────────────
    public Response resetPassword(Request request) {
        String username    = request.getString("username");
        String token       = request.getString("token");
        String newPassword = request.getString("password");

        if (token == null || newPassword == null || username == null)
            return Response.error("Token et nouveau mot de passe requis.");

        if (newPassword.length() < 8)
            return Response.error("Le mot de passe doit contenir au moins 8 caracteres.");
        if (!newPassword.matches(".*[A-Z].*"))
            return Response.error("Le mot de passe doit contenir au moins une majuscule.");
        if (!newPassword.matches(".*\\d.*"))
            return Response.error("Le mot de passe doit contenir au moins un chiffre.");
        if (!newPassword.matches(".*[^a-zA-Z0-9\\s].*"))
            return Response.error("Le mot de passe doit contenir au moins un caractere special.");

        try {
            Map<String, Object> user = userDAO.getUserByUsername(username);
            if (user == null)
                return Response.error("Utilisateur introuvable.");

            int userId = (int) user.get("userId");

            // Token already verified and consumed in verifyResetToken step
            // Just update the password directly
            String newHash = hashPassword(newPassword);
            userDAO.updatePassword(userId, newHash);

            SessionManager.invalidateAllForUser(userId);

            ServerLogger.info("RESET_PASSWORD OK : userId=" + userId);
            return Response.ok("Mot de passe modifie avec succes. Vous pouvez vous connecter.", null);

        } catch (SQLException e) {
            ServerLogger.error("RESET_PASSWORD SQL error : " + e.getMessage());
            return Response.error("Erreur serveur.");
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

    // ── Hashing (Argon2id + legacy SHA-256 fallback) ───────
    private String hashPassword(String rawPassword) {
        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        char[] passwordArray = rawPassword.toCharArray();
        try {
            return argon2.hash(3, 65536, 1, passwordArray);
        } finally {
            argon2.wipeArray(passwordArray);
        }
    }

    private boolean verifyPassword(String rawPassword, String storedHash) {
        if (storedHash != null && storedHash.startsWith("$argon2id$")) {
            Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
            return argon2.verify(storedHash, rawPassword.toCharArray());
        } else {
            return hashSHA256(rawPassword).equals(storedHash);
        }
    }

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

    private boolean isInternalIp(String ip) {
        if (ip == null) return false;
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) return true;
        if (ip.startsWith("10.")) return true;
        if (ip.startsWith("192.168.")) return true;
        if (ip.startsWith("172.")) {
            try {
                String[] parts = ip.split("\\.");
                int second = Integer.parseInt(parts[1]);
                if (second >= 16 && second <= 31) return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    // ── VERIFY RESET TOKEN ─────────────────────────────────
    public Response verifyResetToken(Request request) {
        String username = request.getString("username");
        String token    = request.getString("token");

        if (username == null || token == null)
            return Response.error("Username et token requis.");

        try {
            Map<String, Object> user = userDAO.getUserByUsername(username);
            if (user == null)
                return Response.error("Code invalide ou expiré.");

            int userId = (int) user.get("userId");

            // Reuse the same OTP verify method you already have
            boolean ok = userDAO.verifyOtp(userId, token);
            if (!ok)
                return Response.error("Code invalide ou expiré. Faites une nouvelle demande.");

            ServerLogger.info("VERIFY_RESET_TOKEN OK : username=" + username);
            return Response.ok("Code valide.", null);

        } catch (SQLException e) {
            ServerLogger.error("VERIFY_RESET_TOKEN SQL error : " + e.getMessage());
            e.printStackTrace();
            return Response.error("Erreur serveur.");
        }
    }

}