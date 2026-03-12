package com.chrionline.shared.dto;

import com.chrionline.shared.enums.Action;
import com.google.gson.Gson;

import java.util.Map;

public class Request {
    private Action action;
    private String token;
    private Map<String, Object> data;

    public Request() {}

    public Request(Action action, String token, Map<String, Object> data) {
        this.action = action;
        this.token  = token;
        this.data   = data;
    }

    // Serialization
    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Request fromJson(String json) {
        return new Gson().fromJson(json, Request.class);
    }

    // Helpers
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
        if (val instanceof Double) return ((Double) val).intValue(); // Gson parses numbers as Double
        return Integer.parseInt(val.toString());
    }

    public double getDouble(String key) {
        Object val = get(key);
        if (val == null) return 0.0;
        return Double.parseDouble(val.toString());
    }

    // Getters & Setters
    public Action getAction()               { return action; }
    public void setAction(Action action)  { this.action = action; }

    public String getToken()                { return token; }
    public void setToken(String token)    { this.token = token; }

    public Map<String, Object> getData()                    { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    @Override
    public String toString() { return toJson(); }
}
