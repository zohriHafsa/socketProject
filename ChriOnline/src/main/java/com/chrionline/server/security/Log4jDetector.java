package com.chrionline.server.security;

import com.chrionline.server.core.ServerLogger;
import java.util.regex.Pattern;

public class Log4jDetector {

    private static final Pattern JNDI_PATTERN = Pattern.compile(
            "\\$\\{jndi:(ldap|rmi|dns|nis|iiop|corba|nds|http)://.*}",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern OBFUSCATED_PATTERN = Pattern.compile(
            "\\$\\{.*j.*n.*d.*i.*:.*}",
            Pattern.CASE_INSENSITIVE
    );

    public static boolean isMalicious(String input) {
        if (input == null || input.isEmpty()) return false;

        if (JNDI_PATTERN.matcher(input).find()) {
            ServerLogger.warn("[LOG4J] JNDI injection détectée (classique) : " + sanitize(input));
            return true;
        }

        if (OBFUSCATED_PATTERN.matcher(input).find()) {
            ServerLogger.warn("[LOG4J] JNDI injection détectée (obfusquée) : " + sanitize(input));
            return true;
        }

        return false;
    }

    public static String sanitize(String input) {
        if (input == null) return "null";
        return input.replaceAll("\\$\\{", "[BLOCKED-${]")
                .replaceAll("jndi:", "[BLOCKED-JNDI]")
                .substring(0, Math.min(input.length(), 100)) + "...";
    }
}