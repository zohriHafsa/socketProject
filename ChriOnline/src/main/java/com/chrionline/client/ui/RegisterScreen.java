package com.chrionline.client.ui;

import com.chrionline.client.controller.AuthController;
import com.chrionline.client.network.ServerConnection;
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

public class RegisterScreen {

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

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("auth-scroll");

        VBox centerBox = new VBox(24);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(30, 40, 40, 40));
        centerBox.setMaxWidth(500);

        VBox branding = buildBranding();

        VBox card = new VBox(12);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(28, 36, 24, 36));
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(460);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label userIcon = new Label("\uD83D\uDC64");
        userIcon.setStyle("-fx-font-size: 22px;");
        Label title = new Label("Creer un compte");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 22px;");
        titleRow.getChildren().addAll(userIcon, title);

        Label subtitle = new Label("Rejoignez ChriOnline en quelques secondes");
        subtitle.getStyleClass().add("auth-subtitle");

        Region div = new Region();
        div.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;");

        Label userLabel = new Label("Nom d'utilisateur *");
        userLabel.getStyleClass().add("auth-field-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("ex : Hafsa_zohri");
        usernameField.getStyleClass().add("auth-field");
        VBox userGroup = new VBox(5, userLabel, usernameField);

        Label emailLabel = new Label("Adresse e-mail *");
        emailLabel.getStyleClass().add("auth-field-label");
        TextField emailField = new TextField();
        emailField.setPromptText("ex : hafsa@email.com");
        emailField.getStyleClass().add("auth-field");
        VBox emailGroup = new VBox(5, emailLabel, emailField);

        Label fnLabel = new Label("Prenom *");
        fnLabel.getStyleClass().add("auth-field-label");
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("Prenom");
        firstNameField.getStyleClass().add("auth-field");
        VBox fnGroup = new VBox(5, fnLabel, firstNameField);
        HBox.setHgrow(fnGroup, Priority.ALWAYS);

        Label lnLabel = new Label("Nom de famille *");
        lnLabel.getStyleClass().add("auth-field-label");
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Nom");
        lastNameField.getStyleClass().add("auth-field");
        VBox lnGroup = new VBox(5, lnLabel, lastNameField);
        HBox.setHgrow(lnGroup, Priority.ALWAYS);

        HBox nameRow = new HBox(12, fnGroup, lnGroup);
        nameRow.getStyleClass().add("auth-field-row");

        Label passLabel = new Label("Mot de passe *");
        passLabel.getStyleClass().add("auth-field-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Minimum 8 caracteres");
        passwordField.getStyleClass().add("auth-password-field");
        StackPane passWrapper = wrapWithToggle(passwordField);
        VBox passGroup = new VBox(5, passLabel, passWrapper);

        Label confirmLabel = new Label("Confirmer le mot de passe *");
        confirmLabel.getStyleClass().add("auth-field-label");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Repetez le mot de passe");
        confirmField.getStyleClass().add("auth-password-field");
        StackPane passWrapper2 = wrapWithToggle(confirmField);
        VBox confirmGroup = new VBox(5, confirmLabel, passWrapper2);

        HBox errorBox = new HBox(8);
        errorBox.getStyleClass().add("auth-error");
        errorBox.setVisible(false);
        errorBox.setManaged(false);
        errorBox.setAlignment(Pos.CENTER_LEFT);
        Label errorIcon = new Label("\u26A0\uFE0F");
        errorIcon.setStyle("-fx-font-size: 14px;");
        Label errorText = new Label();
        errorText.getStyleClass().add("auth-error-text");
        errorText.setWrapText(true);
        HBox.setHgrow(errorText, Priority.ALWAYS);
        errorBox.getChildren().addAll(errorIcon, errorText);

        HBox successBox = new HBox(8);
        successBox.getStyleClass().add("auth-success");
        successBox.setVisible(false);
        successBox.setManaged(false);
        successBox.setAlignment(Pos.CENTER_LEFT);
        Label successIcon = new Label("\u2705");
        successIcon.setStyle("-fx-font-size: 14px;");
        Label successText = new Label();
        successText.getStyleClass().add("auth-success-text");
        successText.setWrapText(true);
        HBox.setHgrow(successText, Priority.ALWAYS);
        successBox.getChildren().addAll(successIcon, successText);

        Button registerBtn = new Button("Creer mon compte");
        registerBtn.getStyleClass().add("auth-btn-primary");
        registerBtn.setOnAction(e -> handleRegister(
                usernameField, emailField, firstNameField, lastNameField,
                passwordField, confirmField, registerBtn,
                errorBox, errorText, successBox, successText
        ));

        HBox orDivider = buildOrDivider();

        HBox loginRow = new HBox(4);
        loginRow.setAlignment(Pos.CENTER);
        Label hasAccount = new Label("Deja un compte ?");
        hasAccount.getStyleClass().add("auth-secondary-text");
        Button loginLink = new Button("Se connecter");
        loginLink.getStyleClass().add("auth-link");
        loginLink.setOnAction(e -> NavigationManager.showLogin());
        loginRow.getChildren().addAll(hasAccount, loginLink);

        HBox passHint = new HBox(8);
        passHint.setAlignment(Pos.CENTER_LEFT);
        passHint.setPadding(new Insets(0, 0, 0, 2));
        Label hintIcon = new Label("\u2139\uFE0F");
        hintIcon.setStyle("-fx-font-size: 11px;");
        Label hintText = new Label("Min. 8 caracteres, doit contenir une majuscule");
        hintText.setStyle("-fx-font-size: 11px; -fx-text-fill: #BDC3C7;");
        passHint.getChildren().addAll(hintIcon, hintText);

        card.getChildren().addAll(
                titleRow, subtitle, div,
                userGroup, emailGroup, nameRow,
                passGroup, passHint, confirmGroup,
                errorBox, successBox,
                registerBtn, orDivider, loginRow
        );

        centerBox.getChildren().addAll(branding, card);
        StackPane scrollContent = new StackPane(centerBox);
        scrollContent.setAlignment(Pos.CENTER);
        scroll.setContent(scrollContent);

        Pane particles = buildParticles();
        root.getChildren().addAll(particles, watermark, scroll, backLink);

        Platform.runLater(() -> {
            branding.setOpacity(0); branding.setTranslateY(-15);
            card.setOpacity(0);    card.setTranslateY(20);
            FadeTransition fb = new FadeTransition(Duration.millis(500), branding); fb.setToValue(1);
            TranslateTransition tb = new TranslateTransition(Duration.millis(500), branding); tb.setToY(0); tb.setInterpolator(Interpolator.EASE_OUT);
            FadeTransition fc = new FadeTransition(Duration.millis(600), card); fc.setToValue(1); fc.setDelay(Duration.millis(150));
            TranslateTransition tc = new TranslateTransition(Duration.millis(600), card); tc.setToY(0); tc.setDelay(Duration.millis(150)); tc.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fb, tb, fc, tc).play();
        });

        return root;
    }

    private void handleRegister(TextField usernameField, TextField emailField,
                                TextField firstNameField, TextField lastNameField,
                                PasswordField passwordField, PasswordField confirmField,
                                Button registerBtn,
                                HBox errorBox, Label errorText,
                                HBox successBox, Label successText) {

        String username  = usernameField.getText().trim();
        String email     = emailField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String password  = passwordField.getText();
        String confirm   = confirmField.getText();

        hideError(errorBox);
        hideError(successBox);

        if (username.isEmpty() || email.isEmpty() || firstName.isEmpty()
                || lastName.isEmpty() || password.isEmpty()) {
            showError(errorBox, errorText, "Tous les champs marques * sont obligatoires.");
            shakeNode(errorBox); return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            showError(errorBox, errorText, "Veuillez entrer une adresse e-mail valide.");
            shakeNode(errorBox); return;
        }
        if (password.length() < 8) {
            showError(errorBox, errorText, "Le mot de passe doit contenir au moins 8 caracteres.");
            shakeNode(errorBox); return;
        }
        boolean hasUpperCase = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) { hasUpperCase = true; break; }
        }
        if (!hasUpperCase) {
            showError(errorBox, errorText, "Le mot de passe doit contenir au moins une majuscule.");
            shakeNode(errorBox); return;
        }
        if (!password.equals(confirm)) {
            showError(errorBox, errorText, "Les mots de passe ne correspondent pas.");
            shakeNode(errorBox); return;
        }

        registerBtn.setDisable(true);
        registerBtn.setText("Creation en cours...");

        new Thread(() -> {
            if (!ServerConnection.getInstance().isConnected())
                ServerConnection.getInstance().connect();

            Response registerResp = authController.register(
                    username, email, password, firstName, lastName, "", "");

            Platform.runLater(() -> {
                registerBtn.setDisable(false);
                registerBtn.setText("Creer mon compte");

                if (!registerResp.isSuccess()) {
                    showError(errorBox, errorText, registerResp.getMessage());
                    shakeNode(errorBox);
                    return;
                }

                // ── Succès → aller à la page de vérification email ──
                Map<String, Object> data = castToMap(registerResp.getData());
                int    userId   = getInt(data, "userId");
                String userEmail = getStr(data, "email");

                successText.setText("Un code a ete envoye a " + userEmail);
                successBox.setVisible(true);
                successBox.setManaged(true);

                // Pause courte puis redirige vers vérification
                PauseTransition pause = new PauseTransition(Duration.millis(800));
                pause.setOnFinished(ev ->
                        NavigationManager.showEmailVerification(userId, userEmail));
                pause.play();
            });
        }).start();
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
                pf.setVisible(true); pf.setManaged(true);
                visibleField.setVisible(false); visibleField.setManaged(false);
                eyeBtn.setText("\uD83D\uDC41");
            } else {
                visibleField.setText(pf.getText());
                visibleField.setVisible(true); visibleField.setManaged(true);
                pf.setVisible(false); pf.setManaged(false);
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
                    Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))));
            logo.setFitHeight(48); logo.setPreserveRatio(true); logo.setSmooth(true);
            branding.getChildren().add(logo);
        } catch (Exception ignored) {}
        HBox nameBox = new HBox(0); nameBox.setAlignment(Pos.CENTER);
        Label l1 = new Label("Chri");
        l1.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);");
        Label l2 = new Label("Online");
        l2.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #D2B4DE;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);");
        nameBox.getChildren().addAll(l1, l2);
        Label tagline = new Label("Creez votre compte en quelques clics");
        tagline.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.55);");
        branding.getChildren().addAll(nameBox, tagline);
        return branding;
    }

    private HBox buildOrDivider() {
        HBox box = new HBox(12); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(2, 0, 2, 0));
        Region line1 = new Region(); line1.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;"); HBox.setHgrow(line1, Priority.ALWAYS);
        Label text = new Label("ou"); text.setStyle("-fx-font-size: 12px; -fx-text-fill: #BDC3C7;");
        Region line2 = new Region(); line2.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;"); HBox.setHgrow(line2, Priority.ALWAYS);
        box.getChildren().addAll(line1, text, line2);
        return box;
    }

    private Pane buildParticles() {
        Pane layer = new Pane(); layer.getStyleClass().add("auth-particles"); layer.setMouseTransparent(true);
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < 12; i++) {
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(rand.nextDouble() * 2 + 1.5);
            dot.setFill(javafx.scene.paint.Color.rgb(255, 255, 255, 0.04 + rand.nextDouble() * 0.06));
            dot.setLayoutX(rand.nextDouble() * 1400); dot.setLayoutY(rand.nextDouble() * 900);
            layer.getChildren().add(dot);
            TranslateTransition tt = new TranslateTransition(Duration.seconds(8 + rand.nextDouble() * 12), dot);
            tt.setByY(-40 - rand.nextDouble() * 60); tt.setByX((rand.nextDouble() - 0.5) * 40);
            tt.setCycleCount(Animation.INDEFINITE); tt.setAutoReverse(true); tt.setInterpolator(Interpolator.EASE_BOTH); tt.play();
        }
        return layer;
    }

    private void showError(HBox box, Label text, String msg) {
        text.setText(msg); box.setVisible(true); box.setManaged(true); box.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(250), box); ft.setToValue(1); ft.play();
    }
    private void hideError(HBox box) { box.setVisible(false); box.setManaged(false); }
    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0); tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0)); tt.play();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        if (obj == null) return Map.of();
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return gson.fromJson(gson.toJson(obj), new TypeToken<Map<String, Object>>(){}.getType());
    }
    private String getStr(Map<String, Object> m, String key) {
        if (m == null) return ""; Object v = m.get(key); return v != null ? v.toString() : "";
    }
    private int getInt(Map<String, Object> m, String key) {
        if (m == null) return -1; Object v = m.get(key);
        if (v == null) return -1;
        if (v instanceof Double) return ((Double) v).intValue();
        return Integer.parseInt(v.toString());
    }
}