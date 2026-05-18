package com.chrionline.admin.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.admin.ui.AdminNavigationManager;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AdminDashboard {

    private static final double MAX_W = 1200;
    private static final double H_PAD = 40;

    private static final String ST_EN_ATTENTE = "EN_ATTENTE";
    private static final String ST_VALIDEE    = "VALIDEE";
    private static final String ST_EXPEDIEE   = "EXPEDIEE";
    private static final String ST_LIVREE     = "LIVREE";
    private static final String ST_ANNULEE    = "ANNULEE";

    private static final String LB_EN_ATTENTE = "En attente";
    private static final String LB_VALIDEE    = "Validee";
    private static final String LB_EXPEDIEE   = "Expediee";
    private static final String LB_LIVREE     = "Livree";
    private static final String LB_ANNULEE    = "Annulee";

    private static final String CL_EN_ATTENTE = "#F39C12";
    private static final String CL_VALIDEE    = "#3498DB";
    private static final String CL_EXPEDIEE   = "#1ABC9C";
    private static final String CL_LIVREE     = "#27AE60";
    private static final String CL_ANNULEE    = "#E74C3C";

    private static String[] statuses()      { return new String[]{ST_EN_ATTENTE, ST_VALIDEE, ST_EXPEDIEE, ST_LIVREE, ST_ANNULEE}; }
    private static String[] statusLabels()  { return new String[]{LB_EN_ATTENTE, LB_VALIDEE, LB_EXPEDIEE, LB_LIVREE, LB_ANNULEE}; }
    private static String[] statusColors()  { return new String[]{CL_EN_ATTENTE, CL_VALIDEE, CL_EXPEDIEE, CL_LIVREE, CL_ANNULEE}; }

    private final BorderPane root;
    private VBox             contentArea;
    private StackPane        loadingOverlay;

    private List<Map<String, Object>> allOrders   = new ArrayList<>();
    private List<Map<String, Object>> allUsers    = new ArrayList<>();
    private List<Map<String, Object>> allProducts = new ArrayList<>();

    public AdminDashboard() {
        root = new BorderPane();
        root.getStyleClass().add("admin-root");

        if (!AdminNavigationManager.isLoggedIn() || !AdminNavigationManager.isAdmin()) {
            root.setCenter(buildAccessDenied());
            return;
        }

        root.setTop(buildAdminHeader("Dashboard"));

        contentArea = new VBox(28);
        contentArea.setMaxWidth(MAX_W);
        contentArea.setPadding(new Insets(30, H_PAD, 50, H_PAD));
        contentArea.setAlignment(Pos.TOP_CENTER);

        StackPane centered = new StackPane(contentArea);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setMinWidth(0);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        loadingOverlay = buildLoading();
        root.setCenter(new StackPane(scroll, loadingOverlay));

        loadAllData();

        Platform.runLater(() -> {
            root.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), root);
            ft.setToValue(1);
            ft.play();
        });
    }

    public Node getView() { return root; }

    // ================================================================
    //  SHARED ADMIN HEADER
    // ================================================================

    public static StackPane buildAdminHeader(String currentPage) {
        StackPane w = new StackPane();
        w.getStyleClass().add("header-bar");
        w.setMinWidth(0);

        HBox inner = new HBox(12);
        inner.setMaxWidth(MAX_W);
        inner.setPadding(new Insets(0, H_PAD, 0, H_PAD));
        inner.setMinHeight(70);
        inner.setAlignment(Pos.CENTER_LEFT);

        // ── Logo ─────────────────────────────────────────
        HBox logo = new HBox(4);
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setCursor(javafx.scene.Cursor.HAND);
        logo.setOnMouseClicked(e -> AdminNavigationManager.showAdminDashboard());
        try {
            ImageView img = new ImageView(new Image(
                    Objects.requireNonNull(
                            AdminDashboard.class.getResourceAsStream("/images/logo.png"))));
            img.setFitHeight(30);
            img.setPreserveRatio(true);
            img.setSmooth(true);
            logo.getChildren().add(img);
        } catch (Exception ignored) {}

        Label logoFull   = new Label("Chri");
        logoFull.getStyleClass().add("header-logo-text");
        Label logoAccent = new Label("Online");
        logoAccent.getStyleClass().add("header-logo-accent");
        HBox logoText = new HBox(0, logoFull, logoAccent);
        logoText.setAlignment(Pos.CENTER_LEFT);
        logo.getChildren().add(logoText);

        // ── Admin badge ───────────────────────────────────
        Label adminBadge = new Label(
                AdminNavigationManager.isSuperAdmin() ? "SUPER ADMIN" : "ADMIN");
        adminBadge.setStyle(
                "-fx-background-color: #6C3483;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-background-radius: 4;" +
                        "-fx-padding: 2 7;"
        );

        // ── Nav buttons ───────────────────────────────────
        HBox nav = new HBox(2);
        nav.setAlignment(Pos.CENTER);

        Button nDash = adminNavButton("Dashboard",    "Dashboard".equals(currentPage));
        Button nProd = adminNavButton("Produits",     "Produits".equals(currentPage));
        Button nUsers = adminNavButton("Utilisateurs","Utilisateurs".equals(currentPage));
        Button nOrders = adminNavButton("Commandes",  "Commandes".equals(currentPage));
        Button nCats = adminNavButton("Categories",   "Categories".equals(currentPage));

        nDash.setOnAction(e   -> AdminNavigationManager.showAdminDashboard());
        nProd.setOnAction(e   -> AdminNavigationManager.showManageProducts());
        nUsers.setOnAction(e  -> AdminNavigationManager.showManageUsers());
        nOrders.setOnAction(e -> AdminNavigationManager.showManageOrders());
        nCats.setOnAction(e   -> AdminNavigationManager.showManageCategories());

        nav.getChildren().addAll(nDash, nProd, nUsers, nOrders, nCats);

        // ── Bouton Admins — visible SEULEMENT pour SUPER_ADMIN ──
        if (AdminNavigationManager.isSuperAdmin()) {
            Button nAdmins = adminNavButton("Admins", "Admins".equals(currentPage));
            nAdmins.setOnAction(e -> AdminNavigationManager.showManageAdmins());
            nav.getChildren().add(nAdmins);
        }

        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);

        // ── Right side ────────────────────────────────────
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("header-avatar");
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        avatar.setCursor(javafx.scene.Cursor.HAND);
        avatar.setOnMouseClicked(e -> AdminNavigationManager.showProfile());
        javafx.scene.control.Tooltip.install(avatar, new javafx.scene.control.Tooltip("Mon profil"));

        String initials = "";
        String fn = AdminNavigationManager.getFirstName();
        String ln = AdminNavigationManager.getLastName();
        if (fn != null && !fn.isEmpty()) initials += fn.charAt(0);
        if (ln != null && !ln.isEmpty()) initials += ln.charAt(0);
        if (initials.isEmpty()) initials = "A";
        Label initLabel = new Label(initials.toUpperCase());
        initLabel.getStyleClass().add("header-avatar-text");
        avatar.getChildren().add(initLabel);

        Button logoutBtn = new Button("\u23FB");
        logoutBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #6C3483;" +
                        "-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 4 8;" +
                        "-fx-border-color: transparent;");
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(
                "-fx-background-color: rgba(108,52,131,0.12); -fx-text-fill: #6C3483;" +
                        "-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 4 8;" +
                        "-fx-background-radius: 6;"));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #6C3483;" +
                        "-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 4 8;"));
        logoutBtn.setOnAction(e -> {
            ServerConnection.getInstance().sendAsync(
                    new Request(Action.LOGOUT, AdminNavigationManager.getToken(), null));
            AdminNavigationManager.clearSession();
            AdminNavigationManager.showAdminLogin();
        });

        HBox actions = new HBox(10, avatar, logoutBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        inner.getChildren().addAll(logo, adminBadge, sp1, nav, sp2, actions);
        w.getChildren().add(inner);
        return w;
    }

    private static Button adminNavButton(String text, boolean active) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-link");
        if (active) b.getStyleClass().add("nav-link-active");
        return b;
    }

    // ================================================================
    //  DASHBOARD CONTENT
    // ================================================================

    private void buildDashboard() {
        contentArea.getChildren().clear();
        contentArea.getChildren().addAll(
                buildWelcome(),
                buildStatsRow(),
                buildChartsRow(),
                buildRecentOrders(),
                buildQuickActions()
        );
        animateContent();
    }

    private Node buildWelcome() {
        VBox box = new VBox(4);
        box.setPadding(new Insets(0, 0, 6, 0));
        String name = AdminNavigationManager.getFirstName();
        Label welcome = new Label("Bienvenue, " + (name != null ? name : "Admin"));
        welcome.getStyleClass().add("admin-welcome-title");
        Label sub = new Label("Voici un apercu de votre boutique en temps reel");
        sub.getStyleClass().add("admin-welcome-subtitle");
        box.getChildren().addAll(welcome, sub);
        return box;
    }

    private Node buildStatsRow() {
        long activeUsers = allUsers.stream()
                .filter(u -> Boolean.TRUE.equals(u.get("isActive"))).count();
        long inStock = allProducts.stream()
                .filter(p -> intV(p.get("stockQuantity")) > 0).count();
        long pending = allOrders.stream()
                .filter(o -> ST_EN_ATTENTE.equals(str(o.get("status")))).count();
        double revenue = allOrders.stream()
                .filter(o -> {
                    String s = str(o.get("status"));
                    return ST_VALIDEE.equals(s) || ST_EXPEDIEE.equals(s) || ST_LIVREE.equals(s);
                }).mapToDouble(o -> dbl(o.get("totalAmount"))).sum();

        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER);
        row.getChildren().addAll(
                statCard("Utilisateurs", String.valueOf(allUsers.size()),
                        activeUsers + " actif(s)", "admin-stat-blue"),
                statCard("Produits", String.valueOf(allProducts.size()),
                        inStock + " en stock", "admin-stat-purple"),
                statCard("Commandes", String.valueOf(allOrders.size()),
                        pending + " en attente", "admin-stat-green"),
                statCard("Revenus", String.format("%.0f MAD", revenue),
                        "commandes validees", "admin-stat-orange")
        );
        return row;
    }

    private VBox statCard(String title, String value, String subtitle, String styleClass) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("admin-stat-card", styleClass);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(22));
        card.setMinWidth(0);
        HBox.setHgrow(card, Priority.ALWAYS);
        Label ti = new Label(title); ti.getStyleClass().add("admin-stat-card-title");
        Label va = new Label(value); va.getStyleClass().add("admin-stat-card-value");
        Label su = new Label(subtitle); su.getStyleClass().add("admin-stat-card-subtitle");
        card.getChildren().addAll(ti, va, su);
        card.setOnMouseEntered(e -> scaleAnim(card, 1.03));
        card.setOnMouseExited(e  -> scaleAnim(card, 1.0));
        return card;
    }

    private Node buildChartsRow() {
        HBox row = new HBox(22);
        row.setAlignment(Pos.TOP_CENTER);

        String[] sts = statuses();
        String[] lbs = statusLabels();

        // PieChart
        VBox pieBox = new VBox(12);
        pieBox.getStyleClass().add("admin-chart-card");
        HBox.setHgrow(pieBox, Priority.ALWAYS);
        pieBox.setAlignment(Pos.TOP_CENTER);
        pieBox.setPadding(new Insets(20));
        Label pieTitle = new Label("Repartition des commandes");
        pieTitle.getStyleClass().add("admin-chart-title");

        Map<String, Long> statusCounts = allOrders.stream()
                .collect(Collectors.groupingBy(o -> str(o.get("status")), Collectors.counting()));

        PieChart pie = new PieChart();
        pie.setLegendSide(Side.BOTTOM);
        pie.setLabelsVisible(true);
        pie.setMaxHeight(280);
        pie.getStyleClass().add("admin-pie-chart");

        for (int i = 0; i < sts.length; i++) {
            long count = statusCounts.getOrDefault(sts[i], 0L);
            if (count > 0)
                pie.getData().add(new PieChart.Data(lbs[i] + " (" + count + ")", count));
        }

        Platform.runLater(() -> {
            String[] colors = statusColors();
            for (PieChart.Data data : pie.getData()) {
                for (int i = 0; i < lbs.length; i++) {
                    if (data.getName().startsWith(lbs[i]) && data.getNode() != null) {
                        data.getNode().setStyle("-fx-pie-color: " + colors[i] + ";");
                        break;
                    }
                }
            }
        });

        pieBox.getChildren().addAll(pieTitle,
                pie.getData().isEmpty() ? emptyLabel("Aucune commande") : pie);

        // BarChart
        VBox barBox = new VBox(12);
        barBox.getStyleClass().add("admin-chart-card");
        HBox.setHgrow(barBox, Priority.ALWAYS);
        barBox.setAlignment(Pos.TOP_CENTER);
        barBox.setPadding(new Insets(20));
        Label barTitle = new Label("Revenus par statut (MAD)");
        barTitle.getStyleClass().add("admin-chart-title");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        yAxis.setLabel("MAD");
        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setLegendVisible(false);
        bar.setMaxHeight(280);
        bar.setAnimated(true);
        bar.setCategoryGap(16);
        bar.getStyleClass().add("admin-bar-chart");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < sts.length; i++) {
            final int idx = i;
            double amount = allOrders.stream()
                    .filter(o -> sts[idx].equals(str(o.get("status"))))
                    .mapToDouble(o -> dbl(o.get("totalAmount"))).sum();
            series.getData().add(new XYChart.Data<>(lbs[i], amount));
        }
        bar.getData().add(series);

        Platform.runLater(() -> {
            String[] colors = statusColors();
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    for (int i = 0; i < lbs.length; i++) {
                        if (d.getXValue().equals(lbs[i])) {
                            d.getNode().setStyle("-fx-bar-fill: " + colors[i] + ";");
                            break;
                        }
                    }
                }
            }
        });

        barBox.getChildren().addAll(barTitle, bar);
        row.getChildren().addAll(pieBox, barBox);
        return row;
    }

    private Node buildRecentOrders() {
        VBox section = new VBox(12);
        section.getStyleClass().add("admin-section-card");
        section.setPadding(new Insets(22));

        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Commandes recentes");
        title.getStyleClass().add("admin-section-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button viewAll = new Button("Voir toutes ->");
        viewAll.getStyleClass().add("admin-link-btn");
        viewAll.setOnAction(e -> AdminNavigationManager.showManageOrders());
        titleRow.getChildren().addAll(title, sp, viewAll);

        HBox header = new HBox();
        header.getStyleClass().add("admin-table-header");
        header.setPadding(new Insets(10, 16, 10, 16));
        header.getChildren().addAll(
                tCol("No Commande", 180), tCol("Client", 140),
                tCol("Montant", 130), tCol("Statut", 140), tCol("Date", 180));

        VBox rows = new VBox(0);
        List<Map<String, Object>> recent = allOrders.stream()
                .limit(8).collect(Collectors.toList());

        for (int i = 0; i < recent.size(); i++) {
            Map<String, Object> o = recent.get(i);
            String status = str(o.get("status"));
            HBox tableRow = new HBox();
            tableRow.getStyleClass().add("admin-table-row");
            if (i % 2 == 0) tableRow.getStyleClass().add("admin-table-row-alt");
            tableRow.setPadding(new Insets(11, 16, 11, 16));

            Label idL = tCell(str(o.get("orderId")), 180);
            idL.getStyleClass().add("admin-cell-id");
            Label clientL = tCell(str(o.get("username")), 140);
            Label amtL = tCell(String.format("%.2f MAD", dbl(o.get("totalAmount"))), 130);
            amtL.getStyleClass().add("admin-cell-amount");
            Label statusL = new Label(getStatusLabel(status));
            statusL.getStyleClass().addAll("admin-status-badge",
                    "admin-status-" + status.toLowerCase());
            
            HBox statusBox = new HBox(statusL);
            statusBox.setAlignment(Pos.CENTER_LEFT);
            statusBox.setMinWidth(140);
            statusBox.setMaxWidth(140);
            
            String date = str(o.get("createdAt"));
            if (date.length() > 16) date = date.substring(0, 16);
            Label dateL = tCell(date, 180);
            dateL.getStyleClass().add("admin-cell-date");

            tableRow.getChildren().addAll(idL, clientL, amtL, statusBox, dateL);
            rows.getChildren().add(tableRow);
        }
        if (recent.isEmpty()) rows.getChildren().add(emptyLabel("Aucune commande"));

        section.getChildren().addAll(titleRow, header, rows);
        return section;
    }

    private Node buildQuickActions() {
        Label title = new Label("Actions rapides");
        title.getStyleClass().add("admin-section-title");

        HBox cards = new HBox(20);
        cards.setAlignment(Pos.CENTER);
        cards.getChildren().addAll(
                actionCard("Gerer les produits", "Ajouter, modifier, supprimer",
                        allProducts.size() + " produit(s)",
                        e -> AdminNavigationManager.showManageProducts()),
                actionCard("Gerer les utilisateurs", "Activer, desactiver les comptes",
                        allUsers.size() + " utilisateur(s)",
                        e -> AdminNavigationManager.showManageUsers()),
                actionCard("Gerer les commandes", "Suivre et mettre a jour",
                        allOrders.size() + " commande(s)",
                        e -> AdminNavigationManager.showManageOrders()),
                actionCard("Gerer les categories", "Ajouter, modifier, supprimer",
                        "Categories de produits",
                        e -> AdminNavigationManager.showManageCategories())
        );

        // Carte spéciale SUPER_ADMIN
        if (AdminNavigationManager.isSuperAdmin()) {
            cards.getChildren().add(
                    actionCard("Gerer les admins", "Ajouter, activer, desactiver",
                            "Super Admin seulement",
                            e -> AdminNavigationManager.showManageAdmins())
            );
        }

        return new VBox(14, title, cards);
    }

    private VBox actionCard(String title, String desc, String count,
                            javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        VBox card = new VBox(10);
        card.getStyleClass().add("admin-action-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(26, 20, 26, 20));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setCursor(javafx.scene.Cursor.HAND);

        Label ti = new Label(title); ti.getStyleClass().add("admin-action-title");
        Label de = new Label(desc);  de.getStyleClass().add("admin-action-desc");
        Label ct = new Label(count); ct.getStyleClass().add("admin-action-count");
        Button btn = new Button("Acceder ->");
        btn.getStyleClass().add("admin-action-btn");
        btn.setOnAction(action);
        card.getChildren().addAll(ti, de, ct, btn);

        DropShadow rest  = makeShadow(0.06, 10, 3);
        DropShadow hover = makeShadow(0.15, 20, 8);
        hover.setColor(Color.rgb(108, 52, 131, 0.15));
        card.setEffect(rest);
        card.setOnMouseEntered(e -> { card.setEffect(hover); scaleAnim(card, 1.03); });
        card.setOnMouseExited(e  -> { card.setEffect(rest);  scaleAnim(card, 1.0);  });
        return card;
    }

    // ================================================================
    //  DATA LOADING
    // ================================================================

    @SuppressWarnings("unchecked")
    private void loadAllData() {
        showLoading(true);
        Thread loader = new Thread(() -> {
            ServerConnection conn = ServerConnection.getInstance();
            String token = AdminNavigationManager.getToken();
            Response rOrders   = conn.send(new Request(Action.ADMIN_GET_ALL_ORDERS, token, null));
            Response rUsers    = conn.send(new Request(Action.ADMIN_GET_USERS, token, null));
            Response rProducts = conn.send(new Request(Action.GET_PRODUCTS, token, null));

            Platform.runLater(() -> {
                showLoading(false);
                if (rOrders.isSuccess()   && rOrders.getData()   != null)
                    allOrders   = (List<Map<String, Object>>) rOrders.getData();
                if (rUsers.isSuccess()    && rUsers.getData()    != null)
                    allUsers    = (List<Map<String, Object>>) rUsers.getData();
                if (rProducts.isSuccess() && rProducts.getData() != null)
                    allProducts = (List<Map<String, Object>>) rProducts.getData();
                buildDashboard();
            });
        }, "AdminLoader");
        loader.setDaemon(true);
        loader.start();
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    private Node buildAccessDenied() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(80));
        Label msg = new Label("Acces refuse");
        msg.getStyleClass().add("admin-welcome-title");
        Label sub = new Label("Vous devez etre administrateur.");
        sub.getStyleClass().add("admin-welcome-subtitle");
        Button back = new Button("Retour");
        back.getStyleClass().add("admin-action-btn");
        back.setOnAction(e -> AdminNavigationManager.showAdminLogin());
        box.getChildren().addAll(msg, sub, back);
        return box;
    }

    private Label tCol(String text, double w) {
        Label l = new Label(text);
        l.getStyleClass().add("admin-table-col-header");
        l.setMinWidth(w); l.setMaxWidth(w);
        return l;
    }

    private Label tCell(String text, double w) {
        Label l = new Label(text);
        l.getStyleClass().add("admin-table-cell");
        l.setMinWidth(w); l.setMaxWidth(w);
        return l;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("admin-chart-empty");
        l.setPadding(new Insets(30));
        return l;
    }

    private String getStatusLabel(String s) {
        String[] sts = statuses();
        String[] lbs = statusLabels();
        for (int i = 0; i < sts.length; i++)
            if (sts[i].equals(s)) return lbs[i];
        return s;
    }

    private DropShadow makeShadow(double opacity, double radius, double offsetY) {
        DropShadow ds = new DropShadow();
        ds.setColor(Color.rgb(0, 0, 0, opacity));
        ds.setRadius(radius);
        ds.setOffsetY(offsetY);
        return ds;
    }

    private void scaleAnim(Node n, double to) {
        ScaleTransition s = new ScaleTransition(Duration.millis(200), n);
        s.setToX(to); s.setToY(to); s.play();
    }

    private void animateContent() {
        for (int i = 0; i < contentArea.getChildren().size(); i++) {
            Node n = contentArea.getChildren().get(i);
            n.setOpacity(0); n.setTranslateY(25);
            int delay = i * 80;
            FadeTransition f = new FadeTransition(Duration.millis(450), n);
            f.setToValue(1); f.setDelay(Duration.millis(delay));
            TranslateTransition s = new TranslateTransition(Duration.millis(450), n);
            s.setToY(0); s.setDelay(Duration.millis(delay));
            s.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(f, s).play();
        }
    }

    private StackPane buildLoading() {
        StackPane o = new StackPane();
        o.getStyleClass().add("loading-modern-overlay");
        o.setVisible(false);
        VBox c = new VBox(20); c.getStyleClass().add("loading-card");
        c.setAlignment(Pos.CENTER); c.setMaxWidth(200); c.setMaxHeight(140);
        ProgressIndicator pi = new ProgressIndicator();
        pi.getStyleClass().add("loading-spinner"); pi.setMaxSize(40, 40);
        Label t = new Label("Chargement..."); t.getStyleClass().add("loading-text");
        c.getChildren().addAll(pi, t); o.getChildren().add(c);
        return o;
    }

    private void showLoading(boolean b) {
        Platform.runLater(() -> {
            if (b) {
                loadingOverlay.setOpacity(0);
                loadingOverlay.setVisible(true);
                FadeTransition fi = new FadeTransition(Duration.millis(200), loadingOverlay);
                fi.setToValue(1); fi.play();
            } else {
                FadeTransition fo = new FadeTransition(Duration.millis(200), loadingOverlay);
                fo.setToValue(0);
                fo.setOnFinished(e -> loadingOverlay.setVisible(false));
                fo.play();
            }
        });
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }
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