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
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;

/**
 * TÂCHE 2 — LoginScreen
 * ═════════════════════
 * Utilise AuthController.login() pour envoyer la requête LOGIN.
 *
 * STRUCTURE DE LA RÉPONSE LOGIN (AuthService.java) :
 *   getData() retourne un Map PLAT — pas de sous-objet "user" :
 *   {
 *     "token":     "sess-xxxx",
 *     "userId":    2,
 *     "username":  "jdupont",
 *     "role":      "CLIENT",
 *     "firstName": "Jean",
 *     "lastName":  "Dupont"
 *   }
 *
 * APRÈS LOGIN → NavigationManager.showLanding()
 *   La LandingPage est la vraie page d'accueil (avec logo, menu, etc.)
 *   Elle détecte NavigationManager.isLoggedIn() pour adapter l'affichage.
 */
public class LoginScreen {

    private final AuthController authController = new AuthController();
    private final Gson           gson           = new Gson();

    public Node getView() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f0728, #3b1278, #6d28d9);");

        VBox card = new VBox(20);
        card.setMaxWidth(440);
        card.setPadding(new Insets(48, 50, 48, 50));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.07);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: rgba(255,255,255,0.13);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1;"
        );

        Label brand    = lbl("ChriOnline", 26, FontWeight.BOLD, Color.WHITE);
        Label title    = lbl("Connexion",   22, FontWeight.BOLD, Color.rgb(220, 200, 255));
        Label subtitle = lbl("Accédez à votre compte", 13, FontWeight.NORMAL, Color.rgb(180, 155, 230));

        TextField     usernameField = styledField("Nom d'utilisateur");
        PasswordField passwordField = styledPassword("Mot de passe");

        // Erreur inline — invisible par défaut
        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.rgb(252, 129, 129));
        errorLabel.setFont(Font.font(12));
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button loginBtn = new Button("Se connecter");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setFont(Font.font("System", FontWeight.BOLD, 14));
        loginBtn.setStyle(btnStyle("#f59e0b"));
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(btnStyle("#d97706")));
        loginBtn.setOnMouseExited(e  -> loginBtn.setStyle(btnStyle("#f59e0b")));
        loginBtn.setOnAction(e ->
                handleLogin(usernameField, passwordField, loginBtn, errorLabel));
        passwordField.setOnAction(e ->
                handleLogin(usernameField, passwordField, loginBtn, errorLabel));

        Hyperlink registerLink = new Hyperlink("Pas encore de compte ? S'inscrire");
        registerLink.setTextFill(Color.rgb(167, 139, 250));
        registerLink.setStyle("-fx-border-color: transparent;");
        registerLink.setOnAction(e -> NavigationManager.showRegister());

        Hyperlink backLink = new Hyperlink("← Retour à l'accueil");
        backLink.setTextFill(Color.rgb(140, 120, 200));
        backLink.setStyle("-fx-border-color: transparent; -fx-font-size: 11;");
        backLink.setOnAction(e -> NavigationManager.showLanding());

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.1);");

        card.getChildren().addAll(
                brand, title, subtitle, sep,
                fieldGroup("Nom d'utilisateur", usernameField),
                fieldGroup("Mot de passe",       passwordField),
                errorLabel, loginBtn, registerLink, backLink
        );

        root.getChildren().add(card);
        return root;
    }

    // ── Logique LOGIN ─────────────────────────────────────────────────────────

    private void handleLogin(TextField usernameField, PasswordField passwordField,
                             Button loginBtn, Label errorLabel) {

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError(errorLabel, "Veuillez remplir tous les champs.");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Connexion en cours...");

        new Thread(() -> {
            // Connexion TCP si pas encore établie
            if (!ServerConnection.getInstance().isConnected()) {
                boolean ok = ServerConnection.getInstance().connect();
                if (!ok) {
                    Platform.runLater(() -> {
                        showError(errorLabel, "Serveur inaccessible. Vérifiez qu'il est démarré sur le port 9090.");
                        loginBtn.setDisable(false);
                        loginBtn.setText("Se connecter");
                    });
                    return;
                }
            }

            // AuthController.login() → Request(LOGIN, null, {username, password})
            Response response = authController.login(username, password);

            Platform.runLater(() -> {
                loginBtn.setDisable(false);
                loginBtn.setText("Se connecter");

                if (response.isSuccess()) {
                    // ── getData() retourne un Map PLAT (pas de sous-objet "user") ──
                    // Structure : { token, userId, username, role, firstName, lastName }
                    Map<String, Object> data = castToMap(response.getData());

                    String token     = getStr(data, "token");
                    int    userId    = getInt(data, "userId");
                    String un        = getStr(data, "username");
                    String role      = getStr(data, "role");
                    String firstName = getStr(data, "firstName");
                    String lastName  = getStr(data, "lastName");

                    // Stocker la session dans NavigationManager
                    // (accessible depuis tous les écrans)
                    NavigationManager.setSession(token, userId, un, role, firstName, lastName);

                    // Aller vers LandingPage (la vraie page d'accueil du projet)
                    NavigationManager.showLanding();

                } else {
                    showError(errorLabel, response.getMessage());
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
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(inputStyle());
        return tf;
    }

    private PasswordField styledPassword(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setStyle(inputStyle());
        return pf;
    }

    private String inputStyle() {
        return "-fx-background-color: rgba(255,255,255,0.09);" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: rgba(200,180,255,0.5);" +
                "-fx-background-radius: 9;" +
                "-fx-border-color: rgba(255,255,255,0.15);" +
                "-fx-border-radius: 9;" +
                "-fx-padding: 11 14;" +
                "-fx-font-size: 13;";
    }

    private String btnStyle(String color) {
        return "-fx-background-color:" + color + ";-fx-text-fill:white;" +
                "-fx-background-radius:9;-fx-padding:12 0;-fx-cursor:hand;";
    }
}