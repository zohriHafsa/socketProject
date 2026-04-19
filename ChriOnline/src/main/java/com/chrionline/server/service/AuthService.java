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

import com.chrionline.server.security.ChallengeManager;
import com.chrionline.shared.security.ECDSAUtils;

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

    // ── ADMIN RSA AUTH INIT ────────────────────────────────
    public Response adminAuthInit(Request request, java.net.InetAddress clientAddress) {
        String username = request.getString("username");
        if (username == null || username.isBlank()) {
            return Response.error("Username requis.");
        }

        try {
            Map<String, Object> admin = adminDAO.getAdminByUsername(username);
            if (admin == null) {
                return Response.error("Administrateur introuvable.");
            }

            String ip = clientAddress != null ? clientAddress.getHostAddress() : "127.0.0.1";
            if (!isInternalIp(ip)) {
                ServerLogger.warn("SECURITY ALERT (RSA): Tentative admin hors réseau interne (IP: " + ip + ")");
                return Response.error("Acces refuse. Les administrateurs peuvent se connecter uniquement via le reseau interne.");
            }

            if (!(boolean) admin.get("isActive")) {
                return Response.error("Ce compte admin est desactive.");
            }

            String status = (String) admin.get("status");
            if ("PENDING".equals(status)) {
                // Return a specific custom response so the client knows to route to AdminInitScreen
                return Response.ok("NEEDS_INIT", Map.of("status", "PENDING"));
            }

            // Normal active scenario -> generate challenge
            String challenge = ChallengeManager.getInstance().createChallenge(username);
            ServerLogger.info("ADMIN_AUTH_INIT: Challenge généré pour " + username);
            return Response.ok("Challenge généré.", Map.of("challenge", challenge, "status", "ACTIVE"));

        } catch (SQLException e) {
            ServerLogger.error("adminAuthInit DB error : " + e.getMessage());
            return Response.error("Erreur base de donnees.");
        }
    }

    // ── ADMIN RSA AUTH RESPONSE ────────────────────────────
    public Response adminAuthResponse(Request request, java.net.InetAddress clientAddress) {
        String username = request.getString("username");
        String signature = request.getString("signature");

        if (username == null || signature == null) {
            return Response.error("Username et signature requis.");
        }

        // Always pop the challenge (thread-safe, single-use, eliminates replay)
        String storedChallenge = ChallengeManager.getInstance().getAndRemoveChallenge(username);
        
        if (storedChallenge == null) {
            ServerLogger.warn("ADMIN_AUTH_RESPONSE: Challenge expiré ou inexistant pour " + username);
            return Response.error("Session expirée (Timeout 60s). Veuillez réessayer.");
        }

        try {
            Map<String, Object> admin = adminDAO.getAdminByUsername(username);
            if (admin == null) {
                return Response.error("Administrateur introuvable.");
            }

            String publicKeyBase64 = (String) admin.get("publicKey");
            if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
                return Response.error("Clé publique introuvable. Initialisation requise.");
            }

            java.security.PublicKey pubKey = ECDSAUtils.decodePublicKey(publicKeyBase64);
            
            // Verify signature against the identical exact challenge string we just popped from memory
            byte[] sigBytes = java.util.Base64.getDecoder().decode(signature);
            boolean valid = ECDSAUtils.verify(storedChallenge, sigBytes, pubKey);

            if (!valid) {
                ServerLogger.warn("ADMIN_AUTH_RESPONSE: Signature INVALIDE pour " + username);
                return Response.error("Échec de l'authentification (Signature invalide).");
            }

            // Authentication successful -> Create Session
            int    adminId = (int) admin.get("adminId");
            String email   = (String) admin.get("email");

            String code = com.chrionline.server.service.EmailService.generateCode();
            adminDAO.saveVerifyCode(adminId, code);
            com.chrionline.server.service.EmailService.send2faEmail(email, code);

            ServerLogger.info("LOGIN ADMIN (RSA) OK - 2FA REQUIRED : " + username);

            Map<String, Object> result = new HashMap<>();
            result.put("userId", adminId);
            result.put("email",  email);
            result.put("2fa_required", true);
            result.put("isAdmin", true);
            
            return Response.ok("Verification 2FA requise", result);

        } catch (Exception e) {
            ServerLogger.error("adminAuthResponse error : " + e.getMessage());
            return Response.error("Erreur serveur inattendue lors de la verification.");
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
                String email   = (String) admin.get("email");
                String role    = (boolean) admin.get("isSuper") ? "SUPER_ADMIN" : "ADMIN";
                String token   = SessionManager.createSession(adminId, username, role, email);

                ServerLogger.info("LOGIN ADMIN OK : " + username + " (ID:" + adminId + ", role=" + role + ")");

                Map<String, Object> result = new HashMap<>();
                result.put("token",     token);
                result.put("userId",    adminId);
                result.put("username",  username);
                result.put("email",     email);
                result.put("role",      role);
                result.put("firstName", admin.get("firstName"));
                result.put("lastName",  admin.get("lastName"));
                return Response.ok("Connexion reussie !", result);
            }

            // 2. Cherche dans USERS
            Map<String, Object> user = userDAO.getUserByUsername(username);

            if (user == null)
                return Response.error("Utilisateur introuvable.");

            int userId = (int) user.get("userId");

            if (!(boolean) user.get("isActive")) {
                Object lockedUntil = user.get("lockedUntil");

                if (lockedUntil == null) {
                    return Response.error("Ce compte est desactive.");
                }

                boolean stillLocked = userDAO.checkAndUnlockIfExpired(userId);
                if (stillLocked) {
                    return Response.error("Compte bloque. Reessayez dans quelques minutes.");
                }
                user = userDAO.getUserByUsername(username);
            }

            if (!verifyPassword(password, (String) user.get("passwordHash"))) {
                userDAO.recordFailedAttempt(userId);

                Map<String, Object> updated = userDAO.getUserById(userId);
                int attempts = 0;
                Object att = updated.get("failedAttempts");
                if (att != null) attempts = ((Number) att).intValue();

                if (attempts == 3)
                    return Response.error("Mot de passe incorrect. Compte bloque 15 minutes.");
                else if (attempts == 4)
                    return Response.error("Mot de passe incorrect. Compte bloque 30 minutes.");
                else if (attempts >= 5)
                    return Response.error("Mot de passe incorrect. Compte bloque 1 heure.");
                else
                    return Response.error("Mot de passe incorrect. ");
            }

            userDAO.resetFailedAttempts(userId);

            Object verified = user.get("isVerified");
            boolean isVerified = verified != null &&
                    (Boolean.TRUE.equals(verified) ||
                            "1".equals(verified.toString()) ||
                            "true".equalsIgnoreCase(verified.toString()));

            if (!isVerified)
                return Response.error("Veuillez verifier votre email avant de vous connecter.");

            userId = (int)   user.get("userId");
            String userEmail = (String) user.get("email");

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
        boolean isAdmin = Boolean.TRUE.equals(request.get("isAdmin"));

        if (userId <= 0 || code == null)
            return Response.error("userId et code requis.");

        try {
            if (isAdmin) {
                boolean ok = adminDAO.verifyOtp(userId, code);
                if (ok) {
                    Map<String, Object> admin = adminDAO.getAdminById(userId);
                    String username = (String) admin.get("username");
                    String email    = (String) admin.get("email");
                    String role     = (boolean) admin.get("isSuper") ? "SUPER_ADMIN" : "ADMIN";
                    String token    = SessionManager.createSession(userId, username, role, email);
    
                    ServerLogger.info("LOGIN ADMIN (2FA PASSED) OK : " + username + " (ID:" + userId + ", role=" + role + ")");
    
                    Map<String, Object> result = new HashMap<>();
                    result.put("token",     token);
                    result.put("userId",    userId);
                    result.put("username",  username);
                    result.put("email",     email);
                    result.put("role",      role);
                    result.put("firstName", admin.get("firstName"));
                    result.put("lastName",  admin.get("lastName"));
                    return Response.ok("Connexion réussie !", result);
                }
                return Response.error("Code incorrect ou expiré.");
            } else {
                boolean ok = userDAO.verifyOtp(userId, code);
                if (ok) {
                    Map<String, Object> user = userDAO.getUserById(userId);
                    String username = (String) user.get("username");
                    String email    = (String) user.get("email");
                    String role     = "CLIENT";
                    String token    = SessionManager.createSession(userId, username, role, email);
    
                    ServerLogger.info("LOGIN CLIENT (2FA PASSED) OK : " + username + " (ID:" + userId + ", role=" + role + ")");
    
                    Map<String, Object> result = new HashMap<>();
                    result.put("token",     token);
                    result.put("userId",    userId);
                    result.put("username",  username);
                    result.put("email",     email);
                    result.put("role",      role);
                    result.put("firstName", user.get("firstName"));
                    result.put("lastName",  user.get("lastName"));
                    return Response.ok("Connexion reussie !", result);
                }
                return Response.error("Code incorrect ou expire.");
            }
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
                return Response.ok("Email verifie ! Vous pouvez maintenant vous connecter.", null);
            }
            return Response.error("Code incorrect ou expire.");
        } catch (SQLException e) {
            ServerLogger.error("VERIFY_EMAIL error : " + e.getMessage());
            return Response.error("Erreur : " + e.getMessage());
        }
    }

    // ── FORGOT PASSWORD ────────────────────────────────────
    public Response forgotPassword(Request request) {
        String username = request.getString("username");

        if (username == null || username.isBlank())
            return Response.error("Nom d'utilisateur requis.");

        try {
            Map<String, Object> user = userDAO.getUserByUsername(username);

            if (user == null) {
                ServerLogger.warn("FORGOT_PASSWORD : username inconnu = " + username);
                return Response.ok("Si ce compte existe, un email a ete envoye.", null);
            }

            int    userId = (int)    user.get("userId");
            String email  = (String) user.get("email");

            String token = EmailService.generateCode();

            userDAO.saveVerifyCode(userId, token);

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
            return Response.error("Le mot de passe doit contenir au moins 8 caractères.");
        if (!newPassword.matches(".*[A-Z].*"))
            return Response.error("Le mot de passe doit contenir au moins une lettre majuscule.");
        if (!newPassword.matches(".*\\d.*"))
            return Response.error("Le mot de passe doit contenir au moins un chiffre.");
        if (!newPassword.matches(".*[^a-zA-Z0-9\\s].*"))
            return Response.error("Le mot de passe doit contenir au moins un caractère spécial.");

        try {
            Map<String, Object> user = userDAO.getUserByUsername(username);
            if (user == null)
                return Response.error("Utilisateur introuvable.");

            int    userId    = (int)    user.get("userId");
            String firstName = (String) user.get("firstName");
            String lastName  = (String) user.get("lastName");

            if (firstName != null && !firstName.isBlank()
                    && newPassword.toLowerCase().contains(firstName.toLowerCase()))
                return Response.error("Le mot de passe ne doit pas inclure votre prénom.");

            if (lastName != null && !lastName.isBlank()
                    && newPassword.toLowerCase().contains(lastName.toLowerCase()))
                return Response.error("Le mot de passe ne doit pas inclure votre nom de famille.");

            String newHash = hashPassword(newPassword);
            userDAO.updatePassword(userId, newHash);

            SessionManager.invalidateAllForUser(userId);

            ServerLogger.info("RESET_PASSWORD OK : userId=" + userId);
            return Response.ok("Mot de passe modifié avec succès. Vous pouvez vous connecter.", null);

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
}