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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ManageProductsScreen {

    private static final double MAX_W = 1200;
    private static final double H_PAD = 40;

    private final BorderPane root;
    private VBox productsContainer;
    private StackPane loadingOverlay;
    private HBox statsRow;
    private Label resultLabel;

    private List<Map<String, Object>> allProducts = new ArrayList<>();
    private List<Map<String, Object>> categoriesList = new ArrayList<>();
    private HBox categoryChipsContainer;
    private String currentCategory = "Toutes";
    private String searchQuery = "";

    public ManageProductsScreen() {
        root = new BorderPane();
        root.getStyleClass().add("admin-root");

        if (!NavigationManager.isLoggedIn() || !NavigationManager.isAdmin()) {
            root.setCenter(accessDenied());
            return;
        }

        root.setTop(AdminDashboard.buildAdminHeader("Produits"));

        VBox mainContent = new VBox(20);
        mainContent.setMaxWidth(MAX_W);
        mainContent.setPadding(new Insets(25, H_PAD, 50, H_PAD));
        mainContent.setAlignment(Pos.TOP_CENTER);

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label pageTitle = new Label("Gestion des produits");
        pageTitle.getStyleClass().add("admin-page-title");

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Ajouter un produit");
        addBtn.getStyleClass().add("admin-add-btn");
        addBtn.setOnAction(e -> showProductForm(null));

        Button refreshBtn = new Button("Actualiser");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setStyle("-fx-font-size:12px;-fx-padding:8 16;");
        refreshBtn.setOnAction(e -> loadProducts());

        titleRow.getChildren().addAll(pageTitle, titleSpacer, addBtn, refreshBtn);

        statsRow = new HBox(12);
        statsRow.setAlignment(Pos.CENTER);

        Node toolbar = buildToolbar();
        productsContainer = new VBox(0);

        resultLabel = new Label("");
        resultLabel.getStyleClass().add("admin-mini-stat-label");

        mainContent.getChildren().addAll(titleRow, statsRow, toolbar, productsContainer, resultLabel);

        StackPane centered = new StackPane(mainContent);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setMinWidth(0);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        loadingOverlay = buildLoading();
        root.setCenter(new StackPane(scroll, loadingOverlay));

        loadProducts();

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

    // ═══════════════════════════════════════════════════════
    //  TOOLBAR
    // ═══════════════════════════════════════════════════════

    private Node buildToolbar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);

        TextField search = new TextField();
        search.setPromptText("Rechercher par nom...");
        search.getStyleClass().add("search-field");
        search.setPrefWidth(240);
        search.textProperty().addListener((o, ov, nv) -> {
            searchQuery = nv.trim().toLowerCase();
            displayProducts();
        });

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        categoryChipsContainer = new HBox(6);
        categoryChipsContainer.setAlignment(Pos.CENTER_RIGHT);
        
        updateCategoryFilters();
        loadCategories();

        bar.getChildren().addAll(search, sp, categoryChipsContainer);
        return bar;
    }

    private void loadCategories() {
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_CATEGORIES, null, null))
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.isSuccess() && res.getData() != null) {
                        categoriesList = (List<Map<String, Object>>) res.getData();
                        updateCategoryFilters();
                    }
                }));
    }

    private void updateCategoryFilters() {
        if (categoryChipsContainer == null) return;
        categoryChipsContainer.getChildren().clear();

        List<String> cats = new ArrayList<>();
        cats.add("Toutes");
        for (Map<String, Object> c : categoriesList) {
            cats.add(str(c.get("name")));
        }

        for (String cat : cats) {
            Button chip = new Button(cat);
            chip.getStyleClass().add("admin-filter-chip");
            if (cat.equals(currentCategory)) chip.getStyleClass().add("admin-filter-active");

            chip.setOnAction(e -> {
                currentCategory = cat;
                categoryChipsContainer.getChildren().forEach(n -> 
                    n.getStyleClass().remove("admin-filter-active"));
                chip.getStyleClass().add("admin-filter-active");
                displayProducts();
            });
            categoryChipsContainer.getChildren().add(chip);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  STATS
    // ═══════════════════════════════════════════════════════

    private void rebuildStats() {
        statsRow.getChildren().clear();

        long total = allProducts.size();
        long inStock = allProducts.stream()
                .filter(p -> intV(p.get("stockQuantity")) > 0)
                .count();
        long outStock = total - inStock;
        long lowStock = allProducts.stream()
                .filter(p -> {
                    int s = intV(p.get("stockQuantity"));
                    return s > 0 && s <= 10;
                }).count();

        double totalValue = allProducts.stream()
                .mapToDouble(p -> dbl(p.get("price")) * intV(p.get("stockQuantity")))
                .sum();

        statsRow.getChildren().addAll(
                miniStat("Total", String.valueOf(total), "#6C3483"),
                miniStat("En stock", String.valueOf(inStock), "#27AE60"),
                miniStat("Stock faible", String.valueOf(lowStock), "#F39C12"),
                miniStat("Rupture", String.valueOf(outStock), "#E74C3C"),
                miniStat("Valeur stock", String.format("%.0f MAD", totalValue), "#3498DB")
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

    // ═══════════════════════════════════════════════════════
    //  AFFICHAGE TABLEAU
    // ═══════════════════════════════════════════════════════

    private void displayProducts() {
        productsContainer.getChildren().clear();
        rebuildStats();

        List<Map<String, Object>> filtered = allProducts;

        if (!"Toutes".equals(currentCategory)) {
            filtered = filtered.stream()
                    .filter(p -> currentCategory.equalsIgnoreCase(str(p.get("categoryName"))))
                    .collect(Collectors.toList());
        }

        if (!searchQuery.isEmpty()) {
            filtered = filtered.stream()
                    .filter(p -> str(p.get("name")).toLowerCase().contains(searchQuery)
                            || str(p.get("description")).toLowerCase().contains(searchQuery))
                    .collect(Collectors.toList());
        }

        resultLabel.setText(filtered.size() + " produit(s) affiche(s)");

        if (filtered.isEmpty()) {
            productsContainer.getChildren().add(emptyState("Aucun produit trouve"));
            return;
        }

        HBox header = new HBox();
        header.getStyleClass().add("admin-table-header");
        header.setPadding(new Insets(10, 16, 10, 16));
        header.getChildren().addAll(
                tCol("", 44),
                tCol("ID", 46),
                tCol("Nom", 200),
                tCol("Categorie", 110),
                tCol("Prix", 130),
                tCol("Stock", 90),
                tCol("Statut", 140),
                tCol("Actions", 180)
        );
        productsContainer.getChildren().add(header);

        for (int i = 0; i < filtered.size(); i++) {
            productsContainer.getChildren().add(buildProductRow(filtered.get(i), i));
        }

        // Animations d'entrée
        for (int i = 1; i < productsContainer.getChildren().size(); i++) {
            Node n = productsContainer.getChildren().get(i);
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

    private HBox buildProductRow(Map<String, Object> product, int index) {
        HBox row = new HBox();
        row.getStyleClass().add("admin-table-row");
        if (index % 2 == 0) row.getStyleClass().add("admin-table-row-alt");
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        int    pid   = intV(product.get("productId"));
        String name  = str(product.get("name"));
        String cat   = str(product.get("categoryName"));
        double price = dbl(product.get("price"));
        int    stock = intV(product.get("stockQuantity"));
        String imgUrl = str(product.get("imageUrl"));

        // ── Miniature image ──────────────────────────────
        StackPane thumb = new StackPane();
        thumb.setMinSize(36, 36);
        thumb.setMaxSize(36, 36);
        thumb.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 8;");

        Image thumbImg = loadProductImage(imgUrl);
        if (thumbImg != null && !thumbImg.isError()) {
            ImageView iv = new ImageView(thumbImg);
            iv.setFitWidth(32);
            iv.setFitHeight(32);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            thumb.getChildren().add(iv);
        } else {
            Label letter = new Label(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
            letter.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: rgba(108,52,131,0.2);");
            thumb.getChildren().add(letter);
        }
        thumb.setMinWidth(44);
        thumb.setMaxWidth(44);

        Label idL = tCell(String.valueOf(pid), 46);
        idL.getStyleClass().add("admin-cell-id");

        Label nameL = tCell(name.length() > 24 ? name.substring(0, 22) + ".." : name, 200);
        nameL.setStyle("-fx-font-weight:600;");

        Label catL = tCell(cat, 110);
        catL.getStyleClass().add("admin-cell-date");

        Label priceL = tCell(String.format("%.2f MAD", price), 130);
        priceL.getStyleClass().add("admin-cell-amount");

        Label stockL = tCell(String.valueOf(stock), 90);
        if (stock <= 0) {
            stockL.setStyle("-fx-text-fill:#E74C3C;-fx-font-weight:bold;");
        } else if (stock <= 10) {
            stockL.setStyle("-fx-text-fill:#F39C12;-fx-font-weight:bold;");
        } else {
            stockL.setStyle("-fx-text-fill:#27AE60;-fx-font-weight:bold;");
        }

        Label statusL = new Label(stock > 0 ? "Disponible" : "Indisponible");
        statusL.getStyleClass().add("admin-status-badge");
        statusL.getStyleClass().add(stock > 0 ? "admin-status-validee" : "admin-status-annulee");

        HBox statusBox = new HBox(statusL);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setMinWidth(140);
        statusBox.setMaxWidth(140);

        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setMinWidth(180);
        actions.setMaxWidth(180);

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().add("admin-edit-btn");
        editBtn.setOnAction(e -> showProductForm(product));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().add("admin-delete-btn");
        deleteBtn.setOnAction(e -> confirmDelete(pid, name));

        actions.getChildren().addAll(editBtn, deleteBtn);

        row.getChildren().addAll(thumb, idL, nameL, catL, priceL, stockL, statusBox, actions);
        return row;
    }

    // ═══════════════════════════════════════════════════════
    //  FORMULAIRE — avec upload d'image
    // ═══════════════════════════════════════════════════════

    private void showProductForm(Map<String, Object> existing) {
        boolean isEdit = existing != null;

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        overlay.setAlignment(Pos.CENTER);

        VBox card = new VBox(14);
        card.getStyleClass().add("admin-form-card");
        card.setMaxWidth(540);
        card.setPadding(new Insets(28));
        card.setAlignment(Pos.TOP_CENTER);

        Label title = new Label(isEdit ? "Modifier le produit" : "Nouveau produit");
        title.getStyleClass().add("admin-section-title");

        // ── Zone de prévisualisation d'image ─────────────
        final String[] selectedImagePath = {isEdit ? str(existing.get("imageUrl")) : ""};

        StackPane imagePreview = new StackPane();
        imagePreview.setStyle(
                "-fx-background-color: #F8F9FA;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-color: #E8E8E8;"
                        + "-fx-border-radius: 12;"
                        + "-fx-border-width: 1.5;"
                        + "-fx-border-style: dashed;"
                        + "-fx-cursor: hand;"
        );
        imagePreview.setPrefSize(490, 160);
        imagePreview.setMaxSize(490, 160);

        ImageView previewIv = new ImageView();
        previewIv.setFitWidth(470);
        previewIv.setFitHeight(140);
        previewIv.setPreserveRatio(true);
        previewIv.setSmooth(true);
        previewIv.setVisible(false);

        VBox placeholderContent = new VBox(6);
        placeholderContent.setAlignment(Pos.CENTER);
        Label uploadIcon = new Label("\uD83D\uDCC1");
        uploadIcon.setStyle("-fx-font-size: 28px;");
        Label uploadText = new Label("Cliquer pour choisir une image");
        uploadText.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 13px; -fx-font-weight: 600;");
        Label uploadHint = new Label("PNG, JPG, JPEG, GIF, WEBP");
        uploadHint.setStyle("-fx-text-fill: #BDC3C7; -fx-font-size: 11px;");
        placeholderContent.getChildren().addAll(uploadIcon, uploadText, uploadHint);

        imagePreview.getChildren().addAll(placeholderContent, previewIv);

        // Charger l'image existante si modification
        if (isEdit && !selectedImagePath[0].isEmpty()) {
            Image existImg = loadProductImage(selectedImagePath[0]);
            if (existImg != null && !existImg.isError()) {
                previewIv.setImage(existImg);
                previewIv.setVisible(true);
                placeholderContent.setVisible(false);
            }
        }

        // Label d'erreur (déclaré tôt pour usage dans les handlers)
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill:#E74C3C;-fx-font-size:12px;-fx-font-weight:bold;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // Click sur la zone pour choisir un fichier
        imagePreview.setOnMouseClicked(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une image produit");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
            File file = fc.showOpenDialog(root.getScene().getWindow());
            if (file != null) {
                // Vérifier la taille (max 5 MB)
                if (file.length() > 5 * 1024 * 1024) {
                    errorLabel.setText("L'image ne doit pas depasser 5 MB.");
                    errorLabel.setVisible(true);
                    return;
                }
                String newPath = copyImageToResources(file);
                if (newPath != null) {
                    selectedImagePath[0] = newPath;
                    Image img = new Image(file.toURI().toString());
                    previewIv.setImage(img);
                    previewIv.setVisible(true);
                    placeholderContent.setVisible(false);
                    errorLabel.setVisible(false);

                    // Animation de confirmation
                    imagePreview.setStyle(
                            "-fx-background-color: #F8F9FA;"
                                    + "-fx-background-radius: 12;"
                                    + "-fx-border-color: #27AE60;"
                                    + "-fx-border-radius: 12;"
                                    + "-fx-border-width: 2;"
                                    + "-fx-border-style: solid;"
                                    + "-fx-cursor: hand;"
                    );
                } else {
                    errorLabel.setText("Erreur lors de la copie de l'image.");
                    errorLabel.setVisible(true);
                }
            }
        });

        // Bouton retirer l'image
        Button removeImgBtn = new Button("✕ Retirer l'image");
        removeImgBtn.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: #E74C3C;"
                        + "-fx-font-size: 11px;"
                        + "-fx-cursor: hand;"
                        + "-fx-padding: 4 8;"
        );
        removeImgBtn.setOnAction(ev -> {
            selectedImagePath[0] = "";
            previewIv.setImage(null);
            previewIv.setVisible(false);
            placeholderContent.setVisible(true);
            imagePreview.setStyle(
                    "-fx-background-color: #F8F9FA;"
                            + "-fx-background-radius: 12;"
                            + "-fx-border-color: #E8E8E8;"
                            + "-fx-border-radius: 12;"
                            + "-fx-border-width: 1.5;"
                            + "-fx-border-style: dashed;"
                            + "-fx-cursor: hand;"
            );
        });

        HBox imgActions = new HBox(12);
        imgActions.setAlignment(Pos.CENTER_LEFT);

        Label imgPathLabel = new Label();
        imgPathLabel.setStyle("-fx-text-fill: #95A5A6; -fx-font-size: 11px;");
        if (!selectedImagePath[0].isEmpty()) {
            imgPathLabel.setText(selectedImagePath[0]);
        }

        imgActions.getChildren().addAll(removeImgBtn, imgPathLabel);

        // ── Champs du formulaire ─────────────────────────
        TextField nameField = formField("Nom du produit",
                isEdit ? str(existing.get("name")) : "");

        VBox descGroup = new VBox(4);
        Label descLabel = new Label("Description");
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#7F8C8D;-fx-font-weight:600;");
        TextArea descField = new TextArea(isEdit ? str(existing.get("description")) : "");
        descField.setPromptText("Description du produit");
        descField.getStyleClass().add("admin-form-textarea");
        descField.setPrefRowCount(3);
        descField.setMaxWidth(Double.MAX_VALUE);
        descGroup.getChildren().addAll(descLabel, descField);

        HBox row1 = new HBox(12);
        TextField priceField = formField("Prix (MAD)",
                isEdit ? String.valueOf(dbl(existing.get("price"))) : "");
        priceField.setPrefWidth(200);
        HBox.setHgrow(priceField, Priority.ALWAYS);

        TextField stockField = formField("Stock",
                isEdit ? String.valueOf(intV(existing.get("stockQuantity"))) : "0");
        stockField.setPrefWidth(200);
        HBox.setHgrow(stockField, Priority.ALWAYS);
        row1.getChildren().addAll(priceField, stockField);

        // Catégorie
        VBox catGroup = new VBox(4);
        Label catLabel = new Label("Categorie");
        catLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#7F8C8D;-fx-font-weight:600;");
        ComboBox<String> catCombo = new ComboBox<>();
        for (Map<String, Object> c : categoriesList) {
            catCombo.getItems().add(intV(c.get("categoryId")) + "-" + str(c.get("name")));
        }
        catCombo.setPromptText("Choisir une categorie");
        catCombo.getStyleClass().add("admin-status-combo");
        catCombo.setMaxWidth(Double.MAX_VALUE);

        if (isEdit) {
            int catId = intV(existing.get("categoryId"));
            String catName = str(existing.get("categoryName"));
            catCombo.setValue(catId + "-" + catName);
        }
        catGroup.getChildren().addAll(catLabel, catCombo);

        // ── Boutons ──────────────────────────────────────
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().add("admin-cancel-btn");
        cancelBtn.setOnAction(e -> {
            StackPane parent = (StackPane) root.getCenter();
            parent.getChildren().remove(overlay);
        });

        Button saveBtn = new Button(isEdit ? "Enregistrer" : "Creer le produit");
        saveBtn.getStyleClass().add("admin-action-btn");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(18, 18);
        spinner.setVisible(false);

        saveBtn.setOnAction(e -> {
            String pName    = nameField.getText().trim();
            String pDesc    = descField.getText().trim();
            String priceStr = priceField.getText().trim();
            String stockStr = stockField.getText().trim();
            String catValue = catCombo.getValue();
            String imageUrl = selectedImagePath[0];

            if (pName.isEmpty()) {
                errorLabel.setText("Le nom est requis.");
                errorLabel.setVisible(true);
                return;
            }

            double pPrice;
            int pStock;
            try {
                pPrice = Double.parseDouble(priceStr);
                pStock = Integer.parseInt(stockStr);
            } catch (NumberFormatException ex) {
                errorLabel.setText("Prix et stock doivent etre numeriques.");
                errorLabel.setVisible(true);
                return;
            }

            if (pPrice <= 0) {
                errorLabel.setText("Le prix doit etre positif.");
                errorLabel.setVisible(true);
                return;
            }

            int categoryId = 1;
            if (catValue != null && catValue.contains("-")) {
                try {
                    categoryId = Integer.parseInt(catValue.split("-")[0]);
                } catch (NumberFormatException ignored) {}
            }

            errorLabel.setVisible(false);
            saveBtn.setDisable(true);
            spinner.setVisible(true);
            saveBtn.setGraphic(spinner);

            Map<String, Object> data = new HashMap<>();
            data.put("name", pName);
            data.put("description", pDesc);
            data.put("price", pPrice);
            data.put("stockQuantity", pStock);
            data.put("categoryId", categoryId);
            data.put("imageUrl", imageUrl.isEmpty() ? null : imageUrl);

            Action action;
            if (isEdit) {
                data.put("productId", intV(existing.get("productId")));
                action = Action.ADMIN_EDIT_PRODUCT;
            } else {
                action = Action.ADMIN_ADD_PRODUCT;
            }

            ServerConnection.getInstance()
                    .sendAsync(new Request(action, NavigationManager.getToken(), data))
                    .thenAccept(res -> Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        spinner.setVisible(false);
                        saveBtn.setGraphic(null);

                        if (res.isSuccess()) {
                            StackPane parent = (StackPane) root.getCenter();
                            parent.getChildren().remove(overlay);
                            toast(isEdit ? "Produit modifie !" : "Produit cree !", "success");
                            loadProducts();
                        } else {
                            errorLabel.setText(res.getMessage() != null
                                    ? res.getMessage() : "Erreur serveur");
                            errorLabel.setVisible(true);
                        }
                    }));
        });

        buttons.getChildren().addAll(cancelBtn, saveBtn);

        // ── Assemblage ───────────────────────────────────
        card.getChildren().addAll(
                title,
                imagePreview, imgActions,
                nameField, descGroup, row1, catGroup,
                errorLabel, buttons
        );

        // ── Overlay logic ────────────────────────────────
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

        FadeTransition fi = new FadeTransition(Duration.millis(250), card);
        fi.setToValue(1);
        ScaleTransition si = new ScaleTransition(Duration.millis(250), card);
        si.setToX(1);
        si.setToY(1);
        si.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, si).play();
    }

    // ═══════════════════════════════════════════════════════
    //  COPIE D'IMAGE vers resources + target
    // ═══════════════════════════════════════════════════════

    private String copyImageToResources(File sourceFile) {
        try {
            // Nom unique : timestamp + nom original nettoyé
            String safeName = sourceFile.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
            String uniqueName = System.currentTimeMillis() + "_" + safeName;

            // 1. Copier vers src/main/resources/images/products/
            Path srcDir = Paths.get("src", "main", "resources", "images", "products");
            Files.createDirectories(srcDir);
            Path destSrc = srcDir.resolve(uniqueName);
            Files.copy(sourceFile.toPath(), destSrc, StandardCopyOption.REPLACE_EXISTING);

            // 2. Copier aussi vers target/classes/images/products/
            //    pour disponibilité immédiate via classpath
            try {
                Path targetDir = Paths.get("target", "classes", "images", "products");
                Files.createDirectories(targetDir);
                Path destTarget = targetDir.resolve(uniqueName);
                Files.copy(sourceFile.toPath(), destTarget, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
                // Pas critique si target n'existe pas
            }

            System.out.println("[IMAGE] Copié : " + destSrc.toAbsolutePath());
            return "/images/products/" + uniqueName;

        } catch (Exception e) {
            System.err.println("[IMAGE] Erreur copie : " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════
    //  CHARGEMENT D'IMAGE — multiple fallbacks
    // ═══════════════════════════════════════════════════════

    private Image loadProductImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) return null;
        imageUrl = imageUrl.trim();

        // 1. Classpath resource
        try {
            InputStream is = getClass().getResourceAsStream(imageUrl);
            if (is != null) {
                Image img = new Image(is);
                if (!img.isError()) return img;
            }
        } catch (Exception ignored) {}

        // 2. File system : src/main/resources + path
        try {
            File f = new File("src/main/resources" + imageUrl);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString());
                if (!img.isError()) return img;
            }
        } catch (Exception ignored) {}

        // 3. Chemin absolu
        try {
            File f = new File(imageUrl);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString());
                if (!img.isError()) return img;
            }
        } catch (Exception ignored) {}

        // 4. URL HTTP/HTTPS/file:
        try {
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")
                    || imageUrl.startsWith("file:")) {
                return new Image(imageUrl, true);
            }
        } catch (Exception ignored) {}

        return null;
    }

    // ═══════════════════════════════════════════════════════
    //  FORM FIELD HELPER
    // ═══════════════════════════════════════════════════════

    private TextField formField(String label, String value) {
        TextField field = new TextField(value);
        field.setPromptText(label);
        field.getStyleClass().add("admin-form-field");
        return field;
    }

    // ═══════════════════════════════════════════════════════
    //  CONFIRMATION SUPPRESSION
    // ═══════════════════════════════════════════════════════

    private void confirmDelete(int productId, String productName) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        overlay.setAlignment(Pos.CENTER);

        VBox card = new VBox(12);
        card.getStyleClass().add("admin-form-card");
        card.setMaxWidth(380);
        card.setMaxHeight(170);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);

        Label title = new Label("Supprimer le produit ?");
        title.getStyleClass().add("admin-section-title");

        Label msg = new Label("\"" + productName + "\" sera desactive.");
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

        Button confirmBtn = new Button("Supprimer");
        confirmBtn.getStyleClass().add("admin-delete-confirm-btn");
        confirmBtn.setOnAction(e -> {
            StackPane parent = (StackPane) root.getCenter();
            parent.getChildren().remove(overlay);
            doDelete(productId);
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

    private void doDelete(int productId) {
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.ADMIN_DELETE_PRODUCT,
                        NavigationManager.getToken(),
                        Map.of("productId", productId)))
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        toast("Produit supprime (desactive)", "success");
                        loadProducts();
                    } else {
                        toast(res.getMessage() != null ? res.getMessage() : "Erreur", "error");
                    }
                }));
    }

    // ═══════════════════════════════════════════════════════
    //  CHARGEMENT DONNÉES
    // ═══════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void loadProducts() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_PRODUCTS,
                        NavigationManager.getToken(), null))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null) {
                        allProducts = (List<Map<String, Object>>) res.getData();
                    }
                    displayProducts();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        displayProducts();
                    });
                    return null;
                });
    }

    // ═══════════════════════════════════════════════════════
    //  UTILITAIRES UI
    // ═══════════════════════════════════════════════════════

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
            case "error":   t.getStyleClass().add("toast-error");   break;
            default:        t.getStyleClass().add("toast-info");    break;
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

    // ═══════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════

    private String str(Object o) { return o != null ? o.toString() : ""; }

    private int intV(Object o) {
        if (o == null) return 0;
        if (o instanceof Double)  return ((Double) o).intValue();
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