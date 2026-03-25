package com.chrionline.client.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoginScreen {

    private final StackPane root;
    private VBox cardContent;
    private TextField        usernameField;
    private PasswordField    passwordField;
    private Button           loginBtn;
    private VBox             errorBox;
    private Label            errorLabel;
    private ProgressIndicator spinner;

    public LoginScreen() {
        root = new StackPane();
        root.getStyleClass().add("auth-root");

        // ── Background decorations ──────────────────────
        Pane particles = createParticles();
        particles.setMouseTransparent(true);

        Label watermark = new Label("ChriOnline");
        watermark.getStyleClass().add("auth-brand-watermark");
        watermark.setMouseTransparent(true);

        // ── Back link (top-left) ────────────────────────
        Button backBtn = new Button("\u2190  Retour à l'accueil");
        backBtn.getStyleClass().add("auth-back-link");
        backBtn.setOnAction(e -> NavigationManager.showLanding());
        StackPane.setAlignment(backBtn, Pos.TOP_LEFT);
        StackPane.setMargin(backBtn, new Insets(24, 0, 0, 24));

        // ── Card ────────────────────────────────────────
        VBox card = buildCard();

        root.getChildren().addAll(particles, watermark, card, backBtn);

        Platform.runLater(this::animateEntry);
    }

    public Node getView() { return root; }

    // ═══════════════════════════════════════════════════════
    //                      CARD
    // ═══════════════════════════════════════════════════════

    private VBox buildCard() {
        VBox card = new VBox(0);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(420);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.TOP_CENTER);

        cardContent = new VBox(20);
        cardContent.setPadding(new Insets(40, 40, 36, 40));
        cardContent.setAlignment(Pos.TOP_CENTER);

        // ── Logo ────────────────────────────────────────
        StackPane logoContainer = new StackPane();
        logoContainer.getStyleClass().add("auth-logo-container");
        logoContainer.setPrefHeight(70);
        try {
            ImageView logo = new ImageView(new Image(
                    Objects.requireNonNull(
                            getClass().getResourceAsStream("/images/logo.png"))));
            logo.setFitHeight(56);
            logo.setPreserveRatio(true);
            logo.setSmooth(true);
            DropShadow glow = new DropShadow();
            glow.setColor(Color.rgb(108, 52, 131, 0.3));
            glow.setRadius(12);
            logo.setEffect(glow);
            logoContainer.getChildren().add(logo);
        } catch (Exception ignored) {}

        // ── Title ───────────────────────────────────────
        Label title = new Label("Connexion");
        title.getStyleClass().add("auth-title");

        Label subtitle = new Label("Accédez à votre compte ChriOnline");
        subtitle.getStyleClass().add("auth-subtitle");

        // ── Error box (hidden by default) ───────────────
        errorBox = new VBox();
        errorBox.getStyleClass().add("auth-error");
        errorBox.setVisible(false);
        errorBox.setManaged(false);
        errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error-text");
        errorBox.getChildren().add(errorLabel);

        // ── Username field ──────────────────────────────
        VBox usernameGroup = fieldGroup("Nom d'utilisateur");
        usernameField = new TextField();
        usernameField.setPromptText("Entrez votre nom d'utilisateur");
        usernameField.getStyleClass().add("auth-field");
        usernameField.setOnAction(e -> passwordField.requestFocus());
        usernameGroup.getChildren().add(usernameField);

        // ── Password field ──────────────────────────────
        VBox passwordGroup = fieldGroup("Mot de passe");
        passwordField = new PasswordField();
        passwordField.setPromptText("Entrez votre mot de passe");
        passwordField.getStyleClass().add("auth-password-field");
        passwordField.setOnAction(e -> doLogin());
        passwordGroup.getChildren().add(passwordField);

        // ── Login button ────────────────────────────────
        loginBtn = new Button("Se connecter");
        loginBtn.getStyleClass().add("auth-btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> doLogin());

        spinner = new ProgressIndicator();
        spinner.getStyleClass().add("auth-spinner");
        spinner.setMaxSize(20, 20);
        spinner.setVisible(false);

        // ── Divider ─────────────────────────────────────
        HBox divider = createDivider("ou");

        // ── Register link ───────────────────────────────
        HBox registerRow = new HBox(6);
        registerRow.setAlignment(Pos.CENTER);
        Label noAccount = new Label("Pas encore de compte ?");
        noAccount.getStyleClass().add("auth-secondary-text");
        Button registerLink = new Button("S'inscrire");
        registerLink.getStyleClass().add("auth-link");
        registerLink.setOnAction(e -> NavigationManager.showRegister());
        registerRow.getChildren().addAll(noAccount, registerLink);

        // ── Assemble ────────────────────────────────────
        cardContent.getChildren().addAll(
                logoContainer, title, subtitle,
                errorBox,
                usernameGroup, passwordGroup,
                loginBtn,
                divider,
                registerRow
        );

        card.getChildren().add(cardContent);
        return card;
    }

    // ═══════════════════════════════════════════════════════
    //                    DO LOGIN
    // ═══════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void doLogin() {
        hideError();
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty()) {
            showError("Veuillez entrer votre nom d'utilisateur.");
            shakeField(usernameField);
            return;
        }
        if (pass.isEmpty()) {
            showError("Veuillez entrer votre mot de passe.");
            shakeField(passwordField);
            return;
        }

        setLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("username", user);
        data.put("password", pass);

        ServerConnection conn = ServerConnection.getInstance();
        if (!conn.isConnected()) {
            if (!conn.connect()) {
                setLoading(false);
                showError("Impossible de se connecter au serveur.");
                return;
            }
        }

        conn.sendAsync(new Request(Action.LOGIN, null, data))
                .thenAccept(res -> Platform.runLater(() -> {
                    setLoading(false);
                    if (res.isSuccess() && res.getData() != null) {
                        Map<String, Object> rd = (Map<String, Object>) res.getData();
                        String token = str(rd.get("token"));
                        int userId   = intV(rd.get("userId"));
                        String uname = str(rd.get("username"));
                        String role  = str(rd.get("role"));
                        String fn    = str(rd.get("firstName"));
                        String ln    = str(rd.get("lastName"));

                        NavigationManager.setSession(
                                token, userId, uname, role, fn, ln);

                        // Register UDP for notifications
                        registerUdp(token, userId);

                        // Success animation then navigate
                        successAndNavigate();
                    } else {
                        showError(res.getMessage() != null
                                ? res.getMessage() : "Identifiants incorrects.");
                        shakeField(passwordField);
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

    private void registerUdp(String token, int userId) {
        try {
            com.chrionline.client.network.UDPListener listener =
                    new com.chrionline.client.network.UDPListener(0);
            Thread udpThread = new Thread(listener);
            udpThread.setDaemon(true);
            udpThread.start();

            // Small delay to let socket bind
            Platform.runLater(() -> {
                int port = listener.getPort();
                Map<String, Object> udpData = new HashMap<>();
                udpData.put("userId", userId);
                udpData.put("udpPort", port);
                ServerConnection.getInstance()
                        .sendAsync(new Request(Action.REGISTER_UDP, token, udpData));
            });
        } catch (Exception e) {
            System.err.println("[UDP] Registration failed: " + e.getMessage());
        }
    }

    private void successAndNavigate() {
        VBox card = (VBox) cardContent.getParent();
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; "
                + "-fx-border-color: #27AE60; -fx-border-radius: 20; -fx-border-width: 2;");

        ScaleTransition pulse = new ScaleTransition(Duration.millis(200), card);
        pulse.setToX(1.02); pulse.setToY(1.02);
        ScaleTransition settle = new ScaleTransition(Duration.millis(200), card);
        settle.setToX(1); settle.setToY(1);

        PauseTransition pause = new PauseTransition(Duration.millis(400));
        pause.setOnFinished(e -> {
            if (NavigationManager.isAdmin()) {
                NavigationManager.showAdminDashboard();
            } else {
                NavigationManager.showLanding();
            }
        });

        new SequentialTransition(pulse, settle, pause).play();
    }

    // ═══════════════════════════════════════════════════════
    //                    UI HELPERS
    // ═══════════════════════════════════════════════════════

    private VBox fieldGroup(String label) {
        VBox group = new VBox(6);
        Label l = new Label(label);
        l.getStyleClass().add("auth-field-label");
        group.getChildren().add(l);
        return group;
    }

    private HBox createDivider(String text) {
        Region left = new Region();
        left.getStyleClass().add("auth-divider-line");
        HBox.setHgrow(left, Priority.ALWAYS);

        Label txt = new Label(text);
        txt.getStyleClass().add("auth-divider-text");

        Region right = new Region();
        right.getStyleClass().add("auth-divider-line");
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox divider = new HBox(0, left, txt, right);
        divider.setAlignment(Pos.CENTER);
        divider.setPadding(new Insets(4, 0, 4, 0));
        return divider;
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorBox.setVisible(true);
        errorBox.setManaged(true);
        errorBox.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), errorBox);
        ft.setToValue(1); ft.play();
    }

    private void hideError() {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
    }

    private void setLoading(boolean loading) {
        loginBtn.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        if (loading) {
            loginBtn.setText("");
            loginBtn.setGraphic(spinner);
            spinner.setVisible(true);
        } else {
            loginBtn.setText("Se connecter");
            loginBtn.setGraphic(null);
            spinner.setVisible(false);
        }
    }

    private void shakeField(Node field) {
        TranslateTransition shake = new TranslateTransition(
                Duration.millis(60), field);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
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
        // Card slide up + fade in
        VBox card = (VBox) cardContent.getParent();
        card.setOpacity(0);
        card.setTranslateY(50);

        FadeTransition cardFade = new FadeTransition(Duration.millis(600), card);
        cardFade.setToValue(1);
        cardFade.setDelay(Duration.millis(100));

        TranslateTransition cardSlide = new TranslateTransition(
                Duration.millis(600), card);
        cardSlide.setToY(0);
        cardSlide.setDelay(Duration.millis(100));
        cardSlide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

        new ParallelTransition(cardFade, cardSlide).play();

        // Staggered content
        for (int i = 0; i < cardContent.getChildren().size(); i++) {
            Node n = cardContent.getChildren().get(i);
            n.setOpacity(0);
            n.setTranslateY(15);

            FadeTransition f = new FadeTransition(Duration.millis(400), n);
            f.setToValue(1);
            f.setDelay(Duration.millis(300 + i * 60));

            TranslateTransition s = new TranslateTransition(Duration.millis(400), n);
            s.setToY(0);
            s.setDelay(Duration.millis(300 + i * 60));
            s.setInterpolator(Interpolator.EASE_OUT);

            f.play(); s.play();
        }

        // Auto-focus username
        Platform.runLater(() -> usernameField.requestFocus());
    }

    // ═══════════════════════════════════════════════════════
    private String str(Object o) { return o != null ? o.toString() : ""; }
    private int intV(Object o) {
        if (o == null) return 0;
        if (o instanceof Double) return ((Double) o).intValue();
        if (o instanceof Integer) return (Integer) o;
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }
}