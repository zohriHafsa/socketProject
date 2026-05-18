package com.chrionline.admin.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.client.network.UDPHeartbeatSender;
import com.chrionline.admin.ui.AdminNavigationManager;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import com.chrionline.shared.security.ECDSAUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Map;

public class AdminLoginScreen {

    private final StackPane root;
    private final VBox centerBox;
    private final Gson gson = new Gson();

    // State Variables
    private String currentUsername = null;
    private String activeChallenge = null;
    private File selectedKeyFile = null;
    private boolean isImportMode = false;

    public AdminLoginScreen() {
        root = new StackPane();
        root.getStyleClass().add("auth-root");

        // Background brand
        Label watermark = new Label("ChriOnline");
        watermark.getStyleClass().add("auth-brand-watermark");
        watermark.setMouseTransparent(true);
        StackPane.setAlignment(watermark, Pos.CENTER);

        // No back link needed here

        centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPickOnBounds(false); // allow clicks to pass through if empty

        showInitPhase();

        Pane particles = buildParticles();

        // Ensure centerBox and particles are added
        root.getChildren().addAll(particles, watermark, centerBox);
    }

    public Node getView() {
        return root;
    }

    private void showInitPhase() {
        root.getChildren().removeIf(node -> node instanceof Button && node.getStyleClass().contains("auth-back-link"));
        centerBox.getChildren().clear();
        VBox branding = buildBranding();

        VBox card = createCard("\uD83D\uDD12", "Connexion Admin", "Veuillez entrer votre identifiant");
        card.setMaxWidth(400);

        Label userLabel = new Label("Nom d'utilisateur");
        userLabel.getStyleClass().add("auth-field-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("ex: admin");
        usernameField.getStyleClass().add("auth-field");
        VBox userGroup = new VBox(5, userLabel, usernameField);

        HBox errorBox = createErrorBox();
        Label errorText = (Label) errorBox.getChildren().get(1);

        Button continueBtn = new Button("Continuer");
        continueBtn.getStyleClass().add("auth-btn-primary");
        continueBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(continueBtn, new Insets(10, 0, 0, 0));

        continueBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            if (user.isEmpty()) {
                showError(errorBox, errorText, "Nom d'utilisateur requis.");
                return;
            }
            hideError(errorBox);
            continueBtn.setDisable(true);
            continueBtn.setText("Connexion en cours...");

            new Thread(() -> {
                if (!ServerConnection.getInstance().isConnected()) {
                    boolean ok = ServerConnection.getInstance().connect();
                    if (!ok) {
                        Platform.runLater(() -> {
                            showError(errorBox, errorText,
                                    "Serveur inaccessible. Verifiez qu'il est demarre sur le port 9090.");
                            continueBtn.setDisable(false);
                            continueBtn.setText("Continuer");
                        });
                        return;
                    }
                }

                ServerConnection.getInstance().sendAsync(
                        new Request(Action.ADMIN_AUTH_INIT, null, Map.of("username", user)))
                        .thenAccept(resp -> Platform.runLater(() -> {
                            continueBtn.setDisable(false);
                            continueBtn.setText("Continuer");

                            if (resp.isSuccess()) {
                                Map<String, Object> data = castToMap(resp.getData());
                                String status = getStr(data, "status");

                                if ("PENDING".equals(status)) {
                                    // Needs initialization
                                    AdminNavigationManager.showAdminInit(user);
                                } else if ("ACTIVE".equals(status)) {
                                    currentUsername = user;
                                    activeChallenge = getStr(data, "challenge");

                                    File defaultKey = new File(System.getProperty("user.home")
                                            + "/.chrionline/keystores/" + user + ".p12");
                                    if (defaultKey.exists()) {
                                        selectedKeyFile = defaultKey;
                                        isImportMode = false;
                                    } else {
                                        selectedKeyFile = null;
                                        isImportMode = false;
                                    }

                                    showChallengePhase();
                                }
                            } else {
                                showError(errorBox, errorText, resp.getMessage());
                            }
                        }));
            }).start();
        });

        usernameField.setOnAction(continueBtn.getOnAction()); // Support Enter key

        card.getChildren().addAll(
                userGroup,
                errorBox,
                continueBtn);

