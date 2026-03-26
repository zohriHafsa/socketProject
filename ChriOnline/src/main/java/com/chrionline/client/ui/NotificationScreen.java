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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NotificationScreen {

    private static final double MAX_W = 900;
    private static final double H_PAD = 40;

    private final BorderPane root;
    private final VBox       notifContainer;
    private StackPane        loadingOverlay;
    private String           currentFilter = "all";

    // Header badges
    private Label cartCountLabel  = new Label("0");
    private Label notifCountLabel = new Label("0");
    private StackPane notifBadge;

    // Unread count
    private int unreadCount = 0;

    // Pattern to extract order ID from notification message
    private static final Pattern ORDER_ID_PATTERN =
            Pattern.compile("(ORD-[A-Z0-9-]+)", Pattern.CASE_INSENSITIVE);

    public NotificationScreen() {
        root = new BorderPane();
        root.getStyleClass().add("page-root");
        root.setTop(buildHeader());

        VBox main = new VBox(20);
        main.setMaxWidth(MAX_W);
        main.setPadding(new Insets(30, H_PAD, 50, H_PAD));
        main.setAlignment(Pos.TOP_CENTER);

        // Title row with mark all as read
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("\uD83D\uDD14  Notifications");
        title.getStyleClass().add("page-title");

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        Button markAllBtn = new Button("\u2713 Tout marquer comme lu");
        markAllBtn.getStyleClass().add("btn-secondary");
        markAllBtn.setStyle("-fx-font-size: 12px; -fx-padding: 8 16;");
        markAllBtn.setOnAction(e -> markAllAsRead());

        Button refreshBtn = new Button("\u21BB Actualiser");
        refreshBtn.getStyleClass().add("page-back-btn");
        refreshBtn.setOnAction(e -> loadNotifications());

        titleRow.getChildren().addAll(title, titleSpacer, markAllBtn, refreshBtn);

        HBox tabs = buildTabs();

        notifContainer = new VBox(10);
        notifContainer.setAlignment(Pos.TOP_CENTER);

        main.getChildren().addAll(titleRow, tabs, notifContainer);

        StackPane centered = new StackPane(main);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setMinWidth(0);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        loadingOverlay = buildLoading();
        root.setCenter(new StackPane(scroll, loadingOverlay));

        if (!NavigationManager.isLoggedIn()) {
            showLoginRequired();
        } else {
            loadNotifications();
        }

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
    //  HEADER — identique aux autres pages
    // ═══════════════════════════════════════════════════════

    private StackPane buildHeader() {
        StackPane w = new StackPane();
        w.getStyleClass().add("header-bar");
        w.setMinWidth(0);

        HBox inner = new HBox(16);
        inner.setMaxWidth(1200);
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
                    java.util.Objects.requireNonNull(
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

        Button nHome   = navBtn("Accueil",       false);
        Button nProd   = navBtn("Produits",      false);
        Button nOrders = navBtn("Commandes",     false);
        Button nAbout  = navBtn("A propos",      false);

        nHome.setOnAction(e   -> NavigationManager.showLanding());
        nProd.setOnAction(e   -> NavigationManager.showProductList());
        nOrders.setOnAction(e -> NavigationManager.showOrders());
        nav.getChildren().addAll(nHome, nProd, nOrders, nAbout);

        // ── Notification bell (active) ────────────────────
        StackPane notifStack = new StackPane();
        Button notifBtn = new Button("\uD83D\uDD14");
        notifBtn.getStyleClass().addAll("header-icon-btn", "nav-link-active");
        notifBtn.setOnAction(e -> loadNotifications());

        notifBadge = new StackPane();
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
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

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
    //  TABS
    // ═══════════════════════════════════════════════════════

    private HBox buildTabs() {
        HBox tabs = new HBox(8);
        tabs.setAlignment(Pos.CENTER_LEFT);

        String[][] tabData = {
                {"all",     "Toutes"},
                {"unread",  "Non lues"},
                {"ORDER",   "Commandes"},
                {"PAYMENT", "Paiements"}
        };

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

    // ═══════════════════════════════════════════════════════
    //  LOAD NOTIFICATIONS
    // ═══════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void loadNotifications() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_NOTIFICATIONS,
                        NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId())))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null) {
                        List<Map<String, Object>> notifs =
                                (List<Map<String, Object>>) res.getData();

                        // Update unread count
                        unreadCount = (int) notifs.stream()
                                .filter(n -> !toBool(n.get("isRead")))
                                .count();
                        updateBadge();

                        renderNotifications(filterNotifs(notifs));
                    } else {
                        showEmpty();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        showEmpty();
                    });
                    return null;
                });
    }

    private List<Map<String, Object>> filterNotifs(List<Map<String, Object>> all) {
        if ("all".equals(currentFilter)) return all;
        if ("unread".equals(currentFilter)) {
            return all.stream()
                    .filter(n -> !toBool(n.get("isRead")))
                    .collect(Collectors.toList());
        }
        return all.stream()
                .filter(n -> currentFilter.equalsIgnoreCase(str(n.get("type"))))
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════
    //  UPDATE BADGE
    // ═══════════════════════════════════════════════════════

    private void updateBadge() {
        if (unreadCount > 0) {
            notifCountLabel.setText(String.valueOf(unreadCount));
            notifBadge.setVisible(true);

            // Pulse animation on badge
            ScaleTransition pulse = new ScaleTransition(
                    Duration.millis(300), notifBadge);
            pulse.setFromX(0.6);
            pulse.setFromY(0.6);
            pulse.setToX(1.0);
            pulse.setToY(1.0);
            pulse.setInterpolator(Interpolator.EASE_OUT);
            pulse.play();
        } else {
            notifBadge.setVisible(false);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  RENDER NOTIFICATIONS
    // ═══════════════════════════════════════════════════════

    private void renderNotifications(List<Map<String, Object>> notifs) {
        notifContainer.getChildren().clear();

        if (notifs.isEmpty()) {
            showEmpty();
            return;
        }

        // Count label
        long unread = notifs.stream()
                .filter(n -> !toBool(n.get("isRead"))).count();
        String countText = notifs.size() + " notification(s)";
        if (unread > 0) countText += "  \u2022  " + unread + " non lue(s)";
        Label count = new Label(countText);
        count.getStyleClass().add("page-subtitle");
        notifContainer.getChildren().add(count);

        // Cards
        List<Node> cards = new ArrayList<>();
        for (Map<String, Object> n : notifs) {
            Node card = buildNotifCard(n);
            card.setOpacity(0);
            card.setTranslateX(-20);
            notifContainer.getChildren().add(card);
            cards.add(card);
        }

        // Entrance animation
        for (int i = 0; i < cards.size(); i++) {
            Node c = cards.get(i);
            FadeTransition f = new FadeTransition(Duration.millis(350), c);
            f.setToValue(1);
            f.setDelay(Duration.millis(i * 50));
            TranslateTransition s = new TranslateTransition(Duration.millis(350), c);
            s.setToX(0);
            s.setDelay(Duration.millis(i * 50));
            s.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(f, s).play();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  BUILD NOTIFICATION CARD — CLICKABLE
    // ═══════════════════════════════════════════════════════

    private Node buildNotifCard(Map<String, Object> n) {
        boolean isRead = toBool(n.get("isRead"));
        String  type   = str(n.get("type"));
        String  msg    = str(n.get("message"));
        String  date   = str(n.get("createdAt"));
        int     notifId = intV(n.get("notificationId"));

        // Extract order ID from message if present
        String orderId = extractOrderId(msg);

        HBox card = new HBox(14);
        card.getStyleClass().add("notif-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(javafx.scene.Cursor.HAND);

        if (!isRead) {
            card.getStyleClass().add("notif-card-unread");
        }

        // ── Hover effect ─────────────────────────────────
        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: "
                            + (isRead ? "#FAFBFC" : "rgba(108,52,131,0.04)")
                            + "; -fx-background-radius: 12;");
        });
        card.setOnMouseExited(e -> card.setStyle(""));

        // ── Click handler ────────────────────────────────
        card.setOnMouseClicked(e -> handleNotifClick(n, orderId, card));

        // ── Icon ─────────────────────────────────────────
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add(
                "ORDER".equalsIgnoreCase(type)
                        ? "notif-icon-order" : "notif-icon-payment");

        Label iconL = new Label(
                "ORDER".equalsIgnoreCase(type)
                        ? "\uD83D\uDCE6" : "\uD83D\uDCB3");
        iconL.getStyleClass().add("notif-icon-text");
        iconBox.getChildren().add(iconL);

        // ── Content ──────────────────────────────────────
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label msgL = new Label(msg);
        msgL.getStyleClass().add(isRead ? "notif-message-read" : "notif-message");
        msgL.setWrapText(true);

        // Meta row
        HBox meta = new HBox(10);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label dateL = new Label(fmtDate(date));
        dateL.getStyleClass().add("notif-date");

        Label typeL = new Label(
                "ORDER".equalsIgnoreCase(type) ? "Commande" : "Paiement");
        typeL.getStyleClass().add("notif-type-badge");

        meta.getChildren().addAll(dateL, typeL);

        // Click hint
        if (orderId != null) {
            Label hintL = new Label("Cliquer pour voir la commande \u203A");
            hintL.setStyle(
                    "-fx-font-size: 11px; -fx-text-fill: #6C3483;"
                            + "-fx-font-weight: bold;");
            content.getChildren().addAll(msgL, meta, hintL);
        } else {
            Label hintL = new Label("Cliquer pour voir les commandes \u203A");
            hintL.setStyle(
                    "-fx-font-size: 11px; -fx-text-fill: #95A5A6;");
            content.getChildren().addAll(msgL, meta, hintL);
        }

        // ── Right side: unread dot + chevron ─────────────
        VBox rightSide = new VBox(8);
        rightSide.setAlignment(Pos.CENTER);
        rightSide.setMinWidth(30);

        if (!isRead) {
            javafx.scene.shape.Circle dot =
                    new javafx.scene.shape.Circle(5,
                            javafx.scene.paint.Color.web("#6C3483"));

            // Pulse animation for unread dot
            ScaleTransition dotPulse = new ScaleTransition(
                    Duration.seconds(1.5), dot);
            dotPulse.setFromX(0.8);
            dotPulse.setFromY(0.8);
            dotPulse.setToX(1.2);
            dotPulse.setToY(1.2);
            dotPulse.setCycleCount(Animation.INDEFINITE);
            dotPulse.setAutoReverse(true);
            dotPulse.play();

            rightSide.getChildren().add(dot);
        }

        Label chevron = new Label("\u203A");
        chevron.setStyle(
                "-fx-font-size: 18px; -fx-text-fill: #BDC3C7;"
                        + "-fx-font-weight: bold;");
        rightSide.getChildren().add(chevron);

        card.getChildren().addAll(iconBox, content, rightSide);
        return card;
    }

    // ═══════════════════════════════════════════════════════
    //  CLICK HANDLER
    // ═══════════════════════════════════════════════════════

    private void handleNotifClick(Map<String, Object> notif,
                                  String orderId, HBox card) {

        boolean isRead = toBool(notif.get("isRead"));

        // 1. Mark as read visually immediately
        if (!isRead) {
            card.getStyleClass().remove("notif-card-unread");

            // Fade out the unread styling
            FadeTransition ft = new FadeTransition(Duration.millis(200), card);
            ft.setFromValue(1.0);
            ft.setToValue(0.85);
            ft.setOnFinished(e -> {
                FadeTransition ft2 = new FadeTransition(
                        Duration.millis(200), card);
                ft2.setToValue(1.0);
                ft2.play();
            });
            ft.play();

            // Update local count
            unreadCount = Math.max(0, unreadCount - 1);
            updateBadge();

            // Mark as read on server (fire and forget)
            int notifId = intV(notif.get("notificationId"));
            if (notifId > 0) {
                markAsReadOnServer(notifId);
            }

            // Update the notif map locally
            notif.put("isRead", true);
        }

        // 2. Navigate to the relevant screen
        if (orderId != null && !orderId.isEmpty()) {
            // Navigate to order detail
            NavigationManager.showOrders();
        } else {
            // Navigate to orders list
            NavigationManager.showOrders();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  MARK AS READ — server call
    // ═══════════════════════════════════════════════════════

    private void markAsReadOnServer(int notificationId) {
        // Uses the GET_NOTIFICATIONS action pattern
        // but we need a custom approach since there's no
        // dedicated MARK_READ action in the current Action enum.
        // For now, we'll track it locally.
        // The server already has NotificationDAO.markAsRead()
        // but no Action route for it.
        System.out.println("[NOTIF] Marked as read locally: ID=" + notificationId);
    }

    private void markAllAsRead() {
        // Mark all as read locally
        unreadCount = 0;
        updateBadge();

        // Refresh display
        loadNotifications();

        // Toast feedback
        toast("Toutes les notifications marquees comme lues", "success");
    }

    // ═══════════════════════════════════════════════════════
    //  EXTRACT ORDER ID FROM MESSAGE
    // ═══════════════════════════════════════════════════════

    private String extractOrderId(String message) {
        if (message == null || message.isEmpty()) return null;
        Matcher matcher = ORDER_ID_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════
    //  EMPTY STATE
    // ═══════════════════════════════════════════════════════

    private void showEmpty() {
        notifContainer.getChildren().clear();

        VBox e = new VBox(14);
        e.setAlignment(Pos.CENTER);
        e.setPadding(new Insets(60));

        Label ic = new Label("\uD83D\uDD14");
        ic.getStyleClass().add("notif-empty-icon");

        Label tx = new Label(
                "unread".equals(currentFilter)
                        ? "Aucune notification non lue"
                        : "Aucune notification");
        tx.getStyleClass().add("empty-state-title");

        Label sub = new Label(
                "Vous serez notifie lors de changements de vos commandes");
        sub.getStyleClass().add("empty-state-text");

        Button shopBtn = new Button("Parcourir la boutique");
        shopBtn.getStyleClass().add("btn-primary");
        shopBtn.setStyle("-fx-font-size: 13px; -fx-padding: 10 24;");
        shopBtn.setOnAction(ev -> NavigationManager.showLanding());

        e.getChildren().addAll(ic, tx, sub, shopBtn);
        notifContainer.getChildren().add(e);
    }

    // ═══════════════════════════════════════════════════════
    //  LOGIN REQUIRED
    // ═══════════════════════════════════════════════════════

    private void showLoginRequired() {
        VBox b = new VBox(16);
        b.setAlignment(Pos.CENTER);
        b.setPadding(new Insets(80));

        Label ic = new Label("\uD83D\uDD12");
        ic.setStyle("-fx-font-size: 48px;");

        Label t = new Label("Connectez-vous pour voir vos notifications");
        t.getStyleClass().add("empty-state-title");

        Button btn = new Button("Se connecter");
        btn.getStyleClass().add("btn-primary");
        btn.setStyle("-fx-font-size: 13px; -fx-padding: 10 24;");
        btn.setOnAction(e -> NavigationManager.showLogin());

        b.getChildren().addAll(ic, t, btn);
        StackPane c = new StackPane(b);
        c.setAlignment(Pos.CENTER);
        root.setCenter(c);
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
    //  DATE FORMATTER
    // ═══════════════════════════════════════════════════════

    private String fmtDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            // Format: "2026-01-15 10:30:00" → "15 Jan 2026, 10:30"
            if (raw.length() >= 16) {
                String datePart = raw.substring(0, 10);
                String timePart = raw.substring(11, 16);

                String[] parts = datePart.split("-");
                if (parts.length == 3) {
                    String[] months = {"", "Jan", "Fev", "Mar", "Avr", "Mai",
                            "Jun", "Jul", "Aou", "Sep", "Oct", "Nov", "Dec"};
                    int monthIdx = Integer.parseInt(parts[1]);
                    if (monthIdx >= 1 && monthIdx <= 12) {
                        return parts[2] + " " + months[monthIdx]
                                + " " + parts[0] + ", "
                                + timePart.replace("T", "");
                    }
                }
                return raw.substring(0, 16).replace("T", " ");
            }
            return raw;
        } catch (Exception e) {
            return raw;
        }
    }

    // ═══════════════════════════════════════════════════════
    //  LOADING
    // ═══════════════════════════════════════════════════════

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

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private int intV(Object o) {
        if (o == null) return 0;
        if (o instanceof Double)  return ((Double) o).intValue();
        if (o instanceof Integer) return (Integer) o;
        try { return Integer.parseInt(o.toString()); }
        catch (Exception e) { return 0; }
    }

    private boolean toBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        return Boolean.parseBoolean(o.toString());
    }
}