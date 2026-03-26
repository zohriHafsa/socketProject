package com.chrionline.client.ui;

import com.chrionline.client.controller.ProductController;
import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;

public class ProductDetailScreen {

    private final int               productId;
    private final ProductController productController = new ProductController();
    private final Gson              gson              = new Gson();

    public ProductDetailScreen(int productId) {
        this.productId = productId;
    }

    public Node getView() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #5b0ea6;");
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(40));

        VBox card = new VBox(16);
        card.setMaxWidth(700);
        card.setPadding(new Insets(36, 40, 36, 40));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #c4b5fd;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;"
        );

        // Bouton retour
        Button backBtn = new Button("<- Retour aux produits");
        backBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #6d28d9;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 13;" +
                        "-fx-font-weight: bold;"
        );
        backBtn.setOnAction(e -> NavigationManager.showProductList());

        // Chargement
        Label loadingLabel = new Label("Chargement du produit...");
        loadingLabel.setStyle("-fx-text-fill: #6d28d9; -fx-font-size: 13;");

        // Badge
        Label badgeLabel = new Label();
        badgeLabel.setStyle(
                "-fx-background-color: #6d28d9;" +
                        "-fx-background-radius: 6;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 4 14;"
        );
        badgeLabel.setVisible(false);
        badgeLabel.setManaged(false);

        // Nom
        Label nameLabel = new Label();
        nameLabel.setStyle(
                "-fx-text-fill: #3b0764;" +
                        "-fx-font-size: 26;" +
                        "-fx-font-weight: bold;"
        );
        nameLabel.setWrapText(true);
        nameLabel.setVisible(false);
        nameLabel.setManaged(false);

        // Description
        Label descLabel = new Label();
        descLabel.setStyle("-fx-text-fill: #5b21b6; -fx-font-size: 14;");
        descLabel.setWrapText(true);
        descLabel.setVisible(false);
        descLabel.setManaged(false);

        // Prix
        Label priceLabel = new Label();
        priceLabel.setStyle(
                "-fx-text-fill: #b45309;" +
                        "-fx-font-size: 28;" +
                        "-fx-font-weight: bold;"
        );
        priceLabel.setVisible(false);
        priceLabel.setManaged(false);

        // Stock
        Label stockLabel = new Label();
        stockLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
        stockLabel.setVisible(false);
        stockLabel.setManaged(false);

        // Séparateur
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #e9d5ff;");

        // Quantité
        Label qtyLabel = new Label("Quantite :");
        qtyLabel.setStyle("-fx-text-fill: #6d28d9; -fx-font-weight: bold; -fx-font-size: 13;");

        Spinner<Integer> qtySpinner = new Spinner<>(1, 1, 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(90);

        HBox qtyRow = new HBox(12, qtyLabel, qtySpinner);
        qtyRow.setAlignment(Pos.CENTER_LEFT);
        qtyRow.setVisible(false);
        qtyRow.setManaged(false);

        // Feedback
        Label feedbackLabel = new Label();
        feedbackLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
        feedbackLabel.setWrapText(true);
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);

        // Bouton ajouter
        Button addCartBtn = new Button("Ajouter au panier");
        addCartBtn.setMaxWidth(Double.MAX_VALUE);
        addCartBtn.setStyle(
                "-fx-background-color: #f59e0b;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 15;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 13 0;" +
                        "-fx-cursor: hand;"
        );
        addCartBtn.setDisable(true);

        card.getChildren().addAll(
                backBtn, loadingLabel,
                badgeLabel, nameLabel, descLabel,
                priceLabel, stockLabel, sep,
                qtyRow, feedbackLabel, addCartBtn
        );

        root.getChildren().add(card);

        // Charger les détails depuis le serveur
        new Thread(() -> {
            if (!ServerConnection.getInstance().isConnected())
                ServerConnection.getInstance().connect();

            Response resp = productController.getDetail(
                    NavigationManager.getToken(), productId);

            Platform.runLater(() -> {
                loadingLabel.setVisible(false);
                loadingLabel.setManaged(false);

                if (resp.isSuccess()) {
                    // getData() = Map direct du produit (ProductDAO.mapProduct)
                    Map<String, Object> p = castToMap(resp.getData());

                    String name  = getStr(p, "name");
                    String desc  = getStr(p, "description");
                    double price = getDbl(p, "price");
                    int    stock = getInt(p, "stockQuantity"); // clé exacte DAO
                    String cat   = getStr(p, "categoryName");  // clé exacte DAO

                    if (desc.isEmpty() || desc.equals("..."))
                        desc = "Aucune description disponible.";

                    badgeLabel.setText(cat.isEmpty() ? "Produit" : cat);
                    show(badgeLabel);

                    nameLabel.setText(name);
                    show(nameLabel);

                    descLabel.setText(desc);
                    show(descLabel);

                    priceLabel.setText(String.format("%.2f MAD", price));
                    show(priceLabel);

                    boolean inStock = stock > 0;
                    stockLabel.setText(inStock
                            ? "En stock - " + stock + " disponibles"
                            : "Rupture de stock");
                    stockLabel.setStyle(
                            "-fx-font-size: 13; -fx-font-weight: bold;" +
                                    "-fx-text-fill: " + (inStock ? "#166534" : "#991b1b") + ";"
                    );
                    show(stockLabel);

                    qtySpinner.setValueFactory(
                            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                                    1, Math.max(1, stock), 1));
                    show(qtyRow);

                    addCartBtn.setDisable(!inStock);
                    addCartBtn.setOnMouseEntered(e -> {
                        if (!addCartBtn.isDisabled())
                            addCartBtn.setStyle(
                                    addCartBtn.getStyle().replace("#f59e0b", "#d97706"));
                    });
                    addCartBtn.setOnMouseExited(e -> {
                        if (!addCartBtn.isDisabled())
                            addCartBtn.setStyle(
                                    addCartBtn.getStyle().replace("#d97706", "#f59e0b"));
                    });
                    addCartBtn.setOnAction(e ->
                            handleAddToCart(qtySpinner, addCartBtn, feedbackLabel));

                } else {
                    loadingLabel.setText("Erreur : " + resp.getMessage());
                    show(loadingLabel);
                }
            });
        }).start();

        return root;
    }

    private void handleAddToCart(Spinner<Integer> qtySpinner,
                                 Button addCartBtn, Label feedbackLabel) {
        int userId   = NavigationManager.getUserId();
        int quantity = qtySpinner.getValue();

        if (userId == -1) {
            showFeedback(feedbackLabel, "Vous devez etre connecte.", false);
            return;
        }

        addCartBtn.setDisable(true);
        addCartBtn.setText("Ajout en cours...");

        new Thread(() -> {
            Request req = new Request(
                    Action.ADD_TO_CART,
                    NavigationManager.getToken(),
                    Map.of("userId", userId, "productId", productId, "quantity", quantity)
            );
            Response resp = ServerConnection.getInstance().send(req);

            Platform.runLater(() -> {
                if (resp.isSuccess()) {
                    showFeedback(feedbackLabel, "Produit ajoute au panier !", true);
                    addCartBtn.setText("Ajoute !");
                    addCartBtn.setStyle(
                            "-fx-background-color: #16a34a;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-font-size: 15;" +
                                    "-fx-background-radius: 10;" +
                                    "-fx-padding: 13 0;"
                    );
                } else {
                    showFeedback(feedbackLabel, resp.getMessage(), false);
                    addCartBtn.setDisable(false);
                    addCartBtn.setText("Ajouter au panier");
                    addCartBtn.setStyle(
                            "-fx-background-color: #f59e0b;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-font-size: 15;" +
                                    "-fx-background-radius: 10;" +
                                    "-fx-padding: 13 0;" +
                                    "-fx-cursor: hand;"
                    );
                }
            });
        }).start();
    }

    private void show(Node node) {
        node.setVisible(true);
        node.setManaged(true);
    }

    private void showFeedback(Label lbl, String msg, boolean success) {
        lbl.setText(msg);
        lbl.setStyle(
                "-fx-font-size: 13; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + (success ? "#166534" : "#991b1b") + ";"
        );
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        if (obj == null) return Map.of();
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return gson.fromJson(gson.toJson(obj),
                new TypeToken<Map<String, Object>>(){}.getType());
    }

    private String getStr(Map<String, Object> m, String key) {
        Object v = m.get(key); return v != null ? v.toString() : "";
    }

    private int getInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return 0;
        if (v instanceof Double) return ((Double) v).intValue();
        return Integer.parseInt(v.toString());
    }

    private double getDbl(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return 0.0;
        return Double.parseDouble(v.toString());
    }
}