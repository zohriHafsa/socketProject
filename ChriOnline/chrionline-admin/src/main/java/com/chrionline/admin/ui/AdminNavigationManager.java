package com.chrionline.admin.ui;

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

public class AdminNavigationManager {

    private static StackPane rootContainer;
    private static Scene     scene;

    private static String token;
    private static String username;
    private static String role;
    private static int    userId = -1;
    private static String firstName;
    private static String lastName;
    private static String email;

    public static void initialize(StackPane root, Scene sc) {
        rootContainer = root;
        scene = sc;
        refreshSceneStyles();
    }

    public static void showAdminLogin()      { transitionTo(new AdminLoginScreen().getView(), true); }
    public static void showAdminDashboard()  { transitionTo(new AdminDashboard().getView(), true); }
    public static void showManageUsers()     { transitionTo(new ManageUsersScreen().getView(), true); }
    public static void showManageProducts()  { transitionTo(new ManageProductsScreen().getView(), true); }
    public static void showManageOrders()    { transitionTo(new ManageOrdersScreen().getView(), true); }
    public static void showManageCategories(){ transitionTo(new ManageCategoriesScreen().getView(), true); }
    public static void showManageAdmins()    { transitionTo(new ManageAdminsScreen().getView(), true); }
    public static void showAdminInit(String username) { transitionTo(new AdminInitScreen(username).getView(), true); }
    public static void showProfile()         { transitionTo(new ProfileScreen().getView(), true); }

    public static void showTwoFactor(int uid, String userEmail) {
        transitionTo(new AdminTwoFactorScreen(uid, userEmail).getView(), true);
    }

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

    private static void refreshSceneStyles() {
        if (scene == null) return;
        String cssUrl = resolveStylesheetUrl();
        if (cssUrl == null) {
            System.err.println("[ADMIN-NAV] style.css introuvable.");
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
            File devCss = new File("chrionline-admin/src/main/resources/css/style.css");
            if (devCss.exists()) return copyToTempAndGetUrl(devCss.toPath());

            URL resource = AdminNavigationManager.class.getResource("/css/style.css");
            if (resource != null) {
                Path tempFile = Files.createTempFile("chrionline-admin-style-", ".css");
                tempFile.toFile().deleteOnExit();
                try (var in = resource.openStream()) {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }
                return tempFile.toUri().toString();
            }
        } catch (Exception e) {
            System.err.println("[ADMIN-NAV] Erreur chargement CSS : " + e.getMessage());
        }
        return null;
    }

    private static String copyToTempAndGetUrl(Path source) throws IOException {
        Path tempFile = Files.createTempFile("chrionline-admin-style-", ".css");
        tempFile.toFile().deleteOnExit();
        Files.copy(source, tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile.toUri().toString();
    }

    public static void setSession(String tk, int uid, String un,
                                  String rl, String fn, String ln, String userEmail) {
        token = tk; userId = uid; username = un;
        role = rl; firstName = fn; lastName = ln; email = userEmail;
    }

    public static void clearSession() {
        token = null; userId = -1; username = null;
        role = null; firstName = null; lastName = null; email = null;
    }

    public static boolean isLoggedIn()    { return token != null; }
    public static boolean isAdmin()       { return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role); }
    public static boolean isSuperAdmin()  { return "SUPER_ADMIN".equals(role); }
    public static String  getToken()      { return token; }
    public static int     getUserId()     { return userId; }
    public static String  getUsername()   { return username; }
    public static String  getRole()       { return role; }
    public static String  getFirstName()  { return firstName; }
    public static String  getLastName()   { return lastName; }
    public static String  getEmail()      { return email; }
    public static Scene   getScene()      { return scene; }
}
