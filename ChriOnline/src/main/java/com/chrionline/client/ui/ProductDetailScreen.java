package com.chrionline.client.ui;

import com.chrionline.client.controller.ProductController;
import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

public class ProductDetailScreen {

    private static final double MAX_W = 1200;
    private static final double H_PAD = 40;

    private final int               productId;
    private final ProductController productController = new ProductController();
    private final Gson              gson              = new Gson();

    private final BorderPane root;
    private StackPane        contentArea;
    private StackPane        loadingOverlay;

    private Label cartCountLabel  = new Label("0");
    private Label notifCountLabel = new Label("0");

    public ProductDetailScreen(int productId) {
        this.productId = productId;

        root = new BorderPane();
        root.getStyleClass().add("page-root");
        root.setTop(new HeaderComponent("products", null, null, null).getView());

        contentArea = new StackPane();
        contentArea.setAlignment(Pos.TOP_CENTER);
        contentArea.setMinWidth(0);

        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        loadingOverlay = buildLoading();
        root.setCenter(new StackPane(scroll, loadingOverlay));

        loadProductDetail();

        Platform.runLater(() -> {
            root.setOpacity(0);
            FadeTransition f = new FadeTransition(Duration.millis(400), root);
            f.setToValue(1);
            f.play();
        });
    }

    public Node getView() {
        return root;
    }

    // ═══════════════════════════════════════════════════════
    //  HEADER — identique au ProductListScreen
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
        Button nProd   = navBtn("Produits",  true);
        Button nOrders = navBtn("Commandes", false);
        Button nAbout  = navBtn("A propos",  false);

        nHome.setOnAction(e   -> NavigationManager.showLanding());
        nProd.setOnAction(e   -> NavigationManager.showProductList());
        nOrders.setOnAction(e -> NavigationManager.showOrders());
        nav.getChildren().addAll(nHome, nProd, nOrders, nAbout);

        // ── Notification bell ─────────────────────────────
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

        // ── Cart ──────────────────────────────────────────
        StackPane cartStack = new StackPane();
        Button cartBtn = new Button("\uD83D\uDED2");
        cartBtn.getStyleClass().add("header-icon-btn");
        cartBtn.setOnAction(e -> NavigationManager.showCart());

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
        cartStack.getChildren().addAll(cartBtn, cartBadge);

        // ── User avatar ───────────────────────────────────
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

        HBox actions = new HBox(10, notifStack, cartStack, userBtn);
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
    //  CHARGEMENT PRODUIT
    // ═══════════════════════════════════════════════════════

    private void loadProductDetail() {
        showLoading(true);

        new Thread(() -> {
            if (!ServerConnection.getInstance().isConnected())
                ServerConnection.getInstance().connect();

            Response resp = productController.getDetail(
                    NavigationManager.getToken(), productId);

            Platform.runLater(() -> {
                showLoading(false);

                if (resp.isSuccess()) {
                    Map<String, Object> p = castToMap(resp.getData());
                    buildDetailContent(p);
                } else {
                    showError(resp.getMessage());
                }
            });
        }).start();
    }

    // ═══════════════════════════════════════════════════════
    //  CONTENU DÉTAIL — Layout deux colonnes premium
    // ═══════════════════════════════════════════════════════

