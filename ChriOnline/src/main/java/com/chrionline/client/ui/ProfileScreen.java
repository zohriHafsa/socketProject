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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.*;

public class ProfileScreen {

    private static final double MAX_W = 860;
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

    private boolean isEditing = false;
    private final Map<String, TextField> editFields = new HashMap<>();

    @SuppressWarnings("unchecked")
    private void buildContent() {
        String fn       = orEmpty(NavigationManager.getFirstName());
        String ln       = orEmpty(NavigationManager.getLastName());
        String username = orEmpty(NavigationManager.getUsername());
        String role     = orEmpty(NavigationManager.getRole());
        String email    = orEmpty(NavigationManager.getEmail());
        String address  = orEmpty(NavigationManager.getAddress());
        String phone    = orEmpty(NavigationManager.getPhone());

        String fullName = (fn + " " + ln).trim();
        String displayName = fullName.isEmpty() ? username : fullName;

        String initials = "";
        if (!fn.isEmpty()) initials += fn.substring(0, 1).toUpperCase();
        if (!ln.isEmpty()) initials += ln.substring(0, 1).toUpperCase();
        if (initials.isEmpty() && !username.isEmpty()) initials = username.substring(0, 1).toUpperCase();

        VBox container = new VBox(20);
        container.setMaxWidth(MAX_W);
        container.setPadding(new Insets(30, H_PAD, 60, H_PAD));
        container.setAlignment(Pos.TOP_CENTER);

        // ── Header Row ───────────────────────────────────────
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label pageTitle = new Label("Mon Profil");
        pageTitle.getStyleClass().add("page-title");
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        Button editBtn = new Button(isEditing ? "Annuler" : "Modifier le profil");
        editBtn.getStyleClass().add(isEditing ? "btn-secondary" : "btn-primary");
        editBtn.setOnAction(e -> {
            isEditing = !isEditing;
            buildContent();
        });
        
        headerRow.getChildren().addAll(pageTitle, headerSpacer, editBtn);

        // ════════════════════════════════════════════════════
        //  HERO CARD
        // ════════════════════════════════════════════════════
        HBox heroCard = new HBox(20);
        heroCard.getStyleClass().add("profile-hero-card");
        heroCard.setAlignment(Pos.CENTER_LEFT);
        heroCard.setPadding(new Insets(24, 28, 24, 28));

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("profile-avatar-lg");
        Label avatarLabel = new Label(initials);
        avatarLabel.getStyleClass().add("profile-avatar-text-lg");
        avatar.getChildren().add(avatarLabel);

        VBox heroText = new VBox(5);
        heroText.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(heroText, Priority.ALWAYS);

        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("profile-hero-name");

        Label usernameLabel = new Label("@" + username);
        usernameLabel.getStyleClass().add("profile-hero-username");

        Label roleBadge = new Label(fmtRole(role));
        roleBadge.getStyleClass().addAll("order-status-badge", "profile-badge-client");

        heroText.getChildren().addAll(nameLabel, usernameLabel, roleBadge);

        VBox ordersBox = new VBox(4);
        ordersBox.setAlignment(Pos.CENTER);
        ordersBox.setPadding(new Insets(8, 12, 8, 12));
        ordersBox.getStyleClass().add("profile-orders-box");
        Label ordersIcon = new Label("📦");
        ordersIcon.setStyle("-fx-font-size: 22px; -fx-text-fill: #6C3483;");
        Label ordersCount = new Label("—");
        ordersCount.getStyleClass().add("profile-orders-count");
        Label ordersLabel = new Label("commandes");
        ordersLabel.getStyleClass().add("profile-orders-label");
        ordersBox.getChildren().addAll(ordersIcon, ordersCount, ordersLabel);
        
        heroCard.getChildren().addAll(avatar, heroText, ordersBox);
        loadOrdersCount(ordersCount);

        // ════════════════════════════════════════════════════
        //  INFO CARD
        // ════════════════════════════════════════════════════
        VBox infoCard = new VBox(16);
        infoCard.getStyleClass().add("profile-info-card");

        Label infoTitle = new Label("Informations personnelles");
        infoTitle.getStyleClass().add("profile-section-title");

        Region divider = new Region();
        divider.setStyle("-fx-background-color: #F0EEF6; -fx-pref-height: 1;");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(24);
        infoGrid.setVgap(14);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        infoGrid.getColumnConstraints().addAll(c1, c2);

        if (isEditing) {
            infoGrid.add(infoField("Nom d'utilisateur", "@" + username), 0, 0);
            infoGrid.add(infoField("E-mail", email), 1, 0);
            infoGrid.add(editField("firstName", "Prénom", fn), 0, 1);
            infoGrid.add(editField("lastName",  "Nom de famille", ln), 1, 1);
            infoGrid.add(editField("phone", "Téléphone", phone), 0, 2);
            infoGrid.add(editField("address", "Adresse de livraison", address), 1, 2);
        } else {
            infoGrid.add(infoField("Nom d'utilisateur", username.isEmpty() ? "—" : "@" + username), 0, 0);
            infoGrid.add(infoField("E-mail",           email.isEmpty() ? "—" : email),    1, 0);
            infoGrid.add(infoField("Prénom",           fn.isEmpty() ? "—" : fn),          0, 1);
            infoGrid.add(infoField("Nom de famille",    ln.isEmpty() ? "—" : ln),          1, 1);

            if (!NavigationManager.isAdmin()) {
                infoGrid.add(infoField("Téléphone",    phone.isEmpty() ? "—" : phone),    0, 2);
                infoGrid.add(infoField("Adresse de livraison", address.isEmpty() ? "—" : address), 1, 2);
            }
        }

        infoCard.getChildren().addAll(infoTitle, divider, infoGrid);

        // ════════════════════════════════════════════════════
        //  ACTIONS
        // ════════════════════════════════════════════════════
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (isEditing) {
            Button saveBtn = new Button("💾  Enregistrer les modifications");
            saveBtn.getStyleClass().add("btn-primary");
            saveBtn.setStyle("-fx-font-size:13px;-fx-padding:11 22;");
            saveBtn.setOnAction(e -> doUpdateProfile(saveBtn));
            actions.getChildren().add(saveBtn);
        } else {
            Button ordersBtn = new Button("📦  Mes commandes");
            ordersBtn.getStyleClass().add("btn-primary");
            ordersBtn.setStyle("-fx-font-size:13px;-fx-padding:11 22;");
            ordersBtn.setOnAction(e -> NavigationManager.showOrders());

            Button cartBtn = new Button("🛒  Mon panier");
            cartBtn.getStyleClass().add("btn-secondary");
            cartBtn.setStyle("-fx-font-size:13px;-fx-padding:11 22;");
            cartBtn.setOnAction(e -> NavigationManager.showCart());
            actions.getChildren().addAll(ordersBtn, cartBtn);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Se déconnecter");
        logoutBtn.getStyleClass().add("profile-logout-btn");
        logoutBtn.setOnAction(e -> doLogout());

        actions.getChildren().addAll(spacer, logoutBtn);

        container.getChildren().addAll(headerRow, heroCard, infoCard, actions);

        StackPane centered = new StackPane(container);
        centered.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");
        root.setCenter(scroll);
    }

    private VBox editField(String key, String label, String value) {
        VBox box = new VBox(4);
        Label l = new Label(label);
        l.getStyleClass().add("profile-field-label");
        
        TextField tf = new TextField(value);
        tf.getStyleClass().add("auth-field");
        tf.setStyle("-fx-font-size: 13px; -fx-padding: 8 12;");
        editFields.put(key, tf);
        
        box.getChildren().addAll(l, tf);
        return box;
    }

    private void doUpdateProfile(Button btn) {
        String fn = editFields.get("firstName").getText().trim();
        String ln = editFields.get("lastName").getText().trim();
        String ph = editFields.get("phone").getText().trim();
        String ad = editFields.get("address").getText().trim();

        if (fn.isEmpty() || ln.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Prénom et Nom requis.");
            alert.show();
            return;
        }

        btn.setDisable(true);
        btn.setText("Mise à jour...");

        Map<String, Object> params = new HashMap<>();
        params.put("firstName", fn);
        params.put("lastName", ln);
        params.put("phone", ph);
        params.put("address", ad);

        ServerConnection.getInstance().sendAsync(
            new Request(Action.UPDATE_PROFILE, NavigationManager.getToken(), params)
        ).thenAccept(resp -> Platform.runLater(() -> {
            btn.setDisable(false);
            btn.setText("💾  Enregistrer les modifications");
            
            if (resp.isSuccess()) {
                // Update local session
                NavigationManager.setSession(
                    NavigationManager.getToken(),
                    NavigationManager.getUserId(),
                    NavigationManager.getUsername(),
                    NavigationManager.getRole(),
                    fn, ln,
                    NavigationManager.getEmail(),
                    ad, ph
                );
                isEditing = false;
                buildContent();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, resp.getMessage());
                alert.show();
            }
        }));
    }

