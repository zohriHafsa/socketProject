package com.chrionline.shared.security;

import java.security.SecureRandom;
import java.util.Base64;

public class ChallengeGenerator {
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a 32-byte secure random challenge encoded in Base64 (URL-safe, without padding)
     */
    public static String generateChallenge() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
