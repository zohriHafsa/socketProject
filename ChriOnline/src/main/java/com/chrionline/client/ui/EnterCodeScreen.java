package com.chrionline.client.ui;

import com.chrionline.client.controller.AuthController;
import com.chrionline.shared.dto.Response;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class EnterCodeScreen {

    private final AuthController authController;
    private final String         username;

    public EnterCodeScreen(AuthController authController, String username) {
        this.authController = authController;
        this.username       = username;
    }

    public Node getView() {
        StackPane root = new StackPane();
        root.getStyleClass().add("auth-root");

        // ── Watermark ────────────────────────────────────
        Label watermark = new Label("ChriOnline");
        watermark.getStyleClass().add("auth-brand-watermark");
        watermark.setMouseTransparent(true);
        StackPane.setAlignment(watermark, Pos.CENTER);

        // ── Back link ────────────────────────────────────
        Button backLink = new Button("\u2190  Retour à la connexion");
        backLink.getStyleClass().add("auth-back-link");
        backLink.setOnAction(e -> NavigationManager.showLogin());
        StackPane.setAlignment(backLink, Pos.TOP_LEFT);
        StackPane.setMargin(backLink, new Insets(24, 0, 0, 24));

        // ── Center column ─────────────────────────────────
        VBox centerBox = new VBox(28);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(40));
        centerBox.setMaxWidth(440);

        VBox branding = buildBranding();

        // ── Card ──────────────────────────────────────────
        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(28, 36, 24, 36));
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(400);

        // Title row
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER);
        Label icon = new Label("\uD83D\uDCEC"); // 📬
        icon.setStyle("-fx-font-size: 22px;");
        Label title = new Label("Vérification du code");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 20px;");
        titleRow.getChildren().addAll(icon, title);

        Label subtitle = new Label(
                "Un code à 6 chiffres a été envoyé à l'adresse\n" +
                        "email associée à votre compte."
        );
        subtitle.getStyleClass().add("auth-subtitle");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Divider
        Region div = new Region();
        div.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;");

        // Code field
        Label codeLabel = new Label("Code de vérification");
        codeLabel.getStyleClass().add("auth-field-label");

        TextField codeField = new TextField();
        codeField.setPromptText("_ _ _ _ _ _");
        codeField.getStyleClass().add("auth-field");
        codeField.setMaxWidth(Double.MAX_VALUE);
        // Accept digits only, max 6 chars
        codeField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^0-9]", "");
            if (filtered.length() > 6) filtered = filtered.substring(0, 6);
            if (!filtered.equals(newVal)) codeField.setText(filtered);
        });

        VBox codeGroup = new VBox(5, codeLabel, codeField);

        // Countdown label (cosmetic — no real timer logic needed server-side)
        Label expiryNote = new Label("\u23F1  Ce code expire dans 30 minutes.");
        expiryNote.setStyle(
                "-fx-font-size: 11.5px; " +
                        "-fx-text-fill: #888888;"
        );

        // Error box (hidden)
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

        // Verify button
        Button verifyBtn = new Button("Vérifier le code");
        verifyBtn.getStyleClass().add("auth-btn-primary");
        verifyBtn.setOnAction(e ->
                handleVerify(codeField, verifyBtn, errorBox, errorText));
        codeField.setOnAction(e ->
                handleVerify(codeField, verifyBtn, errorBox, errorText));

        // Resend link row
        HBox resendRow = new HBox(4);
        resendRow.setAlignment(Pos.CENTER);
        Label resendLabel = new Label("Vous n'avez pas reçu le code ?");
        resendLabel.getStyleClass().add("auth-secondary-text");
        Button resendBtn = new Button("Renvoyer");
        resendBtn.getStyleClass().add("auth-link");
        resendBtn.setOnAction(e -> handleResend(resendBtn, errorBox, errorText));
        resendRow.getChildren().addAll(resendLabel, resendBtn);

        card.getChildren().addAll(
                titleRow, subtitle, div,
                codeGroup,
                expiryNote,
                errorBox,
                verifyBtn,
                resendRow
        );

        centerBox.getChildren().addAll(branding, card);

        Pane particles = buildParticles();
        root.getChildren().addAll(particles, watermark, backLink, centerBox);

        // ── Entrance animation (same pattern as your other screens) ──
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

    // ═══════════════════════════════════════════════════════
    //  VERIFY LOGIC
    // ═══════════════════════════════════════════════════════

    private void handleVerify(TextField codeField, Button verifyBtn,
                              HBox errorBox, Label errorText) {

        String code = codeField.getText().trim();

        if (code.length() != 6) {
            showError(errorBox, errorText, "Le code doit contenir 6 chiffres.");
            shakeNode(errorBox);
            return;
        }

        hideError(errorBox);
        verifyBtn.setDisable(true);
        verifyBtn.setText("Vérification...");

        new Thread(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("username", username);
            data.put("token",    code);

            Response response = authController.verifyResetToken(data);

            Platform.runLater(() -> {
                verifyBtn.setDisable(false);
                verifyBtn.setText("Vérifier le code");

                if (response.isSuccess()) {
                    // Pass username + validated code to ResetPasswordScreen
                    NavigationManager.showResetPassword(username, code);
                } else {
                    showError(errorBox, errorText,
                            response.getMessage() != null
                                    ? response.getMessage()
                                    : "Code invalide ou expiré. Veuillez réessayer."
                    );
                    shakeNode(errorBox);
                    codeField.clear();
                    codeField.requestFocus();
                }
            });
        }).start();
    }

    private void handleResend(Button resendBtn, HBox errorBox, Label errorText) {
        resendBtn.setDisable(true);
        resendBtn.setText("Envoi...");
        hideError(errorBox);

        new Thread(() -> {
            Response response = authController.forgotPassword(username);

            Platform.runLater(() -> {
                resendBtn.setDisable(false);
                resendBtn.setText("Renvoyer");

                if (response.isSuccess()) {
                    // Show a brief success hint reusing the error box with green style
                    errorText.setText("\u2713  Nouveau code envoyé !");
                    errorBox.setStyle(
                            "-fx-background-color: #EAFAF1;" +
                                    "-fx-background-radius: 6;" +
                                    "-fx-border-color: #2ECC71;" +
                                    "-fx-border-radius: 6;" +
                                    "-fx-padding: 8 12 8 12;"
                    );
                    errorBox.setVisible(true);
                    errorBox.setManaged(true);
                } else {
                    showError(errorBox, errorText, "Erreur lors du renvoi. Réessayez.");
                }
            });
        }).start();
    }

    // ═══════════════════════════════════════════════════════
    //  BRANDING + PARTICLES — copy of your existing pattern
    // ═══════════════════════════════════════════════════════

    private VBox buildBranding() {
        VBox branding = new VBox(6);
        branding.setAlignment(Pos.CENTER);

        try {
            javafx.scene.image.ImageView logo = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(
                            java.util.Objects.requireNonNull(
                                    getClass().getResourceAsStream("/images/logo.png"))));
            logo.setFitHeight(52);
            logo.setPreserveRatio(true);
            logo.setSmooth(true);
            branding.getChildren().add(logo);
        } catch (Exception ignored) {}

        HBox nameRow = new HBox(0);
        nameRow.setAlignment(Pos.CENTER);
        Label l1 = new Label("Chri");
        l1.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);");
        Label l2 = new Label("Online");
        l2.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: #D2B4DE;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);");
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
            javafx.scene.shape.Circle dot =
                    new javafx.scene.shape.Circle(rand.nextDouble() * 2 + 1.5);
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

    // ═══════════════════════════════════════════════════════
    //  ERROR HELPERS
    // ═══════════════════════════════════════════════════════

    private void showError(HBox errorBox, Label errorText, String msg) {
        errorText.setText(msg);
        // Reset style in case it was green from resend success
        errorBox.setStyle("");
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
}