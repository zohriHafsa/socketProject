package com.chrionline.server.security;

import com.chrionline.server.core.ServerLogger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SYNFloodProtection {

    private static final int MAX_CONNECTIONS_PER_IP = 5;
    private static final long CLEANUP_INTERVAL_MS = 60_000; // 1 minute

    private final Map<String, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastCleanup = new ConcurrentHashMap<>();

    private static SYNFloodProtection instance;

    public static synchronized SYNFloodProtection getInstance() {
        if (instance == null) instance = new SYNFloodProtection();
        return instance;
    }

    private SYNFloodProtection() {}

    public synchronized boolean allowConnection(String ip) {
        cleanupIfNeeded(ip);

        AtomicInteger count = connectionCounts.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int current = count.get();

        if (current >= MAX_CONNECTIONS_PER_IP) {
            ServerLogger.warn("[SYN FLOOD] Connexion refusée pour IP " + ip
                    + " (" + current + " connexions actives)");
            return false;
        }

        count.incrementAndGet();
        ServerLogger.info("[SYN FLOOD] Connexion acceptée pour IP " + ip
                + " (" + count.get() + "/" + MAX_CONNECTIONS_PER_IP + ")");
        return true;
    }

    public synchronized void releaseConnection(String ip) {
        AtomicInteger count = connectionCounts.get(ip);
        if (count != null) {
            int newCount = count.decrementAndGet();
            if (newCount <= 0) {
                connectionCounts.remove(ip);
            }
            ServerLogger.info("[SYN FLOOD] Connexion libérée pour IP " + ip
                    + " (reste: " + Math.max(0, newCount) + ")");
        }
    }

    private void cleanupIfNeeded(String ip) {
        long now = System.currentTimeMillis();
        Long last = lastCleanup.get(ip);

        if (last == null || (now - last) > CLEANUP_INTERVAL_MS) {
            connectionCounts.remove(ip);
            lastCleanup.put(ip, now);
            ServerLogger.info("[SYN FLOOD] Cleanup pour IP " + ip);
        }
    }

    public Map<String, Integer> getActiveConnections() {
        Map<String, Integer> snapshot = new ConcurrentHashMap<>();
        connectionCounts.forEach((ip, count) -> snapshot.put(ip, count.get()));
        return snapshot;
    }
}