package com.chrionline.client.ui.admin;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.client.ui.NavigationManager;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.enums.Action;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ManageUsersScreen {

    private static final double MAX_W = 1200;
    private static final double H_PAD = 40;

    private final BorderPane root;
    private VBox usersContainer;
    private StackPane loadingOverlay;
    private HBox statsRow;
    private Label resultLabel;
    private HBox chartRow;

    private List<Map<String, Object>> allUsers = new ArrayList<>();
    private String currentFilter = "TOUS";
    private String searchQuery = "";

    public ManageUsersScreen() {
        root = new BorderPane();
        root.getStyleClass().add("admin-root");

        if (!NavigationManager.isLoggedIn() || !NavigationManager.isAdmin()) {
            root.setCenter(accessDenied());
            return;
        }

        root.setTop(AdminDashboard.buildAdminHeader("Utilisateurs"));

        VBox mainContent = new VBox(20);
        mainContent.setMaxWidth(MAX_W);
        mainContent.setPadding(new Insets(25, H_PAD, 50, H_PAD));
        mainContent.setAlignment(Pos.TOP_CENTER);

        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label pageTitle = new Label("Gestion des utilisateurs");
        pageTitle.getStyleClass().add("admin-page-title");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        TextField search = new TextField();
        search.setPromptText("Rechercher par nom ou email...");
        search.getStyleClass().add("search-field");
        search.setPrefWidth(260);
        search.textProperty().addListener((o, ov, nv) -> {
            searchQuery = nv.trim().toLowerCase();
            displayUsers();
        });

        Button refreshBtn = new Button("Actualiser");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setStyle("-fx-font-size:12px;-fx-padding:8 16;");
        refreshBtn.setOnAction(e -> loadUsers());

        titleRow.getChildren().addAll(pageTitle, sp, search, refreshBtn);

        statsRow = new HBox(12);
        statsRow.setAlignment(Pos.CENTER);

        chartRow = new HBox(22);
        chartRow.setAlignment(Pos.TOP_CENTER);

        Node toolbar = buildToolbar();

        usersContainer = new VBox(0);

        resultLabel = new Label("");
        resultLabel.getStyleClass().add("admin-mini-stat-label");

        mainContent.getChildren().addAll(titleRow, statsRow, chartRow, toolbar, usersContainer, resultLabel);

        StackPane centered = new StackPane(mainContent);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setMinWidth(0);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        loadingOverlay = buildLoading();
        root.setCenter(new StackPane(scroll, loadingOverlay));

        loadUsers();

        Platform.runLater(() -> {
            root.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), root);
            ft.setToValue(1);
            ft.play();
        });
    }

    public Node getView() {
        return root;
    }

    private Node buildToolbar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        List<Button> filterBtns = new ArrayList<>();
        String[][] filters = {
                {"TOUS", "Tous"},
                {"ACTIFS", "Actifs"},
                {"INACTIFS", "Inactifs"},
                {"ADMIN", "Admins"},
                {"CLIENT", "Clients"}
        };

        HBox filterChips = new HBox(6);
        filterChips.setAlignment(Pos.CENTER_RIGHT);

        for (String[] f : filters) {
            Button chip = new Button(f[1]);
            chip.getStyleClass().add("admin-filter-chip");
            if ("TOUS".equals(f[0])) chip.getStyleClass().add("admin-filter-active");
            chip.setOnAction(e -> {
                currentFilter = f[0];
                filterBtns.forEach(c -> c.getStyleClass().remove("admin-filter-active"));
                chip.getStyleClass().add("admin-filter-active");
                displayUsers();
            });
            filterBtns.add(chip);
            filterChips.getChildren().add(chip);
        }

        bar.getChildren().addAll(sp, filterChips);
        return bar;
    }

    private void rebuildCharts() {
        chartRow.getChildren().clear();

        long active = allUsers.stream()
                .filter(u -> Boolean.TRUE.equals(u.get("isActive")))
                .count();
        long inactive = allUsers.size() - active;

        VBox pieBox = new VBox(10);
        pieBox.getStyleClass().add("admin-chart-card");
        HBox.setHgrow(pieBox, Priority.ALWAYS);
        pieBox.setAlignment(Pos.TOP_CENTER);
        pieBox.setPadding(new Insets(18));

        Label pieTitle = new Label("Statut des comptes");
        pieTitle.getStyleClass().add("admin-chart-title");

        PieChart pie = new PieChart();
        pie.setLegendSide(Side.BOTTOM);
        pie.setMaxHeight(240);
        pie.getStyleClass().add("admin-pie-chart");

        if (active > 0) pie.getData().add(new PieChart.Data("Actifs (" + active + ")", active));
        if (inactive > 0) pie.getData().add(new PieChart.Data("Inactifs (" + inactive + ")", inactive));

        Platform.runLater(() -> {
            for (PieChart.Data d : pie.getData()) {
                if (d.getNode() != null) {
                    if (d.getName().startsWith("Actifs")) {
                        d.getNode().setStyle("-fx-pie-color: #27AE60;");
                    } else {
                        d.getNode().setStyle("-fx-pie-color: #E74C3C;");
                    }
                }
            }
        });

        pieBox.getChildren().addAll(pieTitle, pie);

        long admins = allUsers.stream()
                .filter(u -> "ADMIN".equals(str(u.get("role"))))
                .count();
        long clients = allUsers.size() - admins;

        VBox roleBox = new VBox(10);
        roleBox.getStyleClass().add("admin-chart-card");
        HBox.setHgrow(roleBox, Priority.ALWAYS);
        roleBox.setAlignment(Pos.TOP_CENTER);
        roleBox.setPadding(new Insets(18));

        Label roleTitle = new Label("Repartition des roles");
        roleTitle.getStyleClass().add("admin-chart-title");

        PieChart rolePie = new PieChart();
        rolePie.setLegendSide(Side.BOTTOM);
        rolePie.setMaxHeight(240);
        rolePie.getStyleClass().add("admin-pie-chart");

        if (admins > 0) rolePie.getData().add(new PieChart.Data("Admins (" + admins + ")", admins));
        if (clients > 0) rolePie.getData().add(new PieChart.Data("Clients (" + clients + ")", clients));

        Platform.runLater(() -> {
            for (PieChart.Data d : rolePie.getData()) {
                if (d.getNode() != null) {
                    if (d.getName().startsWith("Admins")) {
                        d.getNode().setStyle("-fx-pie-color: #8E44AD;");
                    } else {
                        d.getNode().setStyle("-fx-pie-color: #3498DB;");
                    }
                }
            }
        });

        roleBox.getChildren().addAll(roleTitle, rolePie);

        chartRow.getChildren().addAll(pieBox, roleBox);
    }

    private void rebuildStats() {
        statsRow.getChildren().clear();

        long total = allUsers.size();
        long active = allUsers.stream().filter(u -> Boolean.TRUE.equals(u.get("isActive"))).count();
        long inactive = total - active;
        long admins = allUsers.stream().filter(u -> "ADMIN".equals(str(u.get("role")))).count();
        long clients = total - admins;

        statsRow.getChildren().addAll(
                miniStat("Total", String.valueOf(total), "#6C3483"),
                miniStat("Actifs", String.valueOf(active), "#27AE60"),
                miniStat("Inactifs", String.valueOf(inactive), "#E74C3C"),
                miniStat("Admins", String.valueOf(admins), "#8E44AD"),
                miniStat("Clients", String.valueOf(clients), "#3498DB")
        );
    }

    private VBox miniStat(String label, String value, String color) {
        VBox box = new VBox(2);
        box.getStyleClass().add("admin-mini-stat");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 16, 10, 16));
        HBox.setHgrow(box, Priority.ALWAYS);

        Label val = new Label(value);
        val.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");

        Label lbl = new Label(label);
        lbl.getStyleClass().add("admin-mini-stat-label");

        box.getChildren().addAll(val, lbl);
        return box;
    }

    private void displayUsers() {
        usersContainer.getChildren().clear();
        rebuildStats();
        rebuildCharts();

        List<Map<String, Object>> filtered = allUsers;

        switch (currentFilter) {
            case "ACTIFS":
                filtered = filtered.stream()
                        .filter(u -> Boolean.TRUE.equals(u.get("isActive")))
                        .collect(Collectors.toList());
                break;
            case "INACTIFS":
                filtered = filtered.stream()
                        .filter(u -> !Boolean.TRUE.equals(u.get("isActive")))
                        .collect(Collectors.toList());
                break;
            case "ADMIN":
                filtered = filtered.stream()
                        .filter(u -> "ADMIN".equals(str(u.get("role"))))
                        .collect(Collectors.toList());
                break;
            case "CLIENT":
                filtered = filtered.stream()
                        .filter(u -> "CLIENT".equals(str(u.get("role"))))
                        .collect(Collectors.toList());
                break;
        }

        if (!searchQuery.isEmpty()) {
            filtered = filtered.stream()
                    .filter(u -> str(u.get("username")).toLowerCase().contains(searchQuery)
                            || str(u.get("email")).toLowerCase().contains(searchQuery)
                            || str(u.get("firstName")).toLowerCase().contains(searchQuery)
                            || str(u.get("lastName")).toLowerCase().contains(searchQuery))
                    .collect(Collectors.toList());
        }

        resultLabel.setText(filtered.size() + " utilisateur(s) affiche(s)");

        if (filtered.isEmpty()) {
            usersContainer.getChildren().add(emptyState("Aucun utilisateur trouve"));
            return;
        }

        HBox header = new HBox();
        header.getStyleClass().add("admin-table-header");
        header.setPadding(new Insets(10, 16, 10, 16));
        header.getChildren().addAll(
                tCol("ID", 50),
                tCol("Utilisateur", 140),
                tCol("Email", 200),
                tCol("Nom complet", 160),
                tCol("Role", 80),
                tCol("Statut", 100),
                tCol("Inscrit le", 120),
                tCol("Action", 120)
        );
        usersContainer.getChildren().add(header);

        for (int i = 0; i < filtered.size(); i++) {
            usersContainer.getChildren().add(buildUserRow(filtered.get(i), i));
        }

        for (int i = 1; i < usersContainer.getChildren().size(); i++) {
            Node n = usersContainer.getChildren().get(i);
            n.setOpacity(0);
            n.setTranslateY(12);
            int delay = (i - 1) * 25;
            FadeTransition f = new FadeTransition(Duration.millis(280), n);
            f.setToValue(1);
            f.setDelay(Duration.millis(delay));
            TranslateTransition s = new TranslateTransition(Duration.millis(280), n);
            s.setToY(0);
            s.setDelay(Duration.millis(delay));
            s.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(f, s).play();
        }
    }

    private HBox buildUserRow(Map<String, Object> user, int index) {
        HBox row = new HBox();
        row.getStyleClass().add("admin-table-row");
        if (index % 2 == 0) row.getStyleClass().add("admin-table-row-alt");
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        int userId = intV(user.get("userId"));
        String username = str(user.get("username"));
        String email = str(user.get("email"));
        String firstName = str(user.get("firstName"));
        String lastName = str(user.get("lastName"));
        String role = str(user.get("role"));
        boolean active = Boolean.TRUE.equals(user.get("isActive"));
        String createdAt = str(user.get("createdAt"));

        Label idL = tCell(String.valueOf(userId), 50);
        idL.getStyleClass().add("admin-cell-id");

        HBox userCell = new HBox(8);
        userCell.setAlignment(Pos.CENTER_LEFT);
        userCell.setMinWidth(140);
        userCell.setMaxWidth(140);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("admin-user-avatar");
        avatar.setMinSize(30, 30);
        avatar.setMaxSize(30, 30);

        String initials = "";
        if (!firstName.isEmpty()) initials += firstName.charAt(0);
        if (!lastName.isEmpty()) initials += lastName.charAt(0);
        if (initials.isEmpty() && !username.isEmpty()) initials = username.substring(0, 1);

        Label initLabel = new Label(initials.toUpperCase());
        initLabel.setStyle("-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;");
        avatar.getChildren().add(initLabel);

        if ("ADMIN".equals(role)) {
            avatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#8E44AD,#6C3483);-fx-background-radius:50;");
        } else {
            avatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#3498DB,#2980B9);-fx-background-radius:50;");
        }

        Label unameL = new Label(username);
        unameL.setStyle("-fx-font-weight:600;-fx-font-size:12px;-fx-text-fill:#2C3E50;");
        userCell.getChildren().addAll(avatar, unameL);

        Label emailL = tCell(email.length() > 26 ? email.substring(0, 24) + ".." : email, 200);
        emailL.getStyleClass().add("admin-cell-date");

        String fullName = (firstName + " " + lastName).trim();
        Label nameL = tCell(fullName.isEmpty() ? "--" : fullName, 160);

        Label roleL = new Label(role);
        roleL.getStyleClass().add("admin-status-badge");
        roleL.getStyleClass().add("ADMIN".equals(role) ? "admin-status-expediee" : "admin-status-validee");
        roleL.setMinWidth(70);

        Label statusL = new Label(active ? "Actif" : "Inactif");
        statusL.getStyleClass().add("admin-status-badge");
        statusL.getStyleClass().add(active ? "admin-status-livree" : "admin-status-annulee");
        statusL.setMinWidth(90);

        String dateStr = createdAt.length() > 10 ? createdAt.substring(0, 10) : createdAt;
        Label dateL = tCell(dateStr, 120);
        dateL.getStyleClass().add("admin-cell-date");

        HBox actionBox = new HBox(6);
        actionBox.setAlignment(Pos.CENTER_LEFT);
        actionBox.setMinWidth(120);

        boolean isSelf = userId == NavigationManager.getUserId();

        Button toggleBtn = new Button(active ? "Desactiver" : "Activer");
        toggleBtn.getStyleClass().add(active ? "admin-toggle-deactivate" : "admin-toggle-activate");
        toggleBtn.setDisable(isSelf);

        if (isSelf) {
            Tooltip tooltip = new Tooltip("Vous ne pouvez pas vous desactiver");
            Tooltip.install(toggleBtn, tooltip);
        }

        toggleBtn.setOnAction(e -> {
            if (!isSelf) confirmToggle(userId, username, active);
        });

        actionBox.getChildren().add(toggleBtn);

        row.getChildren().addAll(idL, userCell, emailL, nameL, roleL, statusL, dateL, actionBox);
        return row;
    }

    private void confirmToggle(int userId, String username, boolean currentlyActive) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        overlay.setAlignment(Pos.CENTER);

        VBox card = new VBox(16);
        card.getStyleClass().add("admin-form-card");
        card.setMaxWidth(400);
        card.setPadding(new Insets(30));
        card.setAlignment(Pos.CENTER);

        Label title = new Label(currentlyActive
                ? "Desactiver " + username + " ?"
                : "Activer " + username + " ?");
        title.getStyleClass().add("admin-section-title");
        title.setWrapText(true);

        Label msg = new Label(currentlyActive
                ? "L'utilisateur ne pourra plus se connecter."
                : "L'utilisateur pourra a nouveau se connecter.");
        msg.setStyle("-fx-font-size:13px;-fx-text-fill:#555;-fx-text-alignment:center;");
        msg.setWrapText(true);

        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().add("admin-cancel-btn");
        cancelBtn.setOnAction(e -> {
            StackPane parent = (StackPane) root.getCenter();
            parent.getChildren().remove(overlay);
        });

        Button confirmBtn = new Button(currentlyActive ? "Desactiver" : "Activer");
        confirmBtn.getStyleClass().add(currentlyActive ? "admin-delete-confirm-btn" : "admin-action-btn");
        confirmBtn.setOnAction(e -> {
            StackPane parent = (StackPane) root.getCenter();
            parent.getChildren().remove(overlay);
            doToggle(userId, username, currentlyActive);
        });

        buttons.getChildren().addAll(cancelBtn, confirmBtn);
        card.getChildren().addAll(title, msg, buttons);

        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                StackPane parent = (StackPane) root.getCenter();
                parent.getChildren().remove(overlay);
            }
        });

        overlay.getChildren().add(card);

        card.setOpacity(0);
        card.setScaleX(0.9);
        card.setScaleY(0.9);

        StackPane body = (StackPane) root.getCenter();
        body.getChildren().add(overlay);

        FadeTransition fi = new FadeTransition(Duration.millis(200), card);
        fi.setToValue(1);
        ScaleTransition si = new ScaleTransition(Duration.millis(200), card);
        si.setToX(1);
        si.setToY(1);
        si.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, si).play();
    }

    private void doToggle(int userId, String username, boolean wasActive) {
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.ADMIN_TOGGLE_USER,
                        NavigationManager.getToken(),
                        Map.of("userId", userId)))
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        toast(username + (wasActive ? " desactive" : " active"), "success");
                        loadUsers();
                    } else {
                        toast(res.getMessage() != null ? res.getMessage() : "Erreur", "error");
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.ADMIN_GET_USERS,
                        NavigationManager.getToken(), null))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null) {
                        allUsers = (List<Map<String, Object>>) res.getData();
                    }
                    displayUsers();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        displayUsers();
                    });
                    return null;
                });
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
        switch (type) {
            case "success": t.getStyleClass().add("toast-success"); break;
            case "error": t.getStyleClass().add("toast-error"); break;
            default: t.getStyleClass().add("toast-info"); break;
        }
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
        new SequentialTransition(new ParallelTransition(fi, si), hold,
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
        try { return Integer.parseInt(o.toString()); }
        catch (Exception e) { return 0; }
    }

    private double dbl(Object o) {
        if (o == null) return 0;
        if (o instanceof Double) return (Double) o;
        try { return Double.parseDouble(o.toString()); }
        catch (Exception e) { return 0; }
    }
}