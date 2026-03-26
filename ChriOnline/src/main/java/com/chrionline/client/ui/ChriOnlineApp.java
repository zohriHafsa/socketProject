package com.chrionline.client.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public class ChriOnlineApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        StackPane root = new StackPane();
        Scene scene = new Scene(root, 1280, 800);

        // Chargement du CSS
        String css = Objects.requireNonNull(
                getClass().getResource("/css/style.css")).toExternalForm();
        scene.getStylesheets().add(css);

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
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}