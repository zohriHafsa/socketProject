package com.chrionline.client;

import com.chrionline.client.ui.ClientApp;

/**
 * Launcher sans dépendance directe à Application.
 * Nécessaire quand le module JavaFX n'est pas dans le module-path au démarrage.
 */
public class ClientLauncher {
    public static void main(String[] args) {
        ClientApp.main(args);
    }
}