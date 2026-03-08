package com.chrionline.server;

import com.chrionline.server.core.TCPServer;

public class ServerMain {
    public static void main(String[] args) {
        TCPServer server = new TCPServer(8081, 10);
        server.start();
    }
}
