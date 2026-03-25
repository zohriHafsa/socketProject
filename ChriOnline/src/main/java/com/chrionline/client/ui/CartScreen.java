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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.*;

public class CartScreen {

    private static final double MAX_W = 1100;
    private static final double H_PAD = 40;

    private final BorderPane root;
    private final StackPane  contentArea;
    private StackPane        loadingOverlay;

    // Stepper
    private int currentStep = 0; // 0=Cart, 1=Shipping, 2=Payment, 3=Confirm
    private final String[] stepNames = {"Panier", "Livraison", "Paiement", "Confirmation"};
    private HBox stepperBar;

    // Data
    private List<Map<String, Object>> cartItems = new ArrayList<>();
    private double cartTotal = 0;
    private int    cartItemCount = 0;
    private String shippingAddress = "";
    private String paymentMethod = "PAIEMENT_FICTIF";
    private String cardLastFour = "";

    public CartScreen() {
        root = new BorderPane();
        root.getStyleClass().add("page-root");
        root.setTop(buildHeader());

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
            f.setToValue(1); f.play();
        });
    }

    public Node getView() { return root; }

    // ═══════════════════════════════════════════════════════
    //                      HEADER
    // ═══════════════════════════════════════════════════════

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

        Label title = new Label("Commande");
        title.getStyleClass().add("page-title");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button ordersBtn = new Button("Mes commandes");
        ordersBtn.getStyleClass().add("page-back-btn");
        ordersBtn.setOnAction(e -> NavigationManager.showOrders());

        inner.getChildren().addAll(back, title, sp, ordersBtn);
        w.getChildren().add(inner);
        return w;
    }

    // ═══════════════════════════════════════════════════════
    //                     STEPPER
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
    //                STEP 0 — CART VIEW
    // ═══════════════════════════════════════════════════════

    private void showCartStep() {
        currentStep = 0;
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

        // Floating cart icon in background
        Label floatingIcon = new Label("\uD83D\uDED2");
        floatingIcon.getStyleClass().add("floating-cart-icon");
        floatingIcon.setMouseTransparent(true);

        HBox content = new HBox(24);
        content.setAlignment(Pos.TOP_CENTER);

        // Left: items
        VBox itemsCol = new VBox(12);
        HBox.setHgrow(itemsCol, Priority.ALWAYS);

        Label itemsTitle = new Label(cartItemCount + " article(s)");
        itemsTitle.getStyleClass().add("page-subtitle");
        itemsCol.getChildren().add(itemsTitle);

        for (Map<String, Object> item : cartItems) {
            itemsCol.getChildren().add(buildCartItem(item));
        }

        // Right: summary
        VBox summaryCol = new VBox(16);
        summaryCol.setMinWidth(300);
        summaryCol.setMaxWidth(300);
        summaryCol.getChildren().add(buildSummaryCard(true));

        content.getChildren().addAll(itemsCol, summaryCol);

        StackPane contentStack = new StackPane(floatingIcon, content);
        contentStack.setAlignment(Pos.TOP_CENTER);
        StackPane.setAlignment(floatingIcon, Pos.CENTER);

        page.getChildren().add(contentStack);
        setContent(page);
    }

    private Node buildCartItem(Map<String, Object> item) {
        HBox card = new HBox(16);
        card.getStyleClass().add("cart-item-card");
        card.setAlignment(Pos.CENTER_LEFT);

        String name = str(item.get("productName"));
        double unitPrice = dbl(item.get("unitPrice"));
        int qty = intV(item.get("quantity"));
        double subtotal = dbl(item.get("subtotal"));
        int pid = intV(item.get("productId"));

        // Image
        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("cart-item-image");
        Label letter = new Label(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        letter.getStyleClass().add("cart-item-image-letter");
        imgBox.getChildren().add(letter);

        // Info
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameL = new Label(name);
        nameL.getStyleClass().add("cart-item-name");
        Label priceL = new Label(String.format("%.2f \u20AC/u", unitPrice));
        priceL.getStyleClass().add("cart-item-unit");
        Button removeBtn = new Button("Retirer");
        removeBtn.getStyleClass().add("cart-remove-btn");
        removeBtn.setOnAction(e -> removeItem(pid));
        info.getChildren().addAll(nameL, priceL, removeBtn);

        // Qty
        Label qtyL = new Label("x" + qty);
        qtyL.getStyleClass().add("qty-label");

        // Subtotal
        Label subL = new Label(String.format("%.2f \u20AC", subtotal));
        subL.getStyleClass().add("cart-item-subtotal");
        subL.setMinWidth(90);

        card.getChildren().addAll(imgBox, info, qtyL, subL);
        return card;
    }

    private VBox buildSummaryCard(boolean showCheckout) {
        VBox card = new VBox(14);
        card.getStyleClass().add("cart-summary-card");

        Label title = new Label("Récapitulatif");
        title.getStyleClass().add("cart-summary-title");

        double shipping = cartTotal >= 50 ? 0 : 4.99;
        double total = cartTotal + shipping;

        card.getChildren().addAll(
                title,
                summaryRow("Articles (" + cartItemCount + ")", String.format("%.2f \u20AC", cartTotal)),
                summaryRow("Livraison", shipping == 0 ? "Gratuite" : String.format("%.2f \u20AC", shipping))
        );

        if (shipping == 0) {
            Label free = new Label("Livraison offerte !");
            free.setStyle("-fx-font-size:11px;-fx-text-fill:#27AE60;-fx-font-weight:bold;");
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
        Label tv = new Label(String.format("%.2f \u20AC", total));
        tv.getStyleClass().add("cart-summary-total-value");
        totalRow.getChildren().addAll(tl, sp, tv);
        card.getChildren().add(totalRow);

        if (showCheckout) {
            Button checkBtn = new Button("Continuer");
            checkBtn.getStyleClass().add("cart-checkout-btn");
            checkBtn.setOnAction(e -> showShippingStep());
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
    //             STEP 1 — SHIPPING ADDRESS
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

        // Left: form
        VBox form = new VBox(16);
        HBox.setHgrow(form, Priority.ALWAYS);

        VBox addrCard = new VBox(14);
        addrCard.getStyleClass().add("checkout-card");

        Label addrTitle = new Label("Adresse de livraison");
        addrTitle.getStyleClass().add("checkout-section-title");

        TextField addrField = new TextField(shippingAddress);
        addrField.setPromptText("Entrez votre adresse complète");
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

        TextField countryField = new TextField("France");
        countryField.getStyleClass().add("auth-field");
        countryField.setStyle("-fx-padding:12 14;");
        HBox.setHgrow(countryField, Priority.ALWAYS);
        zipCountry.getChildren().addAll(zipField, countryField);

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button backBtn = new Button("Retour");
        backBtn.getStyleClass().add("cart-continue-btn");
        backBtn.setStyle("-fx-max-width:120;");
        backBtn.setOnAction(e -> showCartStep());

        Button nextBtn = new Button("Continuer");
        nextBtn.getStyleClass().add("cart-checkout-btn");
        nextBtn.setStyle("-fx-max-width:180;");
        nextBtn.setOnAction(e -> {
            String addr = addrField.getText().trim();
            String city = cityField.getText().trim();
            String zip = zipField.getText().trim();
            if (addr.isEmpty()) addr = "Adresse par défaut";
            shippingAddress = addr + (city.isEmpty() ? "" : ", " + city)
                    + (zip.isEmpty() ? "" : " " + zip);
            showPaymentStep();
        });
        btns.getChildren().addAll(backBtn, nextBtn);

        addrCard.getChildren().addAll(addrTitle, addrField, cityField, zipCountry, btns);
        form.getChildren().add(addrCard);

        // Right: summary
        VBox summaryCol = new VBox(16);
        summaryCol.setMinWidth(300);
        summaryCol.setMaxWidth(300);
        summaryCol.getChildren().add(buildSummaryCard(false));

        content.getChildren().addAll(form, summaryCol);
        page.getChildren().add(content);
        setContent(page);
    }

    // ═══════════════════════════════════════════════════════
    //              STEP 2 — PAYMENT
    // ═══════════════════════════════════════════════════════

    private void showPaymentStep() {
        currentStep = 2;
        VBox page = new VBox(20);
        page.setMaxWidth(MAX_W);
        page.setPadding(new Insets(20, H_PAD, 40, H_PAD));
        page.setAlignment(Pos.TOP_CENTER);

        page.getChildren().add(buildStepper());

        HBox content = new HBox(24);
        content.setAlignment(Pos.TOP_CENTER);

        // Left: payment form
        VBox form = new VBox(16);
        HBox.setHgrow(form, Priority.ALWAYS);

        VBox payCard = new VBox(16);
        payCard.getStyleClass().add("checkout-card");

        Label payTitle = new Label("Méthode de paiement");
        payTitle.getStyleClass().add("checkout-section-title");

        // Method buttons
        VBox methods = new VBox(10);
        String[][] methodList = {
                {"CARTE_BANCAIRE", "Carte bancaire"},
                {"PAIEMENT_FICTIF", "Paiement fictif (test)"},
                {"VIREMENT", "Virement bancaire"}
        };

        List<Button> methodBtns = new ArrayList<>();
        for (String[] m : methodList) {
            Button btn = new Button(m[1]);
            btn.getStyleClass().add("checkout-method-btn");
            btn.setMaxWidth(Double.MAX_VALUE);
            if (m[0].equals(paymentMethod)) {
                btn.getStyleClass().add("checkout-method-active");
            }
            btn.setOnAction(e -> {
                paymentMethod = m[0];
                methodBtns.forEach(b -> b.getStyleClass().remove("checkout-method-active"));
                btn.getStyleClass().add("checkout-method-active");
            });
            methodBtns.add(btn);
            methods.getChildren().add(btn);
        }

        // Card fields (simulated)
        VBox cardFields = new VBox(10);
        Label cardTitle = new Label("Informations de carte (simulation)");
        cardTitle.getStyleClass().add("page-subtitle");

        TextField cardNum = new TextField();
        cardNum.setPromptText("1234 5678 9012 3456");
        cardNum.getStyleClass().add("auth-field");
        cardNum.setStyle("-fx-padding:12 14;");

        HBox cardRow = new HBox(12);
        TextField expiry = new TextField();
        expiry.setPromptText("MM/AA");
        expiry.getStyleClass().add("auth-field");
        expiry.setStyle("-fx-padding:12 14;");
        HBox.setHgrow(expiry, Priority.ALWAYS);

        TextField cvv = new TextField();
        cvv.setPromptText("CVV");
        cvv.getStyleClass().add("auth-field");
        cvv.setStyle("-fx-padding:12 14;");
        HBox.setHgrow(cvv, Priority.ALWAYS);
        cardRow.getChildren().addAll(expiry, cvv);

        cardFields.getChildren().addAll(cardTitle, cardNum, cardRow);

        // Buttons
        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button backBtn = new Button("Retour");
        backBtn.getStyleClass().add("cart-continue-btn");
        backBtn.setStyle("-fx-max-width:120;");
        backBtn.setOnAction(e -> showShippingStep());

        Button payBtn = new Button("Payer");
        payBtn.getStyleClass().add("cart-checkout-btn");
        payBtn.setStyle("-fx-max-width:180;");
        payBtn.setOnAction(e -> {
            String cn = cardNum.getText().trim();
            if (cn.length() >= 4) {
                cardLastFour = cn.substring(cn.length() - 4);
            }
            doPlaceOrder();
        });
        btns.getChildren().addAll(backBtn, payBtn);

        payCard.getChildren().addAll(payTitle, methods, cardFields, btns);
        form.getChildren().add(payCard);

        // Right: summary + shipping info
        VBox summaryCol = new VBox(16);
        summaryCol.setMinWidth(300);
        summaryCol.setMaxWidth(300);
        summaryCol.getChildren().add(buildSummaryCard(false));

        // Shipping recap
        VBox shipRecap = new VBox(8);
        shipRecap.getStyleClass().add("checkout-card");
        Label shipTitle = new Label("Livraison");
        shipTitle.getStyleClass().add("checkout-section-title");
        Label shipAddr = new Label(shippingAddress.isEmpty() ? "Adresse par défaut" : shippingAddress);
        shipAddr.getStyleClass().add("page-subtitle");
        shipAddr.setWrapText(true);
        shipRecap.getChildren().addAll(shipTitle, shipAddr);
        summaryCol.getChildren().add(shipRecap);

        content.getChildren().addAll(form, summaryCol);
        page.getChildren().add(content);
        setContent(page);
    }

    // ═══════════════════════════════════════════════════════
    //           STEP 3 — CONFIRMATION + CELEBRATION
    // ═══════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void showConfirmation(Map<String, Object> result) {
        currentStep = 3;
        VBox page = new VBox(20);
        page.setMaxWidth(MAX_W);
        page.setPadding(new Insets(20, H_PAD, 40, H_PAD));
        page.setAlignment(Pos.TOP_CENTER);

        page.getChildren().add(buildStepper());

        // Celebration
        VBox celebration = new VBox(16);
        celebration.setAlignment(Pos.CENTER);
        celebration.setPadding(new Insets(20, 0, 20, 0));

        Label icon = new Label("\uD83C\uDF89");
        icon.getStyleClass().add("celebration-icon");

        Label title = new Label("Commande confirmée !");
        title.getStyleClass().add("celebration-title");

        Label sub = new Label("Merci pour votre achat. Votre commande est en cours de traitement.");
        sub.getStyleClass().add("celebration-subtitle");

        celebration.getChildren().addAll(icon, title, sub);

        // Receipt
        VBox receipt = new VBox(14);
        receipt.getStyleClass().add("receipt-card");
        receipt.setMaxWidth(600);

        Label recTitle = new Label("Récapitulatif de commande");
        recTitle.getStyleClass().add("receipt-title");

        String orderId = str(result.get("orderId"));
        String payStatus = str(result.get("paymentStatus"));
        String orderStatus = str(result.get("orderStatus"));
        double total = dbl(result.get("totalAmount"));
        String txnRef = str(result.get("transactionRef"));

        receipt.getChildren().addAll(
                recTitle,
                receiptRow("N° Commande", orderId),
                receiptRow("Statut", orderStatus),
                receiptRow("Montant total", String.format("%.2f \u20AC", total)),
                receiptDivider(),
                receiptRow("Paiement", payStatus),
                receiptRow("Méthode", paymentMethod.replace("_", " ")),
                receiptRow("Référence", txnRef.isEmpty() ? "—" : txnRef),
                receiptDivider(),
                receiptRow("Adresse", shippingAddress.isEmpty() ? "Par défaut" : shippingAddress)
        );

        // Status indicator
        Label statusInd = new Label(
                "ACCEPTE".equals(payStatus) ? "\u2705 Paiement accepté" : "\u26A0 En attente de paiement");
        statusInd.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:"
                + ("ACCEPTE".equals(payStatus) ? "#27AE60" : "#F39C12") + ";");
        receipt.getChildren().add(statusInd);

        // Buttons
        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER);
        btns.setPadding(new Insets(10, 0, 0, 0));

        Button viewOrders = new Button("Voir mes commandes");
        viewOrders.getStyleClass().add("btn-primary");
        viewOrders.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        viewOrders.setOnAction(e -> NavigationManager.showOrders());

        Button shopBtn = new Button("Retour à la boutique");
        shopBtn.getStyleClass().add("btn-secondary");
        shopBtn.setStyle("-fx-font-size:13px;-fx-padding:10 24;");
        shopBtn.setOnAction(e -> NavigationManager.showLanding());

        btns.getChildren().addAll(viewOrders, shopBtn);

        page.getChildren().addAll(celebration, receipt, btns);
        setContent(page);

        // Confetti animation
        Platform.runLater(() -> launchConfetti());
    }

    private HBox receiptRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.getStyleClass().add("receipt-label");
        l.setMinWidth(140);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value != null ? value : "—");
        v.getStyleClass().add("receipt-value");
        row.getChildren().addAll(l, sp, v);
        return row;
    }

    private Region receiptDivider() {
        Region d = new Region();
        d.getStyleClass().add("receipt-divider");
        return d;
    }

    // ═══════════════════════════════════════════════════════
    //                    CONFETTI
    // ═══════════════════════════════════════════════════════

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

        // Remove layer after 4s
        PauseTransition cleanup = new PauseTransition(Duration.seconds(4));
        cleanup.setOnFinished(e -> rootStack.getChildren().remove(confettiLayer));
        cleanup.play();
    }

    // ═══════════════════════════════════════════════════════
    //                  PLACE ORDER (DB)
    // ═══════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void doPlaceOrder() {
        showLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", NavigationManager.getUserId());
        data.put("shippingAddress", shippingAddress.isEmpty() ? "Adresse par défaut" : shippingAddress);
        data.put("paymentMethod", paymentMethod);
        if (!cardLastFour.isEmpty()) data.put("cardLastFour", cardLastFour);

        ServerConnection.getInstance()
                .sendAsync(new Request(Action.PLACE_ORDER,
                        NavigationManager.getToken(), data))
                .thenAccept(res -> Platform.runLater(() -> {
                    showLoading(false);
                    if (res.isSuccess() && res.getData() != null) {
                        showConfirmation((Map<String, Object>) res.getData());
                    } else {
                        // Show error on payment step
                        showPaymentStep();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showLoading(false));
                    return null;
                });
    }

    // ═══════════════════════════════════════════════════════
    //                    LOAD CART (DB)
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
                        Map<String, Object> data = (Map<String, Object>) res.getData();
                        cartItems = data.get("items") != null
                                ? (List<Map<String, Object>>) data.get("items")
                                : new ArrayList<>();
                        cartTotal = dbl(data.get("total"));
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

    // ═══════════════════════════════════════════════════════
    //                    UI HELPERS
    // ═══════════════════════════════════════════════════════

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
        Label sub = new Label("Parcourez notre boutique pour ajouter des produits");
        sub.getStyleClass().add("empty-state-text");
        Button shop = new Button("Explorer");
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
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }
    private double dbl(Object o) {
        if (o == null) return 0;
        if (o instanceof Double) return (Double) o;
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }
}