    private void buildDetailContent(Map<String, Object> p) {
        String  name     = getStr(p, "name");
        String  desc     = getStr(p, "description");
        double  price    = getDbl(p, "price");
        int     stock    = getInt(p, "stockQuantity");
        String  cat      = getStr(p, "categoryName");
        String  imageUrl = getStr(p, "imageUrl");
        boolean inStock  = stock > 0;

        if (desc.isEmpty() || "...".equals(desc))
            desc = "Aucune description disponible pour ce produit. "
                    + "Contactez-nous pour plus d'informations.";

        VBox page = new VBox(0);
        page.setMaxWidth(MAX_W);
        page.setPadding(new Insets(24, H_PAD, 60, H_PAD));
        page.setAlignment(Pos.TOP_CENTER);

        // ── Breadcrumb ───────────────────────────────────
        HBox breadcrumb = new HBox(6);
        breadcrumb.setAlignment(Pos.CENTER_LEFT);
        breadcrumb.setPadding(new Insets(0, 0, 24, 0));

        Button bcHome = bcLink("Accueil");
        bcHome.setOnAction(e -> NavigationManager.showLanding());
        Label sep1 = bcSep();
        Button bcProducts = bcLink("Produits");
        bcProducts.setOnAction(e -> NavigationManager.showProductList());
        Label sep2 = bcSep();
        Label bcCurrent = new Label(
                name.length() > 35 ? name.substring(0, 33) + "..." : name);
        bcCurrent.setStyle(
                "-fx-text-fill: #6C3483; -fx-font-size: 13px; -fx-font-weight: bold;");

        breadcrumb.getChildren().addAll(bcHome, sep1, bcProducts, sep2, bcCurrent);

        // ── Deux colonnes ────────────────────────────────
        HBox columns = new HBox(40);
        columns.setAlignment(Pos.TOP_CENTER);

        // ═══ COLONNE GAUCHE — Image ═════════════════════
        VBox leftCol = new VBox(16);
        leftCol.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("detail-image-box");
        imageBox.setMinHeight(420);

        Image productImage = loadProductImage(imageUrl);

        if (productImage != null && !productImage.isError()) {
            ImageView iv = new ImageView(productImage);
            iv.setFitWidth(480);
            iv.setFitHeight(380);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            imageBox.getChildren().add(iv);
        } else {
            Label letter = new Label(
                    name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
            letter.getStyleClass().add("detail-image-letter");
            imageBox.getChildren().add(letter);
        }

        // Badge rupture de stock superposé sur l'image
        if (!inStock) {
            Label outBadge = new Label("RUPTURE DE STOCK");
            outBadge.setStyle(
                    "-fx-background-color: rgba(231,76,60,0.9);"
                            + "-fx-text-fill: white;"
                            + "-fx-font-size: 13px;"
                            + "-fx-font-weight: bold;"
                            + "-fx-padding: 8 24;"
                            + "-fx-background-radius: 6;"
            );
            StackPane.setAlignment(outBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(outBadge, new Insets(16, 16, 0, 0));
            imageBox.getChildren().add(outBadge);
        }

        leftCol.getChildren().add(imageBox);

        // ═══ COLONNE DROITE — Infos ═════════════════════
        VBox rightCol = new VBox(14);
        rightCol.setMinWidth(380);
        rightCol.setMaxWidth(460);
        rightCol.setAlignment(Pos.TOP_LEFT);

        // Catégorie
        Label catBadge = new Label(
                (cat.isEmpty() ? "PRODUIT" : cat).toUpperCase());
        catBadge.getStyleClass().add("detail-category");

        // Nom
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("detail-name");

        // Prix
        HBox priceRow = new HBox(12);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label priceLabel = new Label(String.format("%.2f MAD", price));
        priceLabel.getStyleClass().add("detail-price");

        if (!inStock) {
            Label unavail = new Label("Indisponible");
            unavail.setStyle(
                    "-fx-background-color: rgba(231,76,60,0.1);"
                            + "-fx-text-fill: #E74C3C;"
                            + "-fx-font-size: 12px;"
                            + "-fx-font-weight: bold;"
                            + "-fx-padding: 4 12;"
                            + "-fx-background-radius: 6;"
            );
            priceRow.getChildren().addAll(priceLabel, unavail);
        } else {
            priceRow.getChildren().add(priceLabel);
        }

        // Séparateur
        Region divider = new Region();
        divider.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;");
        divider.setMaxWidth(Double.MAX_VALUE);

        // Description
        Label descTitle = new Label("Description");
        descTitle.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("detail-description");

        // Stock
        Label stockLabel = new Label();
        if (stock > 10) {
            stockLabel.setText("\u2713 En stock — " + stock + " disponibles");
            stockLabel.getStyleClass().add("detail-stock-ok");
        } else if (stock > 0) {
            stockLabel.setText("\u26A0 Plus que " + stock + " en stock !");
            stockLabel.getStyleClass().add("detail-stock-low");
        } else {
            stockLabel.setText("\u2717 Rupture de stock");
            stockLabel.getStyleClass().add("detail-stock-out");
        }

        // Séparateur 2
        Region divider2 = new Region();
        divider2.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;");
        divider2.setMaxWidth(Double.MAX_VALUE);

        // ── Quantité ─────────────────────────────────────
        final int[] quantity = {1};

        HBox qtyRow = new HBox(16);
        qtyRow.setAlignment(Pos.CENTER_LEFT);
        qtyRow.setPadding(new Insets(4, 0, 4, 0));

        Label qtyTitle = new Label("Quantite");
        qtyTitle.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Button minusBtn = new Button("\u2212");
        minusBtn.getStyleClass().add("qty-btn");

        Label qtyLabel = new Label("1");
        qtyLabel.getStyleClass().add("qty-label");

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("qty-btn");

        minusBtn.setOnAction(e -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                qtyLabel.setText(String.valueOf(quantity[0]));
            }
        });

        plusBtn.setOnAction(e -> {
            if (quantity[0] < stock) {
                quantity[0]++;
                qtyLabel.setText(String.valueOf(quantity[0]));
            }
        });

        HBox qtyControls = new HBox(0, minusBtn, qtyLabel, plusBtn);
        qtyControls.setAlignment(Pos.CENTER);
        qtyControls.setStyle(
                "-fx-background-color: #F0F0F0;"
                        + "-fx-background-radius: 20;"
                        + "-fx-padding: 2;"
        );

        qtyRow.getChildren().addAll(qtyTitle, qtyControls);

        if (!inStock) {
            qtyRow.setDisable(true);
            qtyRow.setOpacity(0.4);
        }

        // ── Bouton Ajouter au panier ─────────────────────
        Button addBtn = new Button(
                inStock ? "\uD83D\uDED2  Ajouter au panier" : "Produit indisponible");
        addBtn.getStyleClass().add("detail-add-btn");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setDisable(!inStock);
        addBtn.setOnAction(e -> handleAddToCart(quantity[0], addBtn));

        // Alerte rupture de stock
        VBox stockAlert = new VBox();
        if (!inStock) {
            HBox alertBox = new HBox(10);
            alertBox.setAlignment(Pos.CENTER_LEFT);
            alertBox.setStyle(
                    "-fx-background-color: rgba(231,76,60,0.08);"
                            + "-fx-border-color: rgba(231,76,60,0.2);"
                            + "-fx-border-width: 1;"
                            + "-fx-border-radius: 10;"
                            + "-fx-background-radius: 10;"
                            + "-fx-padding: 12 16;"
            );
            Label alertIcon = new Label("\u26A0\uFE0F");
            alertIcon.setStyle("-fx-font-size: 18px;");
            Label alertText = new Label(
                    "Ce produit est actuellement en rupture de stock. "
                            + "Revenez bientot !");
            alertText.setStyle(
                    "-fx-text-fill: #E74C3C; -fx-font-size: 12px; -fx-wrap-text: true;");
            alertText.setWrapText(true);
            alertBox.getChildren().addAll(alertIcon, alertText);
            stockAlert.getChildren().add(alertBox);
        }

        // ── Infos livraison / retours / garantie ─────────
        HBox infoCards = new HBox(10);
        infoCards.setAlignment(Pos.CENTER_LEFT);
        infoCards.setPadding(new Insets(8, 0, 0, 0));
        infoCards.getChildren().addAll(
                infoCard("\uD83D\uDE9A", "Livraison", "Gratuite des 50 MAD"),
                infoCard("\u21A9\uFE0F", "Retours",   "30 jours"),
                infoCard("\uD83D\uDD12", "Paiement",  "100% securise")
        );

        // ── Assemblage colonne droite ────────────────────
        rightCol.getChildren().addAll(
                catBadge, nameLabel, priceRow,
                divider, descTitle, descLabel,
                stockLabel, divider2,
                qtyRow, addBtn
        );

        if (!stockAlert.getChildren().isEmpty()) {
            rightCol.getChildren().add(stockAlert);
        }

        rightCol.getChildren().add(infoCards);

        columns.getChildren().addAll(leftCol, rightCol);
        page.getChildren().addAll(breadcrumb, columns);

        setContent(page);
    }

