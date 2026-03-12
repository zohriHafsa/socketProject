package com.chrionline.server.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {

    private static final String LOG_FILE = "server.log";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static PrintWriter writer;

    static {
        try {
            File f = new File(LOG_FILE);
            System.out.println("[LOGGER] Log file: " + f.getAbsolutePath());
            writer = new PrintWriter(new FileWriter(f, true), true);
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }

    public static synchronized void info(String msg) {
        log("INFO", msg);
    }

    public static synchronized void warn(String msg) {
        log("WARN", msg);
    }

    public static synchronized void error(String msg) {
        log("ERROR", msg);
    }

    private static void log(String level, String msg) {
        String line = "[" + LocalDateTime.now().format(FMT) + "] ["
                + level + "] [" + Thread.currentThread().getName() + "] " + msg;
        System.out.println(line);
        if (writer != null) {
            writer.println(line);
            writer.flush();
        }
    }
}