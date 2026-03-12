package com.chrionline.server;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.core.TCPServer;

public class ServerMain {
    public static void main(String[] args) {
        ServerLogger.info("Starting ChriOnline Server...");
        TCPServer server = new TCPServer(9090, 10);
        server.start();
    }
}