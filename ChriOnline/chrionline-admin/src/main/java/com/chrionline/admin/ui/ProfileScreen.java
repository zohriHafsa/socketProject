package com.chrionline.admin.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.enums.Action;
import com.chrionline.shared.security.ECDSAUtils;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class ProfileScreen {

    private static final double MAX_W = 860;
    private static final double H_PAD = 40;
    private final BorderPane root;

    private boolean isEditing = false;
    private boolean isRotatingKeys = false;
    private final Map<String, TextField> editFields = new HashMap<>();

    // Rotation inputs
    private PasswordField rotatePassField;
    private PasswordField rotateConfirmField;

    // Persistent feedback across rebuilds
    private String pendingSuccessMessage = null;
    private String pendingErrorMessage = null;

    // Inline feedback controls
    private HBox feedbackBox;
    private Label feedbackIcon;
    private Label feedbackText;

    public ProfileScreen() {
        root = new BorderPane();
        root.getStyleClass().add("page-root");
        root.setTop(AdminDashboard.buildAdminHeader("Profil"));

        if (!AdminNavigationManager.isLoggedIn()) showLoginRequired();
        else buildContent();

        Platform.runLater(() -> {
            root.setOpacity(0);
            FadeTransition f = new FadeTransition(Duration.millis(400), root);
            f.setToValue(1); 
            f.play();
        });
    }

    public Node getView() { return root; }

    private void buildContent() {
        String fn       = orEmpty(AdminNavigationManager.getFirstName());
        String ln       = orEmpty(AdminNavigationManager.getLastName());
        String username = orEmpty(AdminNavigationManager.getUsername());
        String role     = orEmpty(AdminNavigationManager.getRole());
        String email    = orEmpty(AdminNavigationManager.getEmail());

        String fullName = (fn + " " + ln).trim();
        String displayName = fullName.isEmpty() ? username : fullName;

        String initials = "";
        if (!fn.isEmpty()) initials += fn.substring(0, 1).toUpperCase();
        if (!ln.isEmpty()) initials += ln.substring(0, 1).toUpperCase();
        if (initials.isEmpty() && !username.isEmpty()) initials = username.substring(0, 1).toUpperCase();

        VBox container = new VBox(20);
        container.setMaxWidth(MAX_W);
        container.setPadding(new Insets(30, H_PAD, 60, H_PAD));
        container.setAlignment(Pos.TOP_CENTER);

        // ── Header Row ───────────────────────────────────────
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label pageTitle = new Label("Mon Profil");
        pageTitle.getStyleClass().add("page-title");
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        Button editBtn = new Button(isEditing ? "Annuler" : "Modifier le profil");
        editBtn.getStyleClass().add(isEditing ? "btn-secondary" : "btn-primary");
        editBtn.setOnAction(e -> {
            isEditing = !isEditing;
            pendingSuccessMessage = null;
            pendingErrorMessage = null;
            buildContent();
        });
        
        headerRow.getChildren().addAll(pageTitle, headerSpacer, editBtn);

        // ════════════════════════════════════════════════════
        //  HERO CARD
        // ════════════════════════════════════════════════════
        HBox heroCard = new HBox(20);
        heroCard.getStyleClass().add("profile-hero-card");
        heroCard.setAlignment(Pos.CENTER_LEFT);
        heroCard.setPadding(new Insets(24, 28, 24, 28));

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("profile-avatar-lg");
        Label avatarLabel = new Label(initials);
        avatarLabel.getStyleClass().add("profile-avatar-text-lg");
        avatar.getChildren().add(avatarLabel);

        VBox heroText = new VBox(5);
        heroText.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(heroText, Priority.ALWAYS);

        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("profile-hero-name");

        Label usernameLabel = new Label("@" + username);
        usernameLabel.getStyleClass().add("profile-hero-username");

        Label roleBadge = new Label(fmtRole(role));
        roleBadge.getStyleClass().addAll("order-status-badge", "profile-badge-client");
        roleBadge.setStyle("-fx-background-color: #6C3483; -fx-text-fill: white;");

        heroText.getChildren().addAll(nameLabel, usernameLabel, roleBadge);
        
        heroCard.getChildren().addAll(avatar, heroText);

        // ════════════════════════════════════════════════════
        //  INFO CARD
        // ════════════════════════════════════════════════════
        VBox infoCard = new VBox(16);
        infoCard.getStyleClass().add("profile-info-card");

        Label infoTitle = new Label("Informations administratives");
        infoTitle.getStyleClass().add("profile-section-title");

        Region divider = new Region();
        divider.setStyle("-fx-background-color: #F0EEF6; -fx-pref-height: 1;");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(24);
        infoGrid.setVgap(14);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        infoGrid.getColumnConstraints().addAll(c1, c2);

        if (isEditing) {
            infoGrid.add(infoField("Nom d'utilisateur", "@" + username), 0, 0);
            infoGrid.add(infoField("E-mail", email), 1, 0);
            infoGrid.add(editField("firstName", "Prénom", fn), 0, 1);
            infoGrid.add(editField("lastName",  "Nom de famille", ln), 1, 1);
        } else {
            infoGrid.add(infoField("Nom d'utilisateur", username.isEmpty() ? "—" : "@" + username), 0, 0);
            infoGrid.add(infoField("E-mail",           email.isEmpty() ? "—" : email),    1, 0);
            infoGrid.add(infoField("Prénom",           fn.isEmpty() ? "—" : fn),          0, 1);
            infoGrid.add(infoField("Nom de famille",    ln.isEmpty() ? "—" : ln),          1, 1);
        }

        infoCard.getChildren().addAll(infoTitle, divider, infoGrid);

        // ════════════════════════════════════════════════════
        //  SECURITY CARD (Exporter ma clé & Rotation)
        // ════════════════════════════════════════════════════
        VBox securityCard = new VBox(16);
        securityCard.getStyleClass().add("profile-info-card");

        Label secTitle = new Label("Sécurité & Identifiants");
        secTitle.getStyleClass().add("profile-section-title");

        Region secDivider = new Region();
        secDivider.setStyle("-fx-background-color: #F0EEF6; -fx-pref-height: 1;");

        Label secDesc = new Label("Vos identifiants cryptographiques (clé privée RSA) sont sécurisés localement sur cet appareil dans le trousseau de l'application.");
        secDesc.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        secDesc.setWrapText(true);

        HBox secButtons = new HBox(12);
        secButtons.setAlignment(Pos.CENTER_LEFT);

        Button exportKeyBtn = new Button("📤  Exporter ma clé de sécurité (.p12)");
        exportKeyBtn.getStyleClass().add("btn-secondary");
        exportKeyBtn.setStyle("-fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand;");
        exportKeyBtn.setOnAction(evt -> handleExportKey(exportKeyBtn));

        Button rotateKeyBtn = new Button("🔄  Renouveler mes clés (Rotation)");
        rotateKeyBtn.getStyleClass().add("btn-secondary");
        rotateKeyBtn.setStyle("-fx-font-size: 13px; -fx-padding: 10 20; -fx-cursor: hand;");
        rotateKeyBtn.setOnAction(evt -> {
            isRotatingKeys = true;
            buildContent();
        });

        secButtons.getChildren().addAll(exportKeyBtn, rotateKeyBtn);

        // ════════════════════════════════════════════════════
        //  ROTATION FORM
        // ════════════════════════════════════════════════════
        VBox rotateForm = new VBox(12);
        rotateForm.setStyle("-fx-background-color: #F8F9FA; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8;");
        
        Label formTitle = new Label("🔑 Renouvellement de clé cryptographique");
        formTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6C3483;");
        
        Label formDesc = new Label("Cette action va générer une nouvelle paire de clés locales et invalider les anciennes sur le serveur pour révoquer immédiatement tout accès piraté.");
        formDesc.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 12px;");
        formDesc.setWrapText(true);

        HBox passwordsRow = new HBox(15);
        passwordsRow.setAlignment(Pos.CENTER_LEFT);

        VBox passBox = new VBox(4);
        Label passLabel = new Label("Nouvelle Passphrase de la clé");
        passLabel.getStyleClass().add("profile-field-label");
        rotatePassField = new PasswordField();
        rotatePassField.setPromptText("Minimum 6 caractères");
        rotatePassField.getStyleClass().add("auth-field");
        rotatePassField.setStyle("-fx-font-size: 13px; -fx-padding: 8 12;");
        StackPane passWrapper = wrapWithToggle(rotatePassField);
        passBox.getChildren().addAll(passLabel, passWrapper);
        HBox.setHgrow(passBox, Priority.ALWAYS);

        VBox confirmBox = new VBox(4);
        Label confirmLabel = new Label("Confirmer la nouvelle passphrase");
        confirmLabel.getStyleClass().add("profile-field-label");
        rotateConfirmField = new PasswordField();
        rotateConfirmField.setPromptText("Répétez la passphrase");
        rotateConfirmField.getStyleClass().add("auth-field");
        rotateConfirmField.setStyle("-fx-font-size: 13px; -fx-padding: 8 12;");
        StackPane confirmWrapper = wrapWithToggle(rotateConfirmField);
        confirmBox.getChildren().addAll(confirmLabel, confirmWrapper);
        HBox.setHgrow(confirmBox, Priority.ALWAYS);

        passwordsRow.getChildren().addAll(passBox, confirmBox);

        HBox formActions = new HBox(10);
        formActions.setAlignment(Pos.CENTER_LEFT);
        
        Button confirmRotateBtn = new Button("Confirmer la rotation");
        confirmRotateBtn.getStyleClass().add("btn-primary");
        confirmRotateBtn.setStyle("-fx-font-size: 12px; -fx-padding: 8 16;");
        confirmRotateBtn.setOnAction(e -> doRotateKeys(confirmRotateBtn));

        Button cancelRotateBtn = new Button("Annuler");
        cancelRotateBtn.getStyleClass().add("btn-secondary");
        cancelRotateBtn.setStyle("-fx-font-size: 12px; -fx-padding: 8 16;");
        cancelRotateBtn.setOnAction(e -> {
            isRotatingKeys = false;
            buildContent();
        });
        
        formActions.getChildren().addAll(confirmRotateBtn, cancelRotateBtn);
        rotateForm.getChildren().addAll(formTitle, formDesc, passwordsRow, formActions);

        // ════════════════════════════════════════════════════
        //  INLINE FEEDBACK BOX (Inside the security card!)
        // ════════════════════════════════════════════════════
        feedbackBox = new HBox(8);
        feedbackBox.setAlignment(Pos.CENTER_LEFT);
        feedbackBox.setVisible(false);
        feedbackBox.setManaged(false);
        
        feedbackIcon = new Label();
        feedbackIcon.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        feedbackText = new Label();
        feedbackText.setWrapText(true);
        HBox.setHgrow(feedbackText, Priority.ALWAYS);
        
        feedbackBox.getChildren().addAll(feedbackIcon, feedbackText);

        if (isRotatingKeys) {
            securityCard.getChildren().addAll(secTitle, secDivider, secDesc, rotateForm, feedbackBox);
        } else {
            securityCard.getChildren().addAll(secTitle, secDivider, secDesc, secButtons, feedbackBox);
        }

        // ════════════════════════════════════════════════════
        //  ACTIONS
        // ════════════════════════════════════════════════════
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (isEditing) {
            Button saveBtn = new Button("💾  Enregistrer les modifications");
            saveBtn.getStyleClass().add("btn-primary");
            saveBtn.setStyle("-fx-font-size:13px;-fx-padding:11 22;");
            saveBtn.setOnAction(e -> doUpdateProfile(saveBtn));
            actions.getChildren().add(saveBtn);
        } else {
            Button dashboardBtn = new Button("📊  Aller au Dashboard");
            dashboardBtn.getStyleClass().add("btn-primary");
            dashboardBtn.setStyle("-fx-font-size:13px;-fx-padding:11 22;");
            dashboardBtn.setOnAction(e -> AdminNavigationManager.showAdminDashboard());
            actions.getChildren().add(dashboardBtn);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Se déconnecter");
        logoutBtn.getStyleClass().add("profile-logout-btn");
        logoutBtn.setOnAction(e -> doLogout());

        actions.getChildren().addAll(spacer, logoutBtn);

        container.getChildren().addAll(headerRow, heroCard, infoCard, securityCard, actions);

        // Populate any pending feedback
        if (pendingSuccessMessage != null) {
            showSuccess(pendingSuccessMessage);
            pendingSuccessMessage = null;
        } else if (pendingErrorMessage != null) {
            showError(pendingErrorMessage);
            pendingErrorMessage = null;
        }

        StackPane centered = new StackPane(container);
        centered.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");
        root.setCenter(scroll);
    }

    private VBox editField(String key, String label, String value) {
        VBox box = new VBox(4);
        Label l = new Label(label);
        l.getStyleClass().add("profile-field-label");
        
        TextField tf = new TextField(value);
        tf.getStyleClass().add("auth-field");
        tf.setStyle("-fx-font-size: 13px; -fx-padding: 8 12;");
        editFields.put(key, tf);
        
        box.getChildren().addAll(l, tf);
        return box;
    }

    private void showSuccess(String msg) {
        feedbackBox.setVisible(true);
        feedbackBox.setManaged(true);
        feedbackBox.setStyle("-fx-background-color: #E8F8F5; -fx-background-radius: 6; -fx-padding: 10 15; -fx-border-color: #2ECC71; -fx-border-width: 1; -fx-border-radius: 6;");
        feedbackIcon.setText("✓");
        feedbackIcon.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 14px; -fx-font-weight: bold;");
        feedbackText.setText(msg);
        feedbackText.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 13px; -fx-font-weight: bold;");
    }

    private void showError(String msg) {
        feedbackBox.setVisible(true);
        feedbackBox.setManaged(true);
        feedbackBox.setStyle("-fx-background-color: #FDEDEC; -fx-background-radius: 6; -fx-padding: 10 15; -fx-border-color: #E74C3C; -fx-border-width: 1; -fx-border-radius: 6;");
        feedbackIcon.setText("⚠");
        feedbackIcon.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 14px; -fx-font-weight: bold;");
        feedbackText.setText(msg);
        feedbackText.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 13px; -fx-font-weight: bold;");
    }

    private void hideFeedback() {
        feedbackBox.setVisible(false);
        feedbackBox.setManaged(false);
    }

    private void doUpdateProfile(Button btn) {
        hideFeedback();
        String fn = editFields.get("firstName").getText().trim();
        String ln = editFields.get("lastName").getText().trim();

        if (fn.isEmpty() || ln.isEmpty()) {
            showError("Prénom et Nom de famille requis.");
            return;
        }

        btn.setDisable(true);
        btn.setText("Mise à jour...");

        Map<String, Object> params = new HashMap<>();
        params.put("firstName", fn);
        params.put("lastName", ln);
        params.put("phone", "");
        params.put("address", "");

        ServerConnection.getInstance().sendAsync(
            new Request(Action.UPDATE_PROFILE, AdminNavigationManager.getToken(), params)
        ).thenAccept(resp -> Platform.runLater(() -> {
            btn.setDisable(false);
            btn.setText("💾  Enregistrer les modifications");
            
            if (resp.isSuccess()) {
                AdminNavigationManager.setSession(
                    AdminNavigationManager.getToken(),
                    AdminNavigationManager.getUserId(),
                    AdminNavigationManager.getUsername(),
                    AdminNavigationManager.getRole(),
                    fn, ln,
                    AdminNavigationManager.getEmail()
                );
                isEditing = false;
                pendingSuccessMessage = "Profil mis à jour avec succès !";
                buildContent();
            } else {
                showError(resp.getMessage());
            }
        }));
    }

    private void doRotateKeys(Button btn) {
        hideFeedback();
        String pass = rotatePassField.getText();
        String confirm = rotateConfirmField.getText();

        if (pass.isEmpty() || confirm.isEmpty()) {
            showError("Tous les champs sont requis.");
            return;
        }

        if (!pass.equals(confirm)) {
            showError("Les passphrases ne correspondent pas.");
            return;
        }

        if (pass.length() < 6) {
            showError("La passphrase doit faire au moins 6 caractères.");
            return;
        }

        btn.setDisable(true);
        btn.setText("Génération de la nouvelle clé...");

        new Thread(() -> {
            try {
                String username = AdminNavigationManager.getUsername();
                File keystoreDir = new File(System.getProperty("user.home") + "/.chrionline/keystores");
                keystoreDir.mkdirs();
                
                // Atomically generate key into a temp file first!
                File tempFile = new File(keystoreDir, username + "_rotate_temp.p12");
                PublicKey newPubKey = ECDSAUtils.generateAndSaveKeyStore(username, tempFile, pass);
                String newPubKeyBase64 = ECDSAUtils.encodePublicKey(newPubKey);

                // Call server to update our registered key (authenticated with active session token!)
                ServerConnection.getInstance().sendAsync(
                    new Request(Action.ADMIN_ROTATE_KEY, AdminNavigationManager.getToken(), Map.of(
                        "publicKey", newPubKeyBase64
                    ))
                ).thenAccept(resp -> Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("Confirmer la rotation");
                    
                    if (resp.isSuccess()) {
                        try {
                            // Atomic transaction: replace the keystore file only when server confirms OK
                            File actualFile = new File(keystoreDir, username + ".p12");
                            if (actualFile.exists()) {
                                actualFile.delete();
                            }
                            tempFile.renameTo(actualFile);
                            
                            isRotatingKeys = false;
                            pendingSuccessMessage = "Clé de sécurité renouvelée et enregistrée ! Les anciennes clés sont désormais obsolètes et révoquées.";
                            buildContent();
                        } catch (Exception ex) {
                            showError("Erreur d'enregistrement local de la clé : " + ex.getMessage());
                        }
                    } else {
                        // Clean up the temporary file if server rejected it
                        tempFile.delete();
                        showError(resp.getMessage());
                    }
                }));

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("Confirmer la rotation");
                    showError("Erreur cryptographique : " + ex.getMessage());
                });
            }
        }).start();
    }

    private VBox infoField(String label, String value) {
        VBox box = new VBox(4);
        box.getStyleClass().add("profile-info-field");

        Label l = new Label(label);
        l.getStyleClass().add("profile-field-label");

        Label v = new Label(value != null && !value.isEmpty() ? value : "—");
        v.getStyleClass().add("profile-field-value");
        v.setWrapText(true);

        box.getChildren().addAll(l, v);
        return box;
    }

    private void handleExportKey(Button btn) {
        hideFeedback();
        String username = AdminNavigationManager.getUsername();
        if (username == null || username.isEmpty()) return;

        File sourceFile = new File(System.getProperty("user.home") + "/.chrionline/keystores/" + username + ".p12");
        if (!sourceFile.exists()) {
            sourceFile = new File(System.getProperty("user.home") + "/.chrionline/keys/" + username + "_admin.p12");
        }

        if (!sourceFile.exists()) {
            showError("Aucun fichier d'identifiants trouvé pour \"" + username + "\" sur cet appareil.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter mes identifiants de connexion");
        fc.setInitialFileName(username + ".p12");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PKCS12 KeyStore", "*.p12"));

        Stage stage = (Stage) root.getScene().getWindow();
        File destFile = fc.showSaveDialog(stage);
        if (destFile != null) {
            try {
                Files.copy(
                    sourceFile.toPath(),
                    destFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                );
                showSuccess("Vos identifiants ont été exportés avec succès vers :\n" + destFile.getAbsolutePath());
            } catch (Exception ex) {
                showError("Une erreur est survenue lors de l'exportation : " + ex.getMessage());
            }
        }
    }

    private void doLogout() {
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.LOGOUT, AdminNavigationManager.getToken(), null))
                .thenAccept(res -> Platform.runLater(() -> {
                    AdminNavigationManager.clearSession();
                    AdminNavigationManager.showAdminLogin();
                }));
    }

    private void showLoginRequired() {
        VBox b = new VBox(16); b.setAlignment(Pos.CENTER); b.setPadding(new Insets(80));
        Label t = new Label("Connectez-vous pour voir votre profil");
        t.getStyleClass().add("empty-state-title");
        Button btn = new Button("Se connecter"); btn.getStyleClass().add("btn-primary");
        btn.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        btn.setOnAction(e -> AdminNavigationManager.showAdminLogin());
        b.getChildren().addAll(t, btn);
        root.setCenter(b);
    }

    private String orEmpty(String s) { return s != null ? s : ""; }

    private String fmtRole(String role) {
        if (role == null) return "Client";
        switch (role) {
            case "SUPER_ADMIN": return "Super Admin";
            case "ADMIN":       return "Administrateur";
            default:            return "Client";
        }
    }

    private StackPane wrapWithToggle(PasswordField pf) {
        TextField visibleField = new TextField();
        visibleField.setPromptText(pf.getPromptText());
        visibleField.getStyleClass().addAll(pf.getStyleClass());
        visibleField.setStyle(pf.getStyle());
        visibleField.setVisible(false);
        visibleField.setManaged(false);

        pf.textProperty().addListener((obs, o, n) -> {
            if (!visibleField.isFocused()) visibleField.setText(n);
        });
        visibleField.textProperty().addListener((obs, o, n) -> {
            if (!pf.isFocused()) pf.setText(n);
        });

        Button eyeBtn = new Button("\uD83D\uDC41");
        eyeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 0 10 0 0;"
        );
        StackPane.setAlignment(eyeBtn, Pos.CENTER_RIGHT);

        eyeBtn.setOnAction(e -> {
            boolean showing = visibleField.isVisible();
            if (showing) {
                pf.setText(visibleField.getText());
                pf.setVisible(true);
                pf.setManaged(true);
                visibleField.setVisible(false);
                visibleField.setManaged(false);
                eyeBtn.setText("\uD83D\uDC41");
            } else {
                visibleField.setText(pf.getText());
                visibleField.setVisible(true);
                visibleField.setManaged(true);
                pf.setVisible(false);
                pf.setManaged(false);
                eyeBtn.setText("\u00D8");
            }
        });

        StackPane wrapper = new StackPane(pf, visibleField, eyeBtn);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        return wrapper;
    }
}
