package com.chrionline.client.ui;

import com.chrionline.client.controller.AuthController;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NavigationManager {

    private static StackPane rootContainer;
    private static Scene     scene;

    private static String token;
    private static String username;
    private static String role;
    private static int    userId = -1;
    private static String firstName;
    private static String lastName;
    private static String email;
    private static String address;
    private static String phone;

    public static void initialize(StackPane root, Scene sc) {
        rootContainer = root;
        scene = sc;
        refreshSceneStyles();
    }

    // ── Client screens ─────────────────────────────────
    public static void showSplash()              { transitionTo(new SplashScreen().getView(), false); }
    public static void showLanding()             { transitionTo(new LandingPage().getView(), true); }
    public static void showLogin()               { transitionTo(new LoginScreen().getView(), true); }
    public static void showRegister()            { transitionTo(new RegisterScreen().getView(), true); }
    public static void showCart()                { transitionTo(new CartScreen().getView(), true); }
    public static void showOrders()              { transitionTo(new OrderHistoryScreen().getView(), true); }
    public static void showProfile()             { transitionTo(new ProfileScreen().getView(), true); }
    public static void showProductDetail(int id) { transitionTo(new ProductDetailScreen(id).getView(), true); }
    public static void showProductList()         { transitionTo(new ProductListScreen().getView(), true); }
    public static void showNotifications()       { transitionTo(new NotificationScreen().getView(), true); }

    // ── Email verification ─────────────────────────────
    public static void showEmailVerification(int uid, String userEmail) {
        transitionTo(new EmailVerificationScreen(uid, userEmail).getView(), true);
    }

    public static void showTwoFactor(int uid, String userEmail) {
        transitionTo(new TwoFactorScreen(uid, userEmail).getView(), true);
    }

    // ── Password reset ─────────────────────────────────
    public static void showForgotPassword() {
        transitionTo(new ForgotPasswordScreen().getView(), true);
    }

    public static void showEnterCode(String username) {
        transitionTo(new EnterCodeScreen(new AuthController(), username).getView(), true);
    }

    public static void showResetPassword(String username, String resetToken) {
        transitionTo(new ResetPasswordScreen(new AuthController(), username, resetToken).getView(), true);
    }

    // ── Navigation par nom ─────────────────────────────
    public static void navigateTo(String name) {
        Platform.runLater(() -> {
            switch (name) {
                case "landing":          showLanding();          break;
                case "login":            showLogin();            break;
                case "register":         showRegister();         break;
                case "cart":             showCart();             break;
                case "orders":           showOrders();           break;
                case "profile":          showProfile();          break;
                case "products":         showProductList();      break;
                case "notifications":    showNotifications();    break;
                case "forgotPassword":   showForgotPassword();   break;
                default: System.err.println("[NAV] Unknown: " + name);
            }
        });
    }

    // ── Transition avec animation ──────────────────────
    private static void transitionTo(Node newView, boolean animate) {
        Platform.runLater(() -> {
            if (rootContainer == null || scene == null) return;

            if (!animate || rootContainer.getChildren().isEmpty()) {
                rootContainer.getChildren().setAll(newView);
                refreshSceneStyles();
                return;
            }

            Node old = rootContainer.getChildren()
                    .get(rootContainer.getChildren().size() - 1);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(250), old);
            fadeOut.setToValue(0);

            newView.setOpacity(0);
            newView.setScaleX(0.98);
            newView.setScaleY(0.98);

            rootContainer.getChildren().add(newView);

            fadeOut.setOnFinished(e -> {
                rootContainer.getChildren().remove(old);
                refreshSceneStyles();
                FadeTransition fadeIn = new FadeTransition(Duration.millis(350), newView);
                fadeIn.setToValue(1);
                ScaleTransition scaleIn = new ScaleTransition(Duration.millis(350), newView);
                scaleIn.setToX(1);
                scaleIn.setToY(1);
                fadeIn.play();
                scaleIn.play();
            });

            fadeOut.play();
        });
    }

    // ── Refresh CSS ────────────────────────────────────
    private static void refreshSceneStyles() {
        if (scene == null) return;
        String cssUrl = resolveStylesheetUrl();
        if (cssUrl == null) {
            System.err.println("[NAV] style.css introuvable.");
            return;
        }
        scene.getStylesheets().setAll(cssUrl);
        if (scene.getRoot() != null) {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
        }
    }

    private static String resolveStylesheetUrl() {
        try {
            File sourceCss = new File("chrionline-client/src/main/resources/css/style.css");
            if (sourceCss.exists()) return copyToTempAndGetUrl(sourceCss.toPath());

            URL resource = NavigationManager.class.getResource("/css/style.css");
            if (resource != null) {
                Path tempFile = Files.createTempFile("chrionline-style-", ".css");
                tempFile.toFile().deleteOnExit();
                try (var in = resource.openStream()) {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }
                return tempFile.toUri().toString();
            }
        } catch (Exception e) {
            System.err.println("[NAV] Erreur chargement CSS : " + e.getMessage());
        }
        return null;
    }

    private static String copyToTempAndGetUrl(Path source) throws IOException {
        Path tempFile = Files.createTempFile("chrionline-style-", ".css");
        tempFile.toFile().deleteOnExit();
        Files.copy(source, tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile.toUri().toString();
    }

    // ── Session management ─────────────────────────────
    public static void setSession(String tk, int uid, String un,
                                  String rl, String fn, String ln, String userEmail) {
        token = tk; userId = uid; username = un;
        role = rl; firstName = fn; lastName = ln;
        email = userEmail; address = null; phone = null;
    }

    public static void setSession(String tk, int uid, String un, String rl,
                                  String fn, String ln, String userEmail,
                                  String userAddr, String userPhone) {
        token = tk; userId = uid; username = un;
        role = rl; firstName = fn; lastName = ln;
        email = userEmail; address = userAddr; phone = userPhone;
    }

    public static void clearSession() {
        token = null; userId = -1; username = null;
        role = null; firstName = null; lastName = null;
        email = null; address = null; phone = null;
    }

    // ── Getters ────────────────────────────────────────
    public static boolean isLoggedIn()   { return token != null; }
    public static boolean isAdmin()      { return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role); }
    public static boolean isSuperAdmin() { return "SUPER_ADMIN".equals(role); }
    public static String  getToken()     { return token; }
    public static int     getUserId()    { return userId; }
    public static String  getUsername()  { return username; }
    public static String  getRole()      { return role; }
    public static String  getFirstName() { return firstName; }
    public static String  getLastName()  { return lastName; }
    public static String  getEmail()     { return email; }
    public static String  getAddress()   { return address; }
    public static String  getPhone()     { return phone; }
    public static Scene   getScene()     { return scene; }
}