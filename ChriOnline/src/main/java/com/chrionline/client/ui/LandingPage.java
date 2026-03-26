package com.chrionline.client.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.*;

public class LandingPage {

    private static final double MAX_W = 1200;
    private static final double H_PAD = 40;

    private final BorderPane root;
    private final VBox       mainContent;
    private final ScrollPane scrollPane;
    private FlowPane         productsGrid;
    private StackPane        loadingOverlay;
    private Label            cartCountLabel;
    private Label            notifCountLabel;
    private StackPane        headerWrapper;
    private Node             productsSectionRef;

    private Label heroTitle, heroDesc;
    private HBox  heroBtns, heroStats;

    private List<Map<String, Object>> allProducts = new ArrayList<>();
    private String currentCategory = "Tous";

    public LandingPage() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #F5F6FA;");

        cartCountLabel  = new Label("0");
        notifCountLabel = new Label("0");
        headerWrapper   = buildHeader();
        root.setTop(headerWrapper);

        mainContent = new VBox(0);
        mainContent.setAlignment(Pos.TOP_CENTER);
        mainContent.setFillWidth(true);

        scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("scroll-pane");

        mainContent.getChildren().addAll(
                buildHero(),
                buildTicker(),
                buildCategories(),
                buildProducts(),
                buildFeatures(),
                buildCta(),
                buildFooter()
        );

        loadingOverlay = buildLoading();
        StackPane body = new StackPane(scrollPane, loadingOverlay);
        root.setCenter(body);

