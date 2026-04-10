package com.chrionline.client.ui;


import com.chrionline.client.controller.AuthController;
import com.chrionline.shared.dto.Response;
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

import java.util.Objects;

public class ForgotPasswordScreen {

    private final AuthController authController = new AuthController();
    private String currentUsername = "";

    // ─── States ──────────────────────────────────────────────
    private enum ScreenState { ASK_USERNAME, EMAIL_SENT }

    public Node getView() {
        StackPane root = new StackPane();
        root.getStyleClass().add("auth-root");

        // ── Watermark background ─────────────────────────────
        Label watermark = new Label("ChriOnline");
        watermark.getStyleClass().add("auth-brand-watermark");
        watermark.setMouseTransparent(true);
        StackPane.setAlignment(watermark, Pos.CENTER);

        // ── Back to login — top left ─────────────────────────
        Button backLink = new Button("\u2190  Retour à la connexion");
        backLink.getStyleClass().add("auth-back-link");
        backLink.setOnAction(e -> NavigationManager.showLogin());
        StackPane.setAlignment(backLink, Pos.TOP_LEFT);
        StackPane.setMargin(backLink, new Insets(24, 0, 0, 24));

        // ── Center content ───────────────────────────────────
        VBox centerBox = new VBox(28);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(40, 40, 40, 40));
        centerBox.setMaxWidth(440);

        // ── Branding (above the card) ────────────────────────
        VBox branding = buildBranding();

