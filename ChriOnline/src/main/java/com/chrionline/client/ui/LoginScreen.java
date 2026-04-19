package com.chrionline.client.ui;

import com.chrionline.client.controller.AuthController;
import com.chrionline.client.network.ServerConnection;
import com.chrionline.client.network.UDPHeartbeatSender;
import com.chrionline.shared.dto.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.Map;
import java.util.Objects;

public class LoginScreen {

    private final AuthController authController = new AuthController();
    private final Gson           gson           = new Gson();

    public Node getView() {
        StackPane root = new StackPane();
        root.getStyleClass().add("auth-root");

        Label watermark = new Label("ChriOnline");
        watermark.getStyleClass().add("auth-brand-watermark");
        watermark.setMouseTransparent(true);
        StackPane.setAlignment(watermark, Pos.CENTER);

        Button backLink = new Button("\u2190  Retour a l'accueil");
        backLink.getStyleClass().add("auth-back-link");
        backLink.setOnAction(e -> NavigationManager.showLanding());
        StackPane.setAlignment(backLink, Pos.TOP_LEFT);
        StackPane.setMargin(backLink, new Insets(24, 0, 0, 24));

        VBox centerBox = new VBox(28);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(40, 40, 40, 40));
        centerBox.setMaxWidth(440);

        VBox branding = buildBranding();

        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(28, 36, 24, 36));
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(400);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER);
        Label lockIcon = new Label("\uD83D\uDD12");
        lockIcon.setStyle("-fx-font-size: 22px; -fx-text-fill: #2C3E50;");
        Label title = new Label("Connexion");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 22px;");
        titleRow.getChildren().addAll(lockIcon, title);

        Label subtitle = new Label("Entrez vos identifiants pour continuer");
        subtitle.getStyleClass().add("auth-subtitle");

        Region div = new Region();
        div.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;");

        Label userLabel = new Label("Nom d'utilisateur");
        userLabel.getStyleClass().add("auth-field-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("ex : mon_pseudo");
        usernameField.getStyleClass().add("auth-field");
        VBox userGroup = new VBox(5, userLabel, usernameField);

        Label passLabel = new Label("Mot de passe");
        passLabel.getStyleClass().add("auth-field-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.getStyleClass().add("auth-password-field");
        StackPane passWrapper = wrapWithToggle(passwordField);
        VBox passGroup = new VBox(5, passLabel, passWrapper);

        Button forgotLink = new Button("Mot de passe oublié ?");
        forgotLink.getStyleClass().add("auth-link");
        forgotLink.setOnAction(e -> NavigationManager.showForgotPassword());
        HBox forgotRow = new HBox(forgotLink);
        forgotRow.setAlignment(Pos.CENTER_RIGHT);
        forgotRow.setPadding(new Insets(0, 0, 6, 0));

        HBox errorBox = new HBox(8);
        errorBox.getStyleClass().add("auth-error");
        errorBox.setVisible(false);
        errorBox.setManaged(false);
        errorBox.setAlignment(Pos.CENTER_LEFT);
        Label errorIcon = new Label("\u26A0");
        errorIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #E74C3C;");
        Label errorText = new Label();
        errorText.getStyleClass().add("auth-error-text");
        errorText.setWrapText(true);
        HBox.setHgrow(errorText, Priority.ALWAYS);
        errorBox.getChildren().addAll(errorIcon, errorText);

        Button loginBtn = new Button("Se connecter");
        loginBtn.getStyleClass().add("auth-btn-primary");
        loginBtn.setOnAction(e ->
                handleLogin(usernameField, passwordField, loginBtn,
                        errorBox, errorText));
        passwordField.setOnAction(e ->
                handleLogin(usernameField, passwordField, loginBtn,
                        errorBox, errorText));

        HBox registerRow = new HBox(4);
        registerRow.setAlignment(Pos.CENTER);
        Label noAccount = new Label("Pas encore de compte ?");
        noAccount.getStyleClass().add("auth-secondary-text");
        Button registerLink = new Button("S'inscrire");
        registerLink.getStyleClass().add("auth-link");
        registerLink.setOnAction(e -> NavigationManager.showRegister());
        registerRow.getChildren().addAll(noAccount, registerLink);

        card.getChildren().addAll(
                titleRow, subtitle, div,
                userGroup, passGroup,
                forgotRow,
                errorBox,
                loginBtn,
                registerRow
        );

        centerBox.getChildren().addAll(branding, card);

        Pane particles = buildParticles();

        Button adminIconBtn = new Button("\u2699"); 
        adminIconBtn.getStyleClass().add("auth-back-link");
        adminIconBtn.setStyle("-fx-font-size: 22px; -fx-text-fill: rgba(255, 255, 255, 0.4);");
        adminIconBtn.setOnAction(e -> NavigationManager.showAdminLogin());
        StackPane.setAlignment(adminIconBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(adminIconBtn, new Insets(20, 24, 0, 0));

        root.getChildren().addAll(particles, watermark, backLink, adminIconBtn, centerBox);

        Platform.runLater(() -> {
            branding.setOpacity(0);
            branding.setTranslateY(-15);
            card.setOpacity(0);
            card.setTranslateY(20);

            FadeTransition fb = new FadeTransition(Duration.millis(500), branding);
            fb.setToValue(1);
            TranslateTransition tb = new TranslateTransition(Duration.millis(500), branding);
            tb.setToY(0);
            tb.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition fc = new FadeTransition(Duration.millis(600), card);
            fc.setToValue(1);
            fc.setDelay(Duration.millis(150));
            TranslateTransition tc = new TranslateTransition(Duration.millis(600), card);
            tc.setToY(0);
            tc.setDelay(Duration.millis(150));
            tc.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fb, tb, fc, tc).play();
        });

        return root;
    }

    private VBox buildBranding() {
        VBox branding = new VBox(6);
        branding.setAlignment(Pos.CENTER);

        try {
            ImageView logo = new ImageView(new Image(
                    Objects.requireNonNull(
                            getClass().getResourceAsStream("/images/logo.png"))));
            logo.setFitHeight(52);
            logo.setPreserveRatio(true);
            logo.setSmooth(true);
            branding.getChildren().add(logo);
        } catch (Exception ignored) {}

        HBox nameRow = new HBox(0);
        nameRow.setAlignment(Pos.CENTER);
        Label l1 = new Label("Chri");
        l1.setStyle(
                "-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);");
        Label l2 = new Label("Online");
        l2.setStyle(
                "-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: #D2B4DE;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);");
        nameRow.getChildren().addAll(l1, l2);

        Label tagline = new Label("Votre boutique en ligne premium");
        tagline.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.6);");

        branding.getChildren().addAll(nameRow, tagline);
        return branding;
    }

    private Pane buildParticles() {
        Pane layer = new Pane();
        layer.getStyleClass().add("auth-particles");
        layer.setMouseTransparent(true);

        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < 12; i++) {
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(
                    rand.nextDouble() * 2 + 1.5);
            dot.setFill(javafx.scene.paint.Color.rgb(255, 255, 255,
                    0.04 + rand.nextDouble() * 0.06));
            dot.setLayoutX(rand.nextDouble() * 1400);
            dot.setLayoutY(rand.nextDouble() * 900);
            layer.getChildren().add(dot);

            TranslateTransition tt = new TranslateTransition(
                    Duration.seconds(8 + rand.nextDouble() * 12), dot);
            tt.setByY(-40 - rand.nextDouble() * 60);
            tt.setByX((rand.nextDouble() - 0.5) * 40);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.setAutoReverse(true);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }

        return layer;
    }

    private void handleLogin(TextField usernameField, PasswordField passwordField,
                             Button loginBtn, HBox errorBox, Label errorText) {

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError(errorBox, errorText,
                    "Veuillez remplir tous les champs.");
            return;
        }

        hideError(errorBox);
        loginBtn.setDisable(true);
        loginBtn.setText("Connexion en cours...");

        new Thread(() -> {
            if (!ServerConnection.getInstance().isConnected()) {
                boolean ok = ServerConnection.getInstance().connect();
                if (!ok) {
                    Platform.runLater(() -> {
                        showError(errorBox, errorText,
                                "Serveur inaccessible. Verifiez qu'il est demarre sur le port 9090.");
                        loginBtn.setDisable(false);
                        loginBtn.setText("Se connecter");
                    });
                    return;
                }
            }

            Response response = authController.login(username, password);

            Platform.runLater(() -> {
                loginBtn.setDisable(false);
                loginBtn.setText("Se connecter");

                if (response.isSuccess()) {
                    Map<String, Object> data = castToMap(response.getData());

                    if (Boolean.TRUE.equals(data.get("2fa_required"))) {
                        NavigationManager.showTwoFactor(
                                getInt(data, "userId"),
                                getStr(data, "email"),
                                false
                        );
                        return;
                    }

                    // ← MODIFIER ICI (6 paramètres avec email)
                    NavigationManager.setSession(
                            getStr(data, "token"),
                            getInt(data, "userId"),
                            getStr(data, "username"),
                            getStr(data, "role"),
                            getStr(data, "firstName"),
                            getStr(data, "lastName"),
                            getStr(data, "email")  // ← AJOUTER
                    );

                    int uid = getInt(data, "userId");
                    UDPHeartbeatSender heartbeat = new UDPHeartbeatSender(uid);
                    Thread heartbeatThread = new Thread(heartbeat);
                    heartbeatThread.setDaemon(true);
                    heartbeatThread.setName("udp-heartbeat-" + uid);
                    heartbeatThread.start();

                    String role = getStr(data, "role");

                    if ("ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
                        NavigationManager.showAdminDashboard();
                    } else {
                        NavigationManager.showLanding();
                    }

                } else {
                    showError(errorBox, errorText, response.getMessage());
                    shakeNode(errorBox);
                }
            });
        }).start();
    }

    private void showError(HBox errorBox, Label errorText, String msg) {
        errorText.setText(msg);
        errorBox.setVisible(true);
        errorBox.setManaged(true);

        errorBox.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(250), errorBox);
        ft.setToValue(1);
        ft.play();
    }

    private void hideError(HBox errorBox) {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
    }

    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        if (obj == null) return Map.of();
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return gson.fromJson(gson.toJson(obj),
                new TypeToken<Map<String, Object>>(){}.getType());
    }

    private String getStr(Map<String, Object> m, String key) {
        if (m == null) return "";
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private int getInt(Map<String, Object> m, String key) {
        if (m == null) return -1;
        Object v = m.get(key);
        if (v == null) return -1;
        if (v instanceof Double) return ((Double) v).intValue();
        return Integer.parseInt(v.toString());
    }

    private StackPane wrapWithToggle(PasswordField pf) {
        TextField visibleField = new TextField();
        visibleField.setPromptText(pf.getPromptText());
        visibleField.getStyleClass().addAll(pf.getStyleClass());
        visibleField.setVisible(false);
        visibleField.setManaged(false);

        pf.textProperty().addListener((obs, o, n) -> {
            if (!visibleField.isFocused()) visibleField.setText(n);
        });
        visibleField.textProperty().addListener((obs, o, n) -> {
            if (!pf.isFocused()) pf.setText(n);
        });

        Button eyeBtn = new Button("\uD83D\uDC41");
        eyeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 0 10 0 0;"
        );
        StackPane.setAlignment(eyeBtn, Pos.CENTER_RIGHT);

        eyeBtn.setOnAction(e -> {
            boolean showing = visibleField.isVisible();
            if (showing) {
                pf.setText(visibleField.getText());
                pf.setVisible(true);
                pf.setManaged(true);
                visibleField.setVisible(false);
                visibleField.setManaged(false);
                eyeBtn.setText("\uD83D\uDC41");
            } else {
                visibleField.setText(pf.getText());
                visibleField.setVisible(true);
                visibleField.setManaged(true);
                pf.setVisible(false);
                pf.setManaged(false);
                eyeBtn.setText("\u00D8");
            }
        });

        StackPane wrapper = new StackPane(pf, visibleField, eyeBtn);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        return wrapper;
    }
}