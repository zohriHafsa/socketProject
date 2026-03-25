package com.chrionline.client.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.enums.Action;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

public class ProductListScreen {

    private static final double MAX_W = 1200;
    private static final double H_PAD = 40;

    private final BorderPane root;
    private FlowPane         productsGrid;
    private StackPane        loadingOverlay;
    private Label            resultLabel;

    private List<Map<String, Object>> allProducts = new ArrayList<>();
    private String currentCategory = "Tous";
    private String currentSort = "default";

    public ProductListScreen() {
        root = new BorderPane();
        root.getStyleClass().add("page-root");
        root.setTop(buildHeader());

        VBox main = new VBox(20);
        main.setMaxWidth(MAX_W);
        main.setPadding(new Insets(30, H_PAD, 50, H_PAD));
        main.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Catalogue complet");
        title.getStyleClass().add("page-title");

        Node filterBar = buildFilterBar();

        productsGrid = new FlowPane();
        productsGrid.setHgap(20); productsGrid.setVgap(24);
        productsGrid.setAlignment(Pos.CENTER);
        productsGrid.setPrefWrapLength(MAX_W - H_PAD * 2);

        main.getChildren().addAll(title, filterBar, productsGrid);

        StackPane centered = new StackPane(main);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setMinWidth(0);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        loadingOverlay = buildLoading();
        root.setCenter(new StackPane(scroll, loadingOverlay));

        loadProducts();
        Platform.runLater(() -> {
            root.setOpacity(0);
            FadeTransition f = new FadeTransition(Duration.millis(400), root);
            f.setToValue(1); f.play();
        });
    }

    public Node getView() { return root; }

    private StackPane buildHeader() {
        StackPane w = new StackPane();
        w.getStyleClass().add("page-header-bar");
        w.setMinWidth(0);
        HBox inner = new HBox(16);
        inner.setMaxWidth(MAX_W);
        inner.setPadding(new Insets(0, H_PAD, 0, H_PAD));
        inner.setMinHeight(70);
        inner.setAlignment(Pos.CENTER_LEFT);

        Button back = new Button("\u2190 Accueil");
        back.getStyleClass().add("page-back-btn");
        back.setOnAction(e -> NavigationManager.showLanding());

        Label title = new Label("Tous les produits");
        title.getStyleClass().add("page-title");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        TextField search = new TextField();
        search.setPromptText("Rechercher...");
        search.getStyleClass().add("search-field");
        search.setPrefWidth(220);
        search.setOnAction(e -> {
            String kw = search.getText().trim();
            if (!kw.isEmpty()) searchProducts(kw);
            else displayProducts(allProducts);
        });

        Button cartBtn = new Button("Panier");
        cartBtn.getStyleClass().add("page-back-btn");
        cartBtn.setOnAction(e -> NavigationManager.showCart());

        inner.getChildren().addAll(back, title, sp, search, cartBtn);
        w.getChildren().add(inner);
        return w;
    }

    private Node buildFilterBar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("catalog-filter-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        // Categories
        String[] cats = {"Tous", "Smartphones", "Laptops", "T-Shirts", "Pantalons", "Livres Tech", "Romans", "Sports"};
        HBox catChips = new HBox(6);
        catChips.setAlignment(Pos.CENTER_LEFT);

        List<Button> catBtns = new ArrayList<>();
        for (String cat : cats) {
            Button chip = new Button(cat);
            chip.getStyleClass().add("category-chip");
            chip.setStyle("-fx-font-size:11px;-fx-padding:6 14;");
            if ("Tous".equals(cat)) chip.getStyleClass().add("category-chip-active");
            chip.setOnAction(e -> {
                currentCategory = cat;
                catBtns.forEach(b -> b.getStyleClass().remove("category-chip-active"));
                chip.getStyleClass().add("category-chip-active");
                filterAndSort();
            });
            catBtns.add(chip);
            catChips.getChildren().add(chip);
        }

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        // Sort
        HBox sortBox = new HBox(6);
        sortBox.setAlignment(Pos.CENTER_RIGHT);

        Label sortLabel = new Label("Trier :");
        sortLabel.getStyleClass().add("page-subtitle");

        String[][] sorts = {{"default", "Défaut"}, {"price-asc", "Prix \u2191"}, {"price-desc", "Prix \u2193"}, {"name", "A-Z"}};
        List<Button> sortBtns = new ArrayList<>();
        for (String[] s : sorts) {
            Button sb = new Button(s[1]);
            sb.getStyleClass().add("catalog-sort-btn");
            if ("default".equals(s[0])) sb.getStyleClass().add("catalog-sort-active");
            sb.setOnAction(e -> {
                currentSort = s[0];
                sortBtns.forEach(b -> b.getStyleClass().remove("catalog-sort-active"));
                sb.getStyleClass().add("catalog-sort-active");
                filterAndSort();
            });
            sortBtns.add(sb);
            sortBox.getChildren().add(sb);
        }
        sortBox.getChildren().add(0, sortLabel);

        resultLabel = new Label("");
        resultLabel.getStyleClass().add("catalog-result-text");

        bar.getChildren().addAll(catChips, sp, sortBox);

        VBox wrapper = new VBox(8, bar, resultLabel);
        return wrapper;
    }

