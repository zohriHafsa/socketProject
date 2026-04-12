package com.chrionline.server.core;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UDPHeartbeatServer implements Runnable {

    private static final int    PORT        = 9091;
    private static final int    BUFFER_SIZE = 256;

    // ── Rate limiting (UDP Flood protection) ───────────────
    // Max packets per IP in the time window
    private static final int    MAX_PACKETS    = 20;
    private static final long   WINDOW_MS      = 10_000; // 10 seconds
    private static final long   BLOCK_DURATION = 60_000; // 1 minute

    // IP → packet count in current window
    private final Map<String, Integer>  packetCount  = new ConcurrentHashMap<>();
    // IP → window start time
    private final Map<String, Long>     windowStart  = new ConcurrentHashMap<>();
    // IP → block expiry time
    private final Map<String, Long>     blockedUntil = new ConcurrentHashMap<>();

    private volatile boolean running = false;
    private DatagramSocket   socket;

    // ── Singleton ──────────────────────────────────────────
    private static UDPHeartbeatServer instance;

    public static synchronized UDPHeartbeatServer getInstance() {
        if (instance == null) instance = new UDPHeartbeatServer();
        return instance;
    }

    private UDPHeartbeatServer() {}

    // ── Main loop ──────────────────────────────────────────
    @Override
    public void run() {
        try {
            socket  = new DatagramSocket(PORT);
            running = true;
            ServerLogger.info("UDP Heartbeat Server listening on port " + PORT);

            byte[] buffer = new byte[BUFFER_SIZE];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String ip = packet.getAddress().getHostAddress();

                // ── Check if IP is blocked ──────────────────
                if (isBlocked(ip)) {
                    ServerLogger.warn("[UDP FLOOD] Blocked IP still sending : " + ip);
                    continue; // drop the packet
                }

                // ── Rate limiting check ─────────────────────
                if (isRateLimitExceeded(ip)) {
                    blockedUntil.put(ip, System.currentTimeMillis() + BLOCK_DURATION);
                    ServerLogger.warn("[UDP FLOOD] IP blocked for 1 minute : " + ip);
                    continue; // drop the packet
                }

                // ── Process the heartbeat ───────────────────
                String msg = new String(
                        packet.getData(), 0, packet.getLength()).trim();
                processHeartbeat(msg, ip, packet.getAddress(), packet.getPort());
            }

        } catch (Exception e) {
            if (running) ServerLogger.error("UDP Heartbeat error : " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) socket.close();
        }
    }

    // ── Rate limiting logic ────────────────────────────────
    private boolean isBlocked(String ip) {
        Long until = blockedUntil.get(ip);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            // Block expired → unblock
            blockedUntil.remove(ip);
            packetCount.remove(ip);
            windowStart.remove(ip);
            ServerLogger.info("[UDP FLOOD] IP unblocked : " + ip);
            return false;
        }
        return true;
    }

    private boolean isRateLimitExceeded(String ip) {
        long now = System.currentTimeMillis();

        // First packet from this IP
        if (!windowStart.containsKey(ip)) {
            windowStart.put(ip, now);
            packetCount.put(ip, 1);
            return false;
        }

        long windowAge = now - windowStart.get(ip);

        if (windowAge > WINDOW_MS) {
            // Window expired → reset counter
            windowStart.put(ip, now);
            packetCount.put(ip, 1);
            return false;
        }

        // Still in window → increment counter
        int count = packetCount.merge(ip, 1, Integer::sum);
        if (count > MAX_PACKETS) {
            ServerLogger.warn("[UDP FLOOD] Rate limit exceeded for IP : "
                    + ip + " (" + count + " packets in 10s)");
            return true;
        }
        return false;
    }

    // ── Process valid heartbeat ────────────────────────────
    private void processHeartbeat(String msg, String ip,
                                  InetAddress address, int port) {
        try {
            // Message format: "HEARTBEAT:userId"
            if (!msg.startsWith("HEARTBEAT:")) {
                ServerLogger.warn("[UDP] Unknown message from " + ip + " : " + msg);
                return;
            }

            int userId = Integer.parseInt(msg.split(":")[1]);

            // Update last seen in UDPNotifier
            UDPNotifier.getInstance().updateLastSeen(userId);

            ServerLogger.info("[UDP] Heartbeat from userId=" + userId
                    + " (" + ip + ":" + port + ")");

        } catch (Exception e) {
            ServerLogger.warn("[UDP] Invalid heartbeat from " + ip + " : " + msg);
        }
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) socket.close();
        ServerLogger.info("UDP Heartbeat Server stopped.");
    }
}