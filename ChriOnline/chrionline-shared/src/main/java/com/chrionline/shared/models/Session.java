package com.chrionline.shared.models;

import java.time.LocalDateTime;

public class Session {

    private String sessionId;
    private User user;
    private String clientAddress;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean isActive;

    public Session() {}

    public Session(String sessionId, User user, String clientAddress,
                   LocalDateTime createdAt, LocalDateTime expiresAt, boolean isActive) {
        this.sessionId = sessionId;
        this.user = user;
        this.clientAddress = clientAddress;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.isActive = isActive;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getClientAddress() { return clientAddress; }
    public void setClientAddress(String clientAddress) { this.clientAddress = clientAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }

    @Override
    public String toString() {
        return "Session{id='" + sessionId + "', user="
                + (user != null ? user.getUsername() : "null")
                + ", valid=" + isValid() + "}";
    }
}