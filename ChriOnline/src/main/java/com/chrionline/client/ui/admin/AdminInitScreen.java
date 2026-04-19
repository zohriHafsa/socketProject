package com.chrionline.client.ui.admin;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.client.ui.NavigationManager;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.enums.Action;
import com.chrionline.shared.security.RSAUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.security.KeyPair;
import java.util.Map;

public class AdminInitScreen {

    private final VBox root;
    private File selectedDirectory;

    public AdminInitScreen() {
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getStyleClass().add("auth-container");

        VBox formCard = new VBox(15);
        formCard.getStyleClass().add("auth-card");
        formCard.setMaxWidth(450);
        formCard.setPadding(new Insets(30));

        Label title = new Label("Initialisation Administrateur");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 22px; -fx-text-fill: #6C3483; -fx-font-weight: bold;");

        Label subtitle = new Label("Veuillez générer vos clés de sécurité RSA pour terminer la configuration.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");

        TextField userField = new TextField();
        userField.setPromptText("Nom d'utilisateur");
        userField.getStyleClass().add("auth-field");

        TextField tokenField = new TextField();
        tokenField.setPromptText("Token d'activation (reçu par email)");
        tokenField.getStyleClass().add("auth-field");

        HBox dirBox = new HBox(10);
        dirBox.setAlignment(Pos.CENTER_LEFT);
        TextField dirField = new TextField();
        dirField.setPromptText("Dossier de sauvegarde de la clé privée");
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

        PasswordField passField = new PasswordField();
        passField.setPromptText("Passphrase (pour chiffrer la clé privée)");
        passField.getStyleClass().add("auth-field");

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirmer la passphrase");
        confirmField.getStyleClass().add("auth-field");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        Button initBtn = new Button("Générer et Initialiser");
        initBtn.setMaxWidth(Double.MAX_VALUE);
        initBtn.setStyle("-fx-background-color: #6C3483; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px; -fx-cursor: hand;");
        initBtn.setOnAction(e -> handleInit(userField, tokenField, passField, confirmField, errorLabel, initBtn));

        Button backBtn = new Button("Retour à la connexion");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-cursor: hand; -fx-underline: true;");
        backBtn.setOnAction(e -> NavigationManager.navigateTo("LOGIN"));

        formCard.getChildren().addAll(
                title, subtitle,
                new Label("Identification"), userField, tokenField,
                new Label("Sécurité RSA"), dirBox, passField, confirmField,
                errorLabel, initBtn, backBtn
        );

        root.getChildren().add(formCard);
    }

    public Node getView() {
        return root;
    }

    private void handleInit(TextField userField, TextField tokenField, PasswordField passField, PasswordField confirmField, Label error, Button btn) {
        String username = userField.getText().trim();
        String token = tokenField.getText().trim();
        String pass = passField.getText();
        String confirm = confirmField.getText();

        error.setVisible(false);

        if (username.isEmpty() || token.isEmpty() || pass.isEmpty()) {
            showError(error, "Tous les champs sont requis.");
            return;
        }

        if (selectedDirectory == null) {
            showError(error, "Veuillez sélectionner un dossier pour sauvegarder votre clé privée.");
            return;
        }

        if (!pass.equals(confirm)) {
            showError(error, "Les passphrases ne correspondent pas.");
            return;
        }
        
        if (pass.length() < 6) {
            showError(error, "La passphrase doit faire au moins 6 caractères.");
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
                                             "\n\nNE PERDEZ PAS CE FICHIER NI LA PASSPHRASE. Vous en aurez besoin pour vous connecter.");
                        alert.showAndWait();
                        NavigationManager.navigateTo("LOGIN");
                    } else {
                        showError(error, resp.getMessage());
                    }
                }));

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("Générer et Initialiser");
                    showError(error, "Erreur cryptographique : " + ex.getMessage());
                });
            }
        }).start();
    }

    private void showError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
    }
}
