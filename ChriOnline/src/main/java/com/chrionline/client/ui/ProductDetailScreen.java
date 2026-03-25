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
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.*;

public class ProductDetailScreen {

    private static final double MAX_W = 1100;
    private static final double H_PAD = 40;

    private final BorderPane root;
    private final StackPane  contentArea;
    private StackPane        loadingOverlay;
    private final int        productId;
    private int quantity = 1;

    public ProductDetailScreen(int productId) {
        this.productId = productId;
        root = new BorderPane();
        root.getStyleClass().add("page-root");
        root.setTop(buildHeader());

        contentArea = new StackPane();
        contentArea.setAlignment(Pos.TOP_CENTER);
        contentArea.setMinWidth(0);

        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        loadingOverlay = buildLoading();
        root.setCenter(new StackPane(scroll, loadingOverlay));

        loadProduct();
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

        Button back = new Button("\u2190 Boutique");
        back.getStyleClass().add("page-back-btn");
        back.setOnAction(e -> NavigationManager.showLanding());

        Label title = new Label("Détail produit");
        title.getStyleClass().add("page-title");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button cartBtn = new Button("Panier");
        cartBtn.getStyleClass().add("page-back-btn");
        cartBtn.setOnAction(e -> NavigationManager.showCart());

        inner.getChildren().addAll(back, title, sp, cartBtn);
        w.getChildren().add(inner);
        return w;
    }

