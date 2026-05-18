package com.chrionline.server.security;

import java.util.concurrent.ConcurrentHashMap;

public class IPRateLimiter {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long BLOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    private static final ConcurrentHashMap<BlockKey, FailedIpInfo> ipStore = new ConcurrentHashMap<>();

    public static boolean isBlocked(String ip, String username) {
        if (ip == null) return false;
        BlockKey key = new BlockKey(ip, username);
        FailedIpInfo info = ipStore.get(key);
        if (info == null) return false;

        if (info.blockedUntil > System.currentTimeMillis()) {
            return true;
        }

        // Block expired, remove from store to keep memory clean
        if (info.blockedUntil > 0) {
            ipStore.remove(key);
        }
        return false;
    }

    public static long getRemainingBlockTimeSeconds(String ip, String username) {
        if (ip == null) return 0;
        BlockKey key = new BlockKey(ip, username);
        FailedIpInfo info = ipStore.get(key);
        if (info == null) return 0;

        long diffMs = info.blockedUntil - System.currentTimeMillis();
        return diffMs > 0 ? (diffMs / 1000) : 0;
    }

    public static boolean recordFailedAttempt(String ip, String username) {
        if (ip == null) return false;
        BlockKey key = new BlockKey(ip, username);
        
        FailedIpInfo info = ipStore.compute(key, (k, v) -> {
            if (v == null) {
                return new FailedIpInfo(1, 0);
            }
            if (v.blockedUntil > System.currentTimeMillis()) {
                return v; // Already blocked
            }
            return new FailedIpInfo(v.failedAttempts + 1, 0);
        });

        if (info.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            info.blockedUntil = System.currentTimeMillis() + BLOCK_DURATION_MS;
            return true; // Just became blocked
        }
        return false;
    }

    public static void reset(String ip, String username) {
        if (ip == null) return;
        BlockKey key = new BlockKey(ip, username);
        ipStore.remove(key);
    }

    private static class BlockKey {
        final String ip;
        final String username;

        BlockKey(String ip, String username) {
            this.ip = ip;
            this.username = username != null ? username.trim().toLowerCase() : "";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlockKey other = (BlockKey) o;
            return ip.equals(other.ip) && username.equals(other.username);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(ip, username);
        }
    }

    private static class FailedIpInfo {
        int failedAttempts;
        long blockedUntil;

        FailedIpInfo(int failedAttempts, long blockedUntil) {
            this.failedAttempts = failedAttempts;
            this.blockedUntil = blockedUntil;
        }
    }
}
