package com.chrionline.server;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.core.TCPServer;
import com.chrionline.server.core.UDPHeartbeatServer;
import com.chrionline.server.core.UDPNotifier;
import com.chrionline.server.service.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerMain {

    // ═══════════════════ ANSI COLORS ═══════════════════
    static final String R    = "\033[0m";
    static final String B    = "\033[1m";
    static final String DIM  = "\033[2m";
    static final String RED  = "\033[31m";
    static final String GRN  = "\033[32m";
    static final String YLW  = "\033[33m";
    static final String BLU  = "\033[34m";
    static final String MAG  = "\033[35m";
    static final String CYN  = "\033[36m";
    static final String WHT  = "\033[37m";
    static final String BLK  = "\033[30m";
    static final String BG_G = "\033[42m";

    public static void main(String[] args) {

        // ─── Clear screen ───────────────────────────────
        System.out.print("\033[H\033[2J");
        System.out.flush();

        // ─── Banner ─────────────────────────────────────
        System.out.println();
        System.out.println(CYN+B+"  ╔═══════════════════════════════════════════════════════════════════════════════╗"+R);
        System.out.println(CYN+B+"  ║                                                                               ║"+R);
        System.out.println(CYN+B+"  ║"+WHT+B+"   ██████╗██╗  ██╗██████╗ ██╗ ██████╗ ███╗   ██╗██╗     ██╗███╗   ██╗███████╗  "+CYN+B+"║"+R);
        System.out.println(CYN+B+"  ║"+WHT+B+"  ██╔════╝██║  ██║██╔══██╗██║██╔═══██╗████╗  ██║██║     ██║████╗  ██║██╔════╝  "+CYN+B+"║"+R);
        System.out.println(CYN+B+"  ║"+WHT+B+"  ██║     ███████║██████╔╝██║██║   ██║██╔██╗ ██║██║     ██║██╔██╗ ██║█████╗    "+CYN+B+"║"+R);
        System.out.println(CYN+B+"  ║"+WHT+B+"  ██║     ██╔══██║██╔══██╗██║██║   ██║██║╚██╗██║██║     ██║██║╚██╗██║██╔══╝    "+CYN+B+"║"+R);
        System.out.println(CYN+B+"  ║"+WHT+B+"  ╚██████╗██║  ██║██║  ██║██║╚██████╔╝██║ ╚████║███████╗██║██║ ╚████║███████╗  "+CYN+B+"║"+R);
        System.out.println(CYN+B+"  ║"+WHT+B+"   ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝ ╚═════╝ ╚═╝  ╚═══╝╚══════╝╚═╝╚═╝  ╚═══╝╚══════╝  "+CYN+B+"║"+R);
        System.out.println(CYN+B+"  ║"+YLW+B+"                        ━━━  C H R I O N L I N E  ━━━                          "+CYN+B+"║"+R);
        System.out.println(CYN+B+"  ║                                                                               ║"+R);
        System.out.println(CYN+B+"  ║"+WHT+B+"                     Serveur E-Commerce TCP — v1.0                             "+CYN+B+"║"+R);
        System.out.println(CYN+B+"  ║"+DIM+"                Plateforme de vente en ligne ChriOnline                        "+CYN+B+"║"+R);
        System.out.println(CYN+B+"  ║                                                                               ║"+R);
        System.out.println(CYN+B+"  ╚═══════════════════════════════════════════════════════════════════════════════╝"+R);
        System.out.println();

        // ─── Config info ─────────────────────────────────
        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        System.out.println(CYN+B+"  ┌───────────────────────────────────────────────────────┐"+R);
        System.out.println(CYN+B+"  │"+WHT+B+"                   CONFIGURATION                       "+CYN+B+"│"+R);
        System.out.println(CYN+B+"  ├───────────────────────────────────────────────────────┤"+R);
        System.out.println(CYN+B+"  │"+WHT+"  Port TCP            : "+GRN+B+"9090                           "+CYN+B+"│"+R);
        System.out.println(CYN+B+"  │"+WHT+"  Port UDP Heartbeat  : "+GRN+B+"9091                           "+CYN+B+"│"+R);
        System.out.println(CYN+B+"  │"+WHT+"  Pool threads        : "+GRN+B+"10                             "+CYN+B+"│"+R);
        System.out.println(CYN+B+"  │"+WHT+"  Session expiry      : "+GRN+B+"30 min                         "+CYN+B+"│"+R);
        System.out.println(CYN+B+"  │"+WHT+"  Session cleanup     : "+GRN+B+"toutes les 5 min               "+CYN+B+"│"+R);
        System.out.println(CYN+B+"  │"+WHT+"  UDP cleanup         : "+GRN+B+"toutes les 30s                 "+CYN+B+"│"+R);
        System.out.println(CYN+B+"  │"+WHT+"  UDP Flood limit     : "+GRN+B+"20 paquets / 10s par IP        "+CYN+B+"│"+R);
        System.out.println(CYN+B+"  │"+WHT+"  UDP block duration  : "+GRN+B+"60s                            "+CYN+B+"│"+R);
        System.out.println(CYN+B+"  │"+WHT+"  Base de données     : "+GRN+B+"MySQL — chrionline             "+CYN+B+"│"+R);
        System.out.println(CYN+B+"  │"+WHT+"  Logger              : "+GRN+B+"server.log (fichier + console) "+CYN+B+"│"+R);
        System.out.println(CYN+B+"  │"+WHT+"  Démarré le          : "+YLW+now+"            "+CYN+B+"│"+R);
        System.out.println(CYN+B+"  └───────────────────────────────────────────────────────┘"+R);
        System.out.println();

        // ─── Step 1 : Logger ────────────────────────────
        System.out.println(YLW+"  ⏳ [1/5] Initialisation du logger..."+R);
        ServerLogger.info("Starting ChriOnline Server...");
        System.out.println(GRN+B+"  ✅ [1/5] Logger initialisé → console + server.log"+R);

        // ─── Step 2 : Session cleanup scheduler ─────────
        System.out.println(YLW+"  ⏳ [2/5] Démarrage du nettoyeur de sessions..."+R);
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(
                        SessionManager::cleanExpired,
                        5, 5, TimeUnit.MINUTES
                );
        System.out.println(GRN+B+"  ✅ [2/5] Nettoyeur de sessions actif (toutes les 5 min)"+R);

        // ─── Step 3 : UDP Heartbeat Server ──────────────
        System.out.println(YLW+"  ⏳ [3/5] Démarrage du serveur UDP Heartbeat sur port 9091..."+R);
        Thread udpThread = new Thread(UDPHeartbeatServer.getInstance());
        udpThread.setDaemon(true);
        udpThread.setName("udp-heartbeat-server");
        udpThread.start();
        System.out.println(GRN+B+"  ✅ [3/5] UDP Heartbeat Server actif (port 9091)"+R);

        // ─── Step 4 : UDP cleanup scheduler ─────────────
        System.out.println(YLW+"  ⏳ [4/5] Démarrage du nettoyeur UDP..."+R);
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(
                        () -> UDPNotifier.getInstance().cleanInactiveClients(),
                        30, 30, TimeUnit.SECONDS
                );
        System.out.println(GRN+B+"  ✅ [4/5] Nettoyeur UDP actif (toutes les 30s)"+R);

        // ─── Step 5 : TCP Server ─────────────────────────
        System.out.println(YLW+"  ⏳ [5/5] Démarrage du serveur TCP sur le port 9090..."+R);
        System.out.println();

        System.out.println(GRN+B+"  ╔══════════════════════════════════════════════════════════════════╗"+R);
        System.out.println(GRN+B+"  ║                                                                  ║"+R);
        System.out.println(GRN+B+"  ║   🟢  CHRIONLINE SERVER PRÊT — En écoute sur le port 9090        ║"+R);
        System.out.println(GRN+B+"  ║   🟢  UDP HEARTBEAT actif    — En écoute sur le port 9091        ║"+R);
        System.out.println(GRN+B+"  ║                                                                  ║"+R);
        System.out.println(GRN+B+"  ║"+DIM+"   En attente de connexions clients...                            "+GRN+B+"║"+R);
        System.out.println(GRN+B+"  ║"+DIM+"   Les logs s'affichent ici ET dans server.log                    "+GRN+B+"║"+R);
        System.out.println(GRN+B+"  ║"+DIM+"   Ctrl+C pour arrêter le serveur                                 "+GRN+B+"║"+R);
        System.out.println(GRN+B+"  ║                                                                  ║"+R);
        System.out.println(GRN+B+"  ╚══════════════════════════════════════════════════════════════════╝"+R);
        System.out.println();
        System.out.println(DIM+"  ─── 📋 Logs en temps réel ───────────────────────────────────────────"+R);
        System.out.println();

        // ─── Start TCP Server (blocking call) ───────────
        TCPServer server = new TCPServer(9090, 2);
        server.start();
    }
}