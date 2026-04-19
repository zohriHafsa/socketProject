package com.chrionline.client.ui.admin;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.client.network.UDPHeartbeatSender;
import com.chrionline.client.ui.NavigationManager;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import com.chrionline.shared.security.RSAUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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

    public AdminLoginScreen() {
        root = new StackPane();
        root.getStyleClass().add("auth-root");

        // Background brand
        Label watermark = new Label("ChriOnline");
        watermark.getStyleClass().add("auth-brand-watermark");
        watermark.setMouseTransparent(true);
        StackPane.setAlignment(watermark, Pos.CENTER);

        // Back link
        Button backLink = new Button("\u2190 Retour à la connexion client");
        backLink.getStyleClass().add("auth-back-link");
        backLink.setOnAction(e -> NavigationManager.showLogin());
        StackPane.setAlignment(backLink, Pos.TOP_LEFT);
        StackPane.setMargin(backLink, new Insets(24, 0, 0, 24));

        centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMaxWidth(450);

        showInitPhase();

        root.getChildren().addAll(watermark, backLink, centerBox);
    }

    public Node getView() {
        return root;
    }

    private void showInitPhase() {
        centerBox.getChildren().clear();

        VBox card = createCard("Espace Administrateur", "Veuillez entrer votre identifiant");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur admin");
        usernameField.getStyleClass().add("auth-field");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        Button continueBtn = new Button("Continuer");
        continueBtn.getStyleClass().add("auth-btn-primary");
        continueBtn.setMaxWidth(Double.MAX_VALUE);

        continueBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            if (user.isEmpty()) {
                showError(errorLabel, "Nom d'utilisateur requis.");
                return;
            }
            errorLabel.setVisible(false);
            continueBtn.setDisable(true);
            continueBtn.setText("Recherche du challenge...");

            ServerConnection.getInstance().sendAsync(
                new Request(Action.ADMIN_AUTH_INIT, null, Map.of("username", user))
            ).thenAccept(resp -> Platform.runLater(() -> {
                continueBtn.setDisable(false);
                continueBtn.setText("Continuer");

                if (resp.isSuccess()) {
                    Map<String, Object> data = castToMap(resp.getData());
                    String status = getStr(data, "status");
                    
                    if ("PENDING".equals(status)) {
                        // Needs initialization
                        NavigationManager.showAdminInit();
                    } else if ("ACTIVE".equals(status)) {
                        currentUsername = user;
                        activeChallenge = getStr(data, "challenge");
                        showChallengePhase();
                    }
                } else {
                    showError(errorLabel, resp.getMessage());
                }
            }));
        });

        card.getChildren().addAll(
            new Label("Identifiant"), usernameField,
            errorLabel,
            continueBtn
        );

        centerBox.getChildren().add(card);
    }

    private void showChallengePhase() {
        centerBox.getChildren().clear();

        VBox card = createCard("Authentification RSA", "Challenge sécurisé réceptionné.");

        Label greeting = new Label("Bonjour " + currentUsername + ", veuillez fournir votre clé privée.");
        greeting.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        greeting.setWrapText(true);

        // File Selector
        HBox fileBox = new HBox(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        TextField fileField = new TextField();
        fileField.setPromptText("Aucune clé sélectionnée (.key)");
        fileField.getStyleClass().add("auth-field");
        fileField.setEditable(false);
        HBox.setHgrow(fileField, Priority.ALWAYS);

        Button browseBtn = new Button("Parcourir...");
        browseBtn.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-cursor: hand;");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Sélectionner la clé privée RSA");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Private Key Files", "*.key", "*.pem"));
            Stage stage = (Stage) root.getScene().getWindow();
            File file = fc.showOpenDialog(stage);
            if (file != null) {
                selectedKeyFile = file;
                fileField.setText(file.getName());
            }
        });
        fileBox.getChildren().addAll(fileField, browseBtn);

        // Passphrase
        PasswordField passField = new PasswordField();
        passField.setPromptText("Passphrase de déchiffrement");
        passField.getStyleClass().add("auth-password-field");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        Button loginBtn = new Button("S'authentifier");
        loginBtn.getStyleClass().add("auth-btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: #8E44AD;");

        loginBtn.setOnAction(e -> {
            String pass = passField.getText();
            if (selectedKeyFile == null) {
                showError(errorLabel, "Veuillez sélectionner votre fichier de clé.");
                return;
            }
            if (pass.isEmpty()) {
                showError(errorLabel, "Passphrase requise.");
                return;
            }

            errorLabel.setVisible(false);
            loginBtn.setDisable(true);
            loginBtn.setText("Calcul de la signature...");

            new Thread(() -> {
                try {
                    PrivateKey privateKey = RSAUtils.loadPrivateKeyDecrypted(selectedKeyFile, pass);
                    
                    byte[] sigBytes = RSAUtils.sign(activeChallenge, privateKey);
                    String signature = java.util.Base64.getEncoder().encodeToString(sigBytes);

                    ServerConnection.getInstance().sendAsync(
                        new Request(Action.ADMIN_AUTH_RESPONSE, null, Map.of(
                            "username", currentUsername,
                            "signature", signature
                        ))
                    ).thenAccept(resp -> Platform.runLater(() -> {
                        loginBtn.setDisable(false);
                        loginBtn.setText("S'authentifier");

                        if (resp.isSuccess()) {
                            Map<String, Object> data = castToMap(resp.getData());
                            
                            NavigationManager.setSession(
                                getStr(data, "token"),
                                getInt(data, "userId"),
                                getStr(data, "username"),
                                getStr(data, "role"),
                                getStr(data, "firstName"),
                                getStr(data, "lastName"),
                                getStr(data, "email")
                            );

                            int uid = getInt(data, "userId");
                            UDPHeartbeatSender heartbeat = new UDPHeartbeatSender(uid);
                            Thread heartbeatThread = new Thread(heartbeat);
                            heartbeatThread.setDaemon(true);
                            heartbeatThread.start();

                            NavigationManager.showAdminDashboard();
                        } else {
                            showError(errorLabel, "Accès refusé: " + resp.getMessage() + "\nVeuillez recommencer.");
                            // If failed due to timeout, they have to restart the challenge process
                            if(resp.getMessage().contains("expiré")) {
                                showInitPhase();
                            }
                        }
                    }));

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        loginBtn.setDisable(false);
                        loginBtn.setText("S'authentifier");
                        showError(errorLabel, "Erreur cryptographique : Clé ou Passphrase invalide.");
                    });
                }
            }).start();
        });

        card.getChildren().addAll(
            greeting,
            new Label("Fichier Clé Privée"), fileBox,
            new Label("Passphrase"), passField,
            errorLabel,
            loginBtn
        );

        centerBox.getChildren().add(card);
    }

    private VBox createCard(String titleText, String subtitleText) {
        VBox card = new VBox(15);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(30));

        Label title = new Label(titleText);
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 22px; -fx-text-fill: #6C3483;");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("auth-subtitle");

        Region div = new Region();
        div.setStyle("-fx-background-color: #ecf0f1; -fx-pref-height: 1;");

        card.getChildren().addAll(title, subtitle, div);
        return card;
    }

    private void showError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        if (obj == null) return Map.of();
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return gson.fromJson(gson.toJson(obj), new TypeToken<Map<String, Object>>(){}.getType());
    }

    private String getStr(Map<String, Object> m, String key) {
        if (m == null) return "";
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private int getInt(Map<String, Object> m, String key) {
        if (m == null) return -1;
        Object v = m.get(key);
        if (v == null) return -1;
        if (v instanceof Double) return ((Double) v).intValue();
        return Integer.parseInt(v.toString());
    }
}
