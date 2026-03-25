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
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.*;

public class RegisterScreen {

    private final StackPane root;
    private VBox cardContent;

    private TextField     firstNameField, lastNameField;
    private TextField     usernameField, emailField;
    private PasswordField passwordField, confirmField;
    private TextField     phoneField;
    private Button        registerBtn;
    private VBox          errorBox, successBox;
    private Label         errorLabel, successLabel;
    private ProgressIndicator spinner;

    public RegisterScreen() {
        root = new StackPane();
        root.getStyleClass().add("auth-root");

        Pane particles = createParticles();
        particles.setMouseTransparent(true);

        Label watermark = new Label("ChriOnline");
        watermark.getStyleClass().add("auth-brand-watermark");
        watermark.setMouseTransparent(true);

        Button backBtn = new Button("\u2190  Retour à l'accueil");
        backBtn.getStyleClass().add("auth-back-link");
        backBtn.setOnAction(e -> NavigationManager.showLanding());
        StackPane.setAlignment(backBtn, Pos.TOP_LEFT);
        StackPane.setMargin(backBtn, new Insets(24, 0, 0, 24));

        VBox card = buildCard();

        root.getChildren().addAll(particles, watermark, card, backBtn);
        Platform.runLater(this::animateEntry);
    }

    public Node getView() { return root; }

    // ═══════════════════════════════════════════════════════
    //          CARD — compact layout, reduced spacing
    // ═══════════════════════════════════════════════════════

    private VBox buildCard() {
        VBox card = new VBox(0);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(480);
        card.setMaxHeight(640);
        card.setAlignment(Pos.TOP_CENTER);

        cardContent = new VBox(10);                    // reduced from 16 to 10
        cardContent.setPadding(new Insets(28, 36, 24, 36));  // tighter padding
        cardContent.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(cardContent);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("auth-scroll");
        scroll.setMaxHeight(620);

        // ── Logo (smaller) ──────────────────────────────
        StackPane logoBox = new StackPane();
        logoBox.getStyleClass().add("auth-logo-container");
        logoBox.setPrefHeight(45);
        try {
            ImageView logo = new ImageView(new Image(
                    Objects.requireNonNull(
                            getClass().getResourceAsStream("/images/logo.png"))));
            logo.setFitHeight(40);
            logo.setPreserveRatio(true);
            logo.setSmooth(true);
            DropShadow glow = new DropShadow();
            glow.setColor(Color.rgb(108, 52, 131, 0.3));
            glow.setRadius(8);
            logo.setEffect(glow);
            logoBox.getChildren().add(logo);
        } catch (Exception ignored) {}

        // ── Title ───────────────────────────────────────
        Label title = new Label("Créer un compte");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 24px;");        // slightly smaller

        Label subtitle = new Label("Rejoignez ChriOnline en quelques secondes");
        subtitle.getStyleClass().add("auth-subtitle");
        subtitle.setStyle("-fx-font-size: 12px;");

        // ── Error / Success ─────────────────────────────
        errorBox = new VBox();
        errorBox.getStyleClass().add("auth-error");
        errorBox.setVisible(false); errorBox.setManaged(false);
        errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error-text");
        errorBox.getChildren().add(errorLabel);

        successBox = new VBox();
        successBox.getStyleClass().add("auth-success");
        successBox.setVisible(false); successBox.setManaged(false);
        successLabel = new Label();
        successLabel.getStyleClass().add("auth-success-text");
        successBox.getChildren().add(successLabel);

        // ── Row 1: Prénom + Nom ─────────────────────────
        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        VBox fnGroup = compactField("Prénom");
        firstNameField = new TextField();
        firstNameField.setPromptText("Jean");
        firstNameField.getStyleClass().add("auth-field");
        firstNameField.setStyle("-fx-padding: 10 14;");
        fnGroup.getChildren().add(firstNameField);
        HBox.setHgrow(fnGroup, Priority.ALWAYS);

        VBox lnGroup = compactField("Nom");
        lastNameField = new TextField();
        lastNameField.setPromptText("Dupont");
        lastNameField.getStyleClass().add("auth-field");
        lastNameField.setStyle("-fx-padding: 10 14;");
        lnGroup.getChildren().add(lastNameField);
        HBox.setHgrow(lnGroup, Priority.ALWAYS);

        nameRow.getChildren().addAll(fnGroup, lnGroup);

        // ── Row 2: Username + Email ─────────────────────
        HBox userEmailRow = new HBox(12);
        userEmailRow.setAlignment(Pos.CENTER_LEFT);

        VBox userGroup = compactField("Utilisateur *");
        usernameField = new TextField();
        usernameField.setPromptText("jdupont");
        usernameField.getStyleClass().add("auth-field");
        usernameField.setStyle("-fx-padding: 10 14;");
        userGroup.getChildren().add(usernameField);
        HBox.setHgrow(userGroup, Priority.ALWAYS);

        VBox emailGroup = compactField("Email *");
        emailField = new TextField();
        emailField.setPromptText("jean@mail.com");
        emailField.getStyleClass().add("auth-field");
        emailField.setStyle("-fx-padding: 10 14;");
        emailGroup.getChildren().add(emailField);
        HBox.setHgrow(emailGroup, Priority.ALWAYS);

        userEmailRow.getChildren().addAll(userGroup, emailGroup);

        // ── Row 3: Password + Confirm ───────────────────
        HBox passRow = new HBox(12);
        passRow.setAlignment(Pos.CENTER_LEFT);

        VBox passGroup = compactField("Mot de passe *");
        passwordField = new PasswordField();
        passwordField.setPromptText("Min. 6 car.");
        passwordField.getStyleClass().add("auth-password-field");
        passwordField.setStyle("-fx-padding: 10 14;");
        passGroup.getChildren().add(passwordField);
        HBox.setHgrow(passGroup, Priority.ALWAYS);

        VBox confirmGroup = compactField("Confirmer *");
        confirmField = new PasswordField();
        confirmField.setPromptText("Confirmer");
        confirmField.getStyleClass().add("auth-password-field");
        confirmField.setStyle("-fx-padding: 10 14;");
        confirmGroup.getChildren().add(confirmField);
        HBox.setHgrow(confirmGroup, Priority.ALWAYS);

        passRow.getChildren().addAll(passGroup, confirmGroup);

        // ── Phone (single field) ────────────────────────
        VBox phoneGroup = compactField("Téléphone");
        phoneField = new TextField();
        phoneField.setPromptText("+33 6 12 34 56 78");
        phoneField.getStyleClass().add("auth-field");
        phoneField.setStyle("-fx-padding: 10 14;");
        phoneGroup.getChildren().add(phoneField);

        // ── Register button ─────────────────────────────
        registerBtn = new Button("S'inscrire");
        registerBtn.getStyleClass().add("auth-btn-primary");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> doRegister());