    // ═══════════════════════════════════════════════════════
    //  AJOUTER AU PANIER
    // ═══════════════════════════════════════════════════════

    private void handleAddToCart(int quantity, Button addBtn) {
        int userId = NavigationManager.getUserId();

        if (userId == -1) {
            toast("Vous devez etre connecte.", "error");
            return;
        }

        addBtn.setDisable(true);
        String originalText = addBtn.getText();
        addBtn.setText("Ajout en cours...");

        new Thread(() -> {
            Request req = new Request(
                    Action.ADD_TO_CART,
                    NavigationManager.getToken(),
                    Map.of("userId", userId,
                            "productId", productId,
                            "quantity", quantity)
            );
            Response resp = ServerConnection.getInstance().send(req);

            Platform.runLater(() -> {
                if (resp.isSuccess()) {
                    addBtn.setText("\u2713  Ajoute au panier !");
                    addBtn.setStyle(
                            "-fx-background-color: linear-gradient(to right, #27AE60, #2ECC71);"
                                    + "-fx-text-fill: #FFFFFF;"
                                    + "-fx-font-size: 15px;"
                                    + "-fx-font-weight: bold;"
                                    + "-fx-padding: 14 40;"
                                    + "-fx-background-radius: 12;"
                                    + "-fx-effect: dropshadow(gaussian, rgba(39,174,96,0.3), 10, 0, 0, 4);"
                    );
                    toast("Produit ajoute au panier !", "success");

                    // Remettre le bouton normal après 2s
                    PauseTransition pause = new PauseTransition(Duration.seconds(2));
                    pause.setOnFinished(ev -> {
                        addBtn.setText(originalText);
                        addBtn.getStyleClass().clear();
                        addBtn.getStyleClass().add("detail-add-btn");
                        addBtn.setStyle("");
                        addBtn.setDisable(false);
                    });
                    pause.play();

                } else {
                    toast(resp.getMessage() != null
                            ? resp.getMessage() : "Erreur lors de l'ajout", "error");
                    addBtn.setText(originalText);
                    addBtn.setDisable(false);
                }
            });
        }).start();
    }

