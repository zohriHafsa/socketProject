package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    // Sessions expire after 30 minutes of inactivity
    private static final int SESSION_DURATION_MINUTES = 30;

    // ConcurrentHashMap is thread-safe — important because multiple
    // client threads access this at the same time (sockets!)
    private static final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    // Called after successful login — creates a token and stores the session
    public static String createSession(int userId, String role) {
        String token = UUID.randomUUID().toString(); // e.g. "a3f8-..."
        sessions.put(token, new SessionData(
                userId, role,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(SESSION_DURATION_MINUTES)
        ));
        ServerLogger.info("Session created: token=" + token + " userId=" + userId);
        return token;
    }

    // Called on every authenticated request — checks if token is valid + not expired
    public static SessionData getSession(String token) {
        if (token == null) return null;
        SessionData sd = sessions.get(token);
        if (sd == null) return null;

        // Check expiry
        if (LocalDateTime.now().isAfter(sd.expiresAt)) {
            sessions.remove(token); // auto-delete expired session
            ServerLogger.info("Session expired: " + token);
            return null;
        }
        return sd;
    }

    // Called on LOGOUT — removes the session
    public static boolean invalidate(String token) {
        SessionData removed = sessions.remove(token);
        if (removed != null) {
            ServerLogger.info("Session invalidated: " + token);
            return true;
        }
        return false;
    }

    // Called every 5 minutes by a background thread (see ServerMain)
    // Cleans up all expired sessions to avoid memory leaks
    public static void cleanExpired() {
        sessions.entrySet().removeIf(e ->
                LocalDateTime.now().isAfter(e.getValue().expiresAt));
    }

    // Inner class representing one session's data
    public static class SessionData {
        public final int userId;
        public final String role;
        public final LocalDateTime createdAt;
        public final LocalDateTime expiresAt;

        public SessionData(int userId, String role,
                           LocalDateTime createdAt, LocalDateTime expiresAt) {
            this.userId = userId;
            this.role = role;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
    }
}