    // ── Info field (label + value stacked) ────────────────
    private VBox infoField(String label, String value) {
        VBox box = new VBox(4);
        box.getStyleClass().add("profile-info-field");

        Label l = new Label(label);
        l.getStyleClass().add("profile-field-label");

        Label v = new Label(value != null && !value.isEmpty() ? value : "—");
        v.getStyleClass().add("profile-field-value");
        v.setWrapText(true);

        box.getChildren().addAll(l, v);
        return box;
    }

    // ── Stat card (icon + number + label) ─────────────────
    private VBox statCard(String number, String label, String icon) {
        VBox c = new VBox(6);
        c.getStyleClass().add("profile-stat-card");
        c.setAlignment(Pos.CENTER);
        c.setPadding(new Insets(20));

        Label iconL = new Label(icon);
        iconL.setStyle("-fx-font-size: 22px; -fx-text-fill: #2C3E50;");

        Label numL = new Label(number);
        numL.getStyleClass().add("profile-stat-value");

        Label lblL = new Label(label);
        lblL.getStyleClass().add("profile-stat-label");

        c.getChildren().addAll(iconL, numL, lblL);
        return c;
    }

    @SuppressWarnings("unchecked")
    private void loadOrdersCount(Label countLabel) {
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_ORDER_HISTORY, NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId())))
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.isSuccess() && res.getData() != null)
                        countLabel.setText(String.valueOf(((List<?>) res.getData()).size()));
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

    private String orEmpty(String s) { return s != null ? s : ""; }

    private String fmtRole(String role) {
        if (role == null) return "Client";
        switch (role) {
            case "SUPER_ADMIN": return "Super Admin";
            case "ADMIN":       return "Administrateur";
            default:            return "Client";
        }
    }
}