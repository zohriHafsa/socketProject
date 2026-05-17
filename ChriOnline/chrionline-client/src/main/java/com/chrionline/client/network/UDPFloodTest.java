package com.chrionline.client.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPFloodTest {

    public static void main(String[] args) throws Exception {
        InetAddress address = InetAddress.getByName("localhost");
        int port = 9091;

        String msg    = "HEARTBEAT:999"; // fake userId
        byte[] buffer = msg.getBytes();

        System.out.println("[TEST] Starting UDP Flood test — sending 50 packets fast...");

        try (DatagramSocket socket = new DatagramSocket()) {
            for (int i = 1; i <= 50; i++) {
                DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length, address, port);
                socket.send(packet);
                System.out.println("[TEST] Sent packet #" + i);
                // No sleep — send as fast as possible to trigger flood detection
            }
        }

        System.out.println("[TEST] Done. Check server logs for flood detection.");
    }
}