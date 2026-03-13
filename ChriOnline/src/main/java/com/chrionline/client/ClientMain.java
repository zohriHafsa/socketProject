package com.chrionline.client;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;

import java.util.Map;

public class ClientMain {
    public static void main(String[] args) {

        // 1. Connect to TCP server
        ServerConnection connection = ServerConnection.getInstance();
        boolean connected = connection.connect();

        if (!connected) {
            System.err.println("[CLIENT] Failed to connect. Is the server running?");
            return;
        }

        // 2. Test: send a GET_PRODUCTS request
        Request request = new Request(Action.GET_PRODUCTS, null, null);
        Response response = connection.send(request);
        System.out.println("[CLIENT] Response: " + response.toJson());

        // 3. Test: send a LOGIN request
        Request loginRequest = new Request(
                Action.LOGIN,
                null,
                Map.of("email", "jean.dupont@mail.com", "password", "password123")
        );
        Response loginResponse = connection.send(loginRequest);
        System.out.println("[CLIENT] Login response: " + loginResponse.toJson());

        // 4. Disconnect
        connection.disconnect();
    }
}
