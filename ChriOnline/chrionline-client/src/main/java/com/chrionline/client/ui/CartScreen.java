package com.chrionline.client.ui;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.*;

public class CartScreen {

    private static final double MAX_W = 1100;
    private static final double H_PAD = 40;

    private final BorderPane root;
    private final StackPane  contentArea;
    private StackPane        loadingOverlay;

    private int currentStep = 0;
    private final String[] stepNames = {"Panier", "Livraison", "Paiement", "Confirmation"};
    private HBox stepperBar;

    private List<Map<String, Object>> cartItems = new ArrayList<>();
    private double cartTotal      = 0;
    private int    cartItemCount  = 0;
    private String shippingAddress = "";
    private String paymentMethod  = "CARTE_BANCAIRE";
    private String cardLastFour   = "";

    private boolean hasStockIssues = false;

    // Champs de paiement sécurisé
    private ImageView bankLogoView;
    private Label bankNameLabel;
    private TextField cardNumField;
    private TextField expiryField;
    private TextField cvvField;
    private Label validationMessageLabel;
    private Button validate2FAButton;
    private TextField tfaCodeField;
    private VBox tfa2Box;
    private boolean cardValidated = false;
    private boolean tfa2Validated = false;

    public CartScreen() {
        root = new BorderPane();
        root.getStyleClass().add("page-root");
        root.setTop(new HeaderComponent("cart", null, null, null).getView());

        contentArea = new StackPane();
        contentArea.setAlignment(Pos.TOP_CENTER);
        contentArea.setMinWidth(0);

        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-pane");

        loadingOverlay = buildLoading();
        StackPane body = new StackPane(scroll, loadingOverlay);
        root.setCenter(body);

        if (!NavigationManager.isLoggedIn()) {
            showLoginRequired();
        } else {
            loadCart();
        }

        Platform.runLater(() -> {
            root.setOpacity(0);
            FadeTransition f = new FadeTransition(Duration.millis(400), root);
            f.setToValue(1);
            f.play();
        });
    }

    public Node getView() { return root; }

    // ═══════════════════════════════════════════════════════
    //  STEPPER
    // ═══════════════════════════════════════════════════════

    private HBox buildStepper() {
        stepperBar = new HBox(0);
        stepperBar.setAlignment(Pos.CENTER);
        stepperBar.setPadding(new Insets(20, 0, 10, 0));

        for (int i = 0; i < stepNames.length; i++) {
            if (i > 0) {
                Region line = new Region();
                line.getStyleClass().add("step-line");
                if (i <= currentStep) line.getStyleClass().add("step-line-done");
                line.setPrefWidth(50);
                line.setMinHeight(2);
                line.setMaxHeight(2);
                stepperBar.getChildren().add(line);
            }

            VBox stepBox = new VBox(4);
            stepBox.setAlignment(Pos.CENTER);
            stepBox.setMinWidth(70);

            StackPane circle = new StackPane();
            circle.getStyleClass().add("step-circle");
            if (i < currentStep) circle.getStyleClass().add("step-circle-done");
            else if (i == currentStep) circle.getStyleClass().add("step-circle-active");

            Label num = new Label(i < currentStep ? "\u2713" : String.valueOf(i + 1));
            num.getStyleClass().add("step-number");
            if (i <= currentStep) num.getStyleClass().add("step-number-active");
            circle.getChildren().add(num);

            Label label = new Label(stepNames[i]);
            label.getStyleClass().add("step-label");
            if (i < currentStep) label.getStyleClass().add("step-label-done");
            else if (i == currentStep) label.getStyleClass().add("step-label-active");

            stepBox.getChildren().addAll(circle, label);
            stepperBar.getChildren().add(stepBox);
        }
        return stepperBar;
    }

    // ═══════════════════════════════════════════════════════
    //  STEP 0 — PANIER
    // ═══════════════════════════════════════════════════════

