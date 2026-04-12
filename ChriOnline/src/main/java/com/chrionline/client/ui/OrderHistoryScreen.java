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

public class OrderHistoryScreen {

    private static final double MAX_W = 1100;
    private static final double H_PAD = 40;

    private final BorderPane root;
    private final VBox       tableBody;        // rows live here
    private StackPane        loadingOverlay;

    // tracks which orderId row is currently expanded (null = none)
    private String           expandedOrderId = null;

    public OrderHistoryScreen() {
        root = new BorderPane();
        root.getStyleClass().add("page-root");
        root.setTop(new HeaderComponent("orders", null, null, null).getView());

        VBox main = new VBox(20);
        main.setMaxWidth(MAX_W);
        main.setPadding(new Insets(30, H_PAD, 50, H_PAD));
        main.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Historique des commandes");
        title.getStyleClass().add("page-title");

        // ── Table wrapper ──────────────────────────────────
        VBox tableWrapper = new VBox(0);
        tableWrapper.getStyleClass().add("order-table-wrapper");
        tableWrapper.setFillWidth(true);

        // Table header
        HBox tableHeader = buildTableHeader();
        tableBody = new VBox(0);
        tableBody.setFillWidth(true);

        tableWrapper.getChildren().addAll(tableHeader, tableBody);
        main.getChildren().addAll(title, tableWrapper);

        StackPane centered = new StackPane(main);
        centered.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        loadingOverlay = buildLoading();
        root.setCenter(new StackPane(scroll, loadingOverlay));

        if (!NavigationManager.isLoggedIn()) showLoginRequired();
        else loadOrders();

        Platform.runLater(() -> {
            root.setOpacity(0);
            FadeTransition f = new FadeTransition(Duration.millis(400), root);
            f.setToValue(1); f.play();
        });
    }

    public Node getView() { return root; }

    // ── Table header row ───────────────────────────────────
    private HBox buildTableHeader() {
        HBox h = new HBox();
        h.getStyleClass().add("order-table-header");
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(10, 16, 10, 16));

        Label colId     = colHeader("N° Commande", 160, true);
        Label colDate   = colHeader("Date",        160, true);
        Label colAddr   = colHeader("Adresse",     0,   false);  // grows
        Label colStatus = colHeader("Statut",      120, true);
        Label colAmt    = colHeader("Montant",     100, true);
        Label colAct    = colHeader("",            80,  true);   // button column

        HBox.setHgrow(colAddr, Priority.ALWAYS);
        colAddr.setMaxWidth(Double.MAX_VALUE);

