package com.chrionline.client.ui.admin;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.client.ui.NavigationManager;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import com.chrionline.shared.security.RSAUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.security.KeyPair;
import java.util.Map;

public class AdminInitScreen {

    private final StackPane root;
    private File selectedDirectory;
    private final String targetUsername;

    public AdminInitScreen(String targetUsername) {
        this.targetUsername = targetUsername != null ? targetUsername : "";
        root = new StackPane();
        root.getStyleClass().add("auth-root");

        Label watermark = new Label("ChriOnline");
        watermark.getStyleClass().add("auth-brand-watermark");
        watermark.setMouseTransparent(true);
        StackPane.setAlignment(watermark, Pos.CENTER);

        Button backLink = new Button("\u2190 Retour à la connexion");
        backLink.getStyleClass().add("auth-back-link");
        backLink.setOnAction(e -> NavigationManager.navigateTo("login"));
        StackPane.setAlignment(backLink, Pos.TOP_LEFT);
        StackPane.setMargin(backLink, new Insets(24, 0, 0, 24));

        VBox centerBox = new VBox(28);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(40));
        centerBox.setPickOnBounds(false); // crucial for click-through to backLink if it overlaps

        VBox branding = buildBranding();

        VBox formCard = new VBox(16);
        formCard.getStyleClass().add("auth-card");
        formCard.setMaxWidth(600);
        formCard.setPadding(new Insets(30, 40, 30, 40));
        formCard.setAlignment(Pos.CENTER);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER);
        Label icon = new Label("\u2699");
        icon.setStyle("-fx-font-size: 22px; -fx-text-fill: #2C3E50;");
        Label title = new Label("Initialisation");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 22px;");
        titleRow.getChildren().addAll(icon, title);

        Label subtitle = new Label("Bonjour " + this.targetUsername + " ! Générez vos clés de sécurité RSA locales pour finaliser.");
        subtitle.getStyleClass().add("auth-subtitle");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(TextAlignment.CENTER);
        subtitle.setAlignment(Pos.CENTER);
        subtitle.setMaxWidth(Double.MAX_VALUE);

        Region div = new Region();
        div.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;");
        VBox.setMargin(div, new Insets(0, 0, 5, 0));

        // Token
        Label tokenLabel = new Label("Token d'activation");
        tokenLabel.getStyleClass().add("auth-field-label");
        TextField tokenField = new TextField();
        tokenField.setPromptText("Copiez/Collez le token reçu par email");
        tokenField.getStyleClass().add("auth-field");
        VBox tokenGroup = new VBox(5, tokenLabel, tokenField);

        // Directory Selector
        Label dirLabel = new Label("Dossier de sauvegarde");
        dirLabel.getStyleClass().add("auth-field-label");
        
        HBox dirBox = new HBox(10);
        dirBox.setAlignment(Pos.CENTER_LEFT);
        TextField dirField = new TextField();
        dirField.setPromptText("Aucun dossier sélectionné...");
        dirField.getStyleClass().add("auth-field");
        dirField.setEditable(false);
        HBox.setHgrow(dirField, Priority.ALWAYS);

        Button browseBtn = new Button("Parcourir...");
        browseBtn.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-cursor: hand;");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Choisir l'emplacement de sauvegarde");
            Stage stage = (Stage) root.getScene().getWindow();
            File dir = dc.showDialog(stage);
            if (dir != null) {
                selectedDirectory = dir;
                dirField.setText(dir.getAbsolutePath());
            }
        });
        dirBox.getChildren().addAll(dirField, browseBtn);
        VBox dirGroup = new VBox(5, dirLabel, dirBox);

        // Passphrase Row
        Label passLabel = new Label("Passphrase de la clé");
        passLabel.getStyleClass().add("auth-field-label");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Minimum 6 caractères");
        passField.getStyleClass().add("auth-password-field");
        VBox passGroup = new VBox(5, passLabel, passField);
        HBox.setHgrow(passGroup, Priority.ALWAYS);

        Label confirmLabel = new Label("Confirmer la passphrase");
        confirmLabel.getStyleClass().add("auth-field-label");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Répétez la passphrase");
        confirmField.getStyleClass().add("auth-password-field");
        VBox confirmGroup = new VBox(5, confirmLabel, confirmField);
        HBox.setHgrow(confirmGroup, Priority.ALWAYS);

        HBox passwordsRow = new HBox(15, passGroup, confirmGroup);

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

        Button initBtn = new Button("Générer et Initialiser");
        initBtn.getStyleClass().add("auth-btn-primary");
        initBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(initBtn, new Insets(10, 0, 0, 0));
        
        initBtn.setOnAction(e -> handleInit(tokenField, passField, confirmField, errorBox, errorText, initBtn));
        confirmField.setOnAction(initBtn.getOnAction()); // Support Enter key

        formCard.getChildren().addAll(
                titleRow, subtitle, div,
                tokenGroup,
                dirGroup, passwordsRow,
                errorBox, initBtn
        );

        centerBox.getChildren().addAll(branding, formCard);

        Pane particles = buildParticles();
        
        // Ensure backLink is added LAST so it layers ABOVE the centerBox and particles!
        root.getChildren().addAll(particles, watermark, centerBox, backLink);

        // Animation
        javafx.animation.FadeTransition fb = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), branding);
        fb.setFromValue(0);
        fb.setToValue(1);
        javafx.animation.TranslateTransition tb = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(500), branding);
        tb.setFromY(-15);
        tb.setToY(0);
        tb.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        javafx.animation.FadeTransition fc = new javafx.animation.FadeTransition(javafx.util.Duration.millis(600), formCard);
        fc.setFromValue(0);
        fc.setToValue(1);
        javafx.animation.TranslateTransition tc = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(600), formCard);
        tc.setFromY(20);
        tc.setToY(0);
        tc.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        
        Platform.runLater(() -> {
            branding.setOpacity(0);
            formCard.setOpacity(0);
            new javafx.animation.ParallelTransition(fb, tb, fc, tc).play();
        });
    }

    public Node getView() {
        return root;
    }

    private void handleInit(TextField tokenField, PasswordField passField, PasswordField confirmField, HBox errorBox, Label errorText, Button btn) {
        String username = this.targetUsername;
        String token = tokenField.getText().trim();
        String pass = passField.getText();
        String confirm = confirmField.getText();

        hideError(errorBox);

        if (username.isEmpty() || token.isEmpty() || pass.isEmpty()) {
            showError(errorBox, errorText, "Tous les champs sont requis.");
            return;
        }

        if (selectedDirectory == null) {
            showError(errorBox, errorText, "Veuillez sélectionner un dossier pour sauvegarder votre clé privée.");
            return;
        }

        if (!pass.equals(confirm)) {
            showError(errorBox, errorText, "Les passphrases ne correspondent pas.");
            return;
        }
        
        if (pass.length() < 6) {
            showError(errorBox, errorText, "La passphrase doit faire au moins 6 caractères.");
            return;
        }

        btn.setDisable(true);
        btn.setText("Génération en cours...");

        new Thread(() -> {
            try {
                KeyPair rsaKeys = RSAUtils.generateKeyPair();

                File privateKeyFile = new File(selectedDirectory, username + "_private.key");
                RSAUtils.savePrivateKeyEncrypted(rsaKeys.getPrivate(), privateKeyFile, pass);

                String publicKeyBase64 = RSAUtils.encodePublicKey(rsaKeys.getPublic());

                ServerConnection.getInstance().sendAsync(
                    new Request(Action.ADMIN_REGISTER_KEY, null, Map.of(
                        "username", username,
                        "token", token,
                        "publicKey", publicKeyBase64
                    ))
                ).thenAccept(resp -> Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("Générer et Initialiser");
                    
                    if (resp.isSuccess()) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Initialisation réussie");
                        alert.setHeaderText("Configuration RSA terminée");
                        alert.setContentText("Votre clé privée a été sauvegardée avec succès sous : \n" + 
                                             privateKeyFile.getAbsolutePath() + 
                                             "\n\nNE PERDEZ PAS CE FICHIER NI LA PASSPHRASE.");
                        alert.showAndWait();
                        NavigationManager.navigateTo("login");
                    } else {
                        showError(errorBox, errorText, resp.getMessage());
                    }
                }));

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("Générer et Initialiser");
                    showError(errorBox, errorText, "Erreur cryptographique : " + ex.getMessage());
                });
            }
        }).start();
    }

    private void showError(HBox errorBox, Label errorText, String msg) {
        errorText.setText(msg);
        errorBox.setVisible(true);
        errorBox.setManaged(true);
        
        errorBox.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), errorBox);
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
        } catch (Exception ignored) {}

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

            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.seconds(8 + rand.nextDouble() * 12), dot);
            tt.setByY(-40 - rand.nextDouble() * 60);
            tt.setByX((rand.nextDouble() - 0.5) * 40);
            tt.setCycleCount(javafx.animation.Animation.INDEFINITE);
            tt.setAutoReverse(true);
            tt.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
            tt.play();
        }

        return layer;
    }
}
