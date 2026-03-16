package com.chrionline.server;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.core.TCPServer;
import com.chrionline.server.service.SessionManager;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerMain {
    public static void main(String[] args) {
        ServerLogger.info("Starting ChriOnline Server...");

        // Cleans up expired sessions every 5 minutes in the background
        // This must be INSIDE main(), BEFORE server.start()
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(
                        SessionManager::cleanExpired,
                        5, 5, TimeUnit.MINUTES
                );

        TCPServer server = new TCPServer(9090, 10);
        server.start();
    }
}