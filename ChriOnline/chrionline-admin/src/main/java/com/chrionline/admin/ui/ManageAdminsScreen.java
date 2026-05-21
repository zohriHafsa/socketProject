package com.chrionline.admin.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.admin.ui.AdminNavigationManager;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.enums.Action;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
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
import java.util.List;
import java.util.Map;

public class ManageAdminsScreen {

    private static final double MAX_W = 1200;
    private static final double H_PAD = 40;

    private final BorderPane root;
    private VBox adminsContainer;
    private StackPane loadingOverlay;
    private List<Map<String, Object>> allAdmins = new ArrayList<>();

    public ManageAdminsScreen() {
        root = new BorderPane();
        root.getStyleClass().add("admin-root");

        // ── Accès réservé au SUPER_ADMIN uniquement ───────
        if (!AdminNavigationManager.isLoggedIn() || !AdminNavigationManager.isSuperAdmin()) {
            root.setCenter(buildAccessDenied());
            return;
        }

        root.setTop(AdminDashboard.buildAdminHeader("Admins"));

        VBox mainContent = new VBox(24);
        mainContent.setMaxWidth(MAX_W);
        mainContent.setPadding(new Insets(25, H_PAD, 50, H_PAD));
        mainContent.setAlignment(Pos.TOP_CENTER);

        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label pageTitle = new Label("Gestion des Administrateurs");
        pageTitle.getStyleClass().add("admin-page-title");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button refreshBtn = new Button("Actualiser");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setStyle("-fx-font-size:12px;-fx-padding:8 16;");
        refreshBtn.setOnAction(e -> loadAdmins());

        titleRow.getChildren().addAll(pageTitle, sp, refreshBtn);


        // Séparateur
        Region sep1 = new Region();
        sep1.setStyle("-fx-background-color: #e2e8f0; -fx-pref-height: 1;");

        // Formulaire ajout
        VBox addForm = buildAddForm();

        // Séparateur
        Region sep2 = new Region();
        sep2.setStyle("-fx-background-color: #e2e8f0; -fx-pref-height: 1;");

        // Titre liste
        Label listTitle = new Label("Administrateurs existants");
        listTitle.getStyleClass().add("admin-section-title");

        // Tableau des admins
        adminsContainer = new VBox(0);

        mainContent.getChildren().addAll(
                titleRow, sep1, addForm, sep2, listTitle, adminsContainer);

        StackPane centered = new StackPane(mainContent);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setMinWidth(0);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        loadingOverlay = buildLoading();
        root.setCenter(new StackPane(scroll, loadingOverlay));

        loadAdmins();

        Platform.runLater(() -> {
            root.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), root);
            ft.setToValue(1);
            ft.play();
        });
    }

    public Node getView() { return root; }

    // ── Formulaire ajout nouvel admin ─────────────────────

    private VBox buildAddForm() {
        VBox form = new VBox(16);
        form.setPadding(new Insets(22));
        form.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #8E44AD;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;"
        );

        Label formTitle = new Label("Ajouter un nouvel administrateur");
        formTitle.getStyleClass().add("admin-section-title");
        formTitle.setStyle("-fx-text-fill: #8E44AD; -fx-font-size: 15px;" +
                "-fx-font-weight: bold;");

        // Row 1 — username + email
        TextField usernameField = styledField("Nom d'utilisateur");
        TextField emailField    = styledField("Adresse email");

        VBox unGroup = new VBox(5, fieldLabel("Nom d'utilisateur *"), usernameField);
        VBox emGroup = new VBox(5, fieldLabel("Email *"), emailField);
        HBox.setHgrow(unGroup, Priority.ALWAYS);
        HBox.setHgrow(emGroup, Priority.ALWAYS);
        HBox row1 = new HBox(14, unGroup, emGroup);

        // Row 2 — prenom + nom
        TextField firstNameField = styledField("Prenom");
        TextField lastNameField  = styledField("Nom de famille");

        VBox fnGroup = new VBox(5, fieldLabel("Prenom *"), firstNameField);
        VBox lnGroup = new VBox(5, fieldLabel("Nom de famille *"), lastNameField);
        HBox.setHgrow(fnGroup, Priority.ALWAYS);
        HBox.setHgrow(lnGroup, Priority.ALWAYS);
        HBox row2 = new HBox(14, fnGroup, lnGroup);



        HBox errorBox = new HBox(8);
        errorBox.getStyleClass().add("auth-error");
        errorBox.setVisible(false);
        errorBox.setManaged(false);
        errorBox.setAlignment(Pos.CENTER_LEFT);
        Label errorIcon = new Label("\u26A0");
        errorIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #E74C3C;");
        Label errorText = new Label();
        errorText.getStyleClass().add("auth-error-text");
        errorText.setWrapText(true);
        HBox.setHgrow(errorText, Priority.ALWAYS);
        errorBox.getChildren().addAll(errorIcon, errorText);

        HBox successBox = new HBox(8);
        successBox.getStyleClass().add("auth-success");
        successBox.setVisible(false);
        successBox.setManaged(false);
        successBox.setAlignment(Pos.CENTER_LEFT);
        Label successIcon = new Label("\u2705");
        successIcon.setStyle("-fx-font-size: 14px;");
        Label successText = new Label();
        successText.getStyleClass().add("auth-success-text");
        successText.setWrapText(true);
        HBox.setHgrow(successText, Priority.ALWAYS);
        successBox.getChildren().addAll(successIcon, successText);

        // Bouton
        Button createBtn = new Button("Creer l'administrateur");
        createBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #6C3483, #8E44AD);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 9;" +
                        "-fx-padding: 11 28;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 13px;"
        );
        createBtn.setOnMouseEntered(e ->
                createBtn.setStyle(createBtn.getStyle().replace("#6C3483, #8E44AD", "#7D3C98, #9B59B6")));
        createBtn.setOnMouseExited(e ->
                createBtn.setStyle(createBtn.getStyle().replace("#7D3C98, #9B59B6", "#6C3483, #8E44AD")));

        createBtn.setOnAction(e -> {
            String username  = usernameField.getText().trim();
            String email     = emailField.getText().trim();
            String firstName = firstNameField.getText().trim();
            String lastName  = lastNameField.getText().trim();

            hideError(errorBox);
            hideError(successBox);

            if (username.isEmpty() || email.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                showError(errorBox, errorText, "Tous les champs sont obligatoires.");
                shakeNode(errorBox);
                return;
            }

            // 2FA gate: verify the Super Admin's identity before creating
            show2faDialog("Créer un administrateur", () -> handleCreateAdmin(
                usernameField, emailField, firstNameField, lastNameField,
                errorBox, errorText, successBox, successText, createBtn,
                usernameField, emailField, firstNameField, lastNameField
            ));
        });

        form.getChildren().addAll(formTitle, row1, row2, errorBox, successBox, createBtn);
        return form;
    }



    private void handleCreateAdmin(TextField usernameField, TextField emailField,
                                   TextField firstNameField, TextField lastNameField,
                                   HBox errorBox, Label errorText, HBox successBox, Label successText, Button createBtn,
                                   TextField u, TextField em, TextField fn,
                                   TextField ln) {

        String username  = usernameField.getText().trim();
        String email     = emailField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();

        hideError(errorBox);
        hideError(successBox);

        // Validation locale
        if (username.isEmpty() || email.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            showError(errorBox, errorText, "Tous les champs sont obligatoires.");
            shakeNode(errorBox); return;
        }

        createBtn.setDisable(true);
        createBtn.setText("Creation en cours...");

        ServerConnection.getInstance()
                .sendAsync(new Request(Action.ADMIN_CREATE_ADMIN,
                        AdminNavigationManager.getToken(),
                        Map.of(
                                "username",  username,
                                "email",     email,
                                "firstName", firstName,
                                "lastName",  lastName
                        )))
                .thenAccept(resp -> Platform.runLater(() -> {
                    createBtn.setDisable(false);
                    createBtn.setText("Creer l'administrateur");

                    if (resp.isSuccess()) {
                        String tokenInfo = "";
                        if (resp.getData() instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String,Object> data = (Map<String,Object>) resp.getData();
                            if (data.containsKey("activationToken")) {
                                tokenInfo = "\nL'email d'activation a ete envoye (Token: " + data.get("activationToken") + ").";
                            }
                        }
                        showError(successBox, successText, "Administrateur '" + username + "' cree avec succes !" + tokenInfo);
                        // Vider les champs
                        u.clear(); em.clear(); fn.clear(); ln.clear();
                        // Recharger la liste
                        loadAdmins();
                    } else {
                        showError(errorBox, errorText, resp.getMessage() != null ? resp.getMessage() : "Erreur.");
                        shakeNode(errorBox);
                    }
                }));
    }

    // ── Chargement de la liste des admins ─────────────────

    @SuppressWarnings("unchecked")
    private void loadAdmins() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.ADMIN_GET_ALL_ADMINS,
                        AdminNavigationManager.getToken(), null))
                .thenAccept(resp -> Platform.runLater(() -> {
                    showLoading(false);
                    if (resp.isSuccess() && resp.getData() != null) {
                        allAdmins = (List<Map<String, Object>>) resp.getData();
                    }
                    displayAdmins();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        displayAdmins();
                    });
                    return null;
                });
    }

    private void displayAdmins() {
        adminsContainer.getChildren().clear();

        if (allAdmins.isEmpty()) {
            Label none = new Label("Aucun administrateur trouve.");
            none.getStyleClass().add("admin-chart-empty");
            none.setPadding(new Insets(30));
            adminsContainer.getChildren().add(none);
            return;
        }

        // Header du tableau
        HBox header = new HBox();
        header.getStyleClass().add("admin-table-header");
        header.setPadding(new Insets(10, 16, 10, 16));
        header.getChildren().addAll(
                tCol("ID",         60),
                tCol("Username",   140),
                tCol("Email",      200),
                tCol("Nom complet",160),
                tCol("Type",       120),
                tCol("Statut",     120),
                tCol("Cree le",    140),
                tCol("Action",     160)
        );
        adminsContainer.getChildren().add(header);

        for (int i = 0; i < allAdmins.size(); i++) {
            adminsContainer.getChildren().add(buildAdminRow(allAdmins.get(i), i));
        }

        // Animation d'apparition des lignes
        for (int i = 1; i < adminsContainer.getChildren().size(); i++) {
            Node n = adminsContainer.getChildren().get(i);
            n.setOpacity(0);
            n.setTranslateY(12);
            int delay = (i - 1) * 30;
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

    private HBox buildAdminRow(Map<String, Object> admin, int index) {
        HBox row = new HBox();
        row.getStyleClass().add("admin-table-row");
        if (index % 2 == 0) row.getStyleClass().add("admin-table-row-alt");
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        int     adminId   = intV(admin.get("adminId"));
        String  username  = str(admin.get("username"));
        String  email     = str(admin.get("email"));
        String  firstName = str(admin.get("firstName"));
        String  lastName  = str(admin.get("lastName"));
        boolean isActive  = getBool(admin.get("isActive"));
        boolean isSuper   = getBool(admin.get("isSuper"));
        String  createdAt = str(admin.get("createdAt"));

        Label idL = tCell(String.valueOf(adminId), 60);
        idL.getStyleClass().add("admin-cell-id");

        // Avatar avec initiales
        HBox userCell = new HBox(8);
        userCell.setAlignment(Pos.CENTER_LEFT);
        userCell.setMinWidth(140);
        userCell.setMaxWidth(140);

        StackPane avatar = new StackPane();
        avatar.setMinSize(30, 30);
        avatar.setMaxSize(30, 30);
        avatar.setStyle(
                "-fx-background-color: linear-gradient(to bottom right," +
                        (isSuper ? "#E67E22,#D35400" : "#8E44AD,#6C3483") + ");" +
                        "-fx-background-radius: 50;"
        );
        String initials = "";
        if (!firstName.isEmpty()) initials += firstName.charAt(0);
        if (!lastName.isEmpty())  initials += lastName.charAt(0);
        if (initials.isEmpty())   initials = "A";
        Label initL = new Label(initials.toUpperCase());
        initL.setStyle("-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;");
        avatar.getChildren().add(initL);

        Label unL = new Label(username);
        unL.setStyle("-fx-font-weight:600;-fx-font-size:12px;-fx-text-fill:#2C3E50;");
        userCell.getChildren().addAll(avatar, unL);

        Label emailL = tCell(email.length() > 26
                ? email.substring(0, 24) + ".." : email, 200);
        emailL.getStyleClass().add("admin-cell-date");

        String fullName = (firstName + " " + lastName).trim();
        Label nameL = tCell(fullName.isEmpty() ? "--" : fullName, 160);

        // Badge type
        Label typeL = new Label(isSuper ? "SUPER" : "ADMIN");
        typeL.getStyleClass().add("admin-status-badge");
        if (isSuper) {
            typeL.setStyle("-fx-background-color: rgba(211,84,0,0.12); -fx-text-fill: #D35400;");
        } else {
            typeL.setStyle("-fx-background-color: rgba(142,68,173,0.12); -fx-text-fill: #8E44AD;");
        }
        
        HBox typeBox = new HBox(typeL);
        typeBox.setAlignment(Pos.CENTER_LEFT);
        typeBox.setMinWidth(120);
        typeBox.setMaxWidth(120);

        // Badge statut
        Label statusL = new Label(isActive ? "Actif" : "Desactive");
        statusL.getStyleClass().add("admin-status-badge");
        statusL.getStyleClass().add(isActive ? "admin-status-livree" : "admin-status-annulee");
        
        HBox statusBox = new HBox(statusL);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setMinWidth(120);
        statusBox.setMaxWidth(120);

        String dateStr = createdAt.length() > 10 ? createdAt.substring(0, 10) : createdAt;
        Label dateL = tCell(dateStr, 140);
        dateL.getStyleClass().add("admin-cell-date");

        // Actions
        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER_LEFT);
        actionBox.setMinWidth(160);

        boolean isSelf = adminId == AdminNavigationManager.getUserId();

        if (isSuper) {
            // Le super admin ne peut pas être désactivé
            Label protectedL = new Label("Protege");
            protectedL.setStyle(
                    "-fx-text-fill: #6b7280;" +
                            "-fx-font-size: 11px;" +
                            "-fx-font-style: italic;"
            );
            actionBox.getChildren().add(protectedL);
        } else {
            Button toggleBtn = new Button(isActive ? "Desactiver" : "Activer");
            toggleBtn.getStyleClass().add(
                    isActive ? "admin-toggle-deactivate" : "admin-toggle-activate");
            toggleBtn.setDisable(isSelf);
            toggleBtn.setOnAction(e -> doToggleAdmin(adminId, username, isActive));
            actionBox.getChildren().add(toggleBtn);
        }

        row.getChildren().addAll(idL, userCell, emailL, nameL,
                typeBox, statusBox, dateL, actionBox);
        return row;
    }

    // ── Toggle activer/désactiver ─────────────────────────

    private void doToggleAdmin(int adminId, String username, boolean wasActive) {
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.ADMIN_TOGGLE_ADMIN,
                        AdminNavigationManager.getToken(),
                        Map.of("adminId", adminId, "isActive", !wasActive)))
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        toast(username + (wasActive ? " desactive" : " active"), "success");
                        loadAdmins();
                    } else {
                        toast(resp.getMessage() != null
                                ? resp.getMessage() : "Erreur", "error");
                    }
                }));
    }

    // ── Accès refusé ──────────────────────────────────────

    private Node buildAccessDenied() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(80));
        Label msg = new Label("Acces refuse");
        msg.getStyleClass().add("admin-welcome-title");
        Label sub = new Label("Cette page est reservee au Super Administrateur.");
        sub.getStyleClass().add("admin-welcome-subtitle");
        Button back = new Button("Retour");
        back.getStyleClass().add("admin-action-btn");
        back.setOnAction(e -> AdminNavigationManager.showAdminDashboard());
        box.getChildren().addAll(msg, sub, back);
        return box;
    }

    // ── Toast notification ────────────────────────────────

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
        new SequentialTransition(
                new ParallelTransition(fi, si),
                hold,
                new ParallelTransition(fo, so)
        ).play();
    }

    // ── Helpers UI ────────────────────────────────────────

    private void showError(HBox box, Label text, String msg) {
        text.setText(msg); box.setVisible(true); box.setManaged(true); box.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(250), box); ft.setToValue(1); ft.play();
    }
    private void hideError(HBox box) { box.setVisible(false); box.setManaged(false); }
    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0); tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0)); tt.play();
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("auth-field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        return l;
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

    private String str(Object o)    { return o != null ? o.toString() : ""; }
    private boolean getBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        return "true".equals(o.toString()) || "1".equals(o.toString());
    }
    private int intV(Object o) {
        if (o == null) return 0;
        if (o instanceof Double) return ((Double) o).intValue();
        if (o instanceof Integer) return (Integer) o;
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }

    /**
     * Shows a 2FA step-up overlay before the given action.
     * Calls ADMIN_ACTION_2FA_INIT, shows OTP input, then calls onSuccess only on valid code.
     */
    private void show2faDialog(String actionLabel, Runnable onSuccess) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(10, 6, 24, 0.75);");
        overlay.setOpacity(0);

        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setPadding(new Insets(28, 36, 28, 36));
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(480);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setTranslateY(20);
        card.setOpacity(0);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER);
        Label lockIcon = new Label("\uD83D\uDD12");
        lockIcon.setStyle("-fx-font-size: 22px; -fx-text-fill: #2C3E50;");
        Label titleLbl = new Label("Autorisation requise");
        titleLbl.getStyleClass().add("auth-title");
        titleLbl.setStyle("-fx-font-size: 20px;");
        titleRow.getChildren().addAll(lockIcon, titleLbl);

        Label subtitle = new Label(actionLabel + "\n\nUn code à 6 chiffres vient d'être envoyé à votre adresse email...");
        subtitle.getStyleClass().add("auth-subtitle");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-alignment: center;");

        Region div = new Region();
        div.setStyle("-fx-background-color: #E8E8E8; -fx-pref-height: 1;");

        Label loadingLbl = new Label("\u23F3  Envoi du code en cours...");
        loadingLbl.getStyleClass().add("auth-subtitle");

        card.getChildren().addAll(titleRow, subtitle, div, loadingLbl);
        overlay.getChildren().add(card);

        // Attach overlay to the root pane
        StackPane rootStack = (StackPane) root.getCenter();
        rootStack.getChildren().add(overlay);

        // Entrance animation
        Timeline fadeIn = new Timeline(
            new KeyFrame(Duration.millis(0),   new KeyValue(overlay.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(250),  new KeyValue(overlay.opacityProperty(), 1))
        );
        fadeIn.play();
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), card);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition cardFade = new FadeTransition(Duration.millis(300), card);
        cardFade.setToValue(1);
        new ParallelTransition(slideIn, cardFade).play();

        Runnable closeOverlay = () -> {
            Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.millis(0),   new KeyValue(overlay.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(220),  new KeyValue(overlay.opacityProperty(), 0))
            );
            fadeOut.setOnFinished(ev -> rootStack.getChildren().remove(overlay));
            fadeOut.play();
        };

        ServerConnection.getInstance()
            .sendAsync(new Request(Action.ADMIN_ACTION_2FA_INIT, AdminNavigationManager.getToken(), null))
            .thenAccept(initResp -> Platform.runLater(() -> {
                if (!initResp.isSuccess()) {
                    closeOverlay.run();
                    toast("Impossible d'envoyer le code 2FA : " + initResp.getMessage(), "error");
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> initData = (Map<String, Object>) initResp.getData();
                int adminId  = ((Number) initData.get("adminId")).intValue();
                String email = (String) initData.get("email");

                subtitle.setText(actionLabel + "\n\nUn code à 6 chiffres vient d'être envoyé à votre adresse email :\n" + email);

                Label codeLabel = new Label("Code de vérification (2FA)");
                codeLabel.getStyleClass().add("auth-field-label");

                TextField codeField = new TextField();
                codeField.setPromptText("_ _ _ _ _ _");
                codeField.getStyleClass().add("auth-field");
                codeField.setAlignment(Pos.CENTER);
                codeField.setStyle("-fx-font-size: 22px; -fx-text-alignment: center; -fx-letter-spacing: 8px;");
                codeField.textProperty().addListener((obs, o, n) -> {
                    if (!n.matches("\\d*")) codeField.setText(n.replaceAll("[^\\d]", ""));
                    if (n.length() > 6)     codeField.setText(n.substring(0, 6));
                });
                VBox codeGroup = new VBox(5, codeLabel, codeField);

                HBox fbBox = new HBox(8);
                fbBox.getStyleClass().add("auth-error");
                fbBox.setVisible(false);
                fbBox.setManaged(false);
                fbBox.setAlignment(Pos.CENTER_LEFT);
                Label fbIcon = new Label("\u26A0");
                fbIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #E74C3C;");
                Label fbText = new Label();
                fbText.getStyleClass().add("auth-error-text");
                fbText.setWrapText(true);
                HBox.setHgrow(fbText, Priority.ALWAYS);
                fbBox.getChildren().addAll(fbIcon, fbText);

                Button verifyBtn = new Button("Valider le code 2FA");
                verifyBtn.getStyleClass().add("auth-btn-primary");

                Button cancelBtn = new Button("Annuler");
                cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; " +
                    "-fx-font-size: 12px; -fx-cursor: hand; -fx-underline: true;");
                cancelBtn.setOnAction(e -> closeOverlay.run());

                card.getChildren().setAll(titleRow, subtitle, div, codeGroup, fbBox, verifyBtn, cancelBtn);
                codeField.requestFocus();

                Runnable doVerify = () -> {
                    String code = codeField.getText().trim();
                    if (code.length() != 6) {
                        fbText.setText("Le code doit contenir 6 chiffres.");
                        fbBox.setVisible(true); fbBox.setManaged(true);
                        return;
                    }
                    verifyBtn.setDisable(true);
                    verifyBtn.setText("Vérification...");
                    fbBox.setVisible(false); fbBox.setManaged(false);

                    ServerConnection.getInstance()
                        .sendAsync(new Request(Action.ADMIN_ACTION_2FA_VERIFY,
                            AdminNavigationManager.getToken(),
                            Map.of("adminId", adminId, "code", code)))
                        .thenAccept(verifyResp -> Platform.runLater(() -> {
                            if (verifyResp.isSuccess()) {
                                closeOverlay.run();
                                onSuccess.run();
                            } else {
                                verifyBtn.setDisable(false);
                                verifyBtn.setText("Valider le code 2FA");
                                fbText.setText(verifyResp.getMessage() != null
                                    ? verifyResp.getMessage() : "Code incorrect ou expiré.");
                                fbBox.setVisible(true); fbBox.setManaged(true);
                                codeField.clear(); codeField.requestFocus();
                            }
                        }));
                };
                verifyBtn.setOnAction(e -> doVerify.run());
                codeField.setOnAction(e -> doVerify.run());
            }));
    }
}