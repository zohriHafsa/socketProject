package com.chrionline.server.attack;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SYNFloodAttacker {

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 9090;
        int connections = 20; // Tente d'ouvrir 20 connexions (max autorisé = 5)

        System.out.println("[ATTACK] SYN Flood test - " + connections + " connexions...");

        List<Socket> sockets = new ArrayList<>();

        for (int i = 1; i <= connections; i++) {
            try {
                Socket socket = new Socket(host, port);
                sockets.add(socket);
                System.out.println("[ATTACK] Connexion #" + i + " établie");

                // Ne rien envoyer (juste garder la connexion ouverte)
                Thread.sleep(100);

            } catch (Exception e) {
                System.err.println("[ATTACK] Connexion #" + i + " REJETÉE : " + e.getMessage());
            }
        }

        System.out.println("\n[ATTACK] Test terminé. " + sockets.size() + " connexions actives.");
        System.out.println("[ATTACK] Les connexions au-delà de la limite ont dû être rejetées.");
        System.out.println("[ATTACK] Vérifiez les logs serveur pour voir '[SYN FLOOD] Connexion refusée'");

        // Garder les connexions ouvertes 10 secondes pour tester
        Thread.sleep(10_000);

        // Fermer proprement
        for (Socket s : sockets) {
            s.close();
        }
    }
}