        // ── Card ─────────────────────────────────────────────
        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(28, 36, 24, 36));
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(400);

        // We swap content inside the card based on state
        buildAskUsernameState(card);

        centerBox.getChildren().addAll(branding, card);

        // ── Particles layer ──────────────────────────────────
        Pane particles = buildParticles();

        root.getChildren().addAll(particles, watermark, backLink, centerBox);

        // ── Entrance animation ───────────────────────────────
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
    //  STATE 1 — Ask for username
    // ═══════════════════════════════════════════════════════

    private void buildAskUsernameState(VBox card) {
        card.getChildren().clear();

        // Title row
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER);
        Label icon = new Label("\uD83D\uDD13"); // 🔓
        icon.setStyle("-fx-font-size: 22px; -fx-text-fill: #2C3E50;");
        Label title = new Label("Mot de passe oublié");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 20px;");
        titleRow.getChildren().addAll(icon, title);

        Label subtitle = new Label(
                "Entrez votre nom d'utilisateur. Nous enverrons\n" +
                        "un lien de réinitialisation à l'adresse associée."
        );
        subtitle.getStyleClass().add("auth-subtitle");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Divider
        Region div = new Region();
        div.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;");

        // Username field
        Label userLabel = new Label("Nom d'utilisateur");
        userLabel.getStyleClass().add("auth-field-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("ex : mon_pseudo");
        usernameField.getStyleClass().add("auth-field");
        VBox userGroup = new VBox(5, userLabel, usernameField);

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

        // Submit button
        Button submitBtn = new Button("Envoyer le lien");
        submitBtn.getStyleClass().add("auth-btn-primary");
        submitBtn.setOnAction(e ->
                handleForgotPassword(usernameField, submitBtn, errorBox, errorText, card));
        usernameField.setOnAction(e ->
                handleForgotPassword(usernameField, submitBtn, errorBox, errorText, card));

        // Back to login link
        HBox loginRow = new HBox(4);
        loginRow.setAlignment(Pos.CENTER);
        Label remember = new Label("Vous vous en souvenez ?");
        remember.getStyleClass().add("auth-secondary-text");
        Button loginLink = new Button("Se connecter");
        loginLink.getStyleClass().add("auth-link");
        loginLink.setOnAction(e -> NavigationManager.showLogin());
        loginRow.getChildren().addAll(remember, loginLink);

        card.getChildren().addAll(
                titleRow, subtitle, div,
                userGroup,
                errorBox,
                submitBtn,
                loginRow
        );
    }

    // ═══════════════════════════════════════════════════════
    //  STATE 2 — Email sent confirmation
    // ═══════════════════════════════════════════════════════

    private void buildEmailSentState(VBox card) {
        card.getChildren().clear();

//        Label envelope = new Label("\uD83D\uDCEC");
//        envelope.setStyle("-fx-font-size: 52px;");

        Label title = new Label("Email envoyé !");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 22px;");

        Label msg = new Label(
//                "Si un compte existe avec ce nom d'utilisateur,\n" +
//                        "un code de réinitialisation a été envoyé à\n" +
//                        "l'adresse email associée.\n\n" +
                        "Le code expire dans 30 minutes."
        );
        msg.getStyleClass().add("auth-subtitle");
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msg.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");

        // Security tip box
        HBox tipBox = new HBox(8);
        tipBox.setAlignment(Pos.CENTER_LEFT);
        tipBox.setStyle(
                "-fx-background-color: #EAF4FB;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 10 14 10 14;"
        );


        // ── PRIMARY: enter the code ──────────────────────────
        Button enterCodeBtn = new Button("J'ai reçu mon code  →");
        enterCodeBtn.getStyleClass().add("auth-btn-primary");
        enterCodeBtn.setOnAction(e -> NavigationManager.showEnterCode(currentUsername));

        // ── SECONDARY: back to login ─────────────────────────
        HBox backRow = new HBox(4);
        backRow.setAlignment(Pos.CENTER);
        Label backLabel = new Label("Vous n'avez pas reçu d'email ?");
        backLabel.getStyleClass().add("auth-secondary-text");
        Button backBtn = new Button("Retour à la connexion");
        backBtn.getStyleClass().add("auth-link");
        backBtn.setOnAction(e -> NavigationManager.showLogin());
        backRow.getChildren().addAll(backLabel, backBtn);

        card.getChildren().addAll(/*envelope*/title, msg, tipBox, enterCodeBtn, backRow);
        card.setAlignment(Pos.CENTER);

        // Animate
        card.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setToValue(1);
        ft.play();

        ScaleTransition st = new ScaleTransition(Duration.millis(500) /*envelope*/);
        st.setFromX(0.5); st.setFromY(0.5);
        st.setToX(1.0);   st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.setDelay(Duration.millis(200));
        st.play();
    }

    // ═══════════════════════════════════════════════════════
    //  FORGOT PASSWORD LOGIC
    // ═══════════════════════════════════════════════════════

    private void handleForgotPassword(TextField usernameField, Button submitBtn,
                                      HBox errorBox, Label errorText, VBox card) {

        String username = usernameField.getText().trim();
        this.currentUsername = username;

        if (username.isEmpty()) {
            showError(errorBox, errorText, "Veuillez entrer votre nom d'utilisateur.");
            shakeNode(errorBox);
            return;
        }

        hideError(errorBox);
        submitBtn.setDisable(true);
        submitBtn.setText("Envoi en cours...");

        new Thread(() -> {
            // TODO: replace with your actual server call
            // Response response = authController.forgotPassword(username);
            // Simulate network call for now:
            Response response = authController.forgotPassword(username);
            Platform.runLater(() -> {
                submitBtn.setDisable(false);
                submitBtn.setText("Envoyer le lien");

                if (response.isSuccess()) {
                    // Always show "email sent" regardless — never confirm if user exists
                    NavigationManager.showEnterCode(currentUsername);
                } else {
                    // Only show error for server/network failures, NOT for "user not found"
                    // (to prevent user enumeration attacks)
                    if ("SERVER_ERROR".equals(response.getMessage())) {
                        showError(errorBox, errorText,
                                "Erreur serveur. Veuillez réessayer plus tard.");
                        shakeNode(errorBox);
                    } else {
                        // Treat everything else as success (user not found → still show sent)
                        NavigationManager.showEnterCode(currentUsername);
                    }
                }
            });
        }).start();
    }


    private Response simulateForgotPassword(String username) {
        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
        return new Response(true, "EMAIL_SENT", null);
    }

    // ═══════════════════════════════════════════════════════
    //  BRANDING — same as LoginScreen
    // ═══════════════════════════════════════════════════════

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
                "-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);");
        Label l2 = new Label("Online");
        l2.setStyle(
                "-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: #D2B4DE;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);");
        nameRow.getChildren().addAll(l1, l2);

        Label tagline = new Label("Votre boutique en ligne premium");
        tagline.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.6);");

        branding.getChildren().addAll(nameRow, tagline);
        return branding;
    }

    // ═══════════════════════════════════════════════════════
    //  FLOATING PARTICLES — same as LoginScreen
    // ═══════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════
    //  ERROR HELPERS
    // ═══════════════════════════════════════════════════════

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
}
