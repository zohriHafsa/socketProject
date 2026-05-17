package com.chrionline.client.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.enums.Action;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class HeaderComponent {

    private final StackPane headerWrapper;
    
    private final Label cartCountLabel  = new Label("0");
    private final Label notifCountLabel = new Label("0");
    private StackPane notifBadgePane;
    private StackPane cartBadgePane;
    
    private final String activePage;
    private final Consumer<String> onSearch;
    private final Runnable onHomeClick;
    private final Runnable onAboutClick;

    public HeaderComponent(String activePage, Consumer<String> onSearch, Runnable onHomeClick, Runnable onAboutClick) {
        this.activePage = activePage;
        this.onSearch = onSearch;
        this.onHomeClick = onHomeClick;
        this.onAboutClick = onAboutClick;
        
        headerWrapper = new StackPane();
        headerWrapper.getStyleClass().add("header-bar");
        headerWrapper.setMinWidth(0);

        HBox inner = new HBox(16);
        inner.setMaxWidth(1200);
        inner.setPadding(new Insets(0, 40, 0, 40));
        inner.setMinHeight(70);
        inner.setAlignment(Pos.CENTER_LEFT);

        // ── Logo ────────────────────────────────────────
        HBox logo = new HBox(8);
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setCursor(javafx.scene.Cursor.HAND);
        logo.setOnMouseClicked(e -> {
            if (onHomeClick != null) onHomeClick.run();
            else NavigationManager.showLanding();
        });
        
        try {
            ImageView img = new ImageView(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))));
            img.setFitHeight(34);
            img.setPreserveRatio(true);
            img.setSmooth(true);
            logo.getChildren().add(img);
        } catch (Exception ignored) {}

        Label l1 = new Label("Chri");
        l1.getStyleClass().add("header-logo-text");
        Label l2 = new Label("Online");
        l2.getStyleClass().add("header-logo-accent");
        
        HBox logoText = new HBox(0, l1, l2);
        logoText.setAlignment(Pos.CENTER_LEFT);
        logo.getChildren().add(logoText);

        // ── Nav ─────────────────────────────────────────
        HBox nav = new HBox(4);
        nav.setAlignment(Pos.CENTER);

        Button nHome = navBtn("Accueil", "home".equals(activePage));
        nHome.setOnAction(e -> {
            if (onHomeClick != null) onHomeClick.run();
            else NavigationManager.showLanding();
        });

        Button nProd = navBtn("Produits", "products".equals(activePage));
        nProd.setOnAction(e -> NavigationManager.showProductList());

        Button nOrders = navBtn("Commandes", "orders".equals(activePage));
        nOrders.setOnAction(e -> {
            if (NavigationManager.isLoggedIn()) {
                NavigationManager.showOrders();
            } else {
                NavigationManager.navigateTo("login");
            }
        });

        Button nAbout = navBtn("A propos", "about".equals(activePage));
        nAbout.setOnAction(e -> {
            if (onAboutClick != null) onAboutClick.run();
            else NavigationManager.showLanding();
        });

        nav.getChildren().addAll(nHome, nProd, nOrders, nAbout);

        // ── Search ──────────────────────────────────────
        TextField search = new TextField();
        search.setPromptText("Rechercher...");
        search.getStyleClass().add("search-field");
        search.setPrefWidth(220);
        search.setOnAction(e -> {
            String k = search.getText().trim();
            if (onSearch != null) onSearch.accept(k);
            else {
                // Future integration to pass query to showProductList
                NavigationManager.showProductList(); 
            }
        });

        // ── Notification bell ───────────────────────────
        StackPane notifStack = new StackPane();
        Button notifBtn = new Button("\uD83D\uDD14");
        notifBtn.getStyleClass().add("header-icon-btn");
        if ("notifications".equals(activePage)) notifBtn.getStyleClass().add("nav-link-active");
        notifBtn.setOnAction(e -> {
            if (NavigationManager.isLoggedIn()) {
                NavigationManager.showNotifications();
            } else {
                NavigationManager.navigateTo("login");
            }
        });

        notifBadgePane = new StackPane();
        notifBadgePane.getStyleClass().add("notification-badge");
        notifBadgePane.setMaxSize(18, 18);
        notifBadgePane.setMinSize(18, 18);
        notifCountLabel.getStyleClass().add("notification-badge-text");
        notifCountLabel.setStyle("-fx-font-size: 9px;");
        notifBadgePane.getChildren().add(notifCountLabel);
        notifBadgePane.setVisible(false);
        StackPane.setAlignment(notifBadgePane, Pos.TOP_RIGHT);
        StackPane.setMargin(notifBadgePane, new Insets(2, -4, 0, 0));
        notifStack.getChildren().addAll(notifBtn, notifBadgePane);
        notifStack.setVisible(NavigationManager.isLoggedIn());
        notifStack.setManaged(NavigationManager.isLoggedIn());

        // ── Cart button ─────────────────────────────────
        StackPane cartStack = new StackPane();
        Button cartBtn = new Button("\uD83D\uDED2");
        cartBtn.getStyleClass().add("header-icon-btn");
        if ("cart".equals(activePage)) cartBtn.getStyleClass().add("nav-link-active");
        cartBtn.setOnAction(e -> {
            if (NavigationManager.isLoggedIn()) {
                NavigationManager.showCart();
            } else {
                NavigationManager.navigateTo("login");
            }
        });

        cartBadgePane = new StackPane();
        cartBadgePane.getStyleClass().add("notification-badge");
        cartBadgePane.setMaxSize(18, 18);
        cartBadgePane.setMinSize(18, 18);
        cartCountLabel.getStyleClass().add("notification-badge-text");
        cartCountLabel.setStyle("-fx-font-size: 9px;");
        cartBadgePane.getChildren().add(cartCountLabel);
        cartBadgePane.setVisible(false);
        StackPane.setAlignment(cartBadgePane, Pos.TOP_RIGHT);
        StackPane.setMargin(cartBadgePane, new Insets(2, -4, 0, 0));
        cartStack.getChildren().addAll(cartBtn, cartBadgePane);
        cartStack.setVisible(true);
        cartStack.setManaged(true);

        // ── User button ─────────────────────────────────
        Button userBtn = new Button();
        userBtn.getStyleClass().add("header-user-btn");

        if (NavigationManager.isLoggedIn()) {
            String initials = "";
            String fn = NavigationManager.getFirstName();
            String ln = NavigationManager.getLastName();
            if (fn != null && !fn.isEmpty()) initials += fn.charAt(0);
            if (ln != null && !ln.isEmpty()) initials += ln.charAt(0);
            if (initials.isEmpty()) initials = "U";

            StackPane avatar = new StackPane();
            avatar.getStyleClass().add("header-avatar");
            avatar.setMinSize(32, 32);
            avatar.setMaxSize(32, 32);

            Label initLabel = new Label(initials.toUpperCase());
            initLabel.getStyleClass().add("header-avatar-text");
            avatar.getChildren().add(initLabel);

            HBox userContent = new HBox(8);
            userContent.setAlignment(Pos.CENTER);
            String displayName = fn != null ? fn : NavigationManager.getUsername();
            Label nameLabel = new Label(displayName);
            nameLabel.getStyleClass().add("header-user-name");
            userContent.getChildren().addAll(avatar, nameLabel);

            userBtn.setGraphic(userContent);
            userBtn.setText(null);
            userBtn.setOnAction(e -> NavigationManager.showProfile());
        } else {
            userBtn.setText("\uD83D\uDC64 Se connecter");
            userBtn.setOnAction(e -> NavigationManager.navigateTo("login"));
        }

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        HBox actions = new HBox(10, search, notifStack, cartStack, userBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        inner.getChildren().addAll(logo, sp1, nav, sp2, actions);

        headerWrapper.getChildren().add(inner);
        
        updateCartBadge();
        updateNotifBadge();
    }
    
    public StackPane getView() {
        return headerWrapper;
    }

    private Button navBtn(String t, boolean active) {
        Button b = new Button(t);
        b.getStyleClass().add("nav-link");
        if (active) b.getStyleClass().add("nav-link-active");
        return b;
    }
    
    @SuppressWarnings("unchecked")
    private void updateCartBadge() {
        if (!NavigationManager.isLoggedIn()) return;
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_CART,
                        NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId())))
                .thenAccept(r -> Platform.runLater(() -> {
                    if (r.isSuccess() && r.getData() != null) {
                        try {
                            int c = ((Double)((Map<String, Object>) r.getData()).get("itemCount")).intValue();
                            cartCountLabel.setText(String.valueOf(c));
                            cartBadgePane.setVisible(c > 0);
                        } catch (Exception ignored) {}
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    public void updateNotifBadge() {
        if (!NavigationManager.isLoggedIn()) return;
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_NOTIFICATIONS,
                        NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId())))
                .thenAccept(r -> Platform.runLater(() -> {
                    if (r.isSuccess() && r.getData() != null) {
                        List<Map<String, Object>> notifs =
                                (List<Map<String, Object>>) r.getData();
                        long unread = notifs.stream()
                                .filter(n -> {
                                    Object isRead = n.get("isRead");
                                    if (isRead == null) return true;
                                    if (isRead instanceof Boolean) return !(Boolean) isRead;
                                    if (isRead instanceof Number) return ((Number) isRead).intValue() == 0;
                                    String s = isRead.toString().trim().toLowerCase();
                                    return !("true".equals(s) || "1".equals(s));
                                })
                                .count();
                        notifCountLabel.setText(String.valueOf(unread));

                        if (notifBadgePane != null) {
                            notifBadgePane.setVisible(unread > 0);

                            if (unread > 0) {
                                ScaleTransition pulse = new ScaleTransition(
                                        Duration.millis(300), notifBadgePane);
                                pulse.setFromX(0.6);
                                pulse.setFromY(0.6);
                                pulse.setToX(1.0);
                                pulse.setToY(1.0);
                                pulse.setInterpolator(Interpolator.EASE_OUT);
                                pulse.play();
                            }
                        }
                    }
                }));
    }
}
