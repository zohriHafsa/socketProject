package com.chrionline.server.security;

import com.chrionline.server.core.ServerLogger;
import java.util.regex.Pattern;

public class InputSanitizer {

    private static final Pattern CMD_INJECTION_PATTERN = Pattern.compile(
            "[;&|`$()\\n\\r<>]"
    );

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(union|select|insert|update|delete|drop|exec|script|javascript|onerror)",
            Pattern.CASE_INSENSITIVE
    );

    public static boolean isSafe(String input) {
        if (input == null || input.isEmpty()) return true;

        if (CMD_INJECTION_PATTERN.matcher(input).find()) {
            ServerLogger.warn("[INJECTION] Commande shell détectée : " + input);
            return false;
        }

        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            ServerLogger.warn("[INJECTION] SQL injection détectée : " + input);
            return false;
        }

        return true;
    }

    public static String escape(String input) {
        if (input == null) return "";

        return input.replaceAll("[;&|`$()\\n\\r<>]", "")
                .replaceAll("--", "")
                .trim();
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;

        Pattern emailPattern = Pattern.compile(
                "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        );

        boolean valid = emailPattern.matcher(email).matches();
        if (!valid) {
            ServerLogger.warn("[INJECTION] Email invalide (potentiel injection) : " + email);
        }
        return valid;
    }
}