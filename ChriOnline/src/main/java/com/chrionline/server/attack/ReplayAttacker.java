package com.chrionline.server.attack;

import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ReplayAttacker {

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 9090;

        System.out.println("[ATTACK] Replay Attack test...\n");

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 1. Créer une requête valide
            Map<String, Object> data = new HashMap<>();
            data.put("keyword", "iPhone");

            Request originalRequest = new Request(Action.SEARCH_PRODUCTS, null, data);
            String originalJson = originalRequest.toJson();

            System.out.println("[ATTACK] Requête originale :");
            System.out.println(originalJson);
            System.out.println();

            // 2. Envoyer la première fois (doit marcher)
            System.out.println("[ATTACK] Envoi #1 (doit réussir)...");
            out.println(originalJson);
            String response1 = in.readLine();
            Response resp1 = Response.fromJson(response1);
            System.out.println("Réponse #1 : " + resp1.isSuccess() + " - " + resp1.getMessage());
            System.out.println();

            // 3. Attendre un peu
            Thread.sleep(1000);

            // 4. Renvoyer EXACTEMENT la même requête (replay)
            System.out.println("[ATTACK] Envoi #2 (REPLAY - doit échouer)...");
            out.println(originalJson); // Même nonce + timestamp
            String response2 = in.readLine();
            Response resp2 = Response.fromJson(response2);
            System.out.println("Réponse #2 : " + resp2.isSuccess() + " - " + resp2.getMessage());

            if (!resp2.isSuccess() && resp2.getMessage().contains("REPLAY")) {
                System.out.println("\n✅ [SUCCESS] Replay Attack détecté et bloqué !");
            } else {
                System.out.println("\n❌ [FAIL] Replay Attack NON détecté (vulnérable) !");
            }
        }
    }
}