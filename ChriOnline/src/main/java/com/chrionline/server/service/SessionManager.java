package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final int SESSION_DURATION_MINUTES = 30;

    // ── Session interne ────────────────────────
    private static class SessionInfo {
        final int    userId;
        final String username;
        final String role;
        final LocalDateTime createdAt;
        final LocalDateTime expiresAt;

        SessionInfo(int userId, String username, String role) {
            this.userId    = userId;
            this.username  = username;
            this.role      = role;
            this.createdAt = LocalDateTime.now();
            this.expiresAt = createdAt.plusMinutes(SESSION_DURATION_MINUTES);
        }

        boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
    }

    /** token → SessionInfo */
    private static final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    // ── API publique ───────────────────────────

    public static String createSession(int userId, String username, String role) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionInfo(userId, username, role));
        ServerLogger.info("Session created for '" + username
                + "' (ID:" + userId + ") token=" + token.substring(0, 8) + "...");
        return token;
    }

    public static boolean isValid(String token) {
        if (token == null) return false;
        SessionInfo si = sessions.get(token);
        if (si == null) return false;
        if (si.isExpired()) {
            sessions.remove(token);
            ServerLogger.info("Session expired for '" + si.username + "'");
            return false;
        }
        return true;
    }

    public static int    getUserId(String token)  {
        SessionInfo si = sessions.get(token);
        return si != null ? si.userId : -1;
    }
    public static String getUsername(String token) {
        SessionInfo si = sessions.get(token);
        return si != null ? si.username : null;
    }
    public static String getRole(String token) {
        SessionInfo si = sessions.get(token);
        return si != null ? si.role : null;
    }

    public static boolean invalidate(String token) {
        SessionInfo removed = sessions.remove(token);
        if (removed != null) {
            ServerLogger.info("Session invalidated for '" + removed.username + "'");
            return true;
        }
        return false;
    }

    /** Appelé par le scheduler dans ServerMain. */
    public static void cleanExpired() {
        int before  = sessions.size();
        sessions.entrySet().removeIf(e -> e.getValue().isExpired());
        int removed = before - sessions.size();
        if (removed > 0) {
            ServerLogger.info("Session cleanup : " + removed
                    + " session(s) expirée(s). Actives : " + sessions.size());
        }
    }

    public static int getActiveCount() { return sessions.size(); }

    public static void invalidateAllForUser(int userId) {
        sessions.entrySet().removeIf(entry -> {
            if (entry.getValue().userId == userId) {
                ServerLogger.info("Session force-invalidated for userId=" + userId
                        + " (password reset)");
                return true;
            }
            return false;
        });
    }



}