        h.getChildren().addAll(colId, colDate, colAddr, colStatus, colAmt, colAct);
        return h;
    }

    private Label colHeader(String text, double minW, boolean fixed) {
        Label l = new Label(text);
        l.getStyleClass().add("order-table-col-header");
        if (fixed && minW > 0) { l.setMinWidth(minW); l.setMaxWidth(minW); }
        else if (minW > 0)     { l.setMinWidth(minW); }
        return l;
    }

    // ── Load & render ──────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void loadOrders() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_ORDER_HISTORY,
                        NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId())))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null)
                        renderOrders((List<Map<String, Object>>) res.getData());
                    else showEmpty("Impossible de charger les commandes");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> { showLoading(false); showEmpty("Erreur réseau"); });
                    return null;
                });
    }

    private void renderOrders(List<Map<String, Object>> orders) {
        tableBody.getChildren().clear();
        expandedOrderId = null;

        if (orders.isEmpty()) { showEmpty("Aucune commande"); return; }

        List<Node> rows = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            Map<String, Object> o = orders.get(i);
            VBox rowGroup = buildRowGroup(o, i % 2 == 1);
            rowGroup.setOpacity(0);
            rowGroup.setTranslateY(10);
            tableBody.getChildren().add(rowGroup);
            rows.add(rowGroup);
        }

        // staggered entrance
        for (int i = 0; i < rows.size(); i++) {
            Node r = rows.get(i);
            FadeTransition f = new FadeTransition(Duration.millis(350), r);
            f.setToValue(1); f.setDelay(Duration.millis(i * 50));
            TranslateTransition t = new TranslateTransition(Duration.millis(350), r);
            t.setToY(0); t.setDelay(Duration.millis(i * 50));
            t.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(f, t).play();
        }
    }

    // ── One row group = data row + hidden detail pane ──────
    private VBox buildRowGroup(Map<String, Object> order, boolean alt) {
        VBox group = new VBox(0);
        group.setFillWidth(true);

        String orderId = str(order.get("orderId"));
        String status  = str(order.get("status"));
        double total   = dbl(order.get("totalAmount"));
        String date    = str(order.get("createdAt"));
        String addr    = str(order.get("shippingAddress"));

        // ── Data row ───────────────────────────────────────
        HBox row = new HBox();
        row.getStyleClass().addAll("order-table-row", alt ? "order-table-row-alt" : "");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setFillHeight(true);

        Label idL = new Label("#" + orderId);
        idL.getStyleClass().add("order-cell-id");
        idL.setMinWidth(160); idL.setMaxWidth(160);

        Label dateL = new Label(fmtDate(date));
        dateL.getStyleClass().add("order-cell");
        dateL.setMinWidth(160); dateL.setMaxWidth(160);

        Label addrL = new Label(addr.isEmpty() ? "—" : addr);
        addrL.getStyleClass().add("order-cell");
        addrL.setWrapText(false);
        addrL.setEllipsisString("…");
        HBox.setHgrow(addrL, Priority.ALWAYS);
        addrL.setMaxWidth(Double.MAX_VALUE);

        Label badge = new Label(fmtStatus(status));
        badge.getStyleClass().addAll("order-status-badge", "order-status-" + status.toLowerCase());
        HBox badgeBox = new HBox(badge);
        badgeBox.setAlignment(Pos.CENTER_LEFT);
        badgeBox.setMinWidth(120); badgeBox.setMaxWidth(120);

        Label amtL = new Label(String.format("%.2f €", total));
        amtL.getStyleClass().add("order-cell-amount");
        amtL.setMinWidth(100); amtL.setMaxWidth(100);

        Button viewBtn = new Button("▼  Détails");
        viewBtn.getStyleClass().add("order-detail-btn");
        viewBtn.setMinWidth(80);
        viewBtn.setMaxWidth(80);

        row.getChildren().addAll(idL, dateL, addrL, badgeBox, amtL, viewBtn);

        // ── Detail pane (collapsed by default) ────────────
        VBox detailPane = new VBox();
        detailPane.setVisible(false);
        detailPane.setManaged(false);
        detailPane.getStyleClass().add("order-detail-inline");

        // ── Toggle logic ───────────────────────────────────
        viewBtn.setOnAction(e -> {
            boolean isExpanded = detailPane.isVisible();
            if (isExpanded) {
                // close
                collapsePane(detailPane);
                viewBtn.setText("▼  Détails");
                expandedOrderId = null;
            } else {
                // close any previously open row first
                collapseAllInTable();
                expandedOrderId = orderId;
                viewBtn.setText("▲  Fermer");
                loadDetailInto(orderId, detailPane, viewBtn);
            }
        });

        // hover
        DropShadow hoverShadow = new DropShadow();
        hoverShadow.setColor(Color.rgb(108, 52, 131, 0.08));
        hoverShadow.setRadius(8); hoverShadow.setOffsetY(2);
        row.setOnMouseEntered(ev -> row.setEffect(hoverShadow));
        row.setOnMouseExited(ev -> row.setEffect(null));

        group.getChildren().addAll(row, detailPane);
        return group;
    }

    // ── Collapse all currently expanded rows ───────────────
    private void collapseAllInTable() {
        for (Node n : tableBody.getChildren()) {
            if (n instanceof VBox) {
                VBox group = (VBox) n;
                if (group.getChildren().size() >= 2) {
                    Node detPane = group.getChildren().get(1);
                    if (detPane instanceof VBox && detPane.isVisible()) {
                        collapsePane((VBox) detPane);
                        // reset button text
                        Node rowNode = group.getChildren().get(0);
                        if (rowNode instanceof HBox) {
                            HBox rowHBox = (HBox) rowNode;
                            for (Node child : rowHBox.getChildren()) {
                                if (child instanceof Button) {
                                    ((Button) child).setText("▼  Détails");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void collapsePane(VBox pane) {
        pane.setVisible(false);
        pane.setManaged(false);
        pane.getChildren().clear();
    }

    // ── Load detail and inject into inline pane ────────────
    @SuppressWarnings("unchecked")
    private void loadDetailInto(String orderId, VBox detailPane, Button toggleBtn) {
        toggleBtn.setDisable(true);
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_ORDER_STATUS,
                        NavigationManager.getToken(),
                        Map.of("orderId", orderId)))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    toggleBtn.setDisable(false);
                    if (res.isSuccess() && res.getData() != null)
                        fillDetailPane(detailPane, (Map<String, Object>) res.getData());
                    else {
                        toggleBtn.setText("▼  Détails");
                        expandedOrderId = null;
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        toggleBtn.setDisable(false);
                        toggleBtn.setText("▼  Détails");
                        expandedOrderId = null;
                    });
                    return null;
                });
    }

    @SuppressWarnings("unchecked")
    private void fillDetailPane(VBox detailPane, Map<String, Object> order) {
        detailPane.getChildren().clear();

        VBox inner = new VBox(14);
        inner.getStyleClass().add("order-detail-inner");
        inner.setPadding(new Insets(20, 24, 24, 24));

        // ── Side-by-side: Articles (left) | Paiement (right) ──
        HBox sideBySide = new HBox(50);
        sideBySide.setAlignment(Pos.TOP_LEFT);
        sideBySide.setFillHeight(false);

        // ── LEFT: Articles ────────────────────────────────
        VBox leftCol = new VBox(8);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        if (order.get("items") != null) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
            Label itemsTitle = sectionTitle("Articles (" + items.size() + ")");

            // Fixed column widths shared between header and rows
            final double COL_NAME = Double.MAX_VALUE; // grows
            final double COL_QTY  = 44;
            final double COL_SUB  = 80;

            // mini-table header
            HBox itemHeader = new HBox();
            itemHeader.getStyleClass().add("order-detail-item-header");
            itemHeader.setPadding(new Insets(6, 8, 6, 8));

            Label ih1 = new Label("Produit");
            ih1.getStyleClass().add("order-detail-item-col-header");
            ih1.setMaxWidth(COL_NAME);
            HBox.setHgrow(ih1, Priority.ALWAYS);

            Label ih2 = new Label("Qté");
            ih2.getStyleClass().add("order-detail-item-col-header");
            ih2.setMinWidth(COL_QTY); ih2.setMaxWidth(COL_QTY);

            Label ih3 = new Label("Sous-total");
            ih3.getStyleClass().add("order-detail-item-col-header");
            ih3.setMinWidth(COL_SUB); ih3.setMaxWidth(COL_SUB);

            itemHeader.getChildren().addAll(ih1, ih2, ih3);

            VBox itemsTable = new VBox(0);
            itemsTable.getStyleClass().add("order-detail-items-table");
            itemsTable.getChildren().add(itemHeader);

            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> it = items.get(i);
                String name = str(it.get("productName"));
                if (name.isEmpty() || "null".equals(name)) name = str(it.get("productSnapshot"));

                HBox itemRow = new HBox();
                itemRow.getStyleClass().addAll("order-detail-item-row",
                        i % 2 == 1 ? "order-detail-item-row-alt" : "");
                itemRow.setPadding(new Insets(8, 8, 8, 8));
                itemRow.setAlignment(Pos.CENTER_LEFT);

                Label nameL = new Label(name);
                nameL.getStyleClass().add("order-detail-item-name");
                nameL.setMaxWidth(COL_NAME);
                HBox.setHgrow(nameL, Priority.ALWAYS);

                Label qtyL = new Label("\u00d7" + intV(it.get("quantity")));
                qtyL.getStyleClass().add("order-detail-item-qty");
                qtyL.setMinWidth(COL_QTY); qtyL.setMaxWidth(COL_QTY);

                Label subL = new Label(String.format("%.2f \u20ac", dbl(it.get("subtotal"))));
                subL.getStyleClass().add("order-detail-item-sub");
                subL.setMinWidth(COL_SUB); subL.setMaxWidth(COL_SUB);

                itemRow.getChildren().addAll(nameL, qtyL, subL);
                itemsTable.getChildren().add(itemRow);
            }

            leftCol.getChildren().addAll(itemsTitle, itemsTable);
        }

        // ── RIGHT: Paiement ───────────────────────────────
        VBox rightCol = new VBox(8);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        if (order.get("payment") != null) {
            Map<String, Object> pay = (Map<String, Object>) order.get("payment");
            Label payTitle = sectionTitle("Paiement");
            GridPane payGrid = new GridPane();
            payGrid.setHgap(24); payGrid.setVgap(10);
            ColumnConstraints payLCol = new ColumnConstraints(90);
            ColumnConstraints payVCol = new ColumnConstraints(); payVCol.setHgrow(Priority.ALWAYS);
            payGrid.getColumnConstraints().addAll(payLCol, payVCol);
            addGridRow(payGrid, 0, "Statut",    cell(str(pay.get("status"))));
            addGridRow(payGrid, 1, "M\u00e9thode",   cell(str(pay.get("method"))));
            addGridRow(payGrid, 2, "R\u00e9f\u00e9rence", cell(str(pay.get("transactionRef"))));
            rightCol.getChildren().addAll(payTitle, payGrid);
        }

        // add width bindings
        leftCol.prefWidthProperty().bind(sideBySide.widthProperty().multiply(0.6));
        rightCol.prefWidthProperty().bind(sideBySide.widthProperty().multiply(0.4));

        sideBySide.getChildren().addAll(leftCol, rightCol);
        inner.getChildren().add(sideBySide);

        detailPane.getChildren().add(inner);
        detailPane.setOpacity(0);
        detailPane.setVisible(true);
        detailPane.setManaged(true);

        FadeTransition f = new FadeTransition(Duration.millis(300), detailPane);
        f.setToValue(1); f.play();
    }

    // ── Detail helpers ────────────────────────────────────
    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("order-detail-section-title");
        return l;
    }

    private Label cell(String text) {
        Label l = new Label(text == null || text.isEmpty() ? "—" : text);
        l.getStyleClass().add("order-detail-cell");
        l.setWrapText(true);
        return l;
    }

    private void addGridRow(GridPane grid, int row, String labelText, Node valueNode) {
        Label l = new Label(labelText);
        l.getStyleClass().add("order-detail-label");
        grid.add(l, 0, row);
        grid.add(valueNode, 1, row);
    }

    // ── Empty / login ─────────────────────────────────────
    private void showEmpty(String msg) {
        tableBody.getChildren().clear();
        VBox e = new VBox(16); e.setAlignment(Pos.CENTER); e.setPadding(new Insets(60));
        Label ic = new Label("\uD83D\uDCCB"); ic.getStyleClass().add("empty-state-icon");
        Label tx = new Label(msg); tx.getStyleClass().add("empty-state-title");
        Button b = new Button("Explorer"); b.getStyleClass().add("btn-primary");
        b.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        b.setOnAction(ev -> NavigationManager.showLanding());
        e.getChildren().addAll(ic, tx, b); tableBody.getChildren().add(e);
    }

    private void showLoginRequired() {
        VBox b = new VBox(16); b.setAlignment(Pos.CENTER); b.setPadding(new Insets(80));
        Label t = new Label("Connectez-vous pour voir vos commandes");
        t.getStyleClass().add("empty-state-title");
        Button btn = new Button("Se connecter"); btn.getStyleClass().add("btn-primary");
        btn.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        btn.setOnAction(e -> NavigationManager.showLogin());
        b.getChildren().addAll(t, btn);
        StackPane c = new StackPane(b); c.setAlignment(Pos.CENTER);
        root.setCenter(c);
    }

    private StackPane buildLoading() {
        StackPane o = new StackPane(); o.getStyleClass().add("loading-modern-overlay"); o.setVisible(false);
        VBox c = new VBox(20); c.getStyleClass().add("loading-card"); c.setAlignment(Pos.CENTER);
        c.setMaxWidth(200); c.setMaxHeight(140);
        ProgressIndicator pi = new ProgressIndicator(); pi.getStyleClass().add("loading-spinner"); pi.setMaxSize(40, 40);
        Label tt = new Label("Chargement..."); tt.getStyleClass().add("loading-text");
        c.getChildren().addAll(pi, tt); o.getChildren().add(c); return o;
    }

    private void showLoading(boolean b) {
        Platform.runLater(() -> {
            if (b) { loadingOverlay.setOpacity(0); loadingOverlay.setVisible(true);
                FadeTransition fi = new FadeTransition(Duration.millis(200), loadingOverlay); fi.setToValue(1); fi.play();
            } else { FadeTransition fo = new FadeTransition(Duration.millis(200), loadingOverlay);
                fo.setToValue(0); fo.setOnFinished(e -> loadingOverlay.setVisible(false)); fo.play(); }
        });
    }

    // ── Formatters ────────────────────────────────────────
    private String fmtDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try { return raw.length() > 16 ? raw.substring(0, 16).replace("T", " ") : raw; }
        catch (Exception e) { return raw; }
    }

    private String fmtStatus(String s) {
        if (s == null) return "";
        switch (s) {
            case "EN_ATTENTE": return "En attente";
            case "VALIDEE":    return "Validée";
            case "EXPEDIEE":   return "Expédiée";
            case "LIVREE":     return "Livrée";
            case "ANNULEE":    return "Annulée";
            default:           return s;
        }
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }
    private int intV(Object o) {
        if (o == null) return 0;
        if (o instanceof Double) return ((Double)o).intValue();
        if (o instanceof Integer) return (Integer)o;
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }
    private double dbl(Object o) {
        if (o == null) return 0;
        if (o instanceof Double) return (Double)o;
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }
}