package com.chrionline.shared.dto;

import com.chrionline.shared.enums.Action;
import com.google.gson.Gson;
import java.util.Map;
import java.util.UUID;

public class Request {
    private Action action;
    private String token;
    private Map<String, Object> data;

    // ── Anti-replay fields ──
    private String nonce;      // ← AJOUTER
    private long timestamp;    // ← AJOUTER

    public Request() {
        // Générer automatiquement pour chaque requête
        this.nonce = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public Request(Action action, String token, Map<String, Object> data) {
        this();
        this.action = action;
        this.token  = token;
        this.data   = data;
    }

    // Getters/setters
    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // ... le reste du code existant ...

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Request fromJson(String json) {
        return new Gson().fromJson(json, Request.class);
    }

    public Object get(String key) {
        if (data == null) return null;
        return data.get(key);
    }

    public String getString(String key) {
        Object val = get(key);
        return val != null ? val.toString() : null;
    }

    public int getInt(String key) {
        Object val = get(key);
        if (val == null) return 0;
        if (val instanceof Double) return ((Double) val).intValue();
        return Integer.parseInt(val.toString());
    }

    public double getDouble(String key) {
        Object val = get(key);
        if (val == null) return 0.0;
        return Double.parseDouble(val.toString());
    }

    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    @Override
    public String toString() { return toJson(); }
}