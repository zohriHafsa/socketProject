package com.chrionline.client.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.enums.Action;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.*;

public class ProfileScreen {

    private static final double MAX_W = 900;
    private static final double H_PAD = 40;
    private final BorderPane root;

    public ProfileScreen() {
        root = new BorderPane();
        root.getStyleClass().add("page-root");
        root.setTop(new HeaderComponent(null, null, null, null).getView());

        if (!NavigationManager.isLoggedIn()) showLoginRequired();
        else buildContent();

        Platform.runLater(() -> {
            root.setOpacity(0);
            FadeTransition f = new FadeTransition(Duration.millis(400), root);
            f.setToValue(1); f.play();
        });
    }

    public Node getView() { return root; }

    private StackPane buildHeader() {
        StackPane w = new StackPane();
        w.getStyleClass().add("page-header-bar");
        w.setMinWidth(0);
        HBox inner = new HBox(16);
        inner.setMaxWidth(MAX_W);
        inner.setPadding(new Insets(0, H_PAD, 0, H_PAD));
        inner.setMinHeight(70);
        inner.setAlignment(Pos.CENTER_LEFT);

        Button back = new Button("\u2190 Boutique");
        back.getStyleClass().add("page-back-btn");
        back.setOnAction(e -> NavigationManager.showLanding());
        Label title = new Label("Mon Profil");
        title.getStyleClass().add("page-title");
        inner.getChildren().addAll(back, title);
        w.getChildren().add(inner);
        return w;
    }

    @SuppressWarnings("unchecked")
    private void buildContent() {
        VBox container = new VBox(20);
        container.setMaxWidth(MAX_W);
        container.setPadding(new Insets(30, H_PAD, 50, H_PAD));
        container.setAlignment(Pos.TOP_CENTER);

        // ── Avatar + Info card ──────────────────────────
        HBox topCard = new HBox(24);
        topCard.getStyleClass().add("profile-card");
        topCard.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("profile-avatar");
        String initials = "";
        String fn = NavigationManager.getFirstName();
        String ln = NavigationManager.getLastName();
        if (fn != null && !fn.isEmpty()) initials += fn.substring(0, 1).toUpperCase();
        if (ln != null && !ln.isEmpty()) initials += ln.substring(0, 1).toUpperCase();
        if (initials.isEmpty()) initials = "?";
        Label avatarText = new Label(initials);
        avatarText.getStyleClass().add("profile-avatar-text");
        avatar.getChildren().add(avatarText);

        VBox nameBox = new VBox(6);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        String fullName = ((fn != null ? fn : "") + " " + (ln != null ? ln : "")).trim();
        Label nameL = new Label(fullName.isEmpty() ? NavigationManager.getUsername() : fullName);
        nameL.getStyleClass().add("profile-name");
        Label userL = new Label("@" + NavigationManager.getUsername());
        userL.getStyleClass().add("profile-info-label");
        Label roleL = new Label(NavigationManager.getRole());
        roleL.getStyleClass().add("profile-role-badge");
        nameBox.getChildren().addAll(nameL, userL, roleL);

        topCard.getChildren().addAll(avatar, nameBox);

        // ── Stats row ───────────────────────────────────
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER);

        VBox statOrders = statCard("—", "Commandes");
        VBox statCart = statCard("—", "Articles panier");
        VBox statId = statCard(String.valueOf(NavigationManager.getUserId()), "ID Utilisateur");

        statsRow.getChildren().addAll(statOrders, statCart, statId);
        HBox.setHgrow(statOrders, Priority.ALWAYS);
        HBox.setHgrow(statCart, Priority.ALWAYS);
        HBox.setHgrow(statId, Priority.ALWAYS);

        loadStats(statOrders, statCart);

        // ── Info card ───────────────────────────────────
        VBox infoCard = new VBox(14);
        infoCard.getStyleClass().add("profile-card");

        Label infoTitle = new Label("Informations du compte");
        infoTitle.getStyleClass().add("profile-section-title");
        infoCard.getChildren().addAll(infoTitle,
                infoRow("Identifiant", String.valueOf(NavigationManager.getUserId())),
                infoRow("Utilisateur", NavigationManager.getUsername()),
                infoRow("Rôle", NavigationManager.getRole()),
                infoRow("Prénom", fn != null ? fn : "—"),
                infoRow("Nom", ln != null ? ln : "—")
        );

