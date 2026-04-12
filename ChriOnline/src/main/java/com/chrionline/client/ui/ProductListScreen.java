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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.io.InputStream;
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

    private Label cartCountLabel  = new Label("0");
    private Label notifCountLabel = new Label("0");

    public Node getView() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F5F6FA;");
        searchField = new TextField();
        root.setTop(new HeaderComponent("products", query -> {
            searchField.setText(query);
            if (query.isBlank()) applyLocalFilters();
            else searchOnServer(query);
        }, null, null).getView());
        root.setLeft(buildSidebar());
        root.setCenter(buildCenter());
        Platform.runLater(this::loadAllProducts);
        return root;
    }

    // ═══════════════════════════════════════════════════════
    //  HEADER
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

        HBox nav = new HBox(4);
        nav.setAlignment(Pos.CENTER);

        Button nHome   = navBtn("Accueil",   false);
        Button nProd   = navBtn("Produits",  true);
        Button nOrders = navBtn("Commandes", false);
        Button nAbout  = navBtn("A propos",  false);

        nHome.setOnAction(e   -> NavigationManager.showLanding());
        nProd.setOnAction(e   -> NavigationManager.showProductList());
        nOrders.setOnAction(e -> NavigationManager.showOrders());
        nav.getChildren().addAll(nHome, nProd, nOrders, nAbout);

        searchField = new TextField();
        searchField.setPromptText("Rechercher...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, o, n) -> {
            if (n.isBlank()) applyLocalFilters();
            else searchOnServer(n.trim());
        });

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
    //  CARTES PRODUIT — avec affichage réel des images
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
        int     id       = getInt(product, "productId");
        String  name     = getStr(product, "name");
        double  price    = getDbl(product, "price");
        int     stock    = getInt(product, "stockQuantity");
        String  cat      = getStr(product, "categoryName");
        String  imageUrl = getStr(product, "imageUrl");
        boolean inStock  = stock > 0;

        // ── Fixed size card ──────────────────────────────
        VBox card = new VBox(0);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(240);
        card.setMinWidth(240);
        card.setMaxWidth(240);
        card.setPrefHeight(340);
        card.setMinHeight(340);
        card.setMaxHeight(340);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> NavigationManager.showProductDetail(id));

        // ── Image — fills 240x160, cropped like object-fit:cover ──
        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("product-card-image-container");
        imgBox.setPrefHeight(160);
        imgBox.setMinHeight(160);
        imgBox.setMaxHeight(160);
        imgBox.setPrefWidth(240);
        imgBox.setMinWidth(240);
        imgBox.setMaxWidth(240);
        imgBox.setStyle("-fx-background-color: transparent;");

        // Clip so nothing overflows the image box
        Rectangle clip = new Rectangle(240, 160);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        imgBox.setClip(clip);

        Image productImage = loadProductImage(imageUrl);

        if (productImage != null && !productImage.isError()) {
            Region imgRegion = new Region();
            imgRegion.setPrefSize(240, 160);
            imgRegion.setMaxSize(240, 160);
            
            BackgroundImage bgImg = new BackgroundImage(
                productImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true)
            );
            imgRegion.setBackground(new Background(bgImg));
            
            Rectangle imgClip = new Rectangle(240, 160);
            imgClip.setArcWidth(12);
            imgClip.setArcHeight(12);
            imgRegion.setClip(imgClip);
            
            imgBox.getChildren().add(imgRegion);
        } else {
            Label letter = new Label(
                    name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
            letter.getStyleClass().add("product-image-letter");
            imgBox.getChildren().add(letter);
        }

        // ── Body — fixed height to keep card static ──────
        VBox body = new VBox(6);
        body.getStyleClass().add("product-card-body");
        body.setPrefHeight(180);
        body.setMinHeight(180);
        body.setMaxHeight(180);
        VBox.setVgrow(body, Priority.ALWAYS);

        Label catL = new Label(cat.toUpperCase());
        catL.getStyleClass().add("product-card-category");

        Label nameL = new Label(name);
        nameL.getStyleClass().add("product-card-name");
        nameL.setWrapText(true);
        nameL.setMaxWidth(220);
        nameL.setMaxHeight(40);

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

        // Spacer to push button to the bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btn = new Button("Voir le produit");
        btn.getStyleClass().add("product-card-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setDisable(!inStock);
        btn.setOnAction(e -> NavigationManager.showProductDetail(id));

        body.getChildren().addAll(catL, nameL, priceL, stockL, spacer, btn);
        card.getChildren().addAll(imgBox, body);
        return card;
    }

    // ═══════════════════════════════════════════════════════
    //  CHARGEMENT D'IMAGE — multiple fallbacks
    // ═══════════════════════════════════════════════════════

    private Image loadProductImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) return null;
        imageUrl = imageUrl.trim();

        // 1. Classpath resource (target/classes/...)
        try {
            InputStream is = getClass().getResourceAsStream(imageUrl);
            if (is != null) {
                Image img = new Image(is);
                if (!img.isError()) return img;
            }
        } catch (Exception ignored) {}

        // 2. File system: src/main/resources + path
        try {
            File f = new File("src/main/resources" + imageUrl);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString());
                if (!img.isError()) return img;
            }
        } catch (Exception ignored) {}

        // 3. Chemin absolu direct
        try {
            File f = new File(imageUrl);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString());
                if (!img.isError()) return img;
            }
        } catch (Exception ignored) {}

        // 4. URL HTTP/HTTPS/file:
        try {
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")
                    || imageUrl.startsWith("file:")) {
                return new Image(imageUrl, true);
            }
        } catch (Exception ignored) {}

        return null;
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