    @SuppressWarnings("unchecked")
    private void loadProduct() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_PRODUCT_DETAIL, NavigationManager.getToken(),
                        Map.of("productId", productId)))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null) {
                        renderProduct((Map<String, Object>) res.getData());
                    } else {
                        showError("Produit introuvable");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> { showLoading(false); showError("Erreur réseau"); });
                    return null;
                });
    }

    private void renderProduct(Map<String, Object> p) {
        HBox content = new HBox(40);
        content.setMaxWidth(MAX_W);
        content.setPadding(new Insets(40, H_PAD, 60, H_PAD));
        content.setAlignment(Pos.TOP_CENTER);

        String name = str(p.get("name"));
        String desc = str(p.get("description"));
        String cat = str(p.get("categoryName"));
        double price = dbl(p.get("price"));
        int stock = intV(p.get("stockQuantity"));

        // ── Left: image ─────────────────────────────────
        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("detail-image-box");
        imgBox.setMinWidth(420);
        imgBox.setMaxWidth(420);
        imgBox.setMinHeight(400);

        Label letter = new Label(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        letter.getStyleClass().add("detail-image-letter");
        imgBox.getChildren().add(letter);

        // ── Right: info ─────────────────────────────────
        VBox info = new VBox(16);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label catL = new Label(cat.toUpperCase());
        catL.getStyleClass().add("detail-category");

        Label nameL = new Label(name);
        nameL.getStyleClass().add("detail-name");

        Label priceL = new Label(String.format("%.2f \u20AC", price));
        priceL.getStyleClass().add("detail-price");

        Label stockL = new Label();
        if (stock > 10) { stockL.setText("\u2713 En stock (" + stock + " disponibles)"); stockL.getStyleClass().add("detail-stock-ok"); }
        else if (stock > 0) { stockL.setText("\u26A0 Plus que " + stock + " en stock"); stockL.getStyleClass().add("detail-stock-low"); }
        else { stockL.setText("\u2717 Rupture de stock"); stockL.getStyleClass().add("detail-stock-out"); }

        Region div = new Region();
        div.getStyleClass().add("cart-summary-divider");

        Label descTitle = new Label("Description");
        descTitle.getStyleClass().add("checkout-section-title");

        Label descL = new Label(desc.isEmpty() ? "Aucune description disponible." : desc);
        descL.getStyleClass().add("detail-description");

        Region div2 = new Region();
        div2.getStyleClass().add("cart-summary-divider");

        // Quantity selector
        HBox qtyRow = new HBox(12);
        qtyRow.setAlignment(Pos.CENTER_LEFT);

        Label qtyTitle = new Label("Quantité :");
        qtyTitle.getStyleClass().add("page-subtitle");

        Button minusBtn = new Button("-");
        minusBtn.getStyleClass().add("qty-btn");
        Label qtyLabel = new Label("1");
        qtyLabel.getStyleClass().add("qty-label");
        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("qty-btn");

        minusBtn.setOnAction(e -> {
            if (quantity > 1) { quantity--; qtyLabel.setText(String.valueOf(quantity)); }
        });
        plusBtn.setOnAction(e -> {
            if (quantity < stock) { quantity++; qtyLabel.setText(String.valueOf(quantity)); }
        });

        qtyRow.getChildren().addAll(qtyTitle, minusBtn, qtyLabel, plusBtn);

        // Add to cart
        Button addBtn = new Button("Ajouter au panier");
        addBtn.getStyleClass().add("detail-add-btn");
        addBtn.setDisable(stock <= 0);
        addBtn.setOnAction(e -> addToCart(p));

        // Info cards
        HBox infoCards = new HBox(12);
        infoCards.getChildren().addAll(
                infoCard("Livraison", "Express 24-48h"),
                infoCard("Retours", "30 jours"),
                infoCard("Garantie", "2 ans")
        );

        info.getChildren().addAll(catL, nameL, priceL, stockL, div, descTitle, descL,
                div2, qtyRow, addBtn, infoCards);

        content.getChildren().addAll(imgBox, info);

        // Animate
        content.setOpacity(0);
        contentArea.getChildren().setAll(content);

        // Staggered animation
        imgBox.setTranslateX(-30); imgBox.setOpacity(0);
        info.setTranslateX(30); info.setOpacity(0);

        FadeTransition f1 = new FadeTransition(Duration.millis(500), imgBox);
        f1.setToValue(1); f1.setDelay(Duration.millis(100));
        TranslateTransition s1 = new TranslateTransition(Duration.millis(500), imgBox);
        s1.setToX(0); s1.setDelay(Duration.millis(100));
        s1.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition f2 = new FadeTransition(Duration.millis(500), info);
        f2.setToValue(1); f2.setDelay(Duration.millis(200));
        TranslateTransition s2 = new TranslateTransition(Duration.millis(500), info);
        s2.setToX(0); s2.setDelay(Duration.millis(200));
        s2.setInterpolator(Interpolator.EASE_OUT);

        content.setOpacity(1);
        new ParallelTransition(f1, s1, f2, s2).play();
    }

    private VBox infoCard(String title, String value) {
        VBox c = new VBox(4);
        c.getStyleClass().add("detail-info-card");
        c.setAlignment(Pos.CENTER);
        c.setPrefWidth(120);
        Label t = new Label(title); t.getStyleClass().add("detail-info-title");
        Label v = new Label(value); v.getStyleClass().add("detail-info-text");
        c.getChildren().addAll(t, v);
        HBox.setHgrow(c, Priority.ALWAYS);
        return c;
    }

    private void addToCart(Map<String, Object> product) {
        if (!NavigationManager.isLoggedIn()) {
            NavigationManager.showLogin();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", NavigationManager.getUserId());
        data.put("productId", intV(product.get("productId")));
        data.put("quantity", quantity);

        ServerConnection.getInstance()
                .sendAsync(new Request(Action.ADD_TO_CART, NavigationManager.getToken(), data))
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.isSuccess()) showToast("Ajouté au panier !");
                    else showToast(res.getMessage());
                }));
    }

    private void showToast(String msg) {
        Label t = new Label(msg);
        t.getStyleClass().addAll("toast", "toast-text", "toast-success");
        t.setOpacity(0); t.setTranslateY(40);
        StackPane rs = (StackPane) root.getParent();
        if (rs == null) return;
        StackPane.setAlignment(t, Pos.BOTTOM_CENTER);
        StackPane.setMargin(t, new Insets(0, 0, 50, 0));
        rs.getChildren().add(t);
        FadeTransition fi = new FadeTransition(Duration.millis(300), t); fi.setToValue(1);
        TranslateTransition si = new TranslateTransition(Duration.millis(300), t); si.setToY(0); si.setInterpolator(Interpolator.EASE_OUT);
        PauseTransition hold = new PauseTransition(Duration.seconds(3));
        FadeTransition fo = new FadeTransition(Duration.millis(400), t); fo.setToValue(0);
        fo.setOnFinished(e -> rs.getChildren().remove(t));
        new SequentialTransition(new ParallelTransition(fi, si), hold, fo).play();
    }

    private void showError(String msg) {
        VBox e = new VBox(16); e.setAlignment(Pos.CENTER); e.setPadding(new Insets(80));
        Label tx = new Label(msg); tx.getStyleClass().add("empty-state-title");
        Button b = new Button("Retour"); b.getStyleClass().add("btn-primary");
        b.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        b.setOnAction(ev -> NavigationManager.showLanding());
        e.getChildren().addAll(tx, b);
        contentArea.getChildren().setAll(e);
    }

    private StackPane buildLoading() {
        StackPane o = new StackPane(); o.getStyleClass().add("loading-modern-overlay"); o.setVisible(false);
        VBox c = new VBox(20); c.getStyleClass().add("loading-card"); c.setAlignment(Pos.CENTER);
        c.setMaxWidth(200); c.setMaxHeight(140);
        ProgressIndicator pi = new ProgressIndicator(); pi.getStyleClass().add("loading-spinner"); pi.setMaxSize(40, 40);
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
    private int intV(Object o) { if (o==null) return 0; if (o instanceof Double) return ((Double)o).intValue(); if (o instanceof Integer) return (Integer)o; try{return Integer.parseInt(o.toString());}catch(Exception e){return 0;} }
    private double dbl(Object o) { if (o==null) return 0; if (o instanceof Double) return (Double)o; try{return Double.parseDouble(o.toString());}catch(Exception e){return 0;} }
}