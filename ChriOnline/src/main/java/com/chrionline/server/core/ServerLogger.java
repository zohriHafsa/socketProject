package com.chrionline.server.core;

import com.chrionline.server.security.Log4jDetector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {

    // ═══════════════ ANSI COLORS ═══════════════
    private static final String R   = "\033[0m";
    private static final String B   = "\033[1m";
    private static final String DIM = "\033[2m";
    private static final String RED = "\033[31m";
    private static final String GRN = "\033[32m";
    private static final String YLW = "\033[33m";
    private static final String BLU = "\033[34m";
    private static final String MAG = "\033[35m";
    private static final String CYN = "\033[36m";
    private static final String WHT = "\033[37m";

    private static final String LOG_FILE = "server.log";
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FULL_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static PrintWriter writer;

    // ═══════════════ INIT ══════════════════════
    static {
        try {
            File f = new File(LOG_FILE);
            writer = new PrintWriter(new FileWriter(f, true), true);
        } catch (IOException e) {
            System.err.println(RED + B + "  ❌ Logger init failed: " + e.getMessage() + R);
        }
    }

    // ═══════════════ PUBLIC API ════════════════
    public static synchronized void info(String msg)  { log("INFO",  msg); }
    public static synchronized void warn(String msg)  { log("WARN",  msg); }
    public static synchronized void error(String msg) { log("ERROR", msg); }

    // ═══════════════ CORE ══════════════════════
    private static void log(String level, String msg) {
        // ── Protection Log4j (AJOUTER EN PREMIER) ──
        if (Log4jDetector.isMalicious(msg)) {
            msg = "[BLOCKED LOG4J PAYLOAD] " + Log4jDetector.sanitize(msg);
            level = "SECURITY_ALERT";
        }

        String time = LocalDateTime.now().format(TIME_FMT);
        String full = LocalDateTime.now().format(FULL_FMT);

        // ── Client connection ──────────────────────
        if (msg.startsWith("Client connected")) {
            out("");
            out(CYN + "  ╔═══════════════════════════════════════════════════════════════╗" + R);
            out(CYN + "  ║" + GRN + B + "  🔗  NOUVELLE CONNEXION CLIENT                                  " + CYN + "║" + R);
            out(CYN + "  ║" + WHT + "      " + msg + spaces(58 - msg.length()) + CYN + "║" + R);
            out(CYN + "  ╚═══════════════════════════════════════════════════════════════╝" + R);
            return;
        }

        // ── Client disconnection ───────────────────
        if (msg.startsWith("Client disconnected") || msg.startsWith("Connection lost")) {
            out(RED + "  ╔═══════════════════════════════════════════════════════════════╗" + R);
            out(RED + "  ║" + RED + B + "  🔌  CLIENT DÉCONNECTÉ                                         " + RED + "║" + R);
            out(RED + "  ╚═══════════════════════════════════════════════════════════════╝" + R);
            out("");
            return;
        }

        // ── JSON Request received ──────────────────
        boolean isRequest  = msg.startsWith("Received from");
        boolean isResponse = msg.startsWith("Response to");
        int jsonIdx = (isRequest || isResponse) ? msg.indexOf(": {") : -1;
        boolean hasJson = jsonIdx > 0 && msg.contains("}");

        if (hasJson) {
            String prefix = msg.substring(0, jsonIdx);
            String json   = msg.substring(jsonIdx + 2);

            if (isRequest) {
                String addr = prefix.replace("Received from ", "");
                // Extract action from JSON for a nice label
                String action = extractAction(json);

                out("");
                out(DIM + "  " + time + R + "  " +
                        CYN + B + "📥 ┌─── REQUÊTE REÇUE ─── " +
                        WHT + addr + CYN + B +
                        (action != null ? " ─── " + YLW + B + action + CYN + B : "") +
                        " " + repeat("─", Math.max(1, 30 - addr.length() - (action != null ? action.length() : 0))) + R);
                out(CYN + "              │ " + DIM + json + R);
                out(CYN + "              └" + repeat("─", 55) + R);

            } else {
                String addr    = prefix.replace("Response to ", "");
                boolean success = json.contains("\"success\":true");
                String resColor = success ? GRN : RED;
                String resIcon  = success ? "✅" : "❌";
                String resTag   = success ? "RÉPONSE OK" : "RÉPONSE ERREUR";

                out(DIM + "  " + time + R + "  " +
                        resColor + B + "📤 ┌─── " + resIcon + " " + resTag + " ─── " +
                        WHT + addr + resColor + B + " " +
                        repeat("─", Math.max(1, 35 - addr.length())) + R);

                // Truncate very long JSON for display (full in file)
                String displayJson = json.length() > 200
                        ? json.substring(0, 200) + "... (" + json.length() + " chars)"
                        : json;
                out(resColor + "              │ " + DIM + displayJson + R);

                // Write full JSON to file only if truncated
                if (json.length() > 200) {
                    fileOnly("              │ [FULL] " + json);
                }

                out(resColor + "              └" + repeat("─", 55) + R);
                out("");
            }
            return;
        }

        // ── Routing action ─────────────────────────
        if (msg.startsWith("Routing action")) {
            String action = msg.replace("Routing action: ", "");
            out(DIM + "  " + time + R + "  " +
                    MAG + B + "⚡ [ROUTE] " + WHT + B + action + R);
            return;
        }

        // ── Session events ─────────────────────────
        if (msg.startsWith("Session created")) {
            out(DIM + "  " + time + R + "  " +
                    YLW + B + "🔑 [SESSION+] " + WHT + msg + R);
            return;
        }
        if (msg.startsWith("Session expired") || msg.startsWith("Session invalidated")) {
            out(DIM + "  " + time + R + "  " +
                    YLW + "🔒 [SESSION-] " + WHT + msg + R);
            return;
        }

        // ── Business actions ───────────────────────
        if (msg.startsWith("REGISTER")) {
            out(DIM + "  " + time + R + "  " + GRN + B + "📝 [REGISTER] " + WHT + msg + R);
            return;
        }
        if (msg.startsWith("LOGIN")) {
            out(DIM + "  " + time + R + "  " + GRN + B + "🔑 [LOGIN   ] " + WHT + msg + R);
            return;
        }
        if (msg.startsWith("LOGOUT")) {
            out(DIM + "  " + time + R + "  " + YLW + "🚪 [LOGOUT  ] " + WHT + msg + R);
            return;
        }
        if (msg.startsWith("GET_PRODUCTS") || msg.startsWith("GET_PRODUCT_DETAIL")
                || msg.startsWith("SEARCH_PRODUCTS")) {
            out(DIM + "  " + time + R + "  " + CYN + "📦 [PRODUCT ] " + WHT + msg + R);
            return;
        }
        if (msg.startsWith("ADD_TO_CART") || msg.startsWith("REMOVE_FROM_CART")
                || msg.startsWith("CLEAR_CART")) {
            out(DIM + "  " + time + R + "  " + GRN + "🛒 [CART    ] " + WHT + msg + R);
            return;
        }
        if (msg.startsWith("PLACE_ORDER") || msg.startsWith("GET_ORDER")) {
            out(DIM + "  " + time + R + "  " + MAG + B + "📋 [ORDER   ] " + WHT + msg + R);
            return;
        }
        if (msg.startsWith("PROCESS_PAYMENT")) {
            out(DIM + "  " + time + R + "  " + BLU + B + "💳 [PAYMENT ] " + WHT + msg + R);
            return;
        }

        // ── Errors ─────────────────────────────────
        if ("ERROR".equals(level)) {
            out(DIM + "  " + time + R + "  " + RED + B + "❌ [ERROR  ] " + RED + msg + R);
            return;
        }
        if ("WARN".equals(level)) {
            out(DIM + "  " + time + R + "  " + YLW + B + "⚠  [WARN   ] " + YLW + msg + R);
            return;
        }

        // ── Default info ───────────────────────────
        String icon = msg.contains("Starting") || msg.contains("started") ? "🚀" : "ℹ ";
        out(DIM + "  " + time + R + "  " + CYN + icon + " [INFO   ] " + WHT + msg + R);
    }

    // ═══════════════ OUTPUT (console + file) ═══
    private static void out(String line) {
        // Console: with colors
        System.out.println(line);

        // File: same content but strip ANSI codes
        if (writer != null) {
            writer.println(stripAnsi(line));
            writer.flush();
        }
    }

    // Write to file only (for full JSON that was truncated on console)
    private static void fileOnly(String line) {
        if (writer != null) {
            writer.println(line);
            writer.flush();
        }
    }

    // ═══════════════ UTILS ═════════════════════
    private static String stripAnsi(String s) {
        return s.replaceAll("\033\\[[0-9;]*m", "");
    }

    private static String extractAction(String json) {
        int idx = json.indexOf("\"action\":\"");
        if (idx < 0) return null;
        int start = idx + 10;
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    private static String repeat(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    private static String spaces(int n) {
        if (n <= 0) return "";
        return " ".repeat(n);
    }
}