package com.chrionline.admin.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.Map;

public class AdminTwoFactorScreen {

    private final int    userId;
    private final String email;
    private final Gson gson = new Gson();

    public AdminTwoFactorScreen(int userId, String email) {
        this.userId = userId;
        this.email  = email;
    }

    public Node getView() {
        StackPane root = new StackPane();
        root.getStyleClass().add("auth-root");

        Button backLink = new Button("\u2190  Retour a la connexion");
        backLink.getStyleClass().add("auth-back-link");
        backLink.setOnAction(e -> AdminNavigationManager.showAdminLogin());
        StackPane.setAlignment(backLink, Pos.TOP_LEFT);
        StackPane.setMargin(backLink, new Insets(24, 0, 0, 24));

        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(28, 36, 24, 36));
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(400);

        Label title = new Label("Verification 2FA (Admin)");
        title.getStyleClass().add("auth-title");

        Label subtitle = new Label("Code envoye a :\n" + email);
        subtitle.getStyleClass().add("auth-subtitle");
        subtitle.setWrapText(true);

        TextField codeField = new TextField();
        codeField.setPromptText("_ _ _ _ _ _");
        codeField.getStyleClass().add("auth-field");
        codeField.setAlignment(Pos.CENTER);
        codeField.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) codeField.setText(n.replaceAll("[^\\d]", ""));
            if (n.length() > 6) codeField.setText(n.substring(0, 6));
        });

        HBox fbBox = new HBox(8);
        fbBox.getStyleClass().add("auth-error");
        fbBox.setVisible(false);
        fbBox.setManaged(false);
        fbBox.setAlignment(Pos.CENTER_LEFT);
        Label fbIcon = new Label("\u26A0");
        fbIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #E74C3C;");
        Label fbText = new Label();
        fbText.getStyleClass().add("auth-error-text");
        fbText.setWrapText(true);
        fbText.setMinWidth(0);
        HBox.setHgrow(fbText, Priority.ALWAYS);
        fbBox.getChildren().addAll(fbIcon, fbText);

        Button verifyBtn = new Button("Valider");
        verifyBtn.getStyleClass().add("auth-btn-primary");
        verifyBtn.setOnAction(e -> handleVerify(codeField, verifyBtn, fbBox, fbText));

        card.getChildren().addAll(title, subtitle, codeField, fbBox, verifyBtn);
        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().addAll(backLink, card);
        return root;
    }

    private void handleVerify(TextField codeField, Button verifyBtn, HBox fbBox, Label fbText) {
        String code = codeField.getText().trim();
        if (code.length() != 6) {
            fbText.setText("Le code doit contenir 6 chiffres.");
            fbBox.setVisible(true);
            fbBox.setManaged(true);
            return;
        }

        verifyBtn.setDisable(true);
        new Thread(() -> {
            Response resp = ServerConnection.getInstance().send(
                    new Request(Action.VERIFY_2FA, null,
                            Map.of("userId", userId, "code", code, "isAdmin", true))
            );

            Platform.runLater(() -> {
                verifyBtn.setDisable(false);
                if (resp.isSuccess()) {
                    Map<String, Object> data = castToMap(resp.getData());
                    AdminNavigationManager.setSession(
                            getStr(data, "token"),
                            getInt(data, "userId"),
                            getStr(data, "username"),
                            getStr(data, "role"),
                            getStr(data, "firstName"),
                            getStr(data, "lastName"),
                            getStr(data, "email")
                    );
                    AdminNavigationManager.showAdminDashboard();
                } else {
                    fbText.setText(resp.getMessage() != null ? resp.getMessage() : "Code incorrect.");
                    fbBox.setVisible(true);
                    fbBox.setManaged(true);
                }
            });
        }).start();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        if (obj == null) return Map.of();
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return gson.fromJson(gson.toJson(obj),
                new TypeToken<Map<String, Object>>(){}.getType());
    }

    private String getStr(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private int getInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return -1;
        if (v instanceof Double) return ((Double) v).intValue();
        return Integer.parseInt(v.toString());
    }
}
