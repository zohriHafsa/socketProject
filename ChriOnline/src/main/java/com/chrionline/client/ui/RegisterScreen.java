package com.chrionline.client.ui;

import com.chrionline.client.controller.AuthController;
import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;

/**
 * TÂCHE 2 — RegisterScreen
 * ════════════════════════
 * Utilise AuthController.register() puis AuthController.login().
 *
 * REGISTER renvoie : { userId, username }
 * LOGIN renvoie    : { token, userId, username, role, firstName, lastName }  (Map plat)
 */
public class RegisterScreen {

    private final AuthController authController = new AuthController();
    private final Gson           gson           = new Gson();

    public Node getView() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f0728, #3b1278, #6d28d9);");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox card = new VBox(18);
        card.setMaxWidth(460);
        card.setPadding(new Insets(44, 50, 44, 50));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.07);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: rgba(255,255,255,0.13);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1;"
        );

        Label brand = lbl("ChriOnline", 24, FontWeight.BOLD, Color.WHITE);
        Label title = lbl("Créer un compte", 22, FontWeight.BOLD, Color.rgb(220, 200, 255));

        TextField     usernameField  = styledField("ex : jean_dupont");
        TextField     emailField     = styledField("ex : jean@email.com");
        TextField     firstNameField = styledField("Prénom");
        TextField     lastNameField  = styledField("Nom");
        PasswordField passwordField  = styledPassword("Minimum 6 caractères");
        PasswordField confirmField   = styledPassword("Répétez le mot de passe");

        VBox fnGroup = fieldGroup("Prénom *",         firstNameField);
        VBox lnGroup = fieldGroup("Nom de famille *", lastNameField);
        HBox.setHgrow(fnGroup, Priority.ALWAYS);
        HBox.setHgrow(lnGroup, Priority.ALWAYS);
        HBox nameRow = new HBox(12, fnGroup, lnGroup);

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.rgb(252, 129, 129));
        errorLabel.setFont(Font.font(12));
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button registerBtn = new Button("Créer mon compte");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setFont(Font.font("System", FontWeight.BOLD, 14));
        registerBtn.setStyle(btnStyle("#f59e0b"));
        registerBtn.setOnMouseEntered(e -> registerBtn.setStyle(btnStyle("#d97706")));
        registerBtn.setOnMouseExited(e  -> registerBtn.setStyle(btnStyle("#f59e0b")));
        registerBtn.setOnAction(e -> handleRegister(
                usernameField, emailField, firstNameField, lastNameField,
                passwordField, confirmField, registerBtn, errorLabel
        ));

        Hyperlink loginLink = new Hyperlink("Déjà un compte ? Se connecter");
        loginLink.setTextFill(Color.rgb(167, 139, 250));
        loginLink.setStyle("-fx-border-color: transparent;");
        loginLink.setOnAction(e -> NavigationManager.showLogin());

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.1);");

        card.getChildren().addAll(
                brand, title, sep,
                fieldGroup("Nom d'utilisateur *", usernameField),
                fieldGroup("Adresse e-mail *",    emailField),
                nameRow,
                fieldGroup("Mot de passe *",              passwordField),
                fieldGroup("Confirmer le mot de passe *", confirmField),
                errorLabel, registerBtn, loginLink
        );

        StackPane wrapper = new StackPane(card);
        wrapper.setPadding(new Insets(40));
        scroll.setContent(wrapper);
        root.getChildren().add(scroll);
        return root;
    }

    // ── Logique REGISTER ──────────────────────────────────────────────────────

    private void handleRegister(TextField usernameField, TextField emailField,
                                TextField firstNameField, TextField lastNameField,
                                PasswordField passwordField, PasswordField confirmField,
                                Button registerBtn, Label errorLabel) {

        String username  = usernameField.getText().trim();
        String email     = emailField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String password  = passwordField.getText();
        String confirm   = confirmField.getText();

        // Validation locale
        if (username.isEmpty() || email.isEmpty() || firstName.isEmpty()
                || lastName.isEmpty() || password.isEmpty()) {
            showError(errorLabel, "Tous les champs marqués * sont obligatoires.");
            return;
        }
        if (!password.equals(confirm)) {
            showError(errorLabel, "Les mots de passe ne correspondent pas.");
            return;
        }
        if (password.length() < 6) {
            showError(errorLabel, "Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        registerBtn.setDisable(true);
        registerBtn.setText("Création en cours...");

        new Thread(() -> {
            if (!ServerConnection.getInstance().isConnected())
                ServerConnection.getInstance().connect();

            // 1. REGISTER
            Response registerResp = authController.register(
                    username, email, password, firstName, lastName, "", ""
            );

            if (!registerResp.isSuccess()) {
                Platform.runLater(() -> {
                    showError(errorLabel, registerResp.getMessage());
                    registerBtn.setDisable(false);
                    registerBtn.setText("Créer mon compte");
                });
                return;
            }

            // 2. AUTO-LOGIN après inscription réussie
            Response loginResp = authController.login(username, password);

            Platform.runLater(() -> {
                registerBtn.setDisable(false);
                registerBtn.setText("Créer mon compte");

                if (loginResp.isSuccess()) {
                    // getData() = Map plat : { token, userId, username, role, firstName, lastName }
                    Map<String, Object> data = castToMap(loginResp.getData());
                    NavigationManager.setSession(
                            getStr(data, "token"),
                            getInt(data, "userId"),
                            getStr(data, "username"),
                            getStr(data, "role"),
                            getStr(data, "firstName"),
                            getStr(data, "lastName")
                    );
                    NavigationManager.showLanding();
                } else {
                    // Inscription OK mais login échoué → rediriger vers login
                    NavigationManager.showLogin();
                }
            });
        }).start();
    }

    // ── Helpers désérialisation ───────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        if (obj == null) return Map.of();
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return gson.fromJson(gson.toJson(obj),
                new TypeToken<Map<String, Object>>(){}.getType());
    }

    private String getStr(Map<String, Object> m, String key) {
        if (m == null) return "";
        Object v = m.get(key); return v != null ? v.toString() : "";
    }

    private int getInt(Map<String, Object> m, String key) {
        if (m == null) return -1;
        Object v = m.get(key);
        if (v == null) return -1;
        if (v instanceof Double) return ((Double) v).intValue();
        return Integer.parseInt(v.toString());
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private void showError(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }

    private VBox fieldGroup(String labelText, javafx.scene.control.Control field) {
        Label l = new Label(labelText);
        l.setTextFill(Color.rgb(200, 180, 240));
        l.setFont(Font.font(12));
        return new VBox(6, l, field);
    }

    private Label lbl(String text, int size, FontWeight weight, Color color) {
        Label l = new Label(text);
        l.setFont(Font.font("System", weight, size));
        l.setTextFill(color);
        return l;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt); tf.setStyle(inputStyle()); return tf;
    }

    private PasswordField styledPassword(String prompt) {
        PasswordField pf = new PasswordField(); pf.setPromptText(prompt); pf.setStyle(inputStyle()); return pf;
    }

    private String inputStyle() {
        return "-fx-background-color: rgba(255,255,255,0.09);-fx-text-fill: white;" +
                "-fx-prompt-text-fill: rgba(200,180,255,0.5);-fx-background-radius: 9;" +
                "-fx-border-color: rgba(255,255,255,0.15);-fx-border-radius: 9;" +
                "-fx-padding: 10 14;-fx-font-size: 13;";
    }

    private String btnStyle(String color) {
        return "-fx-background-color:" + color + ";-fx-text-fill:white;" +
                "-fx-background-radius:9;-fx-padding:12 0;-fx-cursor:hand;";
    }
}