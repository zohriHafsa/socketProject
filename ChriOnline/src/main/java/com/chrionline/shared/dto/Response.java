package com.chrionline.shared.dto;

import com.google.gson.Gson;

public class Response {
    private boolean success;
    private String  message;
    private Object  data;

    // ── Signature numérique (optionnel — uniquement pour les paiements) ──
    private String signature;   // Signature SHA256withRSA en Base64
    private String publicKey;   // Clé publique du serveur en Base64

    public Response() {}

    public Response(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data    = data;
    }

    // Static factories
    public static Response ok(Object data) {
        return new Response(true, "OK", data);
    }

    public static Response ok(String message, Object data) {
        return new Response(true, message, data);
    }

    public static Response error(String message) {
        return new Response(false, message, null);
    }

    // Serialization
    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Response fromJson(String json) {
        return new Gson().fromJson(json, Response.class);
    }

    // Getters & Setters
    public boolean isSuccess()                  { return success; }
    public void setSuccess(boolean success)  { this.success = success; }

    public String getMessage()                  { return message; }
    public void setMessage(String message)    { this.message = message; }

    public Object getData()                     { return data; }
    public void setData(Object data)          { this.data = data; }

    // ── Signature getters/setters ──
    public String getSignature()                     { return signature; }
    public void setSignature(String signature)    { this.signature = signature; }

    public String getPublicKey()                     { return publicKey; }
    public void setPublicKey(String publicKey)    { this.publicKey = publicKey; }

    @Override
    public String toString() { return toJson(); }
}

