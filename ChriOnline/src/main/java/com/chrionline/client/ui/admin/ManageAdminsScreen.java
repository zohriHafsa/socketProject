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
        if (!NavigationManager.isLoggedIn() || !NavigationManager.isSuperAdmin()) {
            root.setCenter(buildAccessDenied());
            return;
        }

        root.setTop(AdminDashboard.buildAdminHeader("Dashboard"));

        VBox mainContent = new VBox(24);
        mainContent.setMaxWidth(MAX_W);
        mainContent.setPadding(new Insets(25, H_PAD, 50, H_PAD));
        mainContent.setAlignment(Pos.TOP_CENTER);

        // Titre de la page
        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label pageTitle = new Label("Gestion des Administrateurs");
        pageTitle.getStyleClass().add("admin-page-title");

        Label superBadge = new Label("SUPER ADMIN");
        superBadge.setStyle(
                "-fx-background-color: #E74C3C;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 4;" +
                        "-fx-padding: 3 8;"
        );

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button refreshBtn = new Button("Actualiser");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setStyle("-fx-font-size:12px;-fx-padding:8 16;");
        refreshBtn.setOnAction(e -> loadAdmins());

        titleRow.getChildren().addAll(pageTitle, superBadge, sp, refreshBtn);

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
                        "-fx-border-color: #c4b5fd;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;"
        );

        Label formTitle = new Label("Ajouter un nouvel administrateur");
        formTitle.getStyleClass().add("admin-section-title");
        formTitle.setStyle("-fx-text-fill: #6d28d9; -fx-font-size: 15px;" +
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

        // Row 3 — password + confirm
        PasswordField passField    = new PasswordField();
        PasswordField confirmField = new PasswordField();
        passField.setPromptText("Minimum 8 caracteres");
        confirmField.setPromptText("Confirmer le mot de passe");
        passField.getStyleClass().add("auth-field");
        confirmField.getStyleClass().add("auth-field");

        VBox pwGroup = new VBox(5, fieldLabel("Mot de passe *"), passField);
        VBox cpGroup = new VBox(5, fieldLabel("Confirmer *"), confirmField);
        HBox.setHgrow(pwGroup, Priority.ALWAYS);
        HBox.setHgrow(cpGroup, Priority.ALWAYS);
        HBox row3 = new HBox(14, pwGroup, cpGroup);

        // Feedback
        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-font-size: 12px;");
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);

        // Bouton
        Button createBtn = new Button("Creer l'administrateur");
        createBtn.setStyle(
                "-fx-background-color: #6d28d9;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 9;" +
                        "-fx-padding: 11 28;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 13px;"
        );
        createBtn.setOnMouseEntered(e ->
                createBtn.setStyle(createBtn.getStyle().replace("#6d28d9", "#5b21b6")));
        createBtn.setOnMouseExited(e ->
                createBtn.setStyle(createBtn.getStyle().replace("#5b21b6", "#6d28d9")));

        createBtn.setOnAction(e -> handleCreateAdmin(
                usernameField, emailField, firstNameField, lastNameField,
                passField, confirmField, feedbackLabel, createBtn,
                usernameField, emailField, firstNameField, lastNameField,
                passField, confirmField
        ));

        form.getChildren().addAll(formTitle, row1, row2, row3, feedbackLabel, createBtn);
        return form;
    }

    private void handleCreateAdmin(TextField usernameField, TextField emailField,
                                   TextField firstNameField, TextField lastNameField,
                                   PasswordField passField, PasswordField confirmField,
                                   Label feedbackLabel, Button createBtn,
                                   TextField u, TextField em, TextField fn,
                                   TextField ln, PasswordField p, PasswordField c) {

        String username  = usernameField.getText().trim();
        String email     = emailField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String password  = passField.getText();
        String confirm   = confirmField.getText();

        // Validation locale
        if (username.isEmpty() || email.isEmpty() || firstName.isEmpty()
                || lastName.isEmpty() || password.isEmpty()) {
            showFeedback(feedbackLabel, "Tous les champs sont obligatoires.", false);
            return;
        }
        if (password.length() < 8) {
            showFeedback(feedbackLabel, "Le mot de passe doit contenir au moins 8 caracteres.", false);
            return;
        }
        if (!password.equals(confirm)) {
            showFeedback(feedbackLabel, "Les mots de passe ne correspondent pas.", false);
            return;
        }

        createBtn.setDisable(true);
        createBtn.setText("Creation en cours...");

        ServerConnection.getInstance()
                .sendAsync(new Request(Action.ADMIN_CREATE_ADMIN,
                        NavigationManager.getToken(),
                        Map.of(
                                "username",  username,
                                "email",     email,
                                "password",  password,
                                "firstName", firstName,
                                "lastName",  lastName
                        )))
                .thenAccept(resp -> Platform.runLater(() -> {
                    createBtn.setDisable(false);
                    createBtn.setText("Creer l'administrateur");

                    if (resp.isSuccess()) {
                        showFeedback(feedbackLabel,
                                "Administrateur '" + username + "' cree avec succes !", true);
                        // Vider les champs
                        u.clear(); em.clear(); fn.clear(); ln.clear();
                        p.clear(); c.clear();
                        // Recharger la liste
                        loadAdmins();
                    } else {
                        showFeedback(feedbackLabel,
                                resp.getMessage() != null ? resp.getMessage() : "Erreur.", false);
                    }
                }));
    }

    // ── Chargement de la liste des admins ─────────────────

    @SuppressWarnings("unchecked")
    private void loadAdmins() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.ADMIN_GET_ALL_ADMINS,
                        NavigationManager.getToken(), null))
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
                        (isSuper ? "#E74C3C,#C0392B" : "#8E44AD,#6C3483") + ");" +
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
        typeL.getStyleClass().add(isSuper ? "admin-status-annulee" : "admin-status-expediee");
        
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

        boolean isSelf = adminId == NavigationManager.getUserId();

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
                        NavigationManager.getToken(),
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
        back.setOnAction(e -> NavigationManager.showAdminDashboard());
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

    private void showFeedback(Label lbl, String msg, boolean success) {
        lbl.setText(msg);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " +
                (success ? "#166534" : "#991b1b") + ";");
        lbl.setVisible(true);
        lbl.setManaged(true);

        FadeTransition ft = new FadeTransition(Duration.millis(250), lbl);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
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
}