        setupScrollShadow();
        loadProducts();
        if (NavigationManager.isLoggedIn()) {
            updateBadge();
            updateNotifBadge();
        }
        Platform.runLater(this::animateEntry);
    }

    public Node getView() { return root; }

    // ═══════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════

    private StackPane wrap(Node inner, String... css) {
        StackPane w = new StackPane(inner);
        w.setAlignment(Pos.TOP_CENTER);
        w.setMaxWidth(Double.MAX_VALUE);
        w.setMinWidth(0);
        w.setPadding(Insets.EMPTY);
        for (String c : css) w.getStyleClass().add(c);
        return w;
    }

    private VBox container(double top, double bottom) {
        VBox b = new VBox();
        b.setMaxWidth(MAX_W);
        b.setAlignment(Pos.TOP_CENTER);
        b.setPadding(new Insets(top, H_PAD, bottom, H_PAD));
        return b;
    }

    // ═══════════════════════════════════════════════════════
    //                       HEADER
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

        // ── Logo ────────────────────────────────────────
        HBox logo = new HBox(8);
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setCursor(javafx.scene.Cursor.HAND);
        logo.setOnMouseClicked(e -> scrollPane.setVvalue(0));
        try {
            ImageView img = new ImageView(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))));
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

        boolean isAdmin = NavigationManager.isAdmin();

        Label adminBadge = new Label("ADMIN");
        adminBadge.getStyleClass().add("admin-header-badge");
        adminBadge.setVisible(isAdmin);
        adminBadge.setManaged(isAdmin);

        // ── Nav ─────────────────────────────────────────
        HBox nav = new HBox(4);
        nav.setAlignment(Pos.CENTER);

        if (isAdmin) {
            Button nShop = navBtn("Boutique", true);
            nShop.setOnAction(e -> scrollPane.setVvalue(0));

            Button nDashboard = navBtn("Dashboard", false);
            nDashboard.setOnAction(e -> NavigationManager.showAdminDashboard());

            nav.getChildren().addAll(nShop, nDashboard);
        } else {
            Button nHome = navBtn("Accueil", true);
            nHome.setOnAction(e -> scrollPane.setVvalue(0));

            Button nProd = navBtn("Produits", false);
            nProd.setOnAction(e -> NavigationManager.showProductList());

            Button nOrders = navBtn("Commandes", false);
            nOrders.setOnAction(e -> {
                if (NavigationManager.isLoggedIn()) {
                    NavigationManager.showOrders();
                } else {
                    toast("Connectez-vous pour voir vos commandes", "info");
                }
            });

            Button nAbout = navBtn("A propos", false);

            nav.getChildren().addAll(nHome, nProd, nOrders, nAbout);
        }

        // ── Search ──────────────────────────────────────
        TextField search = new TextField();
        search.setPromptText("Rechercher...");
        search.getStyleClass().add("search-field");
        search.setPrefWidth(isAdmin ? 200 : 220);
        search.setOnAction(e -> {
            String k = search.getText().trim();
            if (!k.isEmpty()) searchProducts(k);
            else displayProducts(allProducts);
        });
        search.textProperty().addListener((o, ov, nv) -> {
            if (nv.isEmpty()) {
                currentCategory = "Tous";
                displayProducts(allProducts);
            }
        });

        // ── Notification bell (client only) ─────────────
        StackPane notifStack = new StackPane();
        Button notifBtn = new Button("\uD83D\uDD14");
        notifBtn.getStyleClass().add("header-icon-btn");
        notifBtn.setOnAction(e -> {
            if (NavigationManager.isLoggedIn()) {
                NavigationManager.showNotifications();
            } else {
                toast("Connectez-vous pour voir vos notifications", "info");
            }
        });

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
        notifStack.setVisible(NavigationManager.isLoggedIn() && !isAdmin);
        notifStack.setManaged(NavigationManager.isLoggedIn() && !isAdmin);

        // ── Cart button (client only) ───────────────────
        StackPane cartStack = new StackPane();
        Button cartBtn = new Button("\uD83D\uDED2");
        cartBtn.getStyleClass().add("header-icon-btn");
        cartBtn.setOnAction(e -> {
            if (NavigationManager.isLoggedIn()) {
                NavigationManager.showCart();
            } else {
                toast("Connectez-vous pour accéder au panier", "info");
            }
        });

        StackPane badge = new StackPane();
        badge.getStyleClass().add("notification-badge");
        badge.setMaxSize(18, 18);
        badge.setMinSize(18, 18);
        cartCountLabel.getStyleClass().add("notification-badge-text");
        cartCountLabel.setStyle("-fx-font-size: 9px;");
        badge.getChildren().add(cartCountLabel);
        badge.setVisible(false);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(-4, -4, 0, 0));
        cartStack.getChildren().addAll(cartBtn, badge);
        cartStack.setVisible(!isAdmin);
        cartStack.setManaged(!isAdmin);

        // ── User button ─────────────────────────────────
        Button userBtn = new Button();
        userBtn.getStyleClass().add("header-user-btn");

        if (NavigationManager.isLoggedIn()) {
            String initials = "";
            String fn = NavigationManager.getFirstName();
            String ln = NavigationManager.getLastName();
            if (fn != null && !fn.isEmpty()) initials += fn.charAt(0);
            if (ln != null && !ln.isEmpty()) initials += ln.charAt(0);
            if (initials.isEmpty()) initials = "U";

            StackPane avatar = new StackPane();
            avatar.getStyleClass().add("header-avatar");
            avatar.setMinSize(32, 32);
            avatar.setMaxSize(32, 32);

            if (isAdmin) {
                avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #E74C3C, #C0392B); -fx-background-radius: 16;");
            }

            Label initLabel = new Label(initials.toUpperCase());
            initLabel.getStyleClass().add("header-avatar-text");
            avatar.getChildren().add(initLabel);

            HBox userContent = new HBox(8);
            userContent.setAlignment(Pos.CENTER);
            String displayName = fn != null ? fn : NavigationManager.getUsername();
            if (isAdmin) displayName += " (Admin)";
            Label nameLabel = new Label(displayName);
            nameLabel.getStyleClass().add("header-user-name");
            userContent.getChildren().addAll(avatar, nameLabel);

            userBtn.setGraphic(userContent);
            userBtn.setText(null);
            userBtn.setOnAction(e -> NavigationManager.showProfile());
        } else {
            userBtn.setText("\uD83D\uDC64 Se connecter");
            userBtn.setOnAction(e -> NavigationManager.navigateTo("login"));
        }

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        HBox actions = new HBox(10, search, notifStack, cartStack, userBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        if (isAdmin) {
            inner.getChildren().addAll(logo, adminBadge, sp1, nav, sp2, actions);
        } else {
            inner.getChildren().addAll(logo, sp1, nav, sp2, actions);
        }

        w.getChildren().add(inner);
        return w;
    }

    private Button navBtn(String t, boolean active) {
        Button b = new Button(t);
        b.getStyleClass().add("nav-link");
        if (active) b.getStyleClass().add("nav-link-active");
        return b;
    }

    // ═══════════════════════════════════════════════════════
    //                        HERO
    // ═══════════════════════════════════════════════════════

    private Node buildHero() {
        /*
         * Architecture du hero :
         *
         *   ┌─────────────────────────────────────────────────┐
         *   │  wrapper (full-width, clipped, minHeight 720)   │
         *   │  ┌─────────────────────────────────────────────┐│
         *   │  │  heroContent (StackPane, maxWidth=MAX_W)    ││
         *   │  │  ┌──────────┬──────────────────────────────┐││
         *   │  │  │  LEFT    │        RIGHT                 │││
         *   │  │  │  (text)  │   (image, clipped,           │││
         *   │  │  │  fixed   │    grows to fill)            │││
         *   │  │  │  width   │                              │││
         *   │  │  │  420px   │                              │││
         *   │  │  └──────────┴──────────────────────────────┘││
         *   │  └─────────────────────────────────────────────┘│
         *   │  decor (floating circles, unmanaged)            │
         *   └─────────────────────────────────────────────────┘
         *
         * The LEFT column has a FIXED minWidth/maxWidth so it is
         * NEVER pushed off-screen regardless of image size.
         * The RIGHT column clips its content so the large image
         * never causes horizontal overflow.
         */

        // ── LEFT: text content (fixed width, never compressed) ──
        VBox left = new VBox(22);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setMinWidth(430);
        left.setPrefWidth(450);
        left.setMaxWidth(480);
        left.setPadding(new Insets(0, 20, 0, 0));

        heroTitle = new Label("Découvrez notre\ncollection exclusive");
        heroTitle.getStyleClass().add("hero-title");
        heroTitle.setWrapText(true);
        heroTitle.setMaxWidth(440);

        heroDesc = new Label(
                "Des produits soigneusement sélectionnés pour vous.\n"
                        + "Technologie, mode, livres et sport — tout ce dont\n"
                        + "vous avez besoin, livré directement chez vous.");
        heroDesc.getStyleClass().add("hero-description");
        heroDesc.setWrapText(true);
        heroDesc.setMaxWidth(440);

        heroBtns = new HBox(16);
        heroBtns.setAlignment(Pos.CENTER_LEFT);

        Button shop = new Button("Explorer la boutique");
        shop.getStyleClass().add("hero-btn-primary");
        shop.setOnAction(e -> NavigationManager.showProductList());

        Button more = new Button("En savoir plus");
        more.getStyleClass().add("hero-btn-secondary");
        heroBtns.getChildren().addAll(shop, more);

        heroStats = new HBox(16);
        heroStats.setAlignment(Pos.CENTER_LEFT);
        heroStats.setPadding(new Insets(10, 0, 0, 0));
        heroStats.getChildren().addAll(
                statBox("1 000+", "Produits"),
                statBox("500+", "Clients"),
                statBox("98%", "Satisfaction"));

        left.getChildren().addAll(heroTitle, heroDesc, heroBtns, heroStats);

        // ── RIGHT: image container (fills remaining space, clipped) ──
        StackPane right = new StackPane();
        right.setAlignment(Pos.CENTER);
        right.setMinWidth(0); // can shrink if window is small

        try {
            String url = Objects.requireNonNull(
                    getClass().getResource("/images/hero-shop.png")).toExternalForm();

            Image heroImage = new Image(url, 0, 0, true, true, true);

            ImageView iv = new ImageView(heroImage);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            iv.setOpacity(0);

            /*
             * Instead of fixed fitWidth/fitHeight, we bind the image size
             * to the available space in the right pane.
             * This way the image is always as large as possible
             * without ever pushing the left column off-screen.
             *
             * We multiply by 1.4 so the image overflows slightly
             * (clipped), giving a dramatic "zoomed in" feel
             * that is ~4x the original 850×650 area.
             *
             * Area ratio: original = 850*650 = 552,500
             *             target   = (w*1.4) * (h*1.4) ≈ 2x-4x depending on viewport
             */
            iv.fitWidthProperty().bind(
                    right.widthProperty().multiply(1.4));
            iv.fitHeightProperty().bind(
                    right.heightProperty().multiply(1.4));

            DropShadow sh = new DropShadow();
            sh.setColor(Color.rgb(0, 0, 0, 0.3));
            sh.setRadius(32);
            sh.setOffsetY(14);
            iv.setEffect(sh);

            heroImage.progressProperty().addListener((obs, o, p) -> {
                if (p.doubleValue() >= 1.0) {
                    FadeTransition fi = new FadeTransition(Duration.millis(800), iv);
                    fi.setToValue(1);
                    fi.play();
                }
            });

            TranslateTransition fl = new TranslateTransition(Duration.seconds(3), iv);
            fl.setByY(-14);
            fl.setAutoReverse(true);
            fl.setCycleCount(Animation.INDEFINITE);
            fl.setInterpolator(Interpolator.EASE_BOTH);
            fl.play();

            // Clip the right pane so the oversized image doesn't overflow
            Rectangle rightClip = new Rectangle();
            rightClip.widthProperty().bind(right.widthProperty());
            rightClip.heightProperty().bind(right.heightProperty());
            right.setClip(rightClip);

            right.getChildren().add(iv);
        } catch (Exception e) {
            Label ph = new Label("ChriOnline");
            ph.setStyle("-fx-font-size:52px; -fx-text-fill:rgba(255,255,255,0.07); "
                    + "-fx-font-weight:bold;");
            right.getChildren().add(ph);
        }

        // ── HBox layout: left is NEVER, right ALWAYS ───
        HBox.setHgrow(left, Priority.NEVER);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox inner = new HBox(30, left, right);
        inner.setMaxWidth(MAX_W);
        inner.setPadding(new Insets(60, H_PAD, 60, H_PAD));
        inner.setAlignment(Pos.CENTER_LEFT);

        // ── Decorative circles ──────────────────────────
        Pane decor = new Pane();
        decor.setMouseTransparent(true);
        decor.setManaged(false);
        addCircles(decor);

        // ── Wrapper: clip everything to prevent horizontal overflow ──
        StackPane wrapper = new StackPane(inner, decor);
        wrapper.getStyleClass().add("hero-wrapper");
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setMinWidth(0);
        wrapper.setMinHeight(720);

        Rectangle wrapperClip = new Rectangle();
        wrapperClip.widthProperty().bind(wrapper.widthProperty());
        wrapperClip.heightProperty().bind(wrapper.heightProperty());
        wrapper.setClip(wrapperClip);

        wrapper.layoutBoundsProperty().addListener((obs, ov, nv) -> {
            decor.resize(nv.getWidth(), nv.getHeight());
        });

        return wrapper;
    }

    private VBox statBox(String v, String l) {
        VBox b = new VBox(2);
        b.getStyleClass().add("hero-stat-box");
        b.setAlignment(Pos.CENTER);
        Label vl = new Label(v); vl.getStyleClass().add("hero-stat-value");
        Label ll = new Label(l); ll.getStyleClass().add("hero-stat-label");
        b.getChildren().addAll(vl, ll);
        return b;
    }

    private void addCircles(Pane p) {
        double[][] c = {
                {900, 80, 150, 0.03}, {120, 320, 100, 0.04},
                {700, 380, 75, 0.03}, {1050, 270, 50, 0.05}
        };
        for (double[] s : c) {
            Circle ci = new Circle(s[2], Color.rgb(255, 255, 255, s[3]));
            ci.setLayoutX(s[0]); ci.setLayoutY(s[1]);
            p.getChildren().add(ci);
        }
    }

    // ═══════════════════════════════════════════════════════
    //                       TICKER
    // ═══════════════════════════════════════════════════════

    private Node buildTicker() {
        StackPane w = new StackPane();
        w.getStyleClass().add("ticker-bar");
        w.setMinHeight(42); w.setMaxHeight(42);
        w.setMaxWidth(Double.MAX_VALUE);
        w.setMinWidth(0);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(w.widthProperty());
        clip.heightProperty().bind(w.heightProperty());
        w.setClip(clip);

        String seg = "     \u2605  Livraison gratuite dès 50\u20AC"
                + "          |          Paiement 100% sécurisé"
                + "          |          Retours gratuits sous 30 jours"
                + "          |          Support client 7j/7"
                + "          |          Nouveautés chaque semaine"
                + "          |          Programme fidélité exclusif"
                + "          |          ";

        Label t1 = new Label(seg);
        t1.getStyleClass().add("ticker-text");
        t1.setMinWidth(Region.USE_PREF_SIZE);

        Label t2 = new Label(seg);
        t2.getStyleClass().add("ticker-text");
        t2.setMinWidth(Region.USE_PREF_SIZE);

        HBox band = new HBox(0, t1, t2);
        band.setAlignment(Pos.CENTER_LEFT);

        w.getChildren().add(band);
        StackPane.setAlignment(band, Pos.CENTER_LEFT);

        Platform.runLater(() -> {
            double tw = t1.prefWidth(-1);
            if (tw <= 0) tw = 1400;
            TranslateTransition anim = new TranslateTransition(
                    Duration.seconds(28), band);
            anim.setFromX(0);
            anim.setToX(-tw);
            anim.setInterpolator(Interpolator.LINEAR);
            anim.setCycleCount(Animation.INDEFINITE);
            anim.play();
        });

        return w;
    }

    // ═══════════════════════════════════════════════════════
    //                     CATEGORIES
    // ═══════════════════════════════════════════════════════

    private Node buildCategories() {
        VBox c = container(55, 20); c.setSpacing(16);

        Label t = new Label("Parcourir par catégorie");
        t.getStyleClass().add("section-title");
        Region d = new Region(); d.getStyleClass().add("section-divider");
        Label s = new Label("Trouvez exactement ce que vous cherchez");
        s.getStyleClass().add("section-subtitle");

        HBox chips = new HBox(10);
        chips.setAlignment(Pos.CENTER);
        chips.setPadding(new Insets(8, 0, 0, 0));

        String[] cats = {"Tous", "Smartphones", "Laptops", "T-Shirts",
                "Pantalons", "Livres Tech", "Romans", "Sports"};

        for (String cat : cats) {
            Button chip = new Button(cat);
            chip.getStyleClass().add("category-chip");
            if ("Tous".equals(cat)) chip.getStyleClass().add("category-chip-active");
            chip.setOnAction(e -> {
                currentCategory = cat;
                chips.getChildren().forEach(ch ->
                        ((Button) ch).getStyleClass().remove("category-chip-active"));
                chip.getStyleClass().add("category-chip-active");
                ScaleTransition p1 = new ScaleTransition(Duration.millis(100), chip);
                p1.setToX(1.1); p1.setToY(1.1);
                ScaleTransition p2 = new ScaleTransition(Duration.millis(100), chip);
                p2.setToX(1); p2.setToY(1);
                new SequentialTransition(p1, p2).play();
                filterProducts(cat);
            });
            chips.getChildren().add(chip);
        }

        c.getChildren().addAll(t, d, s, chips);
        return wrap(c, "categories-section-bg");
    }

    // ═══════════════════════════════════════════════════════
    //                      PRODUCTS
    // ═══════════════════════════════════════════════════════

    private Node buildProducts() {
        VBox c = container(40, 60); c.setSpacing(28);

        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label("Nos produits");
        t.getStyleClass().add("section-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label cnt = new Label(""); cnt.getStyleClass().add("section-subtitle");
        cnt.setId("product-count-label");

        Button viewAll = new Button("Voir tout \u2192");
        viewAll.getStyleClass().add("nav-link");
        viewAll.setStyle("-fx-text-fill: #6C3483; -fx-font-weight: bold;");
        viewAll.setOnAction(e -> NavigationManager.showProductList());

        titleRow.getChildren().addAll(t, sp, cnt, viewAll);

        productsGrid = new FlowPane();
        productsGrid.setHgap(24); productsGrid.setVgap(28);
        productsGrid.setAlignment(Pos.CENTER);
        productsGrid.setPrefWrapLength(MAX_W - H_PAD * 2);

        c.getChildren().addAll(titleRow, productsGrid);
        StackPane w = wrap(c, "products-section-bg");
        productsSectionRef = w;
        return w;
    }

    // ═══════════════════════════════════════════════════════
    //                    PRODUCT CARD
    // ═══════════════════════════════════════════════════════

    private Node buildCard(Map<String, Object> p) {
        VBox card = new VBox(0);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(260); card.setMaxWidth(260);

        DropShadow rest = new DropShadow();
        rest.setColor(Color.rgb(0, 0, 0, 0.06));
        rest.setRadius(10); rest.setOffsetY(3);
        card.setEffect(rest);

        String name = str(p.get("name"));
        String cat  = str(p.get("categoryName"));
        double price = dbl(p.get("price"));
        int stock = intV(p.get("stockQuantity"));

        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("product-card-image-container");
        imgBox.setPrefHeight(190); imgBox.setMinHeight(190);
        imgBox.setCursor(javafx.scene.Cursor.HAND);
        imgBox.setOnMouseClicked(e ->
                NavigationManager.showProductDetail(intV(p.get("productId"))));
        Label letter = new Label(
                name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        letter.getStyleClass().add("product-image-letter");
        imgBox.getChildren().add(letter);

        VBox body = new VBox(6); body.getStyleClass().add("product-card-body");
        Label catL = new Label(cat.toUpperCase());
        catL.getStyleClass().add("product-card-category");

        Label nameL = new Label(name);
        nameL.getStyleClass().add("product-card-name");
        nameL.setMaxWidth(230); nameL.setWrapText(true); nameL.setMaxHeight(40);
        nameL.setCursor(javafx.scene.Cursor.HAND);
        nameL.setOnMouseClicked(e ->
                NavigationManager.showProductDetail(intV(p.get("productId"))));

        Label priceL = new Label(String.format("%.2f \u20AC", price));
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

        Button addBtn = new Button("Ajouter au panier");
        addBtn.getStyleClass().add("product-card-btn");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setDisable(stock <= 0);
        addBtn.setOnAction(e -> {
            ScaleTransition s1 = new ScaleTransition(Duration.millis(80), addBtn);
            s1.setToX(0.93); s1.setToY(0.93);
            ScaleTransition s2 = new ScaleTransition(Duration.millis(80), addBtn);
            s2.setToX(1); s2.setToY(1);
            new SequentialTransition(s1, s2).play();
            addToCart(p);
        });

        body.getChildren().addAll(catL, nameL, priceL, stockL, addBtn);
        card.getChildren().addAll(imgBox, body);

        DropShadow hover = new DropShadow();
        hover.setColor(Color.rgb(108, 52, 131, 0.2));
        hover.setRadius(26); hover.setOffsetY(14);

        card.setOnMouseEntered(e -> {
            card.setEffect(hover);
            TranslateTransition u = new TranslateTransition(Duration.millis(250), card);
            u.setToY(-8); u.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition g = new ScaleTransition(Duration.millis(250), card);
            g.setToX(1.03); g.setToY(1.03); g.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(u, g).play();
        });
        card.setOnMouseExited(e -> {
            card.setEffect(rest);
            TranslateTransition d = new TranslateTransition(Duration.millis(300), card);
            d.setToY(0); d.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition s = new ScaleTransition(Duration.millis(300), card);
            s.setToX(1); s.setToY(1); s.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(d, s).play();
        });

        return card;
    }

    // ═══════════════════════════════════════════════════════
    //                      FEATURES
    // ═══════════════════════════════════════════════════════

    private Node buildFeatures() {
        VBox c = container(65, 65); c.setSpacing(30);
        Label t = new Label("Pourquoi ChriOnline ?");
        t.getStyleClass().add("section-title");
        Region d = new Region(); d.getStyleClass().add("section-divider");

        HBox grid = new HBox(24); grid.setAlignment(Pos.CENTER);
        String[][] ft = {
                {"Livraison rapide",  "Livraison express en\n24-48h partout en France"},
                {"Paiement sécurisé", "Vos transactions sont\nprotégées et chiffrées"},
                {"Support 7j/7",      "Notre équipe est à votre\nécoute en permanence"},
                {"Retours gratuits",  "30 jours pour changer\nd'avis, sans aucun frais"}
        };
        String[] icons = {"\u2708", "\u26BF", "\u260E", "\u21A9"};

        for (int i = 0; i < ft.length; i++) {
            VBox fc = new VBox(14); fc.getStyleClass().add("feature-card");
            fc.setAlignment(Pos.CENTER); fc.setPrefWidth(240);
            HBox.setHgrow(fc, Priority.ALWAYS);
            Label ic = new Label(icons[i]); ic.getStyleClass().add("feature-icon");
            Label ti = new Label(ft[i][0]); ti.getStyleClass().add("feature-title");
            Label de = new Label(ft[i][1]); de.getStyleClass().add("feature-desc");
            fc.getChildren().addAll(ic, ti, de);
            fc.setOnMouseEntered(ev -> {
                ScaleTransition s = new ScaleTransition(Duration.millis(200), fc);
                s.setToX(1.05); s.setToY(1.05); s.play();
            });
            fc.setOnMouseExited(ev -> {
                ScaleTransition s = new ScaleTransition(Duration.millis(200), fc);
                s.setToX(1); s.setToY(1); s.play();
            });
            grid.getChildren().add(fc);
        }
        c.getChildren().addAll(t, d, grid);
        return wrap(c, "features-section-bg");
    }

    // ═══════════════════════════════════════════════════════
    //                        CTA
    // ═══════════════════════════════════════════════════════

    private Node buildCta() {
        VBox c = container(60, 60); c.setSpacing(18);
        c.setAlignment(Pos.CENTER);
        Label t = new Label("Prêt à découvrir nos offres ?");
        t.getStyleClass().add("cta-title");
        Label s = new Label("Rejoignez des milliers de clients satisfaits.");
        s.getStyleClass().add("cta-subtitle");

        Button b;
        if (NavigationManager.isLoggedIn()) {
            b = new Button("Explorer la boutique");
            b.getStyleClass().add("cta-btn");
            b.setOnAction(e -> NavigationManager.showProductList());
        } else {
            b = new Button("Créer un compte gratuitement");
            b.getStyleClass().add("cta-btn");
            b.setOnAction(e -> NavigationManager.navigateTo("register"));
        }

        c.getChildren().addAll(t, s, b);
        return wrap(c, "cta-wrapper");
    }

    // ═══════════════════════════════════════════════════════
    //                      FOOTER
    // ═══════════════════════════════════════════════════════

    private Node buildFooter() {
        VBox c = container(50, 30); c.setSpacing(30);

        HBox cols = new HBox(50); cols.setAlignment(Pos.TOP_LEFT);

        VBox c1 = new VBox(12); HBox.setHgrow(c1, Priority.ALWAYS);
        Label br = new Label("ChriOnline"); br.getStyleClass().add("footer-title");
        Label bd = new Label("Votre destination shopping en ligne.\nQualité, confiance et service.");
        bd.getStyleClass().add("footer-text");
        c1.getChildren().addAll(br, bd);

        VBox c2 = new VBox(8);
        Label navTitle = new Label("Navigation");
        navTitle.getStyleClass().add("footer-title");
        c2.getChildren().add(navTitle);

        Button fHome = footerLink("Accueil");
        fHome.setOnAction(e -> scrollPane.setVvalue(0));
        Button fProd = footerLink("Produits");
        fProd.setOnAction(e -> NavigationManager.showProductList());
        Button fOrders = footerLink("Commandes");
        fOrders.setOnAction(e -> {
            if (NavigationManager.isLoggedIn()) NavigationManager.showOrders();
            else toast("Connectez-vous pour voir vos commandes", "info");
        });
        Button fAccount = footerLink("Mon compte");
        fAccount.setOnAction(e -> {
            if (NavigationManager.isLoggedIn()) NavigationManager.showProfile();
            else NavigationManager.navigateTo("login");
        });
        c2.getChildren().addAll(fHome, fProd, fOrders, fAccount);

        VBox c3 = fCol("Catégories", "Smartphones", "Laptops", "Vêtements", "Livres", "Sports");

        VBox c4 = new VBox(10);
        Label ct = new Label("Contact"); ct.getStyleClass().add("footer-title");
        Label em = new Label("contact@chrionline.com"); em.getStyleClass().add("footer-text");
        Label ph = new Label("+33 1 23 45 67 89"); ph.getStyleClass().add("footer-text");
        Label ad = new Label("Paris, France"); ad.getStyleClass().add("footer-text");
        c4.getChildren().addAll(ct, em, ph, ad);

        cols.getChildren().addAll(c1, c2, c3, c4);

        Region sep = new Region(); sep.getStyleClass().add("footer-divider");
        Label copy = new Label("\u00A9 2025 ChriOnline. Tous droits réservés. — Projet académique TCP/UDP");
        copy.getStyleClass().add("footer-bottom-text");

        c.getChildren().addAll(cols, sep, copy);
        return wrap(c, "footer");
    }

    private Button footerLink(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("footer-link");
        return b;
    }

    private VBox fCol(String title, String... links) {
        VBox col = new VBox(8);
        Label t = new Label(title); t.getStyleClass().add("footer-title");
        col.getChildren().add(t);
        for (String l : links) {
            Button b = new Button(l); b.getStyleClass().add("footer-link");
            col.getChildren().add(b);
        }
        return col;
    }

    // ═══════════════════════════════════════════════════════
    //                      LOADING
    // ═══════════════════════════════════════════════════════

    private StackPane buildLoading() {
        StackPane o = new StackPane();
        o.getStyleClass().add("loading-modern-overlay");
        o.setVisible(false);
        VBox card = new VBox(20); card.getStyleClass().add("loading-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(220); card.setMaxHeight(160);
        ProgressIndicator pi = new ProgressIndicator();
        pi.getStyleClass().add("loading-spinner"); pi.setMaxSize(44, 44);
        Label txt = new Label("Chargement...");
        txt.getStyleClass().add("loading-text");
        card.getChildren().addAll(pi, txt);
        o.getChildren().add(card);
        return o;
    }

    private void showLoading(boolean b) {
        Platform.runLater(() -> {
            if (b) {
                loadingOverlay.setOpacity(0);
                loadingOverlay.setVisible(true);
                FadeTransition f = new FadeTransition(Duration.millis(200), loadingOverlay);
                f.setToValue(1); f.play();
            } else {
                FadeTransition f = new FadeTransition(Duration.millis(200), loadingOverlay);
                f.setToValue(0);
                f.setOnFinished(e -> loadingOverlay.setVisible(false));
                f.play();
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    //         DATA — products loaded from MySQL via TCP
    // ═══════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void loadProducts() {
        showLoading(true);
        ServerConnection conn = ServerConnection.getInstance();
        if (!conn.isConnected()) {
            if (!conn.connect()) {
                showLoading(false);
                showEmpty("Impossible de se connecter au serveur", true);
                return;
            }
        }

        conn.sendAsync(new Request(Action.GET_PRODUCTS, null, null))
                .thenAccept(r -> Platform.runLater(() -> {
                    showLoading(false);
                    if (r.isSuccess() && r.getData() != null) {
                        allProducts = (List<Map<String, Object>>) r.getData();
                        if (allProducts.isEmpty()) showEmpty("Aucun produit disponible", false);
                        else displayProducts(allProducts);
                    } else {
                        showEmpty(r.getMessage() != null
                                ? r.getMessage() : "Erreur de chargement", true);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        showEmpty("Erreur réseau : " + ex.getMessage(), true);
                    });
                    return null;
                });
    }

    private void displayProducts(List<Map<String, Object>> products) {
        productsGrid.getChildren().clear();
        Label cnt = (Label) root.lookup("#product-count-label");
        if (cnt != null) cnt.setText(products.size() + " produit(s)");
        if (products.isEmpty()) { showEmpty("Aucun produit trouvé", false); return; }

        List<Node> cards = new ArrayList<>();
        for (Map<String, Object> p : products) {
            Node card = buildCard(p);
            card.setOpacity(0); card.setTranslateY(40);
            card.setScaleX(0.95); card.setScaleY(0.95);
            productsGrid.getChildren().add(card);
            cards.add(card);
        }
        animateCards(cards);
    }

    private void showEmpty(String msg, boolean retry) {
        productsGrid.getChildren().clear();
        VBox e = new VBox(14); e.setAlignment(Pos.CENTER);
        e.setPadding(new Insets(60));
        Label ic = new Label("\uD83D\uDCE6");
        ic.getStyleClass().add("empty-state-icon");
        Label ti = new Label("Aucun résultat");
        ti.getStyleClass().add("empty-state-title");
        Label tx = new Label(msg);
        tx.getStyleClass().add("empty-state-text");
        e.getChildren().addAll(ic, ti, tx);
        if (retry) {
            Button b = new Button("Réessayer"); b.getStyleClass().add("btn-retry");
            b.setOnAction(ev -> loadProducts()); e.getChildren().add(b);
        }
        productsGrid.getChildren().add(e);
    }

    private void filterProducts(String cat) {
        if ("Tous".equals(cat)) { displayProducts(allProducts); return; }
        List<Map<String, Object>> f = new ArrayList<>();
        for (Map<String, Object> p : allProducts)
            if (cat.equalsIgnoreCase(str(p.get("categoryName")))) f.add(p);
        displayProducts(f);
    }

    @SuppressWarnings("unchecked")
    private void searchProducts(String kw) {
        showLoading(true);
        ServerConnection conn = ServerConnection.getInstance();
        if (!conn.isConnected()) { showLoading(false); return; }
        conn.sendAsync(new Request(Action.SEARCH_PRODUCTS, null, Map.of("keyword", kw)))
                .thenAccept(r -> Platform.runLater(() -> {
                    showLoading(false);
                    if (r.isSuccess() && r.getData() != null)
                        displayProducts((List<Map<String, Object>>) r.getData());
                    else showEmpty("Aucun résultat", false);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false); showEmpty("Erreur", true);
                    });
                    return null;
                });
    }

    // ═══════════════════════════════════════════════════════
    //                     ADD TO CART
    // ═══════════════════════════════════════════════════════

    private void addToCart(Map<String, Object> product) {
        if (!NavigationManager.isLoggedIn()) {
            toast("Connectez-vous pour ajouter au panier", "info");
            return;
        }
        Map<String, Object> d = new HashMap<>();
        d.put("userId", NavigationManager.getUserId());
        d.put("productId", intV(product.get("productId")));
        d.put("quantity", 1);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.ADD_TO_CART,
                        NavigationManager.getToken(), d))
                .thenAccept(r -> Platform.runLater(() -> {
                    if (r.isSuccess()) {
                        toast("Produit ajouté au panier !", "success");
                        updateBadge();
                    } else toast(r.getMessage(), "error");
                }));
    }

    // ═══════════════════════════════════════════════════════
    //                  BADGE UPDATES
    // ═══════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void updateBadge() {
        if (!NavigationManager.isLoggedIn()) return;
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_CART,
                        NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId())))
                .thenAccept(r -> Platform.runLater(() -> {
                    if (r.isSuccess() && r.getData() != null) {
                        int c = intV(((Map<String, Object>) r.getData()).get("itemCount"));
                        cartCountLabel.setText(String.valueOf(c));
                        cartCountLabel.getParent().setVisible(c > 0);
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private void updateNotifBadge() {
        if (!NavigationManager.isLoggedIn()) return;
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_NOTIFICATIONS,
                        NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId())))
                .thenAccept(r -> Platform.runLater(() -> {
                    if (r.isSuccess() && r.getData() != null) {
                        List<Map<String, Object>> notifs =
                                (List<Map<String, Object>>) r.getData();
                        long unread = notifs.stream()
                                .filter(n -> {
                                    Object isRead = n.get("isRead");
                                    if (isRead instanceof Boolean) return !(Boolean) isRead;
                                    return false;
                                })
                                .count();
                        notifCountLabel.setText(String.valueOf(unread));
                        notifCountLabel.getParent().setVisible(unread > 0);
                    }
                }));
    }

    // ═══════════════════════════════════════════════════════
    //                       TOAST
    // ═══════════════════════════════════════════════════════

    private void toast(String msg, String type) {
        Label t = new Label(msg);
        t.getStyleClass().addAll("toast", "toast-text");
        switch (type) {
            case "success": t.getStyleClass().add("toast-success"); break;
            case "error":   t.getStyleClass().add("toast-error");   break;
            default:        t.getStyleClass().add("toast-info");    break;
        }
        t.setOpacity(0); t.setTranslateY(40);

        StackPane rs = (StackPane) root.getParent();
        if (rs == null) return;
        StackPane.setAlignment(t, Pos.BOTTOM_CENTER);
        StackPane.setMargin(t, new Insets(0, 0, 50, 0));
        rs.getChildren().add(t);

        FadeTransition fi = new FadeTransition(Duration.millis(300), t);
        fi.setToValue(1);
        TranslateTransition si = new TranslateTransition(Duration.millis(300), t);
        si.setToY(0); si.setInterpolator(Interpolator.EASE_OUT);
        PauseTransition hold = new PauseTransition(Duration.seconds(3));
        FadeTransition fo = new FadeTransition(Duration.millis(400), t);
        fo.setToValue(0);
        TranslateTransition so = new TranslateTransition(Duration.millis(400), t);
        so.setToY(30);
        fo.setOnFinished(e -> rs.getChildren().remove(t));
        new SequentialTransition(
                new ParallelTransition(fi, si), hold,
                new ParallelTransition(fo, so)
        ).play();
    }

    // ═══════════════════════════════════════════════════════
    //                    ANIMATIONS
    // ═══════════════════════════════════════════════════════

    private void animateCards(List<Node> cards) {
        for (int i = 0; i < cards.size(); i++) {
            Node c = cards.get(i); int delay = i * 60;
            FadeTransition f = new FadeTransition(Duration.millis(450), c);
            f.setFromValue(0); f.setToValue(1); f.setDelay(Duration.millis(delay));
            TranslateTransition s = new TranslateTransition(Duration.millis(450), c);
            s.setFromY(40); s.setToY(0); s.setDelay(Duration.millis(delay));
            s.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition sc = new ScaleTransition(Duration.millis(450), c);
            sc.setFromX(0.95); sc.setFromY(0.95); sc.setToX(1); sc.setToY(1);
            sc.setDelay(Duration.millis(delay)); sc.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(f, s, sc).play();
        }
    }

    private void animateEntry() {
        if (headerWrapper != null) {
            headerWrapper.setOpacity(0);
            FadeTransition f = new FadeTransition(Duration.millis(500), headerWrapper);
            f.setToValue(1); f.play();
        }
        Node[] h = {heroTitle, heroDesc, heroBtns, heroStats};
        for (int i = 0; i < h.length; i++) {
            if (h[i] == null) continue; Node n = h[i];
            n.setOpacity(0); n.setTranslateY(35);
            int d = 300 + i * 180;
            FadeTransition f = new FadeTransition(Duration.millis(700), n);
            f.setToValue(1); f.setDelay(Duration.millis(d));
            TranslateTransition s = new TranslateTransition(Duration.millis(700), n);
            s.setToY(0); s.setDelay(Duration.millis(d));
            s.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
            new ParallelTransition(f, s).play();
        }
    }

    private void setupScrollShadow() {
        scrollPane.vvalueProperty().addListener((obs, ov, nv) -> {
            if (nv.doubleValue() > 0.01) {
                if (!headerWrapper.getStyleClass().contains("header-elevated"))
                    headerWrapper.getStyleClass().add("header-elevated");
            } else headerWrapper.getStyleClass().remove("header-elevated");
        });
    }

    private void smoothScroll(Node target) {
        if (target == null) return;
        Platform.runLater(() -> {
            double y = target.getBoundsInParent().getMinY();
            double max = mainContent.getBoundsInLocal().getHeight()
                    - scrollPane.getViewportBounds().getHeight();
            if (max <= 0) return;
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(650),
                    new KeyValue(scrollPane.vvalueProperty(),
                            Math.min(1, Math.max(0, y / max)),
                            Interpolator.EASE_BOTH)));
            tl.play();
        });
    }

    // ═══════════════════════════════════════════════════════
    private String str(Object o) {
        return o != null ? o.toString() : "";
    }
    private int intV(Object o) {
        if (o == null) return 0;
        if (o instanceof Double) return ((Double) o).intValue();
        if (o instanceof Integer) return (Integer) o;
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }
    private double dbl(Object o) {
        if (o == null) return 0;
        if (o instanceof Double) return (Double) o;
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }
}