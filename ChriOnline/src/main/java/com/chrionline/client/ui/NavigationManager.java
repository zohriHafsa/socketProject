package com.chrionline.client.ui;

import com.chrionline.client.ui.admin.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class NavigationManager {

    private static StackPane rootContainer;
    private static Scene     scene;

    private static String token;
    private static String username;
    private static String role;
    private static int    userId = -1;
    private static String firstName;
    private static String lastName;

    public static void initialize(StackPane root, Scene sc) {
        rootContainer = root;
        scene = sc;
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

    // ── Admin screens ──────────────────────────────────
    public static void showAdminDashboard()  { transitionTo(new AdminDashboard().getView(), true); }
    public static void showManageUsers()     { transitionTo(new ManageUsersScreen().getView(), true); }
    public static void showManageProducts()  { transitionTo(new ManageProductsScreen().getView(), true); }
    public static void showManageOrders()    { transitionTo(new ManageOrdersScreen().getView(), true); }

    public static void navigateTo(String name) {
        Platform.runLater(() -> {
            switch (name) {
                case "landing":        showLanding(); break;
                case "login":          showLogin(); break;
                case "register":       showRegister(); break;
                case "cart":           showCart(); break;
                case "orders":         showOrders(); break;
                case "profile":        showProfile(); break;
                case "products":       showProductList(); break;
                case "notifications":  showNotifications(); break;
                case "admin":          showAdminDashboard(); break;
                case "manageUsers":    showManageUsers(); break;
                case "manageProducts": showManageProducts(); break;
                case "manageOrders":   showManageOrders(); break;
                default: System.err.println("[NAV] Unknown: " + name);
            }
        });
    }

    private static void transitionTo(Node newView, boolean animate) {
        Platform.runLater(() -> {
            if (!animate || rootContainer.getChildren().isEmpty()) {
                rootContainer.getChildren().setAll(newView);
                return;
            }
            Node old = rootContainer.getChildren().get(rootContainer.getChildren().size() - 1);
            FadeTransition fadeOut = new FadeTransition(Duration.millis(250), old);
            fadeOut.setToValue(0);
            newView.setOpacity(0);
            newView.setScaleX(0.98);
            newView.setScaleY(0.98);
            rootContainer.getChildren().add(newView);
            fadeOut.setOnFinished(e -> {
                rootContainer.getChildren().remove(old);
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

    // ── Session ────────────────────────────────────────
    public static void setSession(String tk, int uid, String un, String rl, String fn, String ln) {
        token = tk; userId = uid; username = un; role = rl; firstName = fn; lastName = ln;
    }
    public static void clearSession() {
        token = null; userId = -1; username = null; role = null; firstName = null; lastName = null;
    }
    public static boolean isLoggedIn()   { return token != null; }
    public static boolean isAdmin()      { return "ADMIN".equals(role); }
    public static String  getToken()     { return token; }
    public static int     getUserId()    { return userId; }
    public static String  getUsername()  { return username; }
    public static String  getRole()      { return role; }
    public static String  getFirstName() { return firstName; }
    public static String  getLastName()  { return lastName; }
    public static Scene   getScene()     { return scene; }
}