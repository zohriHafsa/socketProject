package com.chrionline.admin.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class AdminApp extends Application {

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 1152, 720);

        String css = resolveStylesheetUrl();
        if (css != null) {
            scene.getStylesheets().setAll(css);
        }

        try {
            Image icon = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/images/logo.png")));
            stage.getIcons().add(icon);
        } catch (Exception ignored) {
        }

        stage.setTitle("ChriOnline — Administration");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.centerOnScreen();

        AdminNavigationManager.initialize(root, scene);
        AdminNavigationManager.showAdminLogin();

        stage.show();

        Platform.runLater(() -> {
            if (scene.getRoot() != null) {
                scene.getRoot().applyCss();
                scene.getRoot().layout();
            }
        });
    }

    private String resolveStylesheetUrl() {
        try {
            File devCss = new File("chrionline-admin/src/main/resources/css/style.css");
            if (devCss.exists()) {
                return copyToTempAndGetUrl(devCss.toPath());
            }
            URL resource = getClass().getResource("/css/style.css");
            if (resource != null) {
                Path tempFile = Files.createTempFile("chrionline-admin-style-", ".css");
                tempFile.toFile().deleteOnExit();
                try (var in = resource.openStream()) {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }
                return tempFile.toUri().toString();
            }
        } catch (Exception e) {
            System.err.println("[ADMIN] Erreur chargement CSS : " + e.getMessage());
        }
        return null;
    }

    private String copyToTempAndGetUrl(Path source) throws IOException {
        Path tempFile = Files.createTempFile("chrionline-admin-style-", ".css");
        tempFile.toFile().deleteOnExit();
        Files.copy(source, tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile.toUri().toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
