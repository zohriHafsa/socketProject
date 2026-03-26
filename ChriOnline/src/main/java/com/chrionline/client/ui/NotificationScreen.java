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

public class NotificationScreen {

    private static final double MAX_W = 900;
    private static final double H_PAD = 40;

    private final BorderPane root;
    private final VBox       notifContainer;
    private StackPane        loadingOverlay;
    private String           currentFilter = "all";

    public NotificationScreen() {
        root = new BorderPane();
        root.getStyleClass().add("page-root");
        root.setTop(buildHeader());

        VBox main = new VBox(20);
        main.setMaxWidth(MAX_W);
        main.setPadding(new Insets(30, H_PAD, 50, H_PAD));
        main.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Notifications");
        title.getStyleClass().add("page-title");

        HBox tabs = buildTabs();

        notifContainer = new VBox(10);
        notifContainer.setAlignment(Pos.TOP_CENTER);

        main.getChildren().addAll(title, tabs, notifContainer);

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
        else loadNotifications();

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
        Label title = new Label("Notifications");
        title.getStyleClass().add("page-title");

        inner.getChildren().addAll(back, title);
        w.getChildren().add(inner);
        return w;
    }

    private HBox buildTabs() {
        HBox tabs = new HBox(8);
        tabs.setAlignment(Pos.CENTER_LEFT);

        String[][] tabData = {{"all", "Toutes"}, {"unread", "Non lues"}, {"ORDER", "Commandes"}, {"PAYMENT", "Paiements"}};
        List<Button> tabBtns = new ArrayList<>();

        for (String[] td : tabData) {
            Button btn = new Button(td[1]);
            btn.getStyleClass().add("notif-tab-btn");
            if ("all".equals(td[0])) btn.getStyleClass().add("notif-tab-active");
            btn.setOnAction(e -> {
                currentFilter = td[0];
                tabBtns.forEach(b -> b.getStyleClass().remove("notif-tab-active"));
                btn.getStyleClass().add("notif-tab-active");
                loadNotifications();
            });
            tabBtns.add(btn);
            tabs.getChildren().add(btn);
        }

        return tabs;
    }

    @SuppressWarnings("unchecked")
    private void loadNotifications() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_NOTIFICATIONS, NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId())))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null) {
                        List<Map<String, Object>> notifs = (List<Map<String, Object>>) res.getData();
                        renderNotifications(filterNotifs(notifs));
                    } else showEmpty();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> { showLoading(false); showEmpty(); });
                    return null;
                });
    }

    private List<Map<String, Object>> filterNotifs(List<Map<String, Object>> all) {
        if ("all".equals(currentFilter)) return all;
        if ("unread".equals(currentFilter)) {
            return all.stream().filter(n -> !toBool(n.get("isRead"))).collect(java.util.stream.Collectors.toList());
        }
        return all.stream().filter(n -> currentFilter.equalsIgnoreCase(str(n.get("type")))).collect(java.util.stream.Collectors.toList());
    }

    private void renderNotifications(List<Map<String, Object>> notifs) {
        notifContainer.getChildren().clear();

        if (notifs.isEmpty()) { showEmpty(); return; }

        Label count = new Label(notifs.size() + " notification(s)");
        count.getStyleClass().add("page-subtitle");
        notifContainer.getChildren().add(count);

        List<Node> cards = new ArrayList<>();
        for (Map<String, Object> n : notifs) {
            Node card = buildNotifCard(n);
            card.setOpacity(0); card.setTranslateX(-20);
            notifContainer.getChildren().add(card);
            cards.add(card);
        }

        for (int i = 0; i < cards.size(); i++) {
            Node c = cards.get(i);
            FadeTransition f = new FadeTransition(Duration.millis(350), c); f.setToValue(1); f.setDelay(Duration.millis(i * 60));
            TranslateTransition s = new TranslateTransition(Duration.millis(350), c); s.setToX(0); s.setDelay(Duration.millis(i * 60)); s.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(f, s).play();
        }
    }

    private Node buildNotifCard(Map<String, Object> n) {
        HBox card = new HBox(14);
        card.getStyleClass().add("notif-card");
        card.setAlignment(Pos.CENTER_LEFT);

        boolean isRead = toBool(n.get("isRead"));
        String type = str(n.get("type"));
        String msg = str(n.get("message"));
        String date = str(n.get("createdAt"));

        if (!isRead) card.getStyleClass().add("notif-card-unread");

        // Icon
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("ORDER".equalsIgnoreCase(type) ? "notif-icon-order" : "notif-icon-payment");
        Label iconL = new Label("ORDER".equalsIgnoreCase(type) ? "\uD83D\uDCE6" : "\uD83D\uDCB3");
        iconL.getStyleClass().add("notif-icon-text");
        iconBox.getChildren().add(iconL);

        // Content
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label msgL = new Label(msg);
        msgL.getStyleClass().add(isRead ? "notif-message-read" : "notif-message");
        msgL.setWrapText(true);

        HBox meta = new HBox(10);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label dateL = new Label(fmtDate(date));
        dateL.getStyleClass().add("notif-date");
        Label typeL = new Label(type);
        typeL.getStyleClass().add("notif-type-badge");
        meta.getChildren().addAll(dateL, typeL);

        content.getChildren().addAll(msgL, meta);

        // Unread dot
        if (!isRead) {
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5, javafx.scene.paint.Color.web("#6C3483"));
            card.getChildren().addAll(iconBox, content, dot);
        } else {
            card.getChildren().addAll(iconBox, content);
        }

        return card;
    }

    private void showEmpty() {
        notifContainer.getChildren().clear();
        VBox e = new VBox(14); e.setAlignment(Pos.CENTER); e.setPadding(new Insets(60));
        Label ic = new Label("\uD83D\uDD14"); ic.getStyleClass().add("notif-empty-icon");
        Label tx = new Label("Aucune notification"); tx.getStyleClass().add("empty-state-title");
        Label sub = new Label("Vous serez notifié lors de changements de vos commandes");
        sub.getStyleClass().add("empty-state-text");
        e.getChildren().addAll(ic, tx, sub);
        notifContainer.getChildren().add(e);
    }

    private void showLoginRequired() {
        VBox b = new VBox(16); b.setAlignment(Pos.CENTER); b.setPadding(new Insets(80));
        Label t = new Label("Connectez-vous pour voir vos notifications");
        t.getStyleClass().add("empty-state-title");
        Button btn = new Button("Se connecter"); btn.getStyleClass().add("btn-primary");
        btn.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        btn.setOnAction(e -> NavigationManager.showLogin());
        b.getChildren().addAll(t, btn);
        StackPane c = new StackPane(b); c.setAlignment(Pos.CENTER);
        root.setCenter(c);
    }

    private String fmtDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try { return raw.length() > 16 ? raw.substring(0, 16).replace("T", " ") : raw; } catch (Exception e) { return raw; }
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
    private boolean toBool(Object o) { if (o == null) return false; if (o instanceof Boolean) return (Boolean)o; return Boolean.parseBoolean(o.toString()); }
}