package com.chrionline.server.security;

import com.chrionline.server.core.ServerLogger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AntiReplayFilter {

    private static final long MAX_AGE_MS = 300_000; // 5 minutes

    private final ConcurrentHashMap<String, Long> seenNonces = new ConcurrentHashMap<>();

    private static AntiReplayFilter instance;

    public static synchronized AntiReplayFilter getInstance() {
        if (instance == null) {
            instance = new AntiReplayFilter();
            instance.startCleanupScheduler();
        }
        return instance;
    }

    private AntiReplayFilter() {}

    public boolean validate(String nonce, long timestamp) {
        long now = System.currentTimeMillis();

        // 1. Vérifier la fraîcheur du timestamp
        if (Math.abs(now - timestamp) > MAX_AGE_MS) {
            ServerLogger.warn("[REPLAY] Timestamp trop ancien/futur : " + timestamp);
            return false;
        }

        // 2. Vérifier si le nonce a déjà été utilisé
        if (seenNonces.putIfAbsent(nonce, now) != null) {
            ServerLogger.warn("[REPLAY] Nonce déjà utilisé (REPLAY ATTACK) : " + nonce);
            return false;
        }

        ServerLogger.info("[REPLAY] Request valide : nonce=" + nonce.substring(0, 8) + "...");
        return true;
    }

    private void startCleanupScheduler() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            long cutoff = System.currentTimeMillis() - MAX_AGE_MS;
            int before = seenNonces.size();

            seenNonces.entrySet().removeIf(entry -> entry.getValue() < cutoff);

            int removed = before - seenNonces.size();
            if (removed > 0) {
                ServerLogger.info("[REPLAY] Cleanup : " + removed + " nonces expirés");
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
}