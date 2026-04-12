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
    private final VBox       ordersContainer;
    private StackPane        loadingOverlay;
    private VBox             detailPanel;

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

        ordersContainer = new VBox(12);
        detailPanel = new VBox(16);
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);

        main.getChildren().addAll(title, ordersContainer, detailPanel);

        StackPane centered = new StackPane(main);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setMinWidth(0);

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
        Label title = new Label("Mes Commandes");
        title.getStyleClass().add("page-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button cartBtn = new Button("Mon panier");
        cartBtn.getStyleClass().add("page-back-btn");
        cartBtn.setOnAction(e -> NavigationManager.showCart());

        inner.getChildren().addAll(back, title, sp, cartBtn);
        w.getChildren().add(inner);
        return w;
    }

    @SuppressWarnings("unchecked")
    private void loadOrders() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_ORDER_HISTORY,
                        NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId())))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null) {
                        renderOrders((List<Map<String, Object>>) res.getData());
                    } else showEmpty("Impossible de charger les commandes");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> { showLoading(false); showEmpty("Erreur réseau"); });
                    return null;
                });
    }

    private void renderOrders(List<Map<String, Object>> orders) {
        ordersContainer.getChildren().clear();
        if (orders.isEmpty()) { showEmpty("Aucune commande"); return; }

        Label count = new Label(orders.size() + " commande(s)");
        count.getStyleClass().add("page-subtitle");
        ordersContainer.getChildren().add(count);

        List<Node> cards = new ArrayList<>();
        for (Map<String, Object> o : orders) {
            Node card = buildOrderCard(o);
            card.setOpacity(0); card.setTranslateY(20);
            ordersContainer.getChildren().add(card);
            cards.add(card);
        }

        for (int i = 0; i < cards.size(); i++) {
            Node c = cards.get(i);
            FadeTransition f = new FadeTransition(Duration.millis(400), c);
            f.setToValue(1); f.setDelay(Duration.millis(i * 70));
            TranslateTransition s = new TranslateTransition(Duration.millis(400), c);
            s.setToY(0); s.setDelay(Duration.millis(i * 70));
            s.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(f, s).play();
        }
    }

    private Node buildOrderCard(Map<String, Object> order) {
        HBox card = new HBox(16);
        card.getStyleClass().add("order-card");
        card.setAlignment(Pos.CENTER_LEFT);

        String orderId = str(order.get("orderId"));
        String status = str(order.get("status"));
        double total = dbl(order.get("totalAmount"));
        String date = str(order.get("createdAt"));
        String addr = str(order.get("shippingAddress"));

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label idL = new Label(orderId);
        idL.getStyleClass().add("order-id");
        Label dateL = new Label(fmtDate(date));
        dateL.getStyleClass().add("order-date");
        info.getChildren().addAll(idL, dateL);
        if (!addr.isEmpty()) {
            Label addrL = new Label(addr);
            addrL.getStyleClass().add("order-address");
            addrL.setWrapText(true);
            info.getChildren().add(addrL);
        }

        Label badge = new Label(fmtStatus(status));
        badge.getStyleClass().addAll("order-status-badge",
                "order-status-" + status.toLowerCase());

        Label amtL = new Label(String.format("%.2f \u20AC", total));
        amtL.getStyleClass().add("order-amount");
        amtL.setMinWidth(100);

        Button viewBtn = new Button("Détails");
        viewBtn.getStyleClass().add("page-back-btn");
        viewBtn.setOnAction(e -> loadDetail(orderId));

        card.getChildren().addAll(info, badge, amtL, viewBtn);

        // Hover
        DropShadow rest = new DropShadow();
        rest.setColor(Color.rgb(0, 0, 0, 0.04)); rest.setRadius(6); rest.setOffsetY(2);
        card.setEffect(rest);
        card.setOnMouseEntered(ev -> {
            DropShadow h = new DropShadow();
            h.setColor(Color.rgb(108, 52, 131, 0.1)); h.setRadius(14); h.setOffsetY(5);
            card.setEffect(h);
            TranslateTransition t = new TranslateTransition(Duration.millis(200), card);
            t.setToY(-2); t.setInterpolator(Interpolator.EASE_OUT); t.play();
        });
        card.setOnMouseExited(ev -> {
            card.setEffect(rest);
            TranslateTransition t = new TranslateTransition(Duration.millis(200), card);
            t.setToY(0); t.play();
        });

        return card;
    }

    @SuppressWarnings("unchecked")
    private void loadDetail(String orderId) {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_ORDER_STATUS,
                        NavigationManager.getToken(),
                        Map.of("orderId", orderId)))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null)
                        showDetail((Map<String, Object>) res.getData());
                }))
                .exceptionally(ex -> { Platform.runLater(() -> showLoading(false)); return null; });
    }

    @SuppressWarnings("unchecked")
    private void showDetail(Map<String, Object> order) {
        detailPanel.getChildren().clear();
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);

        VBox card = new VBox(14);
        card.getStyleClass().add("receipt-card");
        card.setMaxWidth(700);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Button close = new Button("\u2715 Fermer");
        close.getStyleClass().add("page-back-btn");
        close.setOnAction(e -> { detailPanel.setVisible(false); detailPanel.setManaged(false); });
        Label detTitle = new Label("Commande " + str(order.get("orderId")));
        detTitle.getStyleClass().add("receipt-title");
        header.getChildren().addAll(close, detTitle);

        String status = str(order.get("status"));
        Label badge = new Label(fmtStatus(status));
        badge.getStyleClass().addAll("order-status-badge", "order-status-" + status.toLowerCase());

        card.getChildren().addAll(
                header, badge,
                receiptRow("Montant", String.format("%.2f \u20AC", dbl(order.get("totalAmount")))),
                receiptRow("Adresse", str(order.get("shippingAddress"))),
                receiptRow("Créée le", fmtDate(str(order.get("createdAt")))),
                receiptDiv()
        );

        // Items
        if (order.get("items") != null) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
            Label itemsT = new Label("Articles (" + items.size() + ")");
            itemsT.getStyleClass().add("checkout-section-title");
            card.getChildren().add(itemsT);

            for (Map<String, Object> it : items) {
                String name = str(it.get("productName"));
                if (name.isEmpty() || "null".equals(name)) name = str(it.get("productSnapshot"));
                card.getChildren().add(receiptRow(
                        name + " x" + intV(it.get("quantity")),
                        String.format("%.2f \u20AC", dbl(it.get("subtotal")))
                ));
            }
        }

        // Payment
        if (order.get("payment") != null) {
            Map<String, Object> pay = (Map<String, Object>) order.get("payment");
            card.getChildren().addAll(
                    receiptDiv(),
                    receiptRow("Paiement", str(pay.get("status"))),
                    receiptRow("Méthode", str(pay.get("method"))),
                    receiptRow("Référence", str(pay.get("transactionRef")))
            );
        }

        detailPanel.getChildren().add(card);
        detailPanel.setOpacity(0);
        FadeTransition f = new FadeTransition(Duration.millis(400), detailPanel);
        f.setToValue(1); f.play();
    }

    private HBox receiptRow(String label, String value) {
        HBox r = new HBox();
        r.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label); l.getStyleClass().add("receipt-label"); l.setMinWidth(140);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value != null && !value.isEmpty() ? value : "—");
        v.getStyleClass().add("receipt-value"); v.setWrapText(true);
        r.getChildren().addAll(l, sp, v); return r;
    }

    private Region receiptDiv() {
        Region d = new Region(); d.getStyleClass().add("receipt-divider"); return d;
    }

    private void showEmpty(String msg) {
        ordersContainer.getChildren().clear();
        VBox e = new VBox(16); e.setAlignment(Pos.CENTER); e.setPadding(new Insets(60));
        Label ic = new Label("\uD83D\uDCCB"); ic.getStyleClass().add("empty-state-icon");
        Label tx = new Label(msg); tx.getStyleClass().add("empty-state-title");
        Button b = new Button("Explorer"); b.getStyleClass().add("btn-primary");
        b.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        b.setOnAction(ev -> NavigationManager.showLanding());
        e.getChildren().addAll(ic, tx, b); ordersContainer.getChildren().add(e);
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

    private String fmtDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try { return raw.length() > 16 ? raw.substring(0, 16).replace("T", " ") : raw; } catch (Exception e) { return raw; }
    }

    private String fmtStatus(String s) {
        if (s == null) return "";
        switch (s) { case "EN_ATTENTE": return "En attente"; case "VALIDEE": return "Validée";
            case "EXPEDIEE": return "Expédiée"; case "LIVREE": return "Livrée";
            case "ANNULEE": return "Annulée"; default: return s; }
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }
    private int intV(Object o) { if (o == null) return 0; if (o instanceof Double) return ((Double)o).intValue();
        if (o instanceof Integer) return (Integer)o; try{return Integer.parseInt(o.toString());}catch(Exception e){return 0;} }
    private double dbl(Object o) { if (o == null) return 0; if (o instanceof Double) return (Double)o;
        try{return Double.parseDouble(o.toString());}catch(Exception e){return 0;} }
}