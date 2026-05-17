package com.chrionline.client.ui;

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

public class ChriOnlineApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        StackPane root = new StackPane();
        Scene scene = new Scene(root, 1280, 800);

        // Chargement robuste du CSS
        String css = resolveStylesheetUrl();
        if (css != null) {
            System.out.println("[CSS] Charge depuis : " + css);
            scene.getStylesheets().setAll(css);
        } else {
            System.err.println("[UI] style.css introuvable.");
        }

        // Icône
        try {
            Image icon = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/images/logo.png")));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("[UI] Logo not found, skipping icon.");
        }

        stage.setTitle("ChriOnline — Votre boutique en ligne");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.centerOnScreen();

        // Initialiser le NavigationManager
        NavigationManager.initialize(root, scene);

        // Démarrer par le Splash Screen
        NavigationManager.showSplash();

        stage.show();

        // Forcer application CSS après affichage
        Platform.runLater(() -> {
            if (scene.getRoot() != null) {
                scene.getRoot().applyCss();
                scene.getRoot().layout();
            }
        });
    }

    private String resolveStylesheetUrl() {
        try {
            // 1) Mode développement IntelliJ : CSS source → temp file
            File sourceCss = new File("chrionline-client/src/main/resources/css/style.css");
            if (sourceCss.exists()) {
                return copyToTempAndGetUrl(sourceCss.toPath());
            }

            // 2) Fallback classpath → temp file
            URL resource = getClass().getResource("/css/style.css");
            if (resource != null) {
                Path tempFile = Files.createTempFile("chrionline-style-", ".css");
                tempFile.toFile().deleteOnExit();

                try (var in = resource.openStream()) {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                return tempFile.toUri().toString();
            }

        } catch (Exception e) {
            System.err.println("[UI] Erreur chargement CSS : " + e.getMessage());
        }

        return null;
    }

    private String copyToTempAndGetUrl(Path source) throws IOException {
        Path tempFile = Files.createTempFile("chrionline-style-", ".css");
        tempFile.toFile().deleteOnExit();
        Files.copy(source, tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile.toUri().toString();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}