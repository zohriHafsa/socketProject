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

public class ResetPasswordScreen {

    private final String email;
    private final AuthController authController = new AuthController();

    public ResetPasswordScreen(String email) {
        this.email = email;
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
        Label lockIcon = new Label("\uD83D\uDD12");
        lockIcon.setStyle("-fx-font-size: 22px; -fx-text-fill: #2C3E50;");
        Label title = new Label("Nouveau mot de passe");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 22px;");
        titleRow.getChildren().addAll(lockIcon, title);

        Label subtitle = new Label("Entrez le code recu sur " + email + " et votre nouveau mot de passe.");
        subtitle.getStyleClass().add("auth-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMinHeight(Region.USE_PREF_SIZE);
        subtitle.setStyle("-fx-text-alignment: center;");

        // Divider
        Region div = new Region();
        div.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;");

        // Code input
        Label codeLabel = new Label("Code de reinitialisation");
        codeLabel.getStyleClass().add("auth-field-label");
        TextField codeField = new TextField();
        codeField.setPromptText("Ex: 123456");
        codeField.getStyleClass().add("auth-field");
        codeField.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-text-alignment: center;" +
                "-fx-letter-spacing: 4px;"
        );
        codeField.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) codeField.setText(n.replaceAll("[^\\d]", ""));
            if (n.length() > 6)    codeField.setText(n.substring(0, 6));
        });
        VBox codeGroup = new VBox(5, codeLabel, codeField);

        // New Password input
        Label passLabel = new Label("Nouveau mot de passe");
        passLabel.getStyleClass().add("auth-field-label");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Nouveau mot de passe");
        passField.getStyleClass().add("auth-password-field");
        StackPane passWrapper = wrapWithToggle(passField);
        
        Label passInfo = new Label("8+ caracteres, majuscule, chiffre, symbole.");
        passInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");
        VBox passGroup = new VBox(5, passLabel, passWrapper, passInfo);

        // Confirm Password input
        Label confirmLabel = new Label("Confirmer le mot de passe");
        confirmLabel.getStyleClass().add("auth-field-label");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirmer le mot de passe");
        confirmField.getStyleClass().add("auth-password-field");
        StackPane confirmWrapper = wrapWithToggle(confirmField);
        VBox confirmGroup = new VBox(5, confirmLabel, confirmWrapper);

        // Error / Feedback box
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
        fbText.setMinHeight(Region.USE_PREF_SIZE);
        HBox.setHgrow(fbText, Priority.ALWAYS);
        fbBox.getChildren().addAll(fbIcon, fbText);

        // Submit button
        Button submitBtn = new Button("Reinitialiser mon mot de passe");
        submitBtn.getStyleClass().add("auth-btn-primary");
        submitBtn.setOnAction(e -> handleSubmit(codeField, passField, confirmField, submitBtn, fbBox, fbText, fbIcon));
        confirmField.setOnAction(e -> handleSubmit(codeField, passField, confirmField, submitBtn, fbBox, fbText, fbIcon));

        card.getChildren().addAll(titleRow, subtitle, div, codeGroup, passGroup, confirmGroup, fbBox, submitBtn);

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

    private void handleSubmit(TextField codeField, PasswordField passField, PasswordField confirmField, Button submitBtn,
                              HBox fbBox, Label fbText, Label fbIcon) {
        String code = codeField.getText().trim();
        String pass = passField.getText();
        String conf = confirmField.getText();

        if (code.length() != 6) {
            showFeedback(fbBox, fbText, fbIcon, "Le code doit contenir 6 chiffres.", false);
            return;
        }

        if (pass.isEmpty() || conf.isEmpty()) {
            showFeedback(fbBox, fbText, fbIcon, "Veuillez remplir tous les champs.", false);
            return;
        }

        if (!pass.equals(conf)) {
            showFeedback(fbBox, fbText, fbIcon, "Les mots de passe ne correspondent pas.", false);
            return;
        }

        submitBtn.setDisable(true);
        submitBtn.setText("Reinitialisation...");

        new Thread(() -> {
            Response resp = authController.resetPassword(email, code, pass);

            Platform.runLater(() -> {
                submitBtn.setDisable(false);
                submitBtn.setText("Reinitialiser mon mot de passe");

                if (resp.isSuccess()) {
                    showFeedback(fbBox, fbText, fbIcon, 
                        "Mot de passe modifie ! Redirection...", true);
                    
                    javafx.animation.PauseTransition pause =
                            new javafx.animation.PauseTransition(
                                    javafx.util.Duration.seconds(1.5));
                    pause.setOnFinished(e -> NavigationManager.showLogin());
                    pause.play();
                } else {
                    showFeedback(fbBox, fbText, fbIcon,
                            resp.getMessage() != null
                                    ? resp.getMessage()
                                    : "Erreur lors de la reinitialisation.", false);
                }
            });
        }).start();
    }

    private void showFeedback(HBox fbBox, Label fbText, Label fbIcon, String msg, boolean success) {
        fbText.setText(msg);
        fbBox.getStyleClass().removeAll("auth-error", "auth-success");
        if (success) {
            fbBox.getStyleClass().add("auth-success");
            fbBox.setStyle("-fx-padding: 10 14;");
            fbText.setStyle("-fx-font-size: 13px; -fx-text-fill: #166534;");
            fbIcon.setText("✓");
            fbIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #166534;");
        } else {
            fbBox.getStyleClass().add("auth-error");
            fbBox.setStyle(null);
            fbText.setStyle(null);
            fbIcon.setText("\u26A0");
            fbIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #E74C3C;");
            
            // Shake animation
            TranslateTransition tt = new TranslateTransition(Duration.millis(50), fbBox);
            tt.setFromX(0);
            tt.setByX(8);
            tt.setCycleCount(6);
            tt.setAutoReverse(true);
            tt.setOnFinished(e -> fbBox.setTranslateX(0));
            tt.play();
        }
        fbBox.setVisible(true);
        fbBox.setManaged(true);
    }
}