        spinner = new ProgressIndicator();
        spinner.getStyleClass().add("auth-spinner");
        spinner.setMaxSize(20, 20);
        spinner.setVisible(false);

        // ── Divider ─────────────────────────────────────
        HBox divider = createDivider("ou");

        // ── Login link ──────────────────────────────────
        HBox loginRow = new HBox(6);
        loginRow.setAlignment(Pos.CENTER);
        Label hasAccount = new Label("Déjà un compte ?");
        hasAccount.getStyleClass().add("auth-secondary-text");
        Button loginLink = new Button("Se connecter");
        loginLink.getStyleClass().add("auth-link");
        loginLink.setOnAction(e -> NavigationManager.showLogin());
        loginRow.getChildren().addAll(hasAccount, loginLink);

        // ── Assemble ────────────────────────────────────
        cardContent.getChildren().addAll(
                logoBox, title, subtitle,
                errorBox, successBox,
                nameRow,
                userEmailRow,
                passRow,
                phoneGroup,
                registerBtn,
                divider, loginRow
        );

        card.getChildren().add(scroll);
        return card;
    }

    /** Compact field group with reduced spacing */
    private VBox compactField(String label) {
        VBox group = new VBox(4);
        Label l = new Label(label);
        l.getStyleClass().add("auth-field-label");
        l.setStyle("-fx-font-size: 12px;");
        group.getChildren().add(l);
        return group;
    }

    // ═══════════════════════════════════════════════════════
    //                   DO REGISTER
    // ═══════════════════════════════════════════════════════

    private void doRegister() {
        hideMessages();

        String fn       = firstNameField.getText().trim();
        String ln       = lastNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmField.getText();
        String phone    = phoneField.getText().trim();

        if (username.isEmpty()) {
            showError("Le nom d'utilisateur est obligatoire.");
            shakeField(usernameField); return;
        }
        if (username.length() < 3) {
            showError("Min. 3 caractères pour l'utilisateur.");
            shakeField(usernameField); return;
        }
        if (email.isEmpty()) {
            showError("L'email est obligatoire.");
            shakeField(emailField); return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            showError("Format d'email invalide.");
            shakeField(emailField); return;
        }
        if (password.isEmpty()) {
            showError("Le mot de passe est obligatoire.");
            shakeField(passwordField); return;
        }
        if (password.length() < 6) {
            showError("Min. 6 caractères pour le mot de passe.");
            shakeField(passwordField); return;
        }
        if (!password.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas.");
            shakeField(confirmField); return;
        }

        setLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("email", email);
        data.put("password", password);
        data.put("firstName", fn);
        data.put("lastName", ln);
        data.put("address", "");
        data.put("phone", phone);

        ServerConnection conn = ServerConnection.getInstance();
        if (!conn.isConnected()) {
            if (!conn.connect()) {
                setLoading(false);
                showError("Impossible de se connecter au serveur.");
                return;
            }
        }

        conn.sendAsync(new Request(Action.REGISTER, null, data))
                .thenAccept(res -> Platform.runLater(() -> {
                    setLoading(false);
                    if (res.isSuccess()) {
                        showSuccess("Inscription réussie ! Redirection...");
                        setFormDisabled(true);
                        PauseTransition pause = new PauseTransition(Duration.seconds(2));
                        pause.setOnFinished(e -> NavigationManager.showLogin());
                        pause.play();
                    } else {
                        showError(res.getMessage() != null
                                ? res.getMessage() : "Erreur lors de l'inscription.");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoading(false);
                        showError("Erreur réseau : " + ex.getMessage());
                    });
                    return null;
                });
    }

    // ═══════════════════════════════════════════════════════
    //                    UI HELPERS
    // ═══════════════════════════════════════════════════════

    private HBox createDivider(String text) {
        Region left = new Region();
        left.getStyleClass().add("auth-divider-line");
        HBox.setHgrow(left, Priority.ALWAYS);
        Label txt = new Label(text);
        txt.getStyleClass().add("auth-divider-text");
        Region right = new Region();
        right.getStyleClass().add("auth-divider-line");
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox div = new HBox(0, left, txt, right);
        div.setAlignment(Pos.CENTER);
        div.setPadding(new Insets(2, 0, 2, 0));
        return div;
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorBox.setVisible(true); errorBox.setManaged(true);
        errorBox.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), errorBox);
        ft.setToValue(1); ft.play();
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successBox.setVisible(true); successBox.setManaged(true);
        successBox.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), successBox);
        ft.setToValue(1); ft.play();
    }

    private void hideMessages() {
        errorBox.setVisible(false); errorBox.setManaged(false);
        successBox.setVisible(false); successBox.setManaged(false);
    }

    private void setLoading(boolean loading) {
        registerBtn.setDisable(loading);
        if (loading) {
            registerBtn.setText("");
            registerBtn.setGraphic(spinner);
            spinner.setVisible(true);
        } else {
            registerBtn.setText("S'inscrire");
            registerBtn.setGraphic(null);
            spinner.setVisible(false);
        }
    }

    private void setFormDisabled(boolean d) {
        firstNameField.setDisable(d);
        lastNameField.setDisable(d);
        usernameField.setDisable(d);
        emailField.setDisable(d);
        passwordField.setDisable(d);
        confirmField.setDisable(d);
        phoneField.setDisable(d);
        registerBtn.setDisable(d);
    }

    private void shakeField(Node field) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), field);
        shake.setFromX(0); shake.setByX(10);
        shake.setCycleCount(6); shake.setAutoReverse(true);
        shake.setOnFinished(e -> field.setTranslateX(0));
        shake.play();
    }

    // ═══════════════════════════════════════════════════════
    //                   PARTICLES
    // ═══════════════════════════════════════════════════════

    private Pane createParticles() {
        Pane pane = new Pane();
        pane.getStyleClass().add("auth-particles");
        for (int i = 0; i < 25; i++) {
            double r = Math.random() * 3 + 1;
            double a = Math.random() * 0.1 + 0.03;
            Circle c = new Circle(r, Color.rgb(255, 255, 255, a));
            c.setLayoutX(Math.random() * 1400);
            c.setLayoutY(Math.random() * 900);
            pane.getChildren().add(c);
            driftParticle(c);
        }
        return pane;
    }

    private void driftParticle(Circle c) {
        double dur = 5000 + Math.random() * 7000;
        TranslateTransition move = new TranslateTransition(Duration.millis(dur), c);
        move.setByY(-(80 + Math.random() * 200));
        move.setByX((Math.random() - 0.5) * 60);
        FadeTransition fade = new FadeTransition(Duration.millis(dur), c);
        fade.setToValue(0);
        ParallelTransition anim = new ParallelTransition(move, fade);
        anim.setOnFinished(e -> {
            c.setTranslateX(0); c.setTranslateY(0);
            c.setLayoutX(Math.random() * 1400);
            c.setLayoutY(700 + Math.random() * 200);
            c.setOpacity(Math.random() * 0.1 + 0.03);
            driftParticle(c);
        });
        anim.play();
    }

    // ═══════════════════════════════════════════════════════
    //                 ENTRY ANIMATION
    // ═══════════════════════════════════════════════════════

    private void animateEntry() {
        Node card = cardContent.getParent().getParent(); // scroll → card
        card.setOpacity(0);
        card.setTranslateY(50);

        FadeTransition cardFade = new FadeTransition(Duration.millis(600), card);
        cardFade.setToValue(1);
        cardFade.setDelay(Duration.millis(100));

        TranslateTransition cardSlide = new TranslateTransition(Duration.millis(600), card);
        cardSlide.setToY(0);
        cardSlide.setDelay(Duration.millis(100));
        cardSlide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

        new ParallelTransition(cardFade, cardSlide).play();

        for (int i = 0; i < cardContent.getChildren().size(); i++) {
            Node n = cardContent.getChildren().get(i);
            n.setOpacity(0);
            n.setTranslateY(12);
            FadeTransition f = new FadeTransition(Duration.millis(350), n);
            f.setToValue(1);
            f.setDelay(Duration.millis(250 + i * 40));
            TranslateTransition s = new TranslateTransition(Duration.millis(350), n);
            s.setToY(0);
            s.setDelay(Duration.millis(250 + i * 40));
            s.setInterpolator(Interpolator.EASE_OUT);
            f.play(); s.play();
        }

        Platform.runLater(() -> firstNameField.requestFocus());
    }
}