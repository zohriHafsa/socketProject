package com.chrionline.client.controller;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;

import java.util.HashMap;
import java.util.Map;

public class AuthController {

    private final ServerConnection conn = ServerConnection.getInstance();

    public Response register(String username, String email, String password,
                             String firstName, String lastName,
                             String address, String phone) {
        Map<String, Object> data = new HashMap<>();
        data.put("username",  username);
        data.put("email",     email);
        data.put("password",  password);
        data.put("firstName", firstName);
        data.put("lastName",  lastName);
        data.put("address",   address);
        data.put("phone",     phone);
        return conn.send(new Request(Action.REGISTER, null, data));
    }

    public Response login(String username, String password) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        return conn.send(new Request(Action.LOGIN, null, data));
    }

    public Response logout(String token) {
        return conn.send(new Request(Action.LOGOUT, token, null));
    }

    /**
     * Enregistre le port UDP du client auprès du serveur
     * pour recevoir les notifications push.
     */
    public Response registerUdp(String token, int userId, int udpPort) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId",  userId);
        data.put("udpPort", udpPort);
        return conn.send(new Request(Action.REGISTER_UDP, token, data));
    }

    public Response forgotPassword(String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        return conn.send(new Request(Action.FORGOT_PASSWORD, null, data));
    }

    public Response verifyResetToken(Map<String, Object> data) {
        return conn.send(new Request(Action.VERIFY_RESET_TOKEN, null, data));
    }

    public Response resetPassword(Map<String, Object> data) {
        return conn.send(new Request(Action.RESET_PASSWORD, null, data));
    }

    public Response verifyCaptcha(String token) {
        Map<String, Object> data = new HashMap<>();
        data.put("captchaToken", token);
        return conn.send(new Request(Action.VERIFY_CAPTCHA, null, data));
    }
}