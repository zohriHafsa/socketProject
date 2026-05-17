package com.chrionline.server.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {

    // ── Singleton ──────────────────────────────────────────────────────────
    private static final RateLimiter INSTANCE = new RateLimiter();
    public static RateLimiter getInstance() { return INSTANCE; }

    // ── Config ─────────────────────────────────────────────────────────────
    private static final int MAX_TOKENS = 10;
    private static final long REFILL_INTERVAL = 1000;
    private static final int REFILL_AMOUNT = 10;

    // ── State ──────────────────────────────────────────────────────────────
    // Bucket = [tokensLeft, lastRefillTimestamp]
    private final Map<String, long[]> buckets = new ConcurrentHashMap<>();

    private RateLimiter() {}

    public synchronized boolean allowRequest(String ip) {
        long now = System.currentTimeMillis();
        long[] bucket = buckets.computeIfAbsent(ip, k -> new long[]{MAX_TOKENS, now});

        long tokens = bucket[0];
        long lastRefill = bucket[1];

        long elapsed = now - lastRefill;
        if (elapsed >= REFILL_INTERVAL) {
            long refills = elapsed / REFILL_INTERVAL;
            tokens = Math.min(MAX_TOKENS, tokens + refills * REFILL_AMOUNT);
            bucket[1] = now;
        }

        if (tokens <= 0) {
            bucket[0] = tokens;
            return false;
        }

        bucket[0] = tokens - 1;
        return true;
    }
}
