package com.chrionline.client.ui;

import javafx.concurrent.Worker;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;

public class CaptchaWebView extends VBox {

    private final WebEngine engine;
    private String currentToken = null;
    private Runnable onSolvedCallback;

    public CaptchaWebView() {
        WebView webView = new WebView();
        webView.setPrefHeight(120);
        webView.setPrefWidth(320);

        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
        );

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                injectBridge();
            }
        });

        // Start a tiny local HTTP server and load from it
        startLocalServer();
        engine.load("http://localhost:8765/captcha");

        // Force WebView to repaint after loading
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                injectBridge();
                // Force repaint
                webView.setOpacity(0.99);
                javafx.application.Platform.runLater(() -> webView.setOpacity(1.0));
            }
        });

        getChildren().add(webView);
    }

    private void startLocalServer() {
        new Thread(() -> {
            try {
                java.net.ServerSocket server = new java.net.ServerSocket(8765);
                while (true) {
                    java.net.Socket client = server.accept();
                    new Thread(() -> handleRequest(client)).start();
                }
            } catch (Exception e) {
                System.err.println("[CAPTCHA] Server error: " + e.getMessage());
            }
        }, "captcha-server").start();
    }

    private void handleRequest(java.net.Socket client) {
        try {
            // Read the request (we don't need to parse it)
            java.io.InputStream in = client.getInputStream();
            byte[] buf = new byte[4096];
            in.read(buf);

            String html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                body { margin: 0; padding: 4px; background: white; }
              </style>
              <script src="https://www.google.com/recaptcha/api.js" async defer></script>
            </head>
            <body>
              <div class="g-recaptcha"
                   data-sitekey="6LcIHrEsAAAAAAoLx-cjhPKG0zWOr922BwCPrG0O"
                   data-callback="onCaptchaSolved"
                   data-expired-callback="onCaptchaExpired">
              </div>
              <script>
                function onCaptchaSolved(token) {
                  if (window.javaBridge) window.javaBridge.onTokenReceived(token);
                }
                function onCaptchaExpired() {
                  if (window.javaBridge) window.javaBridge.onTokenExpired();
                }
                function resetCaptcha() { grecaptcha.reset(); }
              </script>
            </body>
            </html>
            """;

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Length: " + html.getBytes().length + "\r\n" +
                    "Connection: close\r\n\r\n" +
                    html;

            client.getOutputStream().write(response.getBytes());
            client.close();
        } catch (Exception e) {
            System.err.println("[CAPTCHA] Request error: " + e.getMessage());
        }
    }

    // ── Bridge: lets JavaScript call Java methods ──────────
    private void injectBridge() {
        JSObject window = (JSObject) engine.executeScript("window");
        window.setMember("javaBridge", new JavaBridge());
    }

    // ── Called when user solves the CAPTCHA ────────────────
    public void setOnSolved(Runnable callback) {
        this.onSolvedCallback = callback;
    }

    // ── Returns the token, or null if not solved ───────────
    public String getToken() {
        return currentToken;
    }

    // ── Call this to reset after a failed attempt ──────────
    public void reset() {
        currentToken = null;
        engine.executeScript("resetCaptcha()");
    }

    public boolean isSolved() {
        return currentToken != null && !currentToken.isEmpty();
    }

    // ── Inner class: JavaScript calls methods on this ──────
    public class JavaBridge {
        // Called by JavaScript when CAPTCHA is solved
        public void onTokenReceived(String token) {
            javafx.application.Platform.runLater(() -> {
                currentToken = token;
                System.out.println("[CAPTCHA] Token received: " + token.substring(0, 20) + "...");
                if (onSolvedCallback != null) {
                    onSolvedCallback.run();
                }
            });
        }

        // Called by JavaScript when token expires
        public void onTokenExpired() {
            javafx.application.Platform.runLater(() -> {
                currentToken = null;
                System.out.println("[CAPTCHA] Token expired, user must re-check");
            });
        }
    }
}