package com.chrionline.client.ui.admin;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.client.ui.NavigationManager;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.enums.Action;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ManageOrdersScreen {

    private static final double MAX_W = 1200;
    private static final double H_PAD = 40;

    private static final String ST_EN_ATTENTE = "EN_ATTENTE";
    private static final String ST_VALIDEE = "VALIDEE";
    private static final String ST_EXPEDIEE = "EXPEDIEE";
    private static final String ST_LIVREE = "LIVREE";
    private static final String ST_ANNULEE = "ANNULEE";

    private static final String LB_EN_ATTENTE = "En attente";
    private static final String LB_VALIDEE = "Validee";
    private static final String LB_EXPEDIEE = "Expediee";
    private static final String LB_LIVREE = "Livree";
    private static final String LB_ANNULEE = "Annulee";

    private static final String CL_EN_ATTENTE = "#F39C12";
    private static final String CL_VALIDEE = "#3498DB";
    private static final String CL_EXPEDIEE = "#1ABC9C";
    private static final String CL_LIVREE = "#27AE60";
    private static final String CL_ANNULEE = "#E74C3C";

    private static String[] statuses() {
        return new String[]{ST_EN_ATTENTE, ST_VALIDEE, ST_EXPEDIEE, ST_LIVREE, ST_ANNULEE};
    }
    private static String[] statusLabels() {
        return new String[]{LB_EN_ATTENTE, LB_VALIDEE, LB_EXPEDIEE, LB_LIVREE, LB_ANNULEE};
    }
    private static String[] statusColors() {
        return new String[]{CL_EN_ATTENTE, CL_VALIDEE, CL_EXPEDIEE, CL_LIVREE, CL_ANNULEE};
    }

    private final BorderPane root;
    private VBox ordersContainer;
    private StackPane loadingOverlay;
    private HBox statsRow;

    private List<Map<String, Object>> allOrders = new ArrayList<>();
    private String currentFilter = "TOUTES";
    private String searchQuery = "";

    private final Map<String, Map<String, Object>> detailCache = new HashMap<>();

    public ManageOrdersScreen() {
        root = new BorderPane();
        root.getStyleClass().add("admin-root");

        if (!NavigationManager.isLoggedIn() || !NavigationManager.isAdmin()) {
            root.setCenter(accessDenied());
            return;
        }

        root.setTop(AdminDashboard.buildAdminHeader("Commandes"));

        VBox mainContent = new VBox(20);
        mainContent.setMaxWidth(MAX_W);
        mainContent.setPadding(new Insets(25, H_PAD, 50, H_PAD));
        mainContent.setAlignment(Pos.TOP_CENTER);

        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label pageTitle = new Label("Gestion des commandes");
        pageTitle.getStyleClass().add("admin-page-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        TextField search = new TextField();
        search.setPromptText("Rechercher par ID ou client...");
        search.getStyleClass().add("search-field");
        search.setPrefWidth(240);
        search.textProperty().addListener((o, ov, nv) -> {
            searchQuery = nv.trim().toLowerCase();
            displayOrders();
        });
        Button refreshBtn = new Button("Actualiser");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setStyle("-fx-font-size:12px;-fx-padding:8 16;");
        refreshBtn.setOnAction(e -> loadOrders());
        titleRow.getChildren().addAll(pageTitle, sp, search, refreshBtn);

        statsRow = new HBox(12);
        statsRow.setAlignment(Pos.CENTER);

        Node filterBar = buildFilterBar();
        ordersContainer = new VBox(0);

        mainContent.getChildren().addAll(titleRow, statsRow, filterBar, ordersContainer);

        StackPane centered = new StackPane(mainContent);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setMinWidth(0);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        loadingOverlay = buildLoading();
        root.setCenter(new StackPane(scroll, loadingOverlay));

        loadOrders();

        Platform.runLater(() -> {
            root.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), root);
            ft.setToValue(1);
            ft.play();
        });
    }

    public Node getView() { return root; }

    private Node buildFilterBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(5, 0, 5, 0));

        String[] sts = statuses();
        String[] lbs = statusLabels();

        List<Button> chips = new ArrayList<>();

        Button allChip = filterChip("Toutes", true);
        allChip.setOnAction(e -> {
            currentFilter = "TOUTES";
            chips.forEach(c -> c.getStyleClass().remove("admin-filter-active"));
            allChip.getStyleClass().add("admin-filter-active");
            displayOrders();
        });
        chips.add(allChip);
        bar.getChildren().add(allChip);

        for (int i = 0; i < sts.length; i++) {
            final String status = sts[i];
            final String label = lbs[i];
            Button chip = filterChip(label, false);
            chip.setOnAction(e -> {
                currentFilter = status;
                chips.forEach(c -> c.getStyleClass().remove("admin-filter-active"));
                chip.getStyleClass().add("admin-filter-active");
                displayOrders();
            });
            chips.add(chip);
            bar.getChildren().add(chip);
        }

        return bar;
    }

    private Button filterChip(String text, boolean active) {
        Button b = new Button(text);
        b.getStyleClass().add("admin-filter-chip");
        if (active) b.getStyleClass().add("admin-filter-active");
        return b;
    }

    private void rebuildStats() {
        statsRow.getChildren().clear();
        String[] sts = statuses();
        String[] lbs = statusLabels();
        String[] cls = statusColors();

        long total = allOrders.size();
        statsRow.getChildren().add(miniStat("Total", total, "#6C3483"));

        for (int i = 0; i < sts.length; i++) {
            final String status = sts[i];
            long count = allOrders.stream()
                    .filter(o -> status.equals(str(o.get("status")))).count();
            statsRow.getChildren().add(miniStat(lbs[i], count, cls[i]));
        }
    }

    private VBox miniStat(String label, long value, String color) {
        VBox box = new VBox(2);
        box.getStyleClass().add("admin-mini-stat");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 18, 10, 18));
        HBox.setHgrow(box, Priority.ALWAYS);

        Label val = new Label(String.valueOf(value));
        val.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("admin-mini-stat-label");
        box.getChildren().addAll(val, lbl);
        return box;
    }

    private void displayOrders() {
        ordersContainer.getChildren().clear();
        rebuildStats();

        List<Map<String, Object>> filtered = allOrders;
        if (!"TOUTES".equals(currentFilter)) {
            filtered = filtered.stream()
                    .filter(o -> currentFilter.equals(str(o.get("status"))))
                    .collect(Collectors.toList());
        }
        if (!searchQuery.isEmpty()) {
            filtered = filtered.stream()
                    .filter(o -> str(o.get("orderId")).toLowerCase().contains(searchQuery)
                            || str(o.get("username")).toLowerCase().contains(searchQuery))
                    .collect(Collectors.toList());
        }

        if (filtered.isEmpty()) {
            ordersContainer.getChildren().add(emptyState("Aucune commande trouvee"));
            return;
        }

        String[] sts = statuses();

        HBox header = new HBox();
        header.getStyleClass().add("admin-table-header");
        header.setPadding(new Insets(10, 16, 10, 16));
        header.getChildren().addAll(
                tCol("No Commande", 170), tCol("Client", 120),
                tCol("Montant", 100), tCol("Statut", 120),
                tCol("Date", 140), tCol("Changer statut", 180),
                tCol("", 70));
        ordersContainer.getChildren().add(header);

        for (int i = 0; i < filtered.size(); i++) {
            ordersContainer.getChildren().add(buildOrderRow(filtered.get(i), i, sts));
        }

        for (int i = 1; i < ordersContainer.getChildren().size(); i++) {
            Node n = ordersContainer.getChildren().get(i);
            n.setOpacity(0);
            n.setTranslateY(15);
            int delay = (i - 1) * 30;
            FadeTransition f = new FadeTransition(Duration.millis(300), n);
            f.setToValue(1);
            f.setDelay(Duration.millis(delay));
            TranslateTransition s = new TranslateTransition(Duration.millis(300), n);
            s.setToY(0);
            s.setDelay(Duration.millis(delay));
            s.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(f, s).play();
        }

        Label countLabel = new Label(filtered.size() + " commande(s) affichee(s)");
        countLabel.getStyleClass().add("admin-mini-stat-label");
        countLabel.setPadding(new Insets(8, 0, 0, 0));
        ordersContainer.getChildren().add(countLabel);
    }

    private VBox buildOrderRow(Map<String, Object> order, int index, String[] sts) {
        String orderId = str(order.get("orderId"));
        String status = str(order.get("status"));

        VBox wrapper = new VBox(0);

        HBox row = new HBox();
        row.getStyleClass().add("admin-table-row");
        if (index % 2 == 0) row.getStyleClass().add("admin-table-row-alt");
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        Label idL = tCell(orderId, 170);
        idL.getStyleClass().add("admin-cell-id");

        Label clientL = tCell(str(order.get("username")), 120);

        Label amtL = tCell(String.format("%.2f EUR", dbl(order.get("totalAmount"))), 100);
        amtL.getStyleClass().add("admin-cell-amount");

        Label statusL = new Label(getStatusLabel(status));
        statusL.getStyleClass().add("admin-status-badge");
        statusL.getStyleClass().add("admin-status-" + status.toLowerCase());
        statusL.setMinWidth(110);

        String date = str(order.get("createdAt"));
        Label dateL = tCell(date.length() > 10 ? date.substring(0, 10) : date, 140);
        dateL.getStyleClass().add("admin-cell-date");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(sts);
        statusCombo.setValue(status);
        statusCombo.getStyleClass().add("admin-status-combo");
        statusCombo.setPrefWidth(170);

        statusCombo.setOnAction(e -> {
            String newStatus = statusCombo.getValue();
            if (newStatus != null && !newStatus.equals(status)) {
                updateOrderStatus(orderId, newStatus);
            }
        });

        Button detailBtn = new Button("Details");
        detailBtn.getStyleClass().add("admin-detail-btn");
        detailBtn.setMinWidth(60);

        VBox detailPanel = new VBox(10);
        detailPanel.getStyleClass().add("admin-detail-panel");
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        detailPanel.setPadding(new Insets(16, 20, 16, 40));

        detailBtn.setOnAction(e -> {
            boolean showing = detailPanel.isVisible();
            if (showing) {
                detailPanel.setVisible(false);
                detailPanel.setManaged(false);
                detailBtn.setText("Details");
            } else {
                detailBtn.setText("Fermer");
                if (detailPanel.getChildren().isEmpty()) {
                    loadOrderDetail(orderId, detailPanel);
                }
                detailPanel.setVisible(true);
                detailPanel.setManaged(true);
                detailPanel.setOpacity(0);
                FadeTransition fi = new FadeTransition(Duration.millis(300), detailPanel);
                fi.setToValue(1);
                fi.play();
            }
        });

        row.getChildren().addAll(idL, clientL, amtL, statusL, dateL, statusCombo, detailBtn);
        wrapper.getChildren().addAll(row, detailPanel);
        return wrapper;
    }

    @SuppressWarnings("unchecked")
    private void loadOrderDetail(String orderId, VBox panel) {
        if (detailCache.containsKey(orderId)) {
            renderDetail(detailCache.get(orderId), panel);
            return;
        }

        Label loading = new Label("Chargement des details...");
        loading.getStyleClass().add("admin-mini-stat-label");
        panel.getChildren().add(loading);

        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_ORDER_STATUS,
                        NavigationManager.getToken(), Map.of("orderId", orderId)))
                .thenAccept(res -> Platform.runLater(() -> {
                    panel.getChildren().clear();
                    if (res.isSuccess() && res.getData() != null) {
                        Map<String, Object> data = (Map<String, Object>) res.getData();
                        detailCache.put(orderId, data);
                        renderDetail(data, panel);
                    } else {
                        panel.getChildren().add(new Label("Erreur : "
                                + (res.getMessage() != null ? res.getMessage() : "inconnue")));
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private void renderDetail(Map<String, Object> data, VBox panel) {
        panel.getChildren().clear();

        Label addrTitle = new Label("Adresse de livraison");
        addrTitle.getStyleClass().add("admin-detail-title");
        Label addr = new Label(str(data.get("shippingAddress")));
        addr.getStyleClass().add("admin-detail-text");
        addr.setWrapText(true);
        panel.getChildren().addAll(addrTitle, addr);

        if (data.get("items") != null) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
            if (!items.isEmpty()) {
                Label itemsTitle = new Label("Articles (" + items.size() + ")");
                itemsTitle.getStyleClass().add("admin-detail-title");
                panel.getChildren().add(itemsTitle);

                HBox itemHeader = new HBox();
                itemHeader.setPadding(new Insets(4, 0, 4, 0));
                itemHeader.getChildren().addAll(
                        tCol("Produit", 200), tCol("Qte", 60),
                        tCol("Prix unit.", 100), tCol("Sous-total", 100));
                panel.getChildren().add(itemHeader);

                for (Map<String, Object> item : items) {
                    HBox itemRow = new HBox();
                    itemRow.setPadding(new Insets(3, 0, 3, 0));

                    String productName = str(item.get("productName"));
                    if (productName.isEmpty() || "null".equals(productName)) {
                        productName = str(item.get("productSnapshot"));
                    }

                    itemRow.getChildren().addAll(
                            tCell(productName, 200),
                            tCell(String.valueOf(intV(item.get("quantity"))), 60),
                            tCell(String.format("%.2f EUR", dbl(item.get("unitPrice"))), 100),
                            tCell(String.format("%.2f EUR", dbl(item.get("subtotal"))), 100));
                    panel.getChildren().add(itemRow);
                }
            }
        }

        if (data.get("payment") != null) {
            Map<String, Object> pay = (Map<String, Object>) data.get("payment");
            Label payTitle = new Label("Paiement");
            payTitle.getStyleClass().add("admin-detail-title");

            String payStatus = str(pay.get("status"));
            boolean accepted = "ACCEPTE".equals(payStatus);

            HBox payInfo = new HBox(20);
            payInfo.getChildren().addAll(
                    detailField("Methode", str(pay.get("method"))),
                    detailField("Statut", payStatus),
                    detailField("Montant", String.format("%.2f EUR", dbl(pay.get("amount")))),
                    detailField("Ref.", str(pay.get("transactionRef")))
            );

            Label payInd = new Label(accepted ? "Paiement accepte" : payStatus);
            payInd.setStyle("-fx-font-weight:bold;-fx-text-fill:"
                    + (accepted ? "#27AE60" : "#F39C12") + ";-fx-font-size:12px;");

            panel.getChildren().addAll(payTitle, payInfo, payInd);
        }
    }

    private VBox detailField(String label, String value) {
        VBox f = new VBox(2);
        Label l = new Label(label);
        l.setStyle("-fx-font-size:10px;-fx-text-fill:#95A5A6;-fx-font-weight:bold;");
        Label v = new Label(value != null && !value.isEmpty() ? value : "--");
        v.setStyle("-fx-font-size:12px;-fx-text-fill:#2C3E50;");
        f.getChildren().addAll(l, v);
        return f;
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("status", newStatus);

        ServerConnection.getInstance()
                .sendAsync(new Request(Action.ADMIN_UPDATE_ORDER_STATUS,
                        NavigationManager.getToken(), data))
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        toast("Commande " + orderId + " -> "
                                + getStatusLabel(newStatus), "success");
                        detailCache.remove(orderId);
                        loadOrders();
                    } else {
                        toast(res.getMessage() != null ? res.getMessage()
                                : "Erreur de mise a jour", "error");
                        loadOrders();
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private void loadOrders() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.ADMIN_GET_ALL_ORDERS,
                        NavigationManager.getToken(), null))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null) {
                        allOrders = (List<Map<String, Object>>) res.getData();
                    }
                    displayOrders();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> { showLoading(false); displayOrders(); });
                    return null;
                });
    }

    private String getStatusLabel(String s) {
        String[] sts = statuses();
        String[] lbs = statusLabels();
        for (int i = 0; i < sts.length; i++) {
            if (sts[i].equals(s)) return lbs[i];
        }
        return s;
    }

    private Node accessDenied() {
        VBox b = new VBox(16);
        b.setAlignment(Pos.CENTER);
        b.setPadding(new Insets(80));
        Label m = new Label("Acces refuse");
        m.getStyleClass().add("admin-welcome-title");
        Button bk = new Button("Retour");
        bk.getStyleClass().add("admin-action-btn");
        bk.setOnAction(e -> NavigationManager.showLanding());
        b.getChildren().addAll(m, bk);
        return b;
    }

    private Node emptyState(String msg) {
        VBox b = new VBox(10);
        b.setAlignment(Pos.CENTER);
        b.setPadding(new Insets(40));
        Label t = new Label(msg);
        t.getStyleClass().add("admin-chart-empty");
        b.getChildren().add(t);
        return b;
    }

    private Label tCol(String text, double w) {
        Label l = new Label(text);
        l.getStyleClass().add("admin-table-col-header");
        l.setMinWidth(w);
        l.setMaxWidth(w);
        return l;
    }

    private Label tCell(String text, double w) {
        Label l = new Label(text);
        l.getStyleClass().add("admin-table-cell");
        l.setMinWidth(w);
        l.setMaxWidth(w);
        return l;
    }

    private void toast(String msg, String type) {
        Label t = new Label(msg);
        t.getStyleClass().addAll("toast", "toast-text");
        if ("success".equals(type)) t.getStyleClass().add("toast-success");
        else if ("error".equals(type)) t.getStyleClass().add("toast-error");
        else t.getStyleClass().add("toast-info");
        t.setOpacity(0);
        t.setTranslateY(40);
        StackPane rs = (StackPane) root.getParent();
        if (rs == null) return;
        StackPane.setAlignment(t, Pos.BOTTOM_CENTER);
        StackPane.setMargin(t, new Insets(0, 0, 50, 0));
        rs.getChildren().add(t);
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
        fo.setOnFinished(e -> rs.getChildren().remove(t));
        new SequentialTransition(
                new ParallelTransition(fi, si), hold,
                new ParallelTransition(fo, so)).play();
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
                FadeTransition fi = new FadeTransition(Duration.millis(200), loadingOverlay);
                fi.setToValue(1);
                fi.play();
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
        try { return Integer.parseInt(o.toString()); } catch (NumberFormatException e) { return 0; }
    }
    private double dbl(Object o) {
        if (o == null) return 0;
        if (o instanceof Double) return (Double) o;
        try { return Double.parseDouble(o.toString()); } catch (NumberFormatException e) { return 0; }
    }
}