    private void showCartStep() {
        currentStep = 0;
        hasStockIssues = false;

        VBox page = new VBox(20);
        page.setMaxWidth(MAX_W);
        page.setPadding(new Insets(20, H_PAD, 40, H_PAD));
        page.setAlignment(Pos.TOP_CENTER);
        page.getChildren().add(buildStepper());

        if (cartItems.isEmpty()) {
            page.getChildren().add(buildEmptyCart());
            setContent(page);
            return;
        }

        for (Map<String, Object> item : cartItems) {
            int qty   = intV(item.get("quantity"));
            int stock = intV(item.get("stockQuantity"));
            if (stock < qty) {
                hasStockIssues = true;
                break;
            }
        }

        if (hasStockIssues) {
            HBox alert = new HBox(10);
            alert.setAlignment(Pos.CENTER_LEFT);
            alert.setStyle(
                    "-fx-background-color: rgba(243,156,18,0.1);" +
                            "-fx-border-color: rgba(243,156,18,0.3);" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 10;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 12 20;"
            );
            Label alertIcon = new Label("\u26A0\uFE0F");
            alertIcon.setStyle("-fx-font-size: 18px;");
            Label alertText = new Label(
                    "Certains articles ont un stock insuffisant. " +
                            "Veuillez ajuster les quantites ou retirer les articles concernes.");
            alertText.setStyle(
                    "-fx-text-fill: #E67E22; -fx-font-size: 12px; -fx-wrap-text: true;");
            alertText.setWrapText(true);
            HBox.setHgrow(alertText, Priority.ALWAYS);
            alert.getChildren().addAll(alertIcon, alertText);
            page.getChildren().add(alert);
        }

        HBox content = new HBox(24);
        content.setAlignment(Pos.TOP_CENTER);

        VBox itemsCol = new VBox(12);
        HBox.setHgrow(itemsCol, Priority.ALWAYS);

        Label itemsTitle = new Label(cartItemCount + " article(s) dans votre panier");
        itemsTitle.getStyleClass().add("page-subtitle");
        itemsCol.getChildren().add(itemsTitle);

        for (Map<String, Object> item : cartItems) {
            itemsCol.getChildren().add(buildCartItem(item));
        }

        VBox summaryCol = new VBox(16);
        summaryCol.setMinWidth(300);
        summaryCol.setMaxWidth(300);
        summaryCol.getChildren().add(buildSummaryCard(true));

        content.getChildren().addAll(itemsCol, summaryCol);
        page.getChildren().add(content);
        setContent(page);
    }