        // ── Quick actions ───────────────────────────────
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);

        Button ordersBtn = new Button("Mes commandes");
        ordersBtn.getStyleClass().add("btn-primary");
        ordersBtn.setStyle("-fx-font-size:13px;-fx-padding:10 20;");
        ordersBtn.setOnAction(e -> NavigationManager.showOrders());

        Button cartBtn = new Button("Mon panier");
        cartBtn.getStyleClass().add("btn-secondary");
        cartBtn.setStyle("-fx-font-size:13px;-fx-padding:10 20;");
        cartBtn.setOnAction(e -> NavigationManager.showCart());

        Button logoutBtn = new Button("Se déconnecter");
        logoutBtn.getStyleClass().add("profile-logout-btn");
        logoutBtn.setOnAction(e -> doLogout());

        actions.getChildren().addAll(ordersBtn, cartBtn, logoutBtn);

        container.getChildren().addAll(topCard, statsRow, infoCard, actions);

        StackPane centered = new StackPane(container);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setMinWidth(0);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        root.setCenter(scroll);

        // Animate cards
        Platform.runLater(() -> {
            Node[] nodes = {topCard, statsRow, infoCard, actions};
            for (int i = 0; i < nodes.length; i++) {
                Node n = nodes[i]; n.setOpacity(0); n.setTranslateY(20);
                FadeTransition f = new FadeTransition(Duration.millis(450), n);
                f.setToValue(1); f.setDelay(Duration.millis(100 + i * 100));
                TranslateTransition s = new TranslateTransition(Duration.millis(450), n);
                s.setToY(0); s.setDelay(Duration.millis(100 + i * 100));
                s.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(f, s).play();
            }
        });
    }

    private HBox infoRow(String label, String value) {
        HBox r = new HBox(); r.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label); l.getStyleClass().add("profile-info-label"); l.setMinWidth(140);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value != null ? value : "—"); v.getStyleClass().add("profile-info-value");
        r.getChildren().addAll(l, sp, v); return r;
    }

    private VBox statCard(String value, String label) {
        VBox c = new VBox(4); c.getStyleClass().add("profile-stat-card"); c.setAlignment(Pos.CENTER);
        Label v = new Label(value); v.getStyleClass().add("profile-stat-value");
        Label l = new Label(label); l.getStyleClass().add("profile-stat-label");
        c.getChildren().addAll(v, l); return c;
    }

    @SuppressWarnings("unchecked")
    private void loadStats(VBox ordersCard, VBox cartCard) {
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_ORDER_HISTORY, NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId())))
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.isSuccess() && res.getData() != null) {
                        ((Label) ordersCard.getChildren().get(0)).setText(
                                String.valueOf(((List<?>) res.getData()).size()));
                    }
                }));

        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_CART, NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId())))
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.isSuccess() && res.getData() != null) {
                        Map<String, Object> cart = (Map<String, Object>) res.getData();
                        Object ic = cart.get("itemCount");
                        int count = 0;
                        if (ic instanceof Double) count = ((Double) ic).intValue();
                        else if (ic instanceof Integer) count = (Integer) ic;
                        ((Label) cartCard.getChildren().get(0)).setText(String.valueOf(count));
                    }
                }));
    }

    private void doLogout() {
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.LOGOUT, NavigationManager.getToken(), null))
                .thenAccept(res -> Platform.runLater(() -> {
                    NavigationManager.clearSession();
                    NavigationManager.showLanding();
                }));
    }

    private void showLoginRequired() {
        VBox b = new VBox(16); b.setAlignment(Pos.CENTER); b.setPadding(new Insets(80));
        Label t = new Label("Connectez-vous pour voir votre profil");
        t.getStyleClass().add("empty-state-title");
        Button btn = new Button("Se connecter"); btn.getStyleClass().add("btn-primary");
        btn.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        btn.setOnAction(e -> NavigationManager.showLogin());
        b.getChildren().addAll(t, btn);
        root.setCenter(b);
    }
}