        centerBox.getChildren().addAll(branding, card);
        animateCard(branding);
        animateCard(card);
    }

    private void showChallengePhase() {
        centerBox.getChildren().clear();
        VBox branding = buildBranding();

        // Remove any existing backLink from root first
        root.getChildren().removeIf(node -> node instanceof Button && node.getStyleClass().contains("auth-back-link"));

        // Add back button in the top left
        Button backLink = new Button("\u2190 Retour a la connexion");
        backLink.getStyleClass().add("auth-back-link");
        backLink.setOnAction(evt -> {
            root.getChildren().remove(backLink);
            showInitPhase();
        });
        StackPane.setAlignment(backLink, Pos.TOP_LEFT);
        StackPane.setMargin(backLink, new Insets(24, 0, 0, 24));
        root.getChildren().add(backLink);

        if (selectedKeyFile == null || !selectedKeyFile.exists()) {
            VBox card = createCard("\uD83D\uDD0D", "Identifiants Introuvables", "Sécurité de l'appareil"); // 🔍 Icon
                                                                                                           // instead of
                                                                                                           // ⚠️
            card.setMaxWidth(500);

            Label errorTitle = new Label("No credentials found for this admin on this device.");
            errorTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 14px;"); // Premium
                                                                                                        // neutral Slate
                                                                                                        // instead of
                                                                                                        // Red
            errorTitle.setWrapText(true);
            errorTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            Label errorDesc = new Label(
                    "Aucune clé de sécurité locale n'est associée à cet appareil pour l'administrateur \""
                            + currentUsername
                            + "\". Veuillez importer vos identifiants existants (.p12) pour configurer cet appareil.");
            errorDesc.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
            errorDesc.setWrapText(true);
            errorDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            Button importBtn = new Button("Importer des identifiants (.p12)");
            importBtn.getStyleClass().add("auth-btn-primary");
            importBtn.setMaxWidth(Double.MAX_VALUE);
            VBox.setMargin(importBtn, new Insets(15, 0, 0, 0));

            importBtn.setOnAction(evt -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Importer les identifiants existants");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PKCS12 KeyStore", "*.p12"));
                Stage stage = (Stage) root.getScene().getWindow();
                File file = fc.showOpenDialog(stage);
                if (file != null) {
                    selectedKeyFile = file;
                    isImportMode = true;
                    showChallengePhase();
                }
            });

            card.getChildren().addAll(
                    errorTitle,
                    errorDesc,
                    importBtn);

            centerBox.getChildren().addAll(branding, card);
            animateCard(branding);
            animateCard(card);
            return;
        }

        VBox card = createCard("\uD83D\uDD11", "Authentification RSA",  "Bonjour " + currentUsername + " - déverrouillez vos identifiants.");
        card.setMaxWidth(500);

        VBox fileGroup = new VBox(5);
        Hyperlink forceImportLink = null;

        if (isImportMode) {
            Label importLabel = new Label("📂 Identifiants importés prêts à être enregistrés");
            importLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 12px;");
            Label pathLabel = new Label(selectedKeyFile.getName());
            pathLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-font-family: monospace;");

            Hyperlink cancelLink = new Hyperlink("Annuler l'importation");
            cancelLink.setStyle(
                    "-fx-text-fill: #c0392b; " +
                            "-fx-underline: true; " +
                            "-fx-font-size: 11px; " +
                            "-fx-cursor: hand; " +
                            "-fx-background-color: transparent; " +
                            "-fx-border-color: transparent; " +
                            "-fx-border-width: 0; " +
                            "-fx-padding: 0;");
            cancelLink.setOnAction(evt -> {
                selectedKeyFile = null;
                isImportMode = false;
                showChallengePhase();
            });

            fileGroup.getChildren().addAll(importLabel, pathLabel, cancelLink);
        } else {
            forceImportLink = new Hyperlink("Importer une nouvelle clé / Remplacer (.p12)...");
            forceImportLink.setStyle(
                    "-fx-text-fill: #7f8c8d; " +
                            "-fx-underline: true; " +
                            "-fx-font-size: 11px; " +
                            "-fx-cursor: hand; " +
                            "-fx-background-color: transparent; " +
                            "-fx-border-color: transparent; " +
                            "-fx-border-width: 0; " +
                            "-fx-padding: 0;");
            forceImportLink.setOnAction(evt -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Remplacer par un autre fichier de clé");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PKCS12 KeyStore", "*.p12"));
                Stage stage = (Stage) root.getScene().getWindow();
                File file = fc.showOpenDialog(stage);
                if (file != null) {
                    selectedKeyFile = file;
                    isImportMode = true;
                    showChallengePhase();
                }
            });
        }
        VBox.setMargin(fileGroup, new Insets(5, 0, 10, 0));

        Label passLabel = new Label("Passphrase de déchiffrement");
        passLabel.getStyleClass().add("auth-field-label");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Entrez la passphrase de votre clé");
        passField.getStyleClass().add("auth-password-field");
        StackPane passWrapper = wrapWithToggle(passField);

        VBox passGroup;
        if (!isImportMode && forceImportLink != null) {
            HBox linkRow = new HBox(forceImportLink);
            linkRow.setAlignment(Pos.CENTER_RIGHT);
            VBox.setMargin(linkRow, new Insets(4, 0, 0, 0));
            passGroup = new VBox(5, passLabel, passWrapper, linkRow);
        } else {
            passGroup = new VBox(5, passLabel, passWrapper);
        }

        HBox errorBox = createErrorBox();
        Label errorText = (Label) errorBox.getChildren().get(1);

        Button loginBtn = new Button(isImportMode ? "Importer et s'authentifier" : "S'authentifier");
        loginBtn.getStyleClass().add("auth-btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(loginBtn, new Insets(10, 0, 0, 0));

        loginBtn.setOnAction(e -> {
            String pass = passField.getText();
            if (selectedKeyFile == null) {
                showError(errorBox, errorText, "Aucun identifiant sélectionné.");
                return;
            }
            if (pass.isEmpty()) {
                showError(errorBox, errorText, "Passphrase requise.");
                return;
            }

            hideError(errorBox);
            loginBtn.setDisable(true);
            loginBtn.setText("Calcul de la signature...");

            new Thread(() -> {
                try {
                    PrivateKey privateKey = ECDSAUtils.loadPrivateKeyFromKeyStore(selectedKeyFile, pass);

                    byte[] sigBytes = ECDSAUtils.sign(activeChallenge, privateKey);
                    String signature = java.util.Base64.getEncoder().encodeToString(sigBytes);

                    ServerConnection.getInstance().sendAsync(
                            new Request(Action.ADMIN_AUTH_RESPONSE, null, Map.of(
                                    "username", currentUsername,
                                    "signature", signature)))
                            .thenAccept(resp -> Platform.runLater(() -> {
                                loginBtn.setDisable(false);
                                loginBtn.setText(isImportMode ? "Importer et s'authentifier" : "S'authentifier");

                                if (resp.isSuccess()) {
                                    if (isImportMode && selectedKeyFile != null) {
                                        copyToDefaultKeystore(selectedKeyFile, currentUsername);
                                    }
                                    Map<String, Object> data = castToMap(resp.getData());

                                    if (Boolean.TRUE.equals(data.get("2fa_required"))) {
                                        AdminNavigationManager.showTwoFactor(
                                                getInt(data, "userId"),
                                                getStr(data, "email"));
                                        return;
                                    }

                                    AdminNavigationManager.setSession(
                                            getStr(data, "token"),
                                            getInt(data, "userId"),
                                            getStr(data, "username"),
                                            getStr(data, "role"),
                                            getStr(data, "firstName"),
                                            getStr(data, "lastName"),
                                            getStr(data, "email"));

                                    int uid = getInt(data, "userId");
                                    UDPHeartbeatSender heartbeat = new UDPHeartbeatSender(uid);
                                    Thread heartbeatThread = new Thread(heartbeat);
                                    heartbeatThread.setDaemon(true);
                                    heartbeatThread.start();

                                    AdminNavigationManager.showAdminDashboard();
                                } else {
                                    showError(errorBox, errorText, "Accès refusé: " + resp.getMessage());
                                    if (resp.getMessage().contains("expiré")) {
                                        showInitPhase();
                                    }
                                }
                            }));

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        loginBtn.setDisable(false);
                        loginBtn.setText(isImportMode ? "Importer et s'authentifier" : "S'authentifier");
                        showError(errorBox, errorText, "Erreur cryptographique : Clé ou Passphrase invalide.");
                    });
                }
            }).start();
        });

        passField.setOnAction(loginBtn.getOnAction());

        card.getChildren().addAll(
                fileGroup,
                passGroup,
                errorBox,
                loginBtn);

        centerBox.getChildren().addAll(branding, card);
        animateCard(branding);
        animateCard(card);
    }

    private VBox createCard(String iconStr, String titleText, String subtitleText) {
        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(28, 36, 24, 36));
        card.setAlignment(Pos.CENTER);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER);
        Label icon = new Label(iconStr);
        icon.setStyle("-fx-font-size: 22px; -fx-text-fill: #2C3E50;");
        Label title = new Label(titleText);
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 22px;");
        titleRow.getChildren().addAll(icon, title);

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("auth-subtitle");

        Region div = new Region();
        div.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;");

        card.getChildren().addAll(titleRow, subtitle, div);
        return card;
    }

    private HBox createErrorBox() {
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
        errorText.setMinWidth(0);
        HBox.setHgrow(errorText, Priority.ALWAYS);
        errorBox.getChildren().addAll(errorIcon, errorText);
        return errorBox;
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

    private VBox buildBranding() {
        VBox branding = new VBox(6);
        branding.setAlignment(Pos.CENTER);

        try {
            javafx.scene.image.ImageView logo = new javafx.scene.image.ImageView(new javafx.scene.image.Image(
                    java.util.Objects.requireNonNull(
                            getClass().getResourceAsStream("/images/logo.png"))));
            logo.setFitHeight(52);
            logo.setPreserveRatio(true);
            logo.setSmooth(true);
            branding.getChildren().add(logo);
        } catch (Exception ignored) {
        }

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

    private void animateCard(Node card) {
        FadeTransition fc = new FadeTransition(javafx.util.Duration.millis(600), card);
        fc.setFromValue(0);
        fc.setToValue(1);
        TranslateTransition tc = new TranslateTransition(javafx.util.Duration.millis(600), card);
        tc.setFromY(20);
        tc.setToY(0);
        tc.setInterpolator(Interpolator.EASE_OUT);

        Platform.runLater(() -> {
            card.setOpacity(0);
            new ParallelTransition(fc, tc).play();
        });
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
                    javafx.util.Duration.seconds(8 + rand.nextDouble() * 12), dot);
            tt.setByY(-40 - rand.nextDouble() * 60);
            tt.setByX((rand.nextDouble() - 0.5) * 40);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.setAutoReverse(true);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            tt.play();
        }

        return layer;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        if (obj == null)
            return Map.of();
        if (obj instanceof Map)
            return (Map<String, Object>) obj;
        return gson.fromJson(gson.toJson(obj), new TypeToken<Map<String, Object>>() {
        }.getType());
    }

    private String getStr(Map<String, Object> m, String key) {
        if (m == null)
            return "";
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private int getInt(Map<String, Object> m, String key) {
        if (m == null)
            return -1;
        Object v = m.get(key);
        if (v == null)
            return -1;
        if (v instanceof Double)
            return ((Double) v).intValue();
        return Integer.parseInt(v.toString());
    }

    private StackPane wrapWithToggle(PasswordField pf) {
        TextField visibleField = new TextField();
        visibleField.setPromptText(pf.getPromptText());
        visibleField.getStyleClass().addAll(pf.getStyleClass());
        visibleField.setVisible(false);
        visibleField.setManaged(false);

        pf.textProperty().addListener((obs, o, n) -> {
            if (!visibleField.isFocused())
                visibleField.setText(n);
        });
        visibleField.textProperty().addListener((obs, o, n) -> {
            if (!pf.isFocused())
                pf.setText(n);
        });

        Button eyeBtn = new Button("\uD83D\uDC41");
        eyeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 0 10 0 0;");
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

    private void copyToDefaultKeystore(File sourceFile, String username) {
        try {
            File destDir = new File(System.getProperty("user.home") + "/.chrionline/keystores");
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            File destFile = new File(destDir, username + ".p12");

            if (sourceFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
                return;
            }

            java.nio.file.Files.copy(
                    sourceFile.toPath(),
                    destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[KEY STORE] Key copied successfully to local keychain: " + destFile.getAbsolutePath());
        } catch (Exception ex) {
            System.err.println("[KEY STORE] Failed to copy key to default keychain: " + ex.getMessage());
        }
    }
}
