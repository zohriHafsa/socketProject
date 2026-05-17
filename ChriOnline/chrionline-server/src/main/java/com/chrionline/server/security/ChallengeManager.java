package com.chrionline.server.security;

import com.chrionline.shared.security.ChallengeGenerator;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChallengeManager {

    private static ChallengeManager instance;
    private final ConcurrentHashMap<String, ChallengeData> activeChallenges;
    private final ScheduledExecutorService cleanupExecutor;

    // Challenge validity period in milliseconds (60 seconds)
    private static final long EXPIRY_TIME_MS = 60 * 1000;

    private ChallengeManager() {
        this.activeChallenges = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ChallengeCleanupDaemon");
            t.setDaemon(true);
            return t;
        });
        
        // Run cleanup every minute
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredChallenges, 1, 1, TimeUnit.MINUTES);
    }

    public static synchronized ChallengeManager getInstance() {
        if (instance == null) {
            instance = new ChallengeManager();
        }
        return instance;
    }

    /**
     * Creates a new challenge, assigns it to the username, and sets an expiry.
     * Overwrites any existing challenge for this user.
     */
    public String createChallenge(String username) {
        String challenge = ChallengeGenerator.generateChallenge();
        long expiry = System.currentTimeMillis() + EXPIRY_TIME_MS;
        
        activeChallenges.put(username, new ChallengeData(challenge, expiry));
        return challenge;
    }

    /**
     * Retrieves the challenge for the user and safely removes it from memory 
     * immediately (preventing replay attacks). Returns null if expired or missing.
     */
    public String getAndRemoveChallenge(String username) {
        ChallengeData data = activeChallenges.remove(username);
        
        if (data == null) {
            return null;
        }
        
        if (data.isExpired()) {
            return null; // Token drifted out of time boundary
        }
        
        return data.getChallenge();
    }

    /**
     * Daemon routine to purge state memory of staleness ensuring no memory leak
     * occurs from users requesting a challenge but abandoning the login screen.
     */
    private void cleanupExpiredChallenges() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, ChallengeData>> iterator = activeChallenges.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, ChallengeData> entry = iterator.next();
            if (entry.getValue().isExpired(now)) {
                iterator.remove();
            }
        }
    }

    /**
     * Encapsulated wrapper for mapping a generated challenge to its TTL (Time To Live).
     */
    private static class ChallengeData {
        private final String challenge;
        private final long expiryTime;

        public ChallengeData(String challenge, long expiryTime) {
            this.challenge = challenge;
            this.expiryTime = expiryTime;
        }

        public String getChallenge() {
            return challenge;
        }

        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        public boolean isExpired(long currentTime) {
            return currentTime > expiryTime;
        }
    }
}