    @SuppressWarnings("unchecked")
    private void loadProducts() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_PRODUCTS, null, null))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null) {
                        allProducts = (List<Map<String, Object>>) res.getData();
                        filterAndSort();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showLoading(false));
                    return null;
                });
    }

    @SuppressWarnings("unchecked")
    private void searchProducts(String kw) {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.SEARCH_PRODUCTS, null, Map.of("keyword", kw)))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null)
                        displayProducts((List<Map<String, Object>>) res.getData());
                }))
                .exceptionally(ex -> { Platform.runLater(() -> showLoading(false)); return null; });
    }

    private void filterAndSort() {
        List<Map<String, Object>> filtered = allProducts;
        if (!"Tous".equals(currentCategory)) {
            filtered = filtered.stream()
                    .filter(p -> currentCategory.equalsIgnoreCase(str(p.get("categoryName"))))
                    .collect(Collectors.toList());
        }
        switch (currentSort) {
            case "price-asc":  filtered.sort(Comparator.comparingDouble(p -> dbl(p.get("price")))); break;
            case "price-desc": filtered.sort((a, b) -> Double.compare(dbl(b.get("price")), dbl(a.get("price")))); break;
            case "name":       filtered.sort(Comparator.comparing(p -> str(p.get("name")))); break;
        }
        displayProducts(filtered);
    }

    private void displayProducts(List<Map<String, Object>> products) {
        productsGrid.getChildren().clear();
        resultLabel.setText(products.size() + " produit(s) trouvé(s)");

        if (products.isEmpty()) {
            VBox e = new VBox(12); e.setAlignment(Pos.CENTER); e.setPadding(new Insets(40));
            Label ic = new Label("\uD83D\uDCE6"); ic.getStyleClass().add("empty-state-icon");
            Label tx = new Label("Aucun produit"); tx.getStyleClass().add("empty-state-title");
            e.getChildren().addAll(ic, tx);
            productsGrid.getChildren().add(e);
            return;
        }

        List<Node> cards = new ArrayList<>();
        for (Map<String, Object> p : products) {
            Node card = buildCard(p);
            card.setOpacity(0); card.setTranslateY(30); card.setScaleX(0.95); card.setScaleY(0.95);
            productsGrid.getChildren().add(card);
            cards.add(card);
        }

        for (int i = 0; i < cards.size(); i++) {
            Node c = cards.get(i); int delay = i * 50;
            FadeTransition f = new FadeTransition(Duration.millis(400), c); f.setToValue(1); f.setDelay(Duration.millis(delay));
            TranslateTransition s = new TranslateTransition(Duration.millis(400), c); s.setToY(0); s.setDelay(Duration.millis(delay)); s.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition sc = new ScaleTransition(Duration.millis(400), c); sc.setToX(1); sc.setToY(1); sc.setDelay(Duration.millis(delay));
            new ParallelTransition(f, s, sc).play();
        }
    }

    private Node buildCard(Map<String, Object> p) {
        VBox card = new VBox(0);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(240); card.setMaxWidth(240);

        DropShadow rest = new DropShadow(); rest.setColor(Color.rgb(0,0,0,0.05)); rest.setRadius(8); rest.setOffsetY(2);
        DropShadow hover = new DropShadow(); hover.setColor(Color.rgb(108,52,131,0.15)); hover.setRadius(20); hover.setOffsetY(10);
        card.setEffect(rest);

        String name = str(p.get("name")); String cat = str(p.get("categoryName"));
        double price = dbl(p.get("price")); int stock = intV(p.get("stockQuantity"));

        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("product-card-image-container");
        imgBox.setPrefHeight(170); imgBox.setMinHeight(170);
        Label letter = new Label(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        letter.getStyleClass().add("product-image-letter");
        imgBox.getChildren().add(letter);

        VBox body = new VBox(5); body.getStyleClass().add("product-card-body");
        Label catL = new Label(cat.toUpperCase()); catL.getStyleClass().add("product-card-category");
        Label nameL = new Label(name); nameL.getStyleClass().add("product-card-name"); nameL.setMaxWidth(210); nameL.setWrapText(true); nameL.setMaxHeight(36);
        Label priceL = new Label(String.format("%.2f \u20AC", price)); priceL.getStyleClass().add("product-card-price");

        Label stockL = new Label();
        if (stock > 10) { stockL.setText("En stock"); stockL.getStyleClass().add("product-card-stock"); }
        else if (stock > 0) { stockL.setText(stock + " restant(s)"); stockL.getStyleClass().add("product-card-stock-low"); }
        else { stockL.setText("Rupture"); stockL.getStyleClass().add("product-card-stock-out"); }

        Button viewBtn = new Button("Voir le produit");
        viewBtn.getStyleClass().add("product-card-btn");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(e -> NavigationManager.showProductDetail(intV(p.get("productId"))));

        body.getChildren().addAll(catL, nameL, priceL, stockL, viewBtn);
        card.getChildren().addAll(imgBox, body);

        card.setOnMouseEntered(e -> {
            card.setEffect(hover);
            TranslateTransition u = new TranslateTransition(Duration.millis(200), card); u.setToY(-6); u.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition g = new ScaleTransition(Duration.millis(200), card); g.setToX(1.02); g.setToY(1.02);
            new ParallelTransition(u, g).play();
        });
        card.setOnMouseExited(e -> {
            card.setEffect(rest);
            TranslateTransition d = new TranslateTransition(Duration.millis(250), card); d.setToY(0);
            ScaleTransition s = new ScaleTransition(Duration.millis(250), card); s.setToX(1); s.setToY(1);
            new ParallelTransition(d, s).play();
        });

        return card;
    }

    private StackPane buildLoading() {
        StackPane o = new StackPane(); o.getStyleClass().add("loading-modern-overlay"); o.setVisible(false);
        VBox c = new VBox(20); c.getStyleClass().add("loading-card"); c.setAlignment(Pos.CENTER);
        c.setMaxWidth(200); c.setMaxHeight(140);
        ProgressIndicator pi = new ProgressIndicator(); pi.getStyleClass().add("loading-spinner"); pi.setMaxSize(40,40);
        Label t = new Label("Chargement..."); t.getStyleClass().add("loading-text");
        c.getChildren().addAll(pi, t); o.getChildren().add(c); return o;
    }

    private void showLoading(boolean b) {
        Platform.runLater(() -> {
            if (b) { loadingOverlay.setOpacity(0); loadingOverlay.setVisible(true);
                FadeTransition fi = new FadeTransition(Duration.millis(200), loadingOverlay); fi.setToValue(1); fi.play();
            } else { FadeTransition fo = new FadeTransition(Duration.millis(200), loadingOverlay);
                fo.setToValue(0); fo.setOnFinished(e -> loadingOverlay.setVisible(false)); fo.play(); }
        });
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }
    private int intV(Object o) { if(o==null) return 0; if(o instanceof Double) return ((Double)o).intValue(); if(o instanceof Integer) return (Integer)o; try{return Integer.parseInt(o.toString());}catch(Exception e){return 0;} }
    private double dbl(Object o) { if(o==null) return 0; if(o instanceof Double) return (Double)o; try{return Double.parseDouble(o.toString());}catch(Exception e){return 0;} }
}