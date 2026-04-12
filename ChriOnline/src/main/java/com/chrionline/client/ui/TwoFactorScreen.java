package com.chrionline.client.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
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

public class TwoFactorScreen {

    private final int    userId;
    private final String email;
    private final Gson gson = new Gson();

    public TwoFactorScreen(int userId, String email) {
        this.userId = userId;
        this.email  = email;
    }

    public Node getView() {
        StackPane root = new StackPane();
        root.getStyleClass().add("auth-root");

        // ── Watermark background ─────────────────────────
        Label watermark = new Label("ChriOnline");
        watermark.getStyleClass().add("auth-brand-watermark");
        watermark.setMouseTransparent(true);
        StackPane.setAlignment(watermark, Pos.CENTER);

        // ── Back to login — top left ──────────────────────
        Button backLink = new Button("\u2190  Retour a la connexion");
        backLink.getStyleClass().add("auth-back-link");
        backLink.setOnAction(e -> NavigationManager.showLogin());
        StackPane.setAlignment(backLink, Pos.TOP_LEFT);
        StackPane.setMargin(backLink, new Insets(24, 0, 0, 24));

        // ── Center content ───────────────────────────────
        VBox centerBox = new VBox(28);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(40, 40, 40, 40));
        centerBox.setMaxWidth(500);

        // ── Branding (outside the card) ──────────────────
        VBox branding = buildBranding();

        // ── White card (compact) ─────────────────────────
        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(28, 36, 24, 36));
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(400);

        // Title
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER);
        Label mailIcon = new Label("\uD83D\uDD12");
        mailIcon.setStyle("-fx-font-size: 22px; -fx-text-fill: #2C3E50;");
        Label title = new Label("Authentification a \n   deux facteurs");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 22px;");
        titleRow.getChildren().addAll(mailIcon, title);

        Label subtitle = new Label("Un code de connexion a 6 chiffres a ete envoye a :\n" + email);
        subtitle.getStyleClass().add("auth-subtitle");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-alignment: center;");

        // Divider
        Region div = new Region();
        div.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;");

        // Code input
        Label codeLabel = new Label("Code de connexion (2FA)");
        codeLabel.getStyleClass().add("auth-field-label");
        TextField codeField = new TextField();
        codeField.setPromptText("_ _ _ _ _ _");
        codeField.getStyleClass().add("auth-field");
        codeField.setAlignment(Pos.CENTER);
        codeField.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-text-alignment: center;" +
                "-fx-letter-spacing: 8px;"
        );
        codeField.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) codeField.setText(n.replaceAll("[^\\d]", ""));
            if (n.length() > 6)    codeField.setText(n.substring(0, 6));
        });
        VBox codeGroup = new VBox(5, codeLabel, codeField);

        // Error / Feedback box (hidden by default)
        HBox fbBox = new HBox(8);
        fbBox.getStyleClass().add("auth-error");
        fbBox.setVisible(false);
        fbBox.setManaged(false);
        fbBox.setAlignment(Pos.CENTER_LEFT);
        Label fbIcon = new Label("\u26A0");
        fbIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #E74C3C;");
        Label fbText = new Label();
        fbText.getStyleClass().add("auth-error-text");
        fbText.setWrapText(true);
        HBox.setHgrow(fbText, Priority.ALWAYS);
        fbBox.getChildren().addAll(fbIcon, fbText);

        // Bouton vérifier
        Button verifyBtn = new Button("Valider mon code 2FA");
        verifyBtn.getStyleClass().add("auth-btn-primary");
        verifyBtn.setOnAction(e -> handleVerify(codeField, verifyBtn, fbBox, fbText, fbIcon));
        codeField.setOnAction(e -> handleVerify(codeField, verifyBtn, fbBox, fbText, fbIcon));

        card.getChildren().addAll(titleRow, subtitle, div, codeGroup, fbBox, verifyBtn);

        centerBox.getChildren().addAll(branding, card);

        Pane particles = buildParticles();

        root.getChildren().addAll(particles, watermark, backLink, centerBox);

        // ── Entrance animation ───────────────────────────
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
        l1.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);");
        Label l2 = new Label("Online");
        l2.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: #D2B4DE; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);");
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
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(rand.nextDouble() * 2 + 1.5);
            dot.setFill(javafx.scene.paint.Color.rgb(255, 255, 255, 0.04 + rand.nextDouble() * 0.06));
            dot.setLayoutX(rand.nextDouble() * 1400);
            dot.setLayoutY(rand.nextDouble() * 900);
            layer.getChildren().add(dot);
            TranslateTransition tt = new TranslateTransition(Duration.seconds(8 + rand.nextDouble() * 12), dot);
            tt.setByY(-40 - rand.nextDouble() * 60);
            tt.setByX((rand.nextDouble() - 0.5) * 40);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.setAutoReverse(true);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }
        return layer;
    }

    private void handleVerify(TextField codeField, Button verifyBtn,
                              HBox fbBox, Label fbText, Label fbIcon) {
        String code = codeField.getText().trim();

        if (code.length() != 6) {
            showFeedback(fbBox, fbText, fbIcon, "Le code doit contenir 6 chiffres.", false);
            return;
        }

        verifyBtn.setDisable(true);
        verifyBtn.setText("Verification...");

        new Thread(() -> {
            Response resp = ServerConnection.getInstance().send(
                    new Request(Action.VERIFY_2FA, null,
                            java.util.Map.of("userId", userId, "code", code))
            );

            Platform.runLater(() -> {
                verifyBtn.setDisable(false);
                verifyBtn.setText("Valider mon code 2FA");

                if (resp.isSuccess()) {
                    showFeedback(fbBox, fbText, fbIcon,
                            "Authentification reussie ! Redirection...", true);
                    
                    Map<String, Object> data = castToMap(resp.getData());
                    NavigationManager.setSession(
                            getStr(data, "token"),
                            getInt(data, "userId"),
                            getStr(data, "username"),
                            getStr(data, "role"),
                            getStr(data, "firstName"),
                            getStr(data, "lastName")
                    );

                    javafx.animation.PauseTransition pause =
                            new javafx.animation.PauseTransition(
                                    javafx.util.Duration.seconds(1.5));
                    pause.setOnFinished(e -> {
                        String role = getStr(data, "role");
                        if ("ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
                            NavigationManager.showAdminDashboard();
                        } else {
                            NavigationManager.showLanding();
                        }
                    });
                    pause.play();
                } else {
                    showFeedback(fbBox, fbText, fbIcon,
                            resp.getMessage() != null
                                    ? resp.getMessage()
                                    : "Code incorrect.", false);
                }
            });
        }).start();
    }

    private void showFeedback(HBox fbBox, Label fbText, Label fbIcon, String msg, boolean success) {
        fbText.setText(msg);
        fbBox.getStyleClass().removeAll("auth-error", "auth-success");
        if (success) {
            fbBox.getStyleClass().add("auth-success");
            fbBox.setStyle("-fx-padding: 10 14;"); // inline padding
            fbText.setStyle("-fx-font-size: 13px; -fx-text-fill: #166534;");
            fbIcon.setText("✓");
            fbIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #166534;");
        } else {
            fbBox.getStyleClass().add("auth-error");
            fbBox.setStyle(null); // revert to standard padding
            fbText.setStyle(null);
            fbIcon.setText("\u26A0");
            fbIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #E74C3C;");
        }
        fbBox.setVisible(true);
        fbBox.setManaged(true);
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
}
