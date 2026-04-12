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

public class ResetPasswordScreen {

    private final AuthController authController;
    private final String         username;
    private final String         token;

    public ResetPasswordScreen(AuthController authController,
                               String username, String token) {
        this.authController = authController;
        this.username       = username;
        this.token          = token;
    }

    public Node getView() {
        StackPane root = new StackPane();
        root.getStyleClass().add("auth-root");

        Label watermark = new Label("ChriOnline");
        watermark.getStyleClass().add("auth-brand-watermark");
        watermark.setMouseTransparent(true);
        StackPane.setAlignment(watermark, Pos.CENTER);

        Button backLink = new Button("\u2190  Retour à la connexion");
        backLink.getStyleClass().add("auth-back-link");
        backLink.setOnAction(e -> NavigationManager.showLogin());
        StackPane.setAlignment(backLink, Pos.TOP_LEFT);
        StackPane.setMargin(backLink, new Insets(24, 0, 0, 24));

        VBox centerBox = new VBox(28);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(40));
        centerBox.setMaxWidth(440);

        VBox branding = buildBranding();

        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(28, 36, 24, 36));
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(400);

        // Title
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER);
        Label icon = new Label("\uD83D\uDD10"); // 🔐
        icon.setStyle("-fx-font-size: 22px;");
        Label title = new Label("Nouveau mot de passe");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 20px;");
        titleRow.getChildren().addAll(icon, title);

        Label subtitle = new Label("Choisissez un mot de passe fort d'au moins 8 caractères.");
        subtitle.getStyleClass().add("auth-subtitle");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Region div = new Region();
        div.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;");

        // New password
        Label newPassLabel = new Label("Nouveau mot de passe");
        newPassLabel.getStyleClass().add("auth-field-label");
        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("Minimum 8 caractères");
        newPassField.getStyleClass().add("auth-password-field");
        StackPane newPassWrapper = wrapWithToggle(newPassField);
        VBox newPassGroup = new VBox(5, newPassLabel, newPassWrapper);

        // Confirm password
        Label confirmLabel = new Label("Confirmer le mot de passe");
        confirmLabel.getStyleClass().add("auth-field-label");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Répétez le mot de passe");
        confirmField.getStyleClass().add("auth-password-field");
        StackPane confirmWrapper = wrapWithToggle(confirmField);
        VBox confirmGroup = new VBox(5, confirmLabel, confirmWrapper);

        // Strength indicator
        ProgressBar strengthBar = new ProgressBar(0);
        strengthBar.setMaxWidth(Double.MAX_VALUE);
        strengthBar.setStyle("-fx-accent: #E74C3C;");
        Label strengthLabel = new Label("");
        strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        newPassField.textProperty().addListener((obs, o, n) ->
                updateStrength(n, strengthBar, strengthLabel));

        // Error box
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
        Button saveBtn = new Button("Enregistrer le mot de passe");
        saveBtn.getStyleClass().add("auth-btn-primary");
        saveBtn.setOnAction(e ->
                handleReset(newPassField, confirmField, saveBtn, errorBox, errorText, card));
        confirmField.setOnAction(e ->
                handleReset(newPassField, confirmField, saveBtn, errorBox, errorText, card));

        card.getChildren().addAll(
                titleRow, subtitle, div,
                newPassGroup,
                strengthBar, strengthLabel,
                confirmGroup,
                errorBox,
                saveBtn
        );

        centerBox.getChildren().addAll(branding, card);
        Pane particles = buildParticles();
        root.getChildren().addAll(particles, watermark, backLink, centerBox);

        Platform.runLater(() -> {
            branding.setOpacity(0);
            branding.setTranslateY(-15);
            card.setOpacity(0);
            card.setTranslateY(20);

            FadeTransition fb = new FadeTransition(Duration.millis(500), branding);
            fb.setToValue(1);
            TranslateTransition tb = new TranslateTransition(Duration.millis(500), branding);
            tb.setToY(0); tb.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition fc = new FadeTransition(Duration.millis(600), card);
            fc.setToValue(1); fc.setDelay(Duration.millis(150));
            TranslateTransition tc = new TranslateTransition(Duration.millis(600), card);
            tc.setToY(0); tc.setDelay(Duration.millis(150));
            tc.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fb, tb, fc, tc).play();
        });

        return root;
    }

    // ═══════════════════════════════════════════════════════
    //  RESET LOGIC
    // ═══════════════════════════════════════════════════════

    private void handleReset(PasswordField newPassField, PasswordField confirmField,
                             Button saveBtn, HBox errorBox, Label errorText, VBox card) {



        String newPass  = newPassField.getText();
        String confirm  = confirmField.getText();

        if (newPass.length() < 8) {
            showError(errorBox, errorText, "Le mot de passe doit contenir au moins 8 caractères.");
            shakeNode(errorBox); return;
        }
        if (!newPass.equals(confirm)) {
            showError(errorBox, errorText, "Les mots de passe ne correspondent pas.");
            shakeNode(confirmField); return;
        }

        hideError(errorBox);
        saveBtn.setDisable(true);
        saveBtn.setText("Enregistrement...");

        new Thread(() -> {
            Map<String, Object> data = new HashMap<>();
            data.put("username", username);
            data.put("token",    token);
            data.put("password", newPass);
            Response response = authController.resetPassword(data);

            Platform.runLater(() -> {
                saveBtn.setDisable(false);
                saveBtn.setText("Enregistrer le mot de passe");

                if (response.isSuccess()) {
                    buildSuccessState(card);
                } else {
                    showError(errorBox, errorText,
                            response.getMessage() != null
                                    ? response.getMessage()
                                    : "Erreur. Veuillez recommencer la procédure.");
                    shakeNode(errorBox);
                }
            });
        }).start();
    }

    private void buildSuccessState(VBox card) {
        card.getChildren().clear();

        Label checkIcon = new Label("\u2705");
        checkIcon.setStyle("-fx-font-size: 52px;");

        Label title = new Label("Mot de passe modifié !");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 22px;");

        Label msg = new Label("Votre mot de passe a été mis à jour avec succès.\nVous pouvez maintenant vous connecter.");
        msg.getStyleClass().add("auth-subtitle");
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button loginBtn = new Button("Se connecter");
        loginBtn.getStyleClass().add("auth-btn-primary");
        loginBtn.setOnAction(e -> NavigationManager.showLogin());

        card.getChildren().addAll(checkIcon, title, msg, loginBtn);
        card.setAlignment(Pos.CENTER);

        card.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setToValue(1);
        ft.play();

        ScaleTransition st = new ScaleTransition(Duration.millis(500), checkIcon);
        st.setFromX(0.5); st.setFromY(0.5);
        st.setToX(1.0);   st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.setDelay(Duration.millis(200));
        st.play();
    }

    // ═══════════════════════════════════════════════════════
    //  PASSWORD STRENGTH
    // ═══════════════════════════════════════════════════════

    private void updateStrength(String pass, ProgressBar bar, Label label) {
        int score = 0;
        if (pass.length() >= 8)                          score++;
        if (pass.length() >= 12)                         score++;
        if (pass.matches(".*[A-Z].*"))                   score++;
        if (pass.matches(".*[0-9].*"))                   score++;
        if (pass.matches(".*[^A-Za-z0-9].*"))            score++;

        double progress = score / 5.0;
        bar.setProgress(progress);

        String color, text;
        if      (score <= 1) { color = "#E74C3C"; text = "Très faible"; }
        else if (score == 2) { color = "#E67E22"; text = "Faible"; }
        else if (score == 3) { color = "#F1C40F"; text = "Moyen"; }
        else if (score == 4) { color = "#2ECC71"; text = "Fort"; }
        else                 { color = "#27AE60"; text = "Très fort"; }

        bar.setStyle("-fx-accent: " + color + ";");
        label.setText(text);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + ";");
    }

    // ═══════════════════════════════════════════════════════
    //  PASSWORD TOGGLE — same helper as LoginScreen
    // ═══════════════════════════════════════════════════════

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
                "-fx-background-color: transparent; -fx-border-color: transparent;" +
                        "-fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 0 10 0 0;"
        );
        StackPane.setAlignment(eyeBtn, Pos.CENTER_RIGHT);
        eyeBtn.setOnAction(e -> {
            if (visibleField.isVisible()) {
                pf.setText(visibleField.getText());
                pf.setVisible(true);   pf.setManaged(true);
                visibleField.setVisible(false); visibleField.setManaged(false);
                eyeBtn.setText("\uD83D\uDC41");
            } else {
                visibleField.setText(pf.getText());
                visibleField.setVisible(true);  visibleField.setManaged(true);
                pf.setVisible(false);  pf.setManaged(false);
                eyeBtn.setText("\u00D8");
            }
        });

        StackPane wrapper = new StackPane(pf, visibleField, eyeBtn);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        return wrapper;
    }

    // ── Branding + particles (same as your other screens) ──

    private VBox buildBranding() {
        VBox b = new VBox(6);
        b.setAlignment(Pos.CENTER);
        try {
            javafx.scene.image.ImageView logo = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(java.util.Objects.requireNonNull(
                            getClass().getResourceAsStream("/images/logo.png"))));
            logo.setFitHeight(52); logo.setPreserveRatio(true); logo.setSmooth(true);
            b.getChildren().add(logo);
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
        b.getChildren().addAll(nameRow, tagline);
        return b;
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

    private void showError(HBox errorBox, Label errorText, String msg) {
        errorText.setText(msg);
        errorBox.setStyle("");
        errorBox.setVisible(true);
        errorBox.setManaged(true);
        errorBox.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(250), errorBox);
        ft.setToValue(1);
        ft.play();
    }

    private void hideError(HBox errorBox) {
        errorBox.setVisible(false); errorBox.setManaged(false);
    }

    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0); tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }
}