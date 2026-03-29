package com.chrionline.client.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class EmailVerificationScreen {

    private final int    userId;
    private final String email;

    public EmailVerificationScreen(int userId, String email) {
        this.userId = userId;
        this.email  = email;
    }

    public Node getView() {
        StackPane root = new StackPane();
        root.getStyleClass().add("auth-root");
        root.setAlignment(Pos.CENTER);

        VBox card = new VBox(18);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(36, 40, 36, 40));
        card.setMaxWidth(440);
        card.setAlignment(Pos.CENTER);

        Label title = new Label("\uD83D\uDCE7 Verifiez votre email");
        title.getStyleClass().add("auth-title");

        Label desc = new Label("Un code a 6 chiffres a ete envoye a :\n" + email);
        desc.getStyleClass().add("auth-subtitle");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-alignment: center;");

        // Code input
        TextField codeField = new TextField();
        codeField.setPromptText("Entrez le code a 6 chiffres");
        codeField.getStyleClass().add("auth-field");
        codeField.setMaxWidth(220);
        codeField.setStyle(
                "-fx-font-size: 22px;" +
                        "-fx-text-alignment: center;" +
                        "-fx-letter-spacing: 8px;"
        );
        // Limite à 6 chiffres
        codeField.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) codeField.setText(n.replaceAll("[^\\d]", ""));
            if (n.length() > 6)    codeField.setText(n.substring(0, 6));
        });

        // Feedback
        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);

        // Bouton vérifier
        Button verifyBtn = new Button("Verifier mon email");
        verifyBtn.getStyleClass().add("auth-btn-primary");
        verifyBtn.setOnAction(e ->
                handleVerify(codeField, verifyBtn, feedbackLabel));
        codeField.setOnAction(e ->
                handleVerify(codeField, verifyBtn, feedbackLabel));

        // Retour login
        Button backBtn = new Button("Retour a la connexion");
        backBtn.getStyleClass().add("auth-link");
        backBtn.setOnAction(e -> NavigationManager.showLogin());

        card.getChildren().addAll(
                title, desc, codeField, feedbackLabel, verifyBtn, backBtn);

        root.getChildren().add(card);
        return root;
    }

    private void handleVerify(TextField codeField,
                              Button verifyBtn, Label feedbackLabel) {
        String code = codeField.getText().trim();

        if (code.length() != 6) {
            showFeedback(feedbackLabel, "Le code doit contenir 6 chiffres.", false);
            return;
        }

        verifyBtn.setDisable(true);
        verifyBtn.setText("Verification...");

        new Thread(() -> {
            Response resp = ServerConnection.getInstance().send(
                    new Request(Action.VERIFY_EMAIL, null,
                            java.util.Map.of("userId", userId, "code", code))
            );

            Platform.runLater(() -> {
                verifyBtn.setDisable(false);
                verifyBtn.setText("Verifier mon email");

                if (resp.isSuccess()) {
                    showFeedback(feedbackLabel,
                            "Email verifie ! Redirection...", true);
                    // Pause puis redirige vers login
                    javafx.animation.PauseTransition pause =
                            new javafx.animation.PauseTransition(
                                    javafx.util.Duration.seconds(1.5));
                    pause.setOnFinished(e -> NavigationManager.showLogin());
                    pause.play();
                } else {
                    showFeedback(feedbackLabel,
                            resp.getMessage() != null
                                    ? resp.getMessage()
                                    : "Code incorrect.", false);
                }
            });
        }).start();
    }

    private void showFeedback(Label lbl, String msg, boolean success) {
        lbl.setText(msg);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " +
                (success ? "#166534" : "#991b1b") + ";");
        lbl.setVisible(true);
        lbl.setManaged(true);
    }
}