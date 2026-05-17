package com.chrionline.client.ui;

/**
 * Launcher sans dépendance directe à Application.
 * Nécessaire quand le module JavaFX n'est pas dans le module-path au démarrage.
 */
public class Launcher {
    public static void main(String[] args) {
        ChriOnlineApp.main(args);
    }
}