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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageCategoriesScreen {

    private static final double MAX_W = 1100;
    private static final double H_PAD = 40;

    private final BorderPane root;
    private VBox tableBody;
    private Label countLabel;
    private StackPane loadingOverlay;

    private List<Map<String, Object>> categories = new ArrayList<>();

    public ManageCategoriesScreen() {
        root = new BorderPane();
        root.getStyleClass().add("admin-root");

        if (!AdminNavigationManager.isLoggedIn() || !AdminNavigationManager.isAdmin()) {
            Label deny = new Label("Acces refuse");
            deny.getStyleClass().add("admin-welcome-title");
            root.setCenter(deny);
            return;
        }

        root.setTop(AdminDashboard.buildAdminHeader("Categories"));

        VBox content = new VBox(24);
        content.setMaxWidth(MAX_W);
        content.setPadding(new Insets(30, H_PAD, 50, H_PAD));
        content.setAlignment(Pos.TOP_CENTER);

        StackPane centered = new StackPane(content);
        centered.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        loadingOverlay = buildLoadingOverlay();
        root.setCenter(new StackPane(scroll, loadingOverlay));

        // Toolbar
        HBox toolbar = buildToolbar();

        // Category table
        VBox tableSection = buildTableSection();

        content.getChildren().addAll(toolbar, tableSection);

        Platform.runLater(() -> {
            root.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(350), root);
            ft.setToValue(1);
            ft.play();
        });

        loadCategories();
    }

    public Node getView() { return root; }

    // ─────────────────────────────────────────────────────────────────────────
    //  TOOLBAR
    // ─────────────────────────────────────────────────────────────────────────

    private HBox buildToolbar() {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gestion des Categories");
        title.getStyleClass().add("admin-welcome-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        countLabel = new Label("");
        countLabel.getStyleClass().add("admin-welcome-subtitle");

        Button addBtn = new Button("+ Nouvelle categorie");
        addBtn.getStyleClass().add("admin-action-btn");
        addBtn.setOnAction(e -> showCategoryDialog(null));

        bar.getChildren().addAll(title, spacer, countLabel, addBtn);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TABLE
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildTableSection() {
        VBox section = new VBox(0);
        section.getStyleClass().add("admin-section-card");

        // Header row
        HBox header = new HBox();
        header.getStyleClass().add("admin-table-header");
        header.setPadding(new Insets(12, 16, 12, 16));
        header.getChildren().addAll(
                hCol("ID",          60),
                hCol("Nom",         220),
                hCol("Description", 400),
                hCol("Cree le",     160),
                hCol("Actions",     160)
        );

        tableBody = new VBox(0);

        section.getChildren().addAll(header, tableBody);
        return section;
    }

    private void refreshTable() {
        tableBody.getChildren().clear();

        if (categories.isEmpty()) {
            Label empty = new Label("Aucune categorie trouvee");
            empty.getStyleClass().add("admin-chart-empty");
            empty.setPadding(new Insets(40));
            tableBody.getChildren().add(empty);
            countLabel.setText("0 categorie(s)");
            return;
        }

        countLabel.setText(categories.size() + " categorie(s)");

        List<Node> rows = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            Map<String, Object> cat = categories.get(i);
            HBox row = buildRow(cat, i);
            row.setOpacity(0);
            row.setTranslateY(15);
            tableBody.getChildren().add(row);
            rows.add(row);
        }

        // Stagger animation
        for (int i = 0; i < rows.size(); i++) {
            Node r = rows.get(i);
            int delay = i * 40;
            FadeTransition f = new FadeTransition(Duration.millis(350), r);
            f.setToValue(1);
            f.setDelay(Duration.millis(delay));
            TranslateTransition t = new TranslateTransition(Duration.millis(350), r);
            t.setToY(0);
            t.setDelay(Duration.millis(delay));
            t.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(f, t).play();
        }
    }

    private HBox buildRow(Map<String, Object> cat, int index) {
        HBox row = new HBox();
        row.getStyleClass().add("admin-table-row");
        if (index % 2 == 0) row.getStyleClass().add("admin-table-row-alt");
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        int catId = intV(cat.get("categoryId"));
        String name = str(cat.get("name"));
        String desc = str(cat.get("description"));
        String createdAt = str(cat.get("createdAt"));
        if (createdAt.length() > 10) createdAt = createdAt.substring(0, 10);

        Label idLbl = cell(String.valueOf(catId), 60);
        idLbl.getStyleClass().add("admin-cell-id");

        Label nameLbl = cell(name, 220);
        nameLbl.setStyle("-fx-font-weight: 600;");

        Label descLbl = cell(desc.isEmpty() ? "—" : desc, 400);
        descLbl.setStyle("-fx-text-fill: #888;");

        Label dateLbl = cell(createdAt, 160);
        dateLbl.getStyleClass().add("admin-cell-date");

        // Action buttons
        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().add("admin-link-btn");
        editBtn.setOnAction(e -> showCategoryDialog(cat));

        Button delBtn = new Button("Supprimer");
        delBtn.getStyleClass().add("admin-delete-btn");
        delBtn.setStyle("-fx-text-fill: #E74C3C; -fx-background-color: transparent; " +
                        "-fx-border-color: #E74C3C; -fx-border-radius: 6; -fx-cursor: hand;");
        delBtn.setOnAction(e -> confirmDelete(cat));

        HBox actions = new HBox(8, editBtn, delBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setMinWidth(160);

        row.getChildren().addAll(idLbl, nameLbl, descLbl, dateLbl, actions);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DIALOGS
    // ─────────────────────────────────────────────────────────────────────────

    private void showCategoryDialog(Map<String, Object> existing) {
        boolean isEdit = (existing != null);
        String title = isEdit ? "Modifier la categorie" : "Nouvelle categorie";

        // Overlay
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        overlay.setOpacity(0);

        // Dialog card
        VBox dialog = new VBox(20);
        dialog.setMaxWidth(480);
        dialog.setMaxHeight(380);
        dialog.setStyle("-fx-background-color: #fff; -fx-background-radius: 14; " +
                        "-fx-padding: 32;");
        DropShadow ds = new DropShadow();
        ds.setRadius(30);
        ds.setColor(Color.rgb(0, 0, 0, 0.18));
        dialog.setEffect(ds);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 18; -fx-font-weight: 700; -fx-text-fill: #1a1a2e;");

        // Name field
        Label nameLbl = new Label("Nom *");
        nameLbl.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #444;");
        TextField nameField = new TextField(isEdit ? str(existing.get("name")) : "");
        nameField.setPromptText("Nom de la categorie");
        nameField.setStyle("-fx-font-size: 14; -fx-padding: 10 14; -fx-background-radius: 8; " +
                           "-fx-border-color: #ddd; -fx-border-radius: 8;");

        // Description field
        Label descLbl = new Label("Description");
        descLbl.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #444;");
        TextArea descField = new TextArea(isEdit ? str(existing.get("description")) : "");
        descField.setPromptText("Description (optionnel)");
        descField.setPrefRowCount(3);
        descField.setWrapText(true);
        descField.setStyle("-fx-font-size: 13; -fx-padding: 10 14; -fx-background-radius: 8; " +
                           "-fx-border-color: #ddd; -fx-border-radius: 8;");

        // Error label
        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 12;");

        // Buttons
        Button saveBtn = new Button(isEdit ? "Enregistrer" : "Ajouter");
        saveBtn.setStyle("-fx-background-color: #6C3483; -fx-text-fill: white; " +
                         "-fx-font-size: 14; -fx-font-weight: 700; -fx-background-radius: 8; " +
                         "-fx-padding: 10 28; -fx-cursor: hand;");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; " +
                           "-fx-font-size: 13; -fx-cursor: hand;");

        HBox btnRow = new HBox(12, cancelBtn, saveBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        dialog.getChildren().addAll(titleLbl, nameLbl, nameField, descLbl, descField, errorLbl, btnRow);
        overlay.getChildren().add(dialog);

        // Mount overlay on top of root
        StackPane rootSp = (StackPane) root.getCenter();
        rootSp.getChildren().add(overlay);

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), overlay);
        fadeIn.setToValue(1);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), dialog);
        scaleIn.setFromX(0.92); scaleIn.setFromY(0.92);
        scaleIn.setToX(1);     scaleIn.setToY(1);
        dialog.setScaleX(0.92); dialog.setScaleY(0.92);
        new ParallelTransition(fadeIn, scaleIn).play();

        Runnable close = () -> {
            FadeTransition fo = new FadeTransition(Duration.millis(150), overlay);
            fo.setToValue(0);
            fo.setOnFinished(ev -> rootSp.getChildren().remove(overlay));
            fo.play();
        };

        cancelBtn.setOnAction(e -> close.run());
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) close.run();
        });

        saveBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            if (name.isEmpty()) {
                errorLbl.setText("Le nom est obligatoire.");
                return;
            }
            errorLbl.setText("");
            saveBtn.setDisable(true);

            if (isEdit) {
                int catId = intV(existing.get("categoryId"));
                Map<String, Object> payload = new HashMap<>();
                payload.put("categoryId", catId);
                payload.put("name", name);
                payload.put("description", desc);
                ServerConnection.getInstance()
                        .sendAsync(new Request(Action.ADMIN_EDIT_CATEGORY,
                                AdminNavigationManager.getToken(), payload))
                        .thenAccept(r -> Platform.runLater(() -> {
                            if (r.isSuccess()) {
                                close.run();
                                loadCategories();
                            } else {
                                errorLbl.setText(r.getMessage());
                                saveBtn.setDisable(false);
                            }
                        }));
            } else {
                Map<String, Object> payload = new HashMap<>();
                payload.put("name", name);
                payload.put("description", desc);
                ServerConnection.getInstance()
                        .sendAsync(new Request(Action.ADMIN_ADD_CATEGORY,
                                AdminNavigationManager.getToken(), payload))
                        .thenAccept(r -> Platform.runLater(() -> {
                            if (r.isSuccess()) {
                                close.run();
                                loadCategories();
                            } else {
                                errorLbl.setText(r.getMessage());
                                saveBtn.setDisable(false);
                            }
                        }));
            }
        });
    }

    private void confirmDelete(Map<String, Object> cat) {
        int id = intV(cat.get("categoryId"));
        String name = str(cat.get("name"));

        // Overlay
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        overlay.setOpacity(0);

        VBox dialog = new VBox(10);
        dialog.setMaxWidth(400);
        dialog.setMaxHeight(220);
        dialog.setAlignment(Pos.CENTER);
        dialog.setStyle("-fx-background-color: #fff; -fx-background-radius: 14; -fx-padding: 18 28;");
        DropShadow ds = new DropShadow();
        ds.setRadius(30);
        ds.setColor(Color.rgb(0, 0, 0, 0.18));
        dialog.setEffect(ds);

        Label icon = new Label("\u26A0");
        icon.setStyle("-fx-font-size: 24; -fx-text-fill: #E74C3C;");

        Label msg = new Label("Supprimer \"" + name + "\" ?");
        msg.setStyle("-fx-font-size: 16; -fx-font-weight: 700; -fx-text-fill: #1a1a2e;");

        Label sub = new Label("Cette action est irreversible.\nImpossible si des produits utilisent cette categorie.");
        sub.setStyle("-fx-font-size: 13; -fx-text-fill: #888; -fx-text-alignment: center;");
        sub.setWrapText(true);

        Label errorLbl = new Label("");
        errorLbl.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 12;");

        Button confirmBtn = new Button("Supprimer");
        confirmBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; " +
                            "-fx-font-weight: 700; -fx-background-radius: 8; " +
                            "-fx-padding: 10 24; -fx-cursor: hand;");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; " +
                           "-fx-font-size: 13; -fx-cursor: hand;");

        HBox btnRow = new HBox(12, cancelBtn, confirmBtn);
        btnRow.setAlignment(Pos.CENTER);

        dialog.getChildren().addAll(icon, msg, sub, errorLbl, btnRow);
        overlay.getChildren().add(dialog);

        StackPane rootSp = (StackPane) root.getCenter();
        rootSp.getChildren().add(overlay);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), overlay);
        fadeIn.setToValue(1);
        fadeIn.play();

        Runnable close = () -> {
            FadeTransition fo = new FadeTransition(Duration.millis(150), overlay);
            fo.setToValue(0);
            fo.setOnFinished(ev -> rootSp.getChildren().remove(overlay));
            fo.play();
        };

        cancelBtn.setOnAction(e -> close.run());
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) close.run();
        });

        confirmBtn.setOnAction(e -> {
            confirmBtn.setDisable(true);
            Map<String, Object> payload = new HashMap<>();
            payload.put("categoryId", id);
            ServerConnection.getInstance()
                    .sendAsync(new Request(Action.ADMIN_DELETE_CATEGORY,
                            AdminNavigationManager.getToken(), payload))
                    .thenAccept(r -> Platform.runLater(() -> {
                        if (r.isSuccess()) {
                            close.run();
                            loadCategories();
                        } else {
                            errorLbl.setText(r.getMessage());
                            confirmBtn.setDisable(false);
                        }
                    }));
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadCategories() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_CATEGORIES,
                        AdminNavigationManager.getToken(), null))
                .thenAccept(r -> Platform.runLater(() -> {
                    showLoading(false);
                    if (r.isSuccess() && r.getData() != null) {
                        categories = (List<Map<String, Object>>) r.getData();
                    } else {
                        categories = new ArrayList<>();
                    }
                    refreshTable();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        categories = new ArrayList<>();
                        refreshTable();
                    });
                    return null;
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Label hCol(String text, double w) {
        Label l = new Label(text);
        l.getStyleClass().add("admin-table-col-header");
        l.setMinWidth(w);
        l.setMaxWidth(w);
        return l;
    }

    private Label cell(String text, double w) {
        Label l = new Label(text);
        l.getStyleClass().add("admin-table-cell");
        l.setMinWidth(w);
        l.setMaxWidth(w);
        l.setWrapText(false);
        return l;
    }

    private StackPane buildLoadingOverlay() {
        StackPane o = new StackPane();
        o.setStyle("-fx-background-color: rgba(255,255,255,0.7);");
        o.setVisible(false);
        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(44, 44);
        o.getChildren().add(pi);
        return o;
    }

    private void showLoading(boolean b) {
        if (loadingOverlay != null)
            loadingOverlay.setVisible(b);
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }

    private int intV(Object o) {
        if (o == null) return 0;
        if (o instanceof Double) return ((Double) o).intValue();
        if (o instanceof Integer) return (Integer) o;
        try { return Integer.parseInt(o.toString()); }
        catch (Exception e) { return 0; }
    }
}
