package com.chrionline.client.ui;

import com.chrionline.client.controller.ProductController;
import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProductListScreen {

    private static final double MAX_W = 1200;
    private static final double H_PAD = 40;

    private final ProductController productController = new ProductController();
    private final Gson              gson              = new Gson();

    private List<Map<String, Object>> allProducts = new ArrayList<>();
    private FlowPane                  productGrid;
    private TextField                 searchField;
    private ComboBox<String>          categoryBox;
    private Slider                    priceSlider;
    private Label                     priceValueLabel;
    private Label                     statusLabel;

    // badge labels — kept as fields so they can be updated
    private Label cartCountLabel  = new Label("0");
    private Label notifCountLabel = new Label("0");

    public Node getView() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F5F6FA;");
        root.setTop(buildHeader());
        root.setLeft(buildSidebar());
        root.setCenter(buildCenter());
        Platform.runLater(this::loadAllProducts);
        return root;
    }

    // ═══════════════════════════════════════════════════════
    //  HEADER — copie exacte de LandingPage.buildHeader()
    //  + champ de recherche ajouté
    // ═══════════════════════════════════════════════════════

    private StackPane buildHeader() {
        StackPane w = new StackPane();
        w.getStyleClass().add("header-bar");
        w.setMinWidth(0);

        HBox inner = new HBox(16);
        inner.setMaxWidth(MAX_W);
        inner.setPadding(new Insets(0, H_PAD, 0, H_PAD));
        inner.setMinHeight(70);
        inner.setAlignment(Pos.CENTER_LEFT);

        // ── Logo ─────────────────────────────────────────
        HBox logo = new HBox(8);
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setCursor(javafx.scene.Cursor.HAND);
        logo.setOnMouseClicked(e -> NavigationManager.showLanding());
        try {
            ImageView img = new ImageView(new Image(
                    Objects.requireNonNull(
                            getClass().getResourceAsStream("/images/logo.png"))));
            img.setFitHeight(34);
            img.setPreserveRatio(true);
            img.setSmooth(true);
            logo.getChildren().add(img);
        } catch (Exception ignored) {}

        Label l1 = new Label("Chri");
        l1.getStyleClass().add("header-logo-text");
        Label l2 = new Label("Online");
        l2.getStyleClass().add("header-logo-accent");
        logo.getChildren().addAll(l1, l2);

        // ── Nav ──────────────────────────────────────────
        HBox nav = new HBox(4);
        nav.setAlignment(Pos.CENTER);

        Button nHome   = navBtn("Accueil",   false);
        Button nProd   = navBtn("Produits",  true);    // active
        Button nOrders = navBtn("Commandes", false);
        Button nAbout  = navBtn("A propos",  false);

        nHome.setOnAction(e   -> NavigationManager.showLanding());
        nProd.setOnAction(e   -> NavigationManager.showProductList());
        nOrders.setOnAction(e -> NavigationManager.showOrders());
        nav.getChildren().addAll(nHome, nProd, nOrders, nAbout);

        // ── Search field (ajout) ──────────────────────────
        searchField = new TextField();
        searchField.setPromptText("Rechercher...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, o, n) -> {
            if (n.isBlank()) applyLocalFilters();
            else searchOnServer(n.trim());
        });

        // ── Notification bell avec badge ──────────────────
        StackPane notifStack = new StackPane();
        Button notifBtn = new Button("\uD83D\uDD14");
        notifBtn.getStyleClass().add("header-icon-btn");
        notifBtn.setOnAction(e -> NavigationManager.showNotifications());

        StackPane notifBadge = new StackPane();
        notifBadge.getStyleClass().add("notification-badge");
        notifBadge.setMaxSize(18, 18);
        notifBadge.setMinSize(18, 18);
        notifCountLabel.getStyleClass().add("notification-badge-text");
        notifCountLabel.setStyle("-fx-font-size: 9px;");
        notifBadge.getChildren().add(notifCountLabel);
        notifBadge.setVisible(false);
        StackPane.setAlignment(notifBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(notifBadge, new Insets(-4, -4, 0, 0));
        notifStack.getChildren().addAll(notifBtn, notifBadge);

        // ── Cart avec badge ───────────────────────────────
        StackPane cartStack = new StackPane();
        Button cartBtn2 = new Button("\uD83D\uDED2");
        cartBtn2.getStyleClass().add("header-icon-btn");
        cartBtn2.setOnAction(e -> NavigationManager.showCart());

        StackPane cartBadge = new StackPane();
        cartBadge.getStyleClass().add("notification-badge");
        cartBadge.setMaxSize(18, 18);
        cartBadge.setMinSize(18, 18);
        cartCountLabel.getStyleClass().add("notification-badge-text");
        cartCountLabel.setStyle("-fx-font-size: 9px;");
        cartBadge.getChildren().add(cartCountLabel);
        cartBadge.setVisible(false);
        StackPane.setAlignment(cartBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(cartBadge, new Insets(-4, -4, 0, 0));
        cartStack.getChildren().addAll(cartBtn2, cartBadge);

        // ── User avatar button ────────────────────────────
        Button userBtn = new Button();
        userBtn.getStyleClass().add("header-user-btn");

        String fn = NavigationManager.getFirstName();
        String ln = NavigationManager.getLastName();
        String initials = "";
        if (fn != null && !fn.isEmpty()) initials += fn.charAt(0);
        if (ln != null && !ln.isEmpty()) initials += ln.charAt(0);
        if (initials.isEmpty()) initials = "U";

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("header-avatar");
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);

        Label initLabel = new Label(initials.toUpperCase());
        initLabel.getStyleClass().add("header-avatar-text");
        avatar.getChildren().add(initLabel);

        String displayName = (fn != null && !fn.isEmpty())
                ? fn : NavigationManager.getUsername();
        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("header-user-name");

        HBox userContent = new HBox(8, avatar, nameLabel);
        userContent.setAlignment(Pos.CENTER);
        userBtn.setGraphic(userContent);
        userBtn.setText(null);
        userBtn.setOnAction(e -> NavigationManager.showProfile());

        // ── Layout ───────────────────────────────────────
        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);

        HBox actions = new HBox(10,
                searchField, notifStack, cartStack, userBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        inner.getChildren().addAll(logo, sp1, nav, sp2, actions);
        w.getChildren().add(inner);
        return w;
    }

    private Button navBtn(String text, boolean active) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-link");
        if (active) b.getStyleClass().add("nav-link-active");
        return b;
    }

    // ═══════════════════════════════════════════════════════
    //  SIDEBAR
    // ═══════════════════════════════════════════════════════

    private VBox buildSidebar() {
        VBox side = new VBox(18);
        side.setPrefWidth(220);
        side.setPadding(new Insets(28, 20, 28, 20));
        side.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-width: 0 1 0 0;"
        );

        Label title = new Label("Filtres");
        title.setStyle(
                "-fx-font-size: 16;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #3b0764;"
        );

        Label catLbl = new Label("Categorie");
        catLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12;");

        categoryBox = new ComboBox<>();
        categoryBox.setMaxWidth(Double.MAX_VALUE);
        categoryBox.setValue("Toutes");
        categoryBox.setOnAction(e -> applyLocalFilters());

        Label priceLbl = new Label("Prix maximum");
        priceLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12;");

        priceValueLabel = new Label("Tous");
        priceValueLabel.setStyle(
                "-fx-text-fill: #3b0764;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13;"
        );

        priceSlider = new Slider(0, 5000, 5000);
        priceSlider.setBlockIncrement(100);
        priceSlider.valueProperty().addListener((obs, o, n) -> {
            double v = n.doubleValue();
            priceValueLabel.setText(v >= priceSlider.getMax()
                    ? "Tous" : String.format("<= %.0f MAD", v));
            applyLocalFilters();
        });

        Button resetBtn = new Button("Reinitialiser");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.getStyleClass().add("nav-link");
        resetBtn.setOnAction(e -> {
            searchField.clear();
            categoryBox.setValue("Toutes");
            priceSlider.setValue(priceSlider.getMax());
            displayProducts(allProducts);
        });

        side.getChildren().addAll(
                title, catLbl, categoryBox,
                priceLbl, priceValueLabel, priceSlider,
                resetBtn
        );
        return side;
    }

    // ═══════════════════════════════════════════════════════
    //  CENTRE
    // ═══════════════════════════════════════════════════════

    private VBox buildCenter() {
        VBox center = new VBox(20);
        center.setPadding(new Insets(28));
        center.setStyle("-fx-background-color: #F5F6FA;");

        Label sectionTitle = new Label("Nos Produits");
        sectionTitle.setStyle(
                "-fx-font-size: 22;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #3b0764;"
        );

        statusLabel = new Label("Chargement des produits...");
        statusLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13;");

        productGrid = new FlowPane();
        productGrid.setHgap(24);
        productGrid.setVgap(28);
        productGrid.setStyle("-fx-background-color: transparent;");

        ScrollPane scroll = new ScrollPane(productGrid);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;"
        );
        VBox.setVgrow(scroll, Priority.ALWAYS);

        center.getChildren().addAll(sectionTitle, statusLabel, scroll);
        return center;
    }

    // ═══════════════════════════════════════════════════════
    //  RÉSEAU
    // ═══════════════════════════════════════════════════════

    private void loadAllProducts() {
        new Thread(() -> {
            if (!ServerConnection.getInstance().isConnected())
                ServerConnection.getInstance().connect();

            Response resp = productController.getAll(NavigationManager.getToken());

            if (resp.isSuccess()) {
                List<Map<String, Object>> products = castToList(resp.getData());

                List<String> cats = new ArrayList<>();
                cats.add("Toutes");
                for (Map<String, Object> p : products) {
                    String c = getStr(p, "categoryName");
                    if (!c.isEmpty() && !cats.contains(c)) cats.add(c);
                }

                double maxPrice = products.stream()
                        .mapToDouble(p -> getDbl(p, "price"))
                        .max().orElse(5000);

                allProducts = products;

                Platform.runLater(() -> {
                    categoryBox.getItems().setAll(cats);
                    categoryBox.setValue("Toutes");
                    priceSlider.setMax(maxPrice);
                    priceSlider.setValue(maxPrice);
                    statusLabel.setVisible(false);
                    displayProducts(products);
                });
            } else {
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur : " + resp.getMessage());
                    statusLabel.setVisible(true);
                });
            }
        }).start();
    }

    private void searchOnServer(String keyword) {
        new Thread(() -> {
            Response resp = productController.search(
                    NavigationManager.getToken(), keyword);
            if (resp.isSuccess()) {
                List<Map<String, Object>> results = castToList(resp.getData());
                Platform.runLater(() -> displayProducts(filterByPrice(results)));
            }
        }).start();
    }

    private void applyLocalFilters() {
        if (searchField != null && !searchField.getText().isBlank()) return;
        List<Map<String, Object>> filtered = new ArrayList<>(allProducts);
        String cat = categoryBox.getValue();
        if (cat != null && !cat.equals("Toutes"))
            filtered.removeIf(p -> !cat.equals(getStr(p, "categoryName")));
        displayProducts(filterByPrice(filtered));
    }

    private List<Map<String, Object>> filterByPrice(List<Map<String, Object>> list) {
        if (priceSlider == null || priceSlider.getValue() >= priceSlider.getMax())
            return list;
        double max = priceSlider.getValue();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> p : list)
            if (getDbl(p, "price") <= max) out.add(p);
        return out;
    }

    // ═══════════════════════════════════════════════════════
    //  CARTES — mêmes classes CSS que LandingPage
    // ═══════════════════════════════════════════════════════

    private void displayProducts(List<Map<String, Object>> products) {
        productGrid.getChildren().clear();
        if (products.isEmpty()) {
            Label empty = new Label("Aucun produit trouve.");
            empty.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14;");
            productGrid.getChildren().add(empty);
            return;
        }
        for (Map<String, Object> p : products)
            productGrid.getChildren().add(buildCard(p));
    }

    private VBox buildCard(Map<String, Object> product) {
        int     id      = getInt(product, "productId");
        String  name    = getStr(product, "name");
        double  price   = getDbl(product, "price");
        int     stock   = getInt(product, "stockQuantity");
        String  cat     = getStr(product, "categoryName");
        boolean inStock = stock > 0;

        VBox card = new VBox(6);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(240);
        card.setMaxWidth(240);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> NavigationManager.showProductDetail(id));

        // Image placeholder (lettre initiale) — même que LandingPage
        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("product-card-image-container");
        imgBox.setPrefHeight(160);
        imgBox.setMinHeight(160);
        Label letter = new Label(
                name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        letter.getStyleClass().add("product-image-letter");
        imgBox.getChildren().add(letter);

        VBox body = new VBox(6);
        body.getStyleClass().add("product-card-body");

        Label catL = new Label(cat.toUpperCase());
        catL.getStyleClass().add("product-card-category");

        Label nameL = new Label(name);
        nameL.getStyleClass().add("product-card-name");
        nameL.setWrapText(true);
        nameL.setMaxWidth(220);

        Label priceL = new Label(String.format("%.2f MAD", price));
        priceL.getStyleClass().add("product-card-price");

        Label stockL = new Label();
        if (stock > 10) {
            stockL.setText("En stock");
            stockL.getStyleClass().add("product-card-stock");
        } else if (stock > 0) {
            stockL.setText("Plus que " + stock + " en stock");
            stockL.getStyleClass().add("product-card-stock-low");
        } else {
            stockL.setText("Rupture de stock");
            stockL.getStyleClass().add("product-card-stock-out");
        }

        Button btn = new Button("Voir le produit");
        btn.getStyleClass().add("product-card-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setDisable(!inStock);
        btn.setOnAction(e -> NavigationManager.showProductDetail(id));

        body.getChildren().addAll(catL, nameL, priceL, stockL, btn);
        card.getChildren().addAll(imgBox, body);
        return card;
    }

    // ═══════════════════════════════════════════════════════
    //  LOGOUT
    // ═══════════════════════════════════════════════════════

    private void handleLogout() {
        new Thread(() -> {
            try {
                new com.chrionline.client.controller.AuthController()
                        .logout(NavigationManager.getToken());
            } catch (Exception ignored) {}
            ServerConnection.getInstance().disconnect();
            NavigationManager.clearSession();
            Platform.runLater(NavigationManager::showLanding);
        }).start();
    }

    // ═══════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castToList(Object obj) {
        if (obj == null) return new ArrayList<>();
        if (obj instanceof List) return (List<Map<String, Object>>) obj;
        return gson.fromJson(gson.toJson(obj),
                new TypeToken<List<Map<String, Object>>>(){}.getType());
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