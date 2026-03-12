package com.chrionline.shared.models;

import com.chrionline.shared.enums.ProtocolType;
import java.time.LocalDateTime;

public class Notification {

    private int notificationId;
    private User user;
    private String message;
    private String type;
    private boolean isRead;
    private ProtocolType protocol;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(int notificationId, User user, String message, String type,
                        boolean isRead, ProtocolType protocol, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.user = user;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.protocol = protocol;
        this.createdAt = createdAt;
    }

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { this.isRead = read; }

    public ProtocolType getProtocol() { return protocol; }
    public void setProtocol(ProtocolType protocol) { this.protocol = protocol; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Notification{id=" + notificationId + ", type='" + type
                + "', read=" + isRead + ", message='" + message + "'}";
    }
}