    private Node buildCartItem(Map<String, Object> item) {
        String name      = str(item.get("productName"));
        double unitPrice = dbl(item.get("unitPrice"));
        int    qty       = intV(item.get("quantity"));
        double subtotal  = dbl(item.get("subtotal"));
        int    pid       = intV(item.get("productId"));
        int    stock     = intV(item.get("stockQuantity"));
        String imageUrl  = str(item.get("imageUrl"));

        boolean stockIssue = stock < qty;
        boolean outOfStock = stock <= 0;

        VBox wrapper = new VBox(0);

        HBox card = new HBox(16);
        card.getStyleClass().add("cart-item-card");
        card.setAlignment(Pos.CENTER_LEFT);

        if (stockIssue) {
            card.setStyle(card.getStyle()
                    + "-fx-border-color: rgba(243,156,18,0.4);"
                    + "-fx-border-width: 1;"
                    + "-fx-border-radius: 14;");
        }
        if (outOfStock) {
            card.setStyle(card.getStyle()
                    + "-fx-border-color: rgba(231,76,60,0.4);"
                    + "-fx-border-width: 1.5;"
                    + "-fx-border-radius: 14;"
                    + "-fx-opacity: 0.7;");
        }

        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("cart-item-image");

        Image productImage = loadProductImage(imageUrl);

        if (productImage != null && !productImage.isError()) {
            ImageView iv = new ImageView(productImage);
            iv.setFitWidth(60);
            iv.setFitHeight(60);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            imgBox.getChildren().add(iv);
        } else {
            Label letter = new Label(
                    name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
            letter.getStyleClass().add("cart-item-image-letter");
            imgBox.getChildren().add(letter);
        }

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameL = new Label(name);
        nameL.getStyleClass().add("cart-item-name");

        Label priceL = new Label(String.format("%.2f MAD/u", unitPrice));
        priceL.getStyleClass().add("cart-item-unit");

        Button removeBtn = new Button("Retirer");
        removeBtn.getStyleClass().add("cart-remove-btn");
        removeBtn.setOnAction(e -> removeItem(pid));

        info.getChildren().addAll(nameL, priceL, removeBtn);

        Label qtyL = new Label("x" + qty);
        qtyL.getStyleClass().add("qty-label");

        Label subL = new Label(String.format("%.2f MAD", subtotal));
        subL.getStyleClass().add("cart-item-subtotal");
        subL.setMinWidth(100);

        card.getChildren().addAll(imgBox, info, qtyL, subL);
        wrapper.getChildren().add(card);

        if (outOfStock) {
            HBox stockWarn = new HBox(8);
            stockWarn.setAlignment(Pos.CENTER_LEFT);
            stockWarn.setStyle(
                    "-fx-background-color: rgba(231,76,60,0.08);" +
                            "-fx-background-radius: 0 0 14 14;" +
                            "-fx-padding: 8 18;"
            );
            Label wIcon = new Label("\u2717");
            wIcon.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
            Label wText = new Label("Rupture de stock — veuillez retirer cet article");
            wText.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11px;");
            Region wSp = new Region();
            HBox.setHgrow(wSp, Priority.ALWAYS);
            Button wRemove = new Button("Retirer");
            wRemove.getStyleClass().add("cart-remove-btn");
            wRemove.setOnAction(e -> removeItem(pid));
            stockWarn.getChildren().addAll(wIcon, wText, wSp, wRemove);
            wrapper.getChildren().add(stockWarn);

        } else if (stockIssue) {
            HBox stockWarn = new HBox(8);
            stockWarn.setAlignment(Pos.CENTER_LEFT);
            stockWarn.setStyle(
                    "-fx-background-color: rgba(243,156,18,0.08);" +
                            "-fx-background-radius: 0 0 14 14;" +
                            "-fx-padding: 8 18;"
            );
            Label wIcon = new Label("\u26A0");
            wIcon.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");
            Label wText = new Label(
                    "Stock insuffisant : " + stock + " disponible(s), " + qty + " demande(s)");
            wText.setStyle("-fx-text-fill: #E67E22; -fx-font-size: 11px;");
            stockWarn.getChildren().addAll(wIcon, wText);
            wrapper.getChildren().add(stockWarn);
        }

        return wrapper;
    }

    private VBox buildSummaryCard(boolean showCheckout) {
        VBox card = new VBox(14);
        card.getStyleClass().add("cart-summary-card");

        Label title = new Label("Recapitulatif");
        title.getStyleClass().add("cart-summary-title");

        double shipping = cartTotal >= 50 ? 0 : 4.99;
        double total = cartTotal + shipping;

        card.getChildren().addAll(
                title,
                summaryRow("Articles (" + cartItemCount + ")",
                        String.format("%.2f MAD", cartTotal)),
                summaryRow("Livraison",
                        shipping == 0 ? "Gratuite" : String.format("%.2f MAD", shipping))
        );

        if (shipping == 0) {
            Label free = new Label("\u2713 Livraison offerte !");
            free.setStyle(
                    "-fx-font-size:11px;-fx-text-fill:#27AE60;-fx-font-weight:bold;");
            card.getChildren().add(free);
        }

        Region div = new Region();
        div.getStyleClass().add("cart-summary-divider");
        card.getChildren().add(div);

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label tl = new Label("Total");
        tl.getStyleClass().add("cart-summary-total-label");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label tv = new Label(String.format("%.2f MAD", total));
        tv.getStyleClass().add("cart-summary-total-value");
        totalRow.getChildren().addAll(tl, sp, tv);
        card.getChildren().add(totalRow);

        if (showCheckout) {
            Button checkBtn = new Button("Continuer la commande");
            checkBtn.getStyleClass().add("cart-checkout-btn");

            if (hasStockIssues) {
                checkBtn.setDisable(true);
                checkBtn.setText("Resolvez les problemes de stock");
                checkBtn.setStyle(
                        "-fx-background-color: #BDC3C7;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 13px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 14 0;" +
                                "-fx-background-radius: 10;" +
                                "-fx-max-width: 1.7976931348623157E308;"
                );
            } else {
                checkBtn.setOnAction(e -> showShippingStep());
            }

            card.getChildren().add(checkBtn);

            Button contBtn = new Button("Continuer mes achats");
            contBtn.getStyleClass().add("cart-continue-btn");
            contBtn.setOnAction(e -> NavigationManager.showLanding());
            card.getChildren().add(contBtn);
        }

        return card;
    }

    private HBox summaryRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.getStyleClass().add("cart-summary-row-label");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value);
        v.getStyleClass().add("cart-summary-row-value");
        row.getChildren().addAll(l, sp, v);
        return row;
    }

    // ═══════════════════════════════════════════════════════
    //  STEP 1 — LIVRAISON
    // ═══════════════════════════════════════════════════════

    private void showShippingStep() {
        currentStep = 1;
        VBox page = new VBox(20);
        page.setMaxWidth(MAX_W);
        page.setPadding(new Insets(20, H_PAD, 40, H_PAD));
        page.setAlignment(Pos.TOP_CENTER);
        page.getChildren().add(buildStepper());

        HBox content = new HBox(24);
        content.setAlignment(Pos.TOP_CENTER);

        VBox form = new VBox(16);
        HBox.setHgrow(form, Priority.ALWAYS);

        VBox addrCard = new VBox(14);
        addrCard.getStyleClass().add("checkout-card");

        Label addrTitle = new Label("\uD83D\uDCE6  Adresse de livraison");
        addrTitle.getStyleClass().add("checkout-section-title");

        TextField addrField = new TextField(shippingAddress);
        addrField.setPromptText("Adresse complete");
        addrField.getStyleClass().add("auth-field");
        addrField.setStyle("-fx-padding:12 14;");

        TextField cityField = new TextField();
        cityField.setPromptText("Ville");
        cityField.getStyleClass().add("auth-field");
        cityField.setStyle("-fx-padding:12 14;");

        HBox zipCountry = new HBox(12);
        TextField zipField = new TextField();
        zipField.setPromptText("Code postal");
        zipField.getStyleClass().add("auth-field");
        zipField.setStyle("-fx-padding:12 14;");
        HBox.setHgrow(zipField, Priority.ALWAYS);

        TextField countryField = new TextField("Maroc");
        countryField.getStyleClass().add("auth-field");
        countryField.setStyle("-fx-padding:12 14;");
        HBox.setHgrow(countryField, Priority.ALWAYS);
        zipCountry.getChildren().addAll(zipField, countryField);

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button backBtn = new Button("\u2190 Retour");
        backBtn.getStyleClass().add("cart-continue-btn");
        backBtn.setStyle("-fx-max-width:120;");
        backBtn.setOnAction(e -> showCartStep());

        Button nextBtn = new Button("Continuer \u2192");
        nextBtn.getStyleClass().add("cart-checkout-btn");
        nextBtn.setStyle("-fx-max-width:180;");
        nextBtn.setOnAction(e -> {
            String addr = addrField.getText().trim();
            String city = cityField.getText().trim();
            String zip  = zipField.getText().trim();
            if (addr.isEmpty()) addr = "Adresse par defaut";
            shippingAddress = addr
                    + (city.isEmpty() ? "" : ", " + city)
                    + (zip.isEmpty() ? "" : " " + zip);
            showPaymentStep();
        });
        btns.getChildren().addAll(backBtn, nextBtn);

        addrCard.getChildren().addAll(addrTitle, addrField, cityField, zipCountry, btns);
        form.getChildren().add(addrCard);

        VBox summaryCol = new VBox(16);
        summaryCol.setMinWidth(300);
        summaryCol.setMaxWidth(300);
        summaryCol.getChildren().add(buildSummaryCard(false));

        content.getChildren().addAll(form, summaryCol);
        page.getChildren().add(content);
        setContent(page);
    }

    // ═══════════════════════════════════════════════════════
    //  STEP 2 — PAIEMENT SÉCURISÉ (NOUVEAU)
    // ═══════════════════════════════════════════════════════

    private void showPaymentStep() {
        currentStep = 2;
        cardValidated = false;
        tfa2Validated = false;

        VBox page = new VBox(20);
        page.setMaxWidth(MAX_W);
        page.setPadding(new Insets(20, H_PAD, 40, H_PAD));
        page.setAlignment(Pos.TOP_CENTER);
        page.getChildren().add(buildStepper());

        HBox content = new HBox(24);
        content.setAlignment(Pos.TOP_CENTER);

        VBox form = new VBox(16);
        HBox.setHgrow(form, Priority.ALWAYS);

        VBox payCard = new VBox(16);
        payCard.getStyleClass().add("checkout-card");

        Label payTitle = new Label("\uD83D\uDCB3  Informations de carte");
        payTitle.getStyleClass().add("checkout-section-title");

        // ── Logo banque + nom ────────────────────────────────
        HBox bankInfoBox = new HBox(12);
        bankInfoBox.setAlignment(Pos.CENTER_LEFT);
        bankInfoBox.setVisible(false);
        bankInfoBox.setManaged(false);
        bankInfoBox.setStyle(
                "-fx-background-color: rgba(108,52,131,0.05);" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 12 16;"
        );

        bankLogoView = new ImageView();
        bankLogoView.setFitWidth(80);
        bankLogoView.setFitHeight(40);
        bankLogoView.setPreserveRatio(true);
        bankLogoView.setSmooth(true);

        bankNameLabel = new Label();
        bankNameLabel.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #6C3483;"
        );

        bankInfoBox.getChildren().addAll(bankLogoView, bankNameLabel);

        // ── Champs carte ─────────────────────────────────────
        cardNumField = new TextField();
        cardNumField.setPromptText("Numéro de carte");
        cardNumField.getStyleClass().add("auth-field");
        cardNumField.setStyle("-fx-padding:12 14; -fx-font-size:14px;");

        // Auto-formatage 16 chiffres avec espaces
        cardNumField.textProperty().addListener((obs, old, neu) -> {
            if (neu.length() > 19) {
                cardNumField.setText(old);
                return;
            }

            String cleaned = neu.replaceAll("\\s+", "");
            if (!cleaned.matches("\\d*")) {
                cardNumField.setText(old);
                return;
            }

            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < cleaned.length(); i++) {
                if (i > 0 && i % 4 == 0) formatted.append(" ");
                formatted.append(cleaned.charAt(i));
            }

            if (!formatted.toString().equals(neu)) {
                cardNumField.setText(formatted.toString());
                cardNumField.positionCaret(formatted.length());
            }

            // Validation en temps réel dès 6 chiffres (BIN)
            if (cleaned.length() >= 6) {
                validateCardRealtime(cleaned, bankInfoBox);
            } else {
                bankInfoBox.setVisible(false);
                bankInfoBox.setManaged(false);
            }
        });

        HBox cardRow = new HBox(12);

        expiryField = new TextField();
        expiryField.setPromptText("MM/AA");
        expiryField.getStyleClass().add("auth-field");
        expiryField.setStyle("-fx-padding:12 14;");
        HBox.setHgrow(expiryField, Priority.ALWAYS);

        // Auto-formatage MM/AA
        expiryField.textProperty().addListener((obs, old, neu) -> {
            if (neu.length() > 5) {
                expiryField.setText(old);
                return;
            }

            String cleaned = neu.replaceAll("[^\\d]", "");
            if (cleaned.length() > 4) cleaned = cleaned.substring(0, 4);

            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < cleaned.length(); i++) {
                if (i == 2) formatted.append("/");
                formatted.append(cleaned.charAt(i));
            }

            if (!formatted.toString().equals(neu)) {
                expiryField.setText(formatted.toString());
                expiryField.positionCaret(formatted.length());
            }
        });

        cvvField = new TextField();
        cvvField.setPromptText("CVV");
        cvvField.getStyleClass().add("auth-field");
        cvvField.setStyle("-fx-padding:12 14;");
        cvvField.setMaxWidth(100);

        // Limiter à 3-4 chiffres
        cvvField.textProperty().addListener((obs, old, neu) -> {
            if (!neu.matches("\\d*") || neu.length() > 4) {
                cvvField.setText(old);
            }
        });

        cardRow.getChildren().addAll(expiryField, cvvField);

        // ── Message de validation ────────────────────────────
        validationMessageLabel = new Label();
        validationMessageLabel.setWrapText(true);
        validationMessageLabel.setVisible(false);
        validationMessageLabel.setManaged(false);

        // ── Bouton validation carte ──────────────────────────
        Button validateCardBtn = new Button("Valider la carte");
        validateCardBtn.getStyleClass().add("cart-checkout-btn");
        validateCardBtn.setStyle("-fx-max-width:200;");
        validateCardBtn.setOnAction(e -> validateCardFull());

        // ── Section 2FA (cachée au début) ─────────────────────
        tfa2Box = new VBox(12);
        tfa2Box.setVisible(false);
        tfa2Box.setManaged(false);
        tfa2Box.setStyle(
                "-fx-background-color: rgba(39,174,96,0.05);" +
                        "-fx-border-color: rgba(39,174,96,0.3);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 16;"
        );

        Label tfa2Title = new Label("\uD83D\uDD12  Confirmation 2FA");
        tfa2Title.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #27AE60;"
        );

        Label tfa2Info = new Label(
                "Un code de sécurité a été envoyé à votre email. " +
                        "Veuillez le saisir ci-dessous pour finaliser le paiement."
        );
        tfa2Info.setWrapText(true);
        tfa2Info.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        tfaCodeField = new TextField();
        tfaCodeField.setPromptText("Code 6 chiffres");
        tfaCodeField.getStyleClass().add("auth-field");
        tfaCodeField.setStyle("-fx-padding:12 14;");
        tfaCodeField.setMaxWidth(200);

        validate2FAButton = new Button("Valider et payer");
        validate2FAButton.getStyleClass().add("cart-checkout-btn");
        validate2FAButton.setStyle("-fx-max-width:200;");
        validate2FAButton.setOnAction(e -> verify2FAAndPay());

        tfa2Box.getChildren().addAll(tfa2Title, tfa2Info, tfaCodeField, validate2FAButton);

        // ── Boutons navigation ───────────────────────────────
        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_RIGHT);

        Button backBtn = new Button("\u2190 Retour");
        backBtn.getStyleClass().add("cart-continue-btn");
        backBtn.setStyle("-fx-max-width:120;");
        backBtn.setOnAction(e -> showShippingStep());

        btns.getChildren().add(backBtn);

        // ── Assemblage ───────────────────────────────────────
        payCard.getChildren().addAll(
                payTitle,
                bankInfoBox,
                cardNumField,
                cardRow,
                validationMessageLabel,
                validateCardBtn,
                tfa2Box,
                btns
        );

        form.getChildren().add(payCard);

        // ── Colonne résumé ───────────────────────────────────
        VBox summaryCol = new VBox(16);
        summaryCol.setMinWidth(300);
        summaryCol.setMaxWidth(300);
        summaryCol.getChildren().add(buildSummaryCard(false));

        VBox shipRecap = new VBox(8);
        shipRecap.getStyleClass().add("checkout-card");
        Label shipTitle = new Label("\uD83D\uDCE6 Livraison");
        shipTitle.getStyleClass().add("checkout-section-title");
        Label shipAddr = new Label(
                shippingAddress.isEmpty() ? "Adresse par défaut" : shippingAddress);
        shipAddr.getStyleClass().add("page-subtitle");
        shipAddr.setWrapText(true);
        shipRecap.getChildren().addAll(shipTitle, shipAddr);
        summaryCol.getChildren().add(shipRecap);

        content.getChildren().addAll(form, summaryCol);
        page.getChildren().add(content);
        setContent(page);
    }

    /**
     * Validation en temps réel du BIN (affichage du logo banque).
     */
    private void validateCardRealtime(String cardNumber, HBox bankInfoBox) {
        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", cardNumber);
        data.put("expiry", "12/99");
        data.put("cvv", "123");

        ServerConnection.getInstance()
                .sendAsync(new Request(Action.VALIDATE_CARD,
                        NavigationManager.getToken(), data))
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.isSuccess() && res.getData() != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> cardInfo = (Map<String, Object>) res.getData();

                        String bankName = str(cardInfo.get("bankName"));
                        String logoPath = str(cardInfo.get("bankLogo"));

                        try {
                            InputStream is = getClass().getResourceAsStream(logoPath);
                            if (is != null) {
                                bankLogoView.setImage(new Image(is));
                                bankNameLabel.setText(bankName);
                                bankInfoBox.setVisible(true);
                                bankInfoBox.setManaged(true);
                            }
                        } catch (Exception ex) {
                            bankNameLabel.setText(bankName);
                            bankLogoView.setImage(null);
                            bankInfoBox.setVisible(true);
                            bankInfoBox.setManaged(true);
                        }
                    }
                }));
    }

    /**
     * Validation complète de la carte (avec date + CVV).
     */
    private void validateCardFull() {
        String cardNumber = cardNumField.getText().trim();
        String expiry = expiryField.getText().trim();
        String cvv = cvvField.getText().trim();

        if (cardNumber.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
            showValidationMessage("Veuillez remplir tous les champs.", false);
            return;
        }

        showLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("cardNumber", cardNumber);
        data.put("expiry", expiry);
        data.put("cvv", cvv);

        ServerConnection.getInstance()
                .sendAsync(new Request(Action.VALIDATE_CARD,
                        NavigationManager.getToken(), data))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);

                    if (res.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> cardInfo = (Map<String, Object>) res.getData();

                        cardLastFour = str(cardInfo.get("last4"));
                        cardValidated = true;

                        showValidationMessage("✅ Carte validée avec succès !", true);
                        send2FACode();

                    } else {
                        cardValidated = false;
                        showValidationMessage("❌ " + res.getMessage(), false);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        showValidationMessage("Erreur réseau.", false);
                    });
                    return null;
                });
    }

    /**
     * Envoie le code 2FA pour le paiement.
     */
    private void send2FACode() {
        double shipping = cartTotal >= 50 ? 0 : 4.99;
        double total = cartTotal + shipping;

        Map<String, Object> data = new HashMap<>();
        data.put("amount", total);
        data.put("orderId", "TEMP-" + System.currentTimeMillis());
        data.put("email", NavigationManager.getEmail());

        ServerConnection.getInstance()
                .sendAsync(new Request(Action.SEND_PAYMENT_2FA,
                        NavigationManager.getToken(), data))
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        tfa2Box.setVisible(true);
                        tfa2Box.setManaged(true);
                        showValidationMessage(
                                "✉️ Code de confirmation envoyé à votre email !", true);
                    }
                }));
    }

    /**
     * Vérifie le code 2FA et finalise le paiement.
     */
    private void verify2FAAndPay() {
        String code = tfaCodeField.getText().trim();

        if (code.isEmpty() || !code.matches("\\d{6}")) {
            showValidationMessage("Code invalide (6 chiffres requis).", false);
            return;
        }

        showLoading(true);

        ServerConnection.getInstance()
                .sendAsync(new Request(Action.VERIFY_PAYMENT_2FA,
                        NavigationManager.getToken(), Map.of("code", code)))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);

                    if (res.isSuccess()) {
                        tfa2Validated = true;
                        doPlaceOrder();
                    } else {
                        showValidationMessage("❌ " + res.getMessage(), false);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        showValidationMessage("Erreur réseau.", false);
                    });
                    return null;
                });
    }

    /**
     * Affiche un message de validation (succès/erreur).
     */
    private void showValidationMessage(String message, boolean success) {
        validationMessageLabel.setText(message);
        validationMessageLabel.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + (success ? "#27AE60" : "#E74C3C") + ";" +
                        "-fx-padding: 10 0;"
        );
        validationMessageLabel.setVisible(true);
        validationMessageLabel.setManaged(true);

        if (success) {
            PauseTransition pause = new PauseTransition(Duration.seconds(5));
            pause.setOnFinished(e -> {
                validationMessageLabel.setVisible(false);
                validationMessageLabel.setManaged(false);
            });
            pause.play();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  STEP 3 — CONFIRMATION + CÉLÉBRATION
    // ═══════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void showConfirmation(Map<String, Object> result) {
        currentStep = 3;
        VBox page = new VBox(20);
        page.setMaxWidth(MAX_W);
        page.setPadding(new Insets(20, H_PAD, 40, H_PAD));
        page.setAlignment(Pos.TOP_CENTER);
        page.getChildren().add(buildStepper());

        VBox celebration = new VBox(16);
        celebration.setAlignment(Pos.CENTER);
        celebration.setPadding(new Insets(20, 0, 20, 0));

        Label icon = new Label("\uD83C\uDF89");
        icon.getStyleClass().add("celebration-icon");
        Label cTitle = new Label("Commande confirmee !");
        cTitle.getStyleClass().add("celebration-title");
        Label cSub = new Label(
                "Merci pour votre achat. Votre commande est en cours de traitement.");
        cSub.getStyleClass().add("celebration-subtitle");
        celebration.getChildren().addAll(icon, cTitle, cSub);

        VBox receipt = new VBox(14);
        receipt.getStyleClass().add("receipt-card");
        receipt.setMaxWidth(600);

        Label recTitle = new Label("Recapitulatif de commande");
        recTitle.getStyleClass().add("receipt-title");

        String orderId    = str(result.get("orderId"));
        String payStatus  = str(result.get("paymentStatus"));
        String orderStatus = str(result.get("orderStatus"));
        double total      = dbl(result.get("totalAmount"));
        String txnRef     = str(result.get("transactionRef"));

        receipt.getChildren().addAll(
                recTitle,
                receiptRow("N\u00B0 Commande", orderId),
                receiptRow("Statut", orderStatus),
                receiptRow("Montant total", String.format("%.2f MAD", total)),
                receiptDivider(),
                receiptRow("Paiement", payStatus),
                receiptRow("Methode", "CARTE_BANCAIRE"),
                receiptRow("Reference", txnRef.isEmpty() ? "\u2014" : txnRef),
                receiptDivider(),
                receiptRow("Adresse",
                        shippingAddress.isEmpty() ? "Par defaut" : shippingAddress)
        );

        Label statusInd = new Label(
                "ACCEPTE".equals(payStatus)
                        ? "\u2705 Paiement accepte"
                        : "\u26A0 En attente de paiement");
        statusInd.setStyle(
                "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:"
                        + ("ACCEPTE".equals(payStatus) ? "#27AE60" : "#F39C12") + ";");
        receipt.getChildren().add(statusInd);

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER);
        btns.setPadding(new Insets(10, 0, 0, 0));

        Button viewOrders = new Button("Voir mes commandes");
        viewOrders.getStyleClass().add("btn-primary");
        viewOrders.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        viewOrders.setOnAction(e -> NavigationManager.showOrders());

        Button shopBtn = new Button("Retour a la boutique");
        shopBtn.getStyleClass().add("btn-secondary");
        shopBtn.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        shopBtn.setOnAction(e -> NavigationManager.showLanding());

        btns.getChildren().addAll(viewOrders, shopBtn);
        page.getChildren().addAll(celebration, receipt, btns);
        setContent(page);

        Platform.runLater(this::launchConfetti);
    }

    private HBox receiptRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.getStyleClass().add("receipt-label");
        l.setMinWidth(140);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value != null ? value : "\u2014");
        v.getStyleClass().add("receipt-value");
        row.getChildren().addAll(l, sp, v);
        return row;
    }

    private Region receiptDivider() {
        Region d = new Region();
        d.getStyleClass().add("receipt-divider");
        return d;
    }

    private void launchConfetti() {
        Pane confettiLayer = new Pane();
        confettiLayer.setMouseTransparent(true);
        confettiLayer.setManaged(false);

        StackPane rootStack = (StackPane) root.getParent();
        if (rootStack == null) return;
        rootStack.getChildren().add(confettiLayer);

        Color[] colors = {
                Color.web("#6C3483"), Color.web("#F39C12"),
                Color.web("#27AE60"), Color.web("#E74C3C"),
                Color.web("#3498DB"), Color.web("#8E44AD")
        };

        Random rand = new Random();
        for (int i = 0; i < 60; i++) {
            Rectangle conf = new Rectangle(8, 8);
            conf.setFill(colors[rand.nextInt(colors.length)]);
            conf.setRotate(rand.nextDouble() * 360);
            conf.setLayoutX(200 + rand.nextDouble() * 900);
            conf.setLayoutY(-20);
            conf.setOpacity(0.9);
            confettiLayer.getChildren().add(conf);

            double dur = 1500 + rand.nextDouble() * 2000;
            TranslateTransition fall = new TranslateTransition(
                    Duration.millis(dur), conf);
            fall.setByY(800 + rand.nextDouble() * 200);
            fall.setByX((rand.nextDouble() - 0.5) * 200);
            fall.setInterpolator(Interpolator.EASE_IN);

            RotateTransition spin = new RotateTransition(
                    Duration.millis(dur), conf);
            spin.setByAngle(360 + rand.nextDouble() * 720);

            FadeTransition fade = new FadeTransition(
                    Duration.millis(dur), conf);
            fade.setFromValue(0.9);
            fade.setToValue(0);

            ParallelTransition anim = new ParallelTransition(fall, spin, fade);
            anim.setDelay(Duration.millis(rand.nextDouble() * 500));
            anim.play();
        }

        PauseTransition cleanup = new PauseTransition(Duration.seconds(4));
        cleanup.setOnFinished(e -> rootStack.getChildren().remove(confettiLayer));
        cleanup.play();
    }

    // ═══════════════════════════════════════════════════════
    //  PLACE ORDER
    // ═══════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void doPlaceOrder() {
        showLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", NavigationManager.getUserId());
        data.put("shippingAddress",
                shippingAddress.isEmpty() ? "Adresse par defaut" : shippingAddress);
        data.put("paymentMethod", "CARTE_BANCAIRE");
        if (!cardLastFour.isEmpty()) data.put("cardLastFour", cardLastFour);

        ServerConnection.getInstance()
                .sendAsync(new Request(Action.PLACE_ORDER,
                        NavigationManager.getToken(), data))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null) {
                        showConfirmation((Map<String, Object>) res.getData());
                    } else {
                        showPaymentStep();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showLoading(false));
                    return null;
                });
    }

    // ═══════════════════════════════════════════════════════
    //  LOAD CART
    // ═══════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void loadCart() {
        showLoading(true);
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.GET_CART,
                        NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId())))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null) {
                        Map<String, Object> data =
                                (Map<String, Object>) res.getData();
                        cartItems = data.get("items") != null
                                ? (List<Map<String, Object>>) data.get("items")
                                : new ArrayList<>();
                        cartTotal     = dbl(data.get("total"));
                        cartItemCount = intV(data.get("itemCount"));
                        showCartStep();
                    } else {
                        showCartStep();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        showCartStep();
                    });
                    return null;
                });
    }

    private void removeItem(int productId) {
        ServerConnection.getInstance()
                .sendAsync(new Request(Action.REMOVE_FROM_CART,
                        NavigationManager.getToken(),
                        Map.of("userId", NavigationManager.getUserId(),
                                "productId", productId)))
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.isSuccess()) loadCart();
                }));
    }

    private Image loadProductImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) return null;
        imageUrl = imageUrl.trim();

        try {
            InputStream is = getClass().getResourceAsStream(imageUrl);
            if (is != null) {
                Image img = new Image(is);
                if (!img.isError()) return img;
            }
        } catch (Exception ignored) {}

        return null;
    }

    private void setContent(Node node) {
        node.setOpacity(0);
        contentArea.getChildren().setAll(node);
        FadeTransition f = new FadeTransition(Duration.millis(350), node);
        f.setToValue(1);
        f.play();
    }

    private VBox buildEmptyCart() {
        VBox empty = new VBox(16);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(60));

        Label icon = new Label("\uD83D\uDED2");
        icon.getStyleClass().add("empty-state-icon");
        Label text = new Label("Votre panier est vide");
        text.getStyleClass().add("empty-state-title");
        Label sub = new Label(
                "Parcourez notre boutique pour ajouter des produits");
        sub.getStyleClass().add("empty-state-text");
        Button shop = new Button("Explorer la boutique");
        shop.getStyleClass().add("btn-primary");
        shop.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        shop.setOnAction(e -> NavigationManager.showLanding());
        empty.getChildren().addAll(icon, text, sub, shop);
        return empty;
    }

    private void showLoginRequired() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(80));
        Label t = new Label("Connectez-vous pour voir votre panier");
        t.getStyleClass().add("empty-state-title");
        Button b = new Button("Se connecter");
        b.getStyleClass().add("btn-primary");
        b.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        b.setOnAction(e -> NavigationManager.showLogin());
        box.getChildren().addAll(t, b);
        contentArea.getChildren().setAll(box);
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

    private double dbl(Object o) {
        if (o == null) return 0;
        if (o instanceof Double) return (Double) o;
        try { return Double.parseDouble(o.toString()); }
        catch (Exception e) { return 0; }
    }
}