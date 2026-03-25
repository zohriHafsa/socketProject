package com.chrionline.client.ui;

import com.chrionline.client.network.ServerConnection;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class SplashScreen {

    private final StackPane   root;
    private final ProgressBar progressBar;
    private final Label       statusLabel;
    private final VBox        contentBox;
    private final Pane        particleLayer = new Pane();

    /** Flag set when connection attempt finishes (success or fail). */
    private final AtomicBoolean connectionDone = new AtomicBoolean(false);

    public SplashScreen() {
        root = new StackPane();
        root.getStyleClass().add("splash-root");

        particleLayer.setMouseTransparent(true);
        createParticles();

        contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(40));
        contentBox.setMaxWidth(460);

        ImageView logo = buildLogo();

        Label title = new Label("ChriOnline");
        title.getStyleClass().add("splash-title");

        Label subtitle = new Label("Votre boutique en ligne");
        subtitle.getStyleClass().add("splash-subtitle");

        HBox separator = buildSeparator();

        Region spacer = new Region();
        spacer.setPrefHeight(10);

        progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("splash-progress");
        progressBar.setPrefWidth(320);
        progressBar.setPrefHeight(6);

        statusLabel = new Label("Initialisation...");
        statusLabel.getStyleClass().add("splash-status");

        Label version = new Label("Version 1.0.0");
        version.getStyleClass().add("splash-version");

        contentBox.getChildren().addAll(logo, title, subtitle, separator,
                spacer, progressBar, statusLabel, version);

        root.getChildren().addAll(particleLayer, contentBox);
        Platform.runLater(this::playSequence);
    }

    public Node getView() { return root; }

    // ── Logo ────────────────────────────────────────────
    private ImageView buildLogo() {
        ImageView logo;
        try {
            Image img = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/images/logo.png")));
            logo = new ImageView(img);
            logo.setFitWidth(90); logo.setFitHeight(90);
            logo.setPreserveRatio(true); logo.setSmooth(true);

            DropShadow glow = new DropShadow();
            glow.setColor(Color.rgb(142, 68, 173, 0.5));
            glow.setRadius(18); glow.setSpread(0.15);
            logo.setEffect(glow);

            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 18)),
                    new KeyFrame(Duration.seconds(1.5),
                            new KeyValue(glow.radiusProperty(), 28, Interpolator.EASE_BOTH)));
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
        } catch (Exception e) {
            logo = new ImageView();
            logo.setFitWidth(90); logo.setFitHeight(90);
        }
        return logo;
    }

    // ── Separator ───────────────────────────────────────
    private HBox buildSeparator() {
        Region left  = new Region(); left.setPrefSize(60, 2);
        left.setStyle("-fx-background-color:rgba(255,255,255,0.2);-fx-background-radius:1;");
        Region dot   = new Region(); dot.setPrefSize(8, 8);
        dot.setStyle("-fx-background-color:#F39C12;-fx-background-radius:4;");
        Region right = new Region(); right.setPrefSize(60, 2);
        right.setStyle("-fx-background-color:rgba(255,255,255,0.2);-fx-background-radius:1;");
        HBox sep = new HBox(12, left, dot, right);
        sep.setAlignment(Pos.CENTER);
        return sep;
    }

    // ── Particles ───────────────────────────────────────
    private void createParticles() {
        for (int i = 0; i < 35; i++) {
            double r = Math.random() * 3 + 1;
            double a = Math.random() * 0.12 + 0.03;
            Circle c = new Circle(r, Color.rgb(255, 255, 255, a));
            c.setLayoutX(Math.random() * 1400);
            c.setLayoutY(Math.random() * 900);
            particleLayer.getChildren().add(c);
            driftParticle(c);
        }
    }

    private void driftParticle(Circle c) {
        double dur = 4000 + Math.random() * 7000;
        TranslateTransition move = new TranslateTransition(Duration.millis(dur), c);
        move.setByY(-(100 + Math.random() * 200));
        move.setByX((Math.random() - 0.5) * 80);
        FadeTransition fade = new FadeTransition(Duration.millis(dur), c);
        fade.setToValue(0);
        ParallelTransition anim = new ParallelTransition(move, fade);
        anim.setOnFinished(e -> {
            c.setTranslateX(0); c.setTranslateY(0);
            c.setLayoutX(Math.random() * 1400);
            c.setLayoutY(650 + Math.random() * 250);
            c.setOpacity(Math.random() * 0.12 + 0.03);
            driftParticle(c);
        });
        anim.play();
    }

    // ── Sequence ────────────────────────────────────────
    private void playSequence() {
        // Staggered entry
        for (int i = 0; i < contentBox.getChildren().size(); i++) {
            Node node = contentBox.getChildren().get(i);
            node.setOpacity(0); node.setTranslateY(25);

            FadeTransition f = new FadeTransition(Duration.millis(500), node);
            f.setToValue(1); f.setDelay(Duration.millis(180 + i * 110));

            TranslateTransition s = new TranslateTransition(Duration.millis(500), node);
            s.setToY(0); s.setDelay(Duration.millis(180 + i * 110));
            s.setInterpolator(Interpolator.EASE_OUT);

            f.play(); s.play();
        }

        String[] steps = {
                "Initialisation de l'interface...",
                "Chargement des ressources...",
                "Connexion au serveur...",
                "Vérification des services...",
                "Préparation de la boutique...",
                "Presque prêt..."
        };

        Timeline tl = new Timeline();
        for (int i = 0; i < steps.length; i++) {
            final int idx = i;
            double prog = (double) (i + 1) / steps.length;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(1100 + i * 550), e -> {
                statusLabel.setText(steps[idx]);
                Timeline bar = new Timeline(new KeyFrame(Duration.millis(350),
                        new KeyValue(progressBar.progressProperty(), prog, Interpolator.EASE_BOTH)));
                bar.play();
            }));
        }

        // Connect at step 3
        tl.getKeyFrames().add(new KeyFrame(Duration.millis(2200), e -> connectAsync()));

        // Transition: wait for connection + minimum time
        double minFinish = 1100 + steps.length * 550 + 400;
        tl.getKeyFrames().add(new KeyFrame(Duration.millis(minFinish), e -> {
            // If connection still in progress, wait a bit more
            if (!connectionDone.get()) {
                statusLabel.setText("Connexion en cours...");
                waitForConnection();
            } else {
                goToLanding();
            }
        }));

        tl.play();
    }

    private void connectAsync() {
        Thread t = new Thread(() -> {
            boolean ok = ServerConnection.getInstance().connect();
            connectionDone.set(true);
            Platform.runLater(() -> statusLabel.setText(
                    ok ? "Connecté au serveur" : "Mode hors-ligne"));
        }, "SplashConnect");
        t.setDaemon(true);
        t.start();
    }

    /** Poll every 200ms until connection attempt finishes, then navigate. */
    private void waitForConnection() {
        Timeline poll = new Timeline(new KeyFrame(Duration.millis(200), e -> {}));
        poll.setCycleCount(25); // max 5 seconds
        poll.setOnFinished(e -> goToLanding());

        // Check on each cycle
        poll.currentTimeProperty().addListener((obs, ov, nv) -> {
            if (connectionDone.get()) {
                poll.stop();
                goToLanding();
            }
        });
        poll.play();
    }

    private void goToLanding() {
        Platform.runLater(() -> {
            statusLabel.setText("Bienvenue !");
            // Small pause for the "Bienvenue" to be visible
            PauseTransition pause = new PauseTransition(Duration.millis(300));
            pause.setOnFinished(e -> NavigationManager.showLanding());
            pause.play();
        });
    }
}