    // ═══════════════════════════════════════════════════════
    //  COMPOSANTS UI
    // ═══════════════════════════════════════════════════════

    private VBox infoCard(String icon, String title, String text) {
        VBox card = new VBox(4);
        card.getStyleClass().add("detail-info-card");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(120);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 20px;");
        Label ttl = new Label(title);
        ttl.getStyleClass().add("detail-info-title");
        Label txt = new Label(text);
        txt.getStyleClass().add("detail-info-text");
        txt.setWrapText(true);

        card.getChildren().addAll(ico, ttl, txt);
        return card;
    }

    private Button bcLink(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: #95A5A6;"
                        + "-fx-font-size: 13px;"
                        + "-fx-cursor: hand;"
                        + "-fx-padding: 0;"
        );
        b.setOnMouseEntered(e -> b.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: #6C3483;"
                        + "-fx-font-size: 13px;"
                        + "-fx-cursor: hand;"
                        + "-fx-padding: 0;"
                        + "-fx-underline: true;"
        ));
        b.setOnMouseExited(e -> b.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: #95A5A6;"
                        + "-fx-font-size: 13px;"
                        + "-fx-cursor: hand;"
                        + "-fx-padding: 0;"
        ));
        return b;
    }

    private Label bcSep() {
        Label s = new Label("\u203A");
        s.setStyle("-fx-text-fill: #BDC3C7; -fx-font-size: 14px;");
        return s;
    }

    // ═══════════════════════════════════════════════════════
    //  CHARGEMENT D'IMAGE — multiple fallbacks
    // ═══════════════════════════════════════════════════════

    private Image loadProductImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) return null;
        imageUrl = imageUrl.trim();

        try {
            InputStream is = getClass().getResourceAsStream(imageUrl);
            if (is != null) {
                Image img = new Image(is);
                if (!img.isError()) return img;
            }
        } catch (Exception ignored) {}

        try {
            File f = new File("src/main/resources" + imageUrl);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString());
                if (!img.isError()) return img;
            }
        } catch (Exception ignored) {}

        try {
            File f = new File(imageUrl);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString());
                if (!img.isError()) return img;
            }
        } catch (Exception ignored) {}

        try {
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")
                    || imageUrl.startsWith("file:")) {
                return new Image(imageUrl, true);
            }
        } catch (Exception ignored) {}

        return null;
    }

    // ═══════════════════════════════════════════════════════
    //  TOAST
    // ═══════════════════════════════════════════════════════

    private void toast(String msg, String type) {
        Label t = new Label(msg);
        t.getStyleClass().addAll("toast", "toast-text");
        switch (type) {
            case "success": t.getStyleClass().add("toast-success"); break;
            case "error":   t.getStyleClass().add("toast-error");   break;
            default:        t.getStyleClass().add("toast-info");    break;
        }
        t.setOpacity(0);
        t.setTranslateY(40);

        StackPane body = (StackPane) root.getCenter();
        StackPane.setAlignment(t, Pos.BOTTOM_CENTER);
        StackPane.setMargin(t, new Insets(0, 0, 40, 0));
        body.getChildren().add(t);

        FadeTransition fi = new FadeTransition(Duration.millis(300), t);
        fi.setToValue(1);
        TranslateTransition si = new TranslateTransition(Duration.millis(300), t);
        si.setToY(0);
        si.setInterpolator(Interpolator.EASE_OUT);
        PauseTransition hold = new PauseTransition(Duration.seconds(3));
        FadeTransition fo = new FadeTransition(Duration.millis(400), t);
        fo.setToValue(0);
        TranslateTransition so = new TranslateTransition(Duration.millis(400), t);
        so.setToY(30);
        fo.setOnFinished(e -> body.getChildren().remove(t));
        new SequentialTransition(
                new ParallelTransition(fi, si), hold,
                new ParallelTransition(fo, so)).play();
    }

    // ═══════════════════════════════════════════════════════
    //  ERREUR / LOADING
    // ═══════════════════════════════════════════════════════

    private void showError(String message) {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(80));

        Label icon = new Label("\u26A0\uFE0F");
        icon.setStyle("-fx-font-size: 48px;");

        Label title = new Label("Produit introuvable");
        title.getStyleClass().add("empty-state-title");

        Label msg = new Label(message != null ? message : "Une erreur est survenue.");
        msg.getStyleClass().add("empty-state-text");
        msg.setWrapText(true);

        Button retryBtn = new Button("Reessayer");
        retryBtn.getStyleClass().add("btn-primary");
        retryBtn.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        retryBtn.setOnAction(e -> loadProductDetail());

        Button backBtn = new Button("Retour aux produits");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        backBtn.setOnAction(e -> NavigationManager.showProductList());

        HBox buttons = new HBox(12, retryBtn, backBtn);
        buttons.setAlignment(Pos.CENTER);

        box.getChildren().addAll(icon, title, msg, buttons);
        setContent(box);
    }

    private void setContent(Node node) {
        node.setOpacity(0);
        contentArea.getChildren().setAll(node);
        FadeTransition f = new FadeTransition(Duration.millis(350), node);
        f.setToValue(1);
        f.play();
    }

    private StackPane buildLoading() {
        StackPane o = new StackPane();
        o.getStyleClass().add("loading-modern-overlay");
        o.setVisible(false);

        VBox c = new VBox(20);
        c.getStyleClass().add("loading-card");
        c.setAlignment(Pos.CENTER);
        c.setMaxWidth(200);
        c.setMaxHeight(140);

        ProgressIndicator pi = new ProgressIndicator();
        pi.getStyleClass().add("loading-spinner");
        pi.setMaxSize(40, 40);

        Label t = new Label("Chargement...");
        t.getStyleClass().add("loading-text");

        c.getChildren().addAll(pi, t);
        o.getChildren().add(c);
        return o;
    }

    private void showLoading(boolean b) {
        Platform.runLater(() -> {
            if (b) {
                loadingOverlay.setOpacity(0);
                loadingOverlay.setVisible(true);
                FadeTransition fi = new FadeTransition(
                        Duration.millis(200), loadingOverlay);
                fi.setToValue(1);
                fi.play();
            } else {
                FadeTransition fo = new FadeTransition(
                        Duration.millis(200), loadingOverlay);
                fo.setToValue(0);
                fo.setOnFinished(e -> loadingOverlay.setVisible(false));
                fo.play();
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════

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