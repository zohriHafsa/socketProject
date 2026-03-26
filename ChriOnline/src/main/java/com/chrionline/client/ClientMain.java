package com.chrionline.client;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;

public class ClientMain {

    // ═══════════════════════ ANSI COLORS ═══════════════════════
    static final String R    = "\033[0m";
    static final String B    = "\033[1m";
    static final String DIM  = "\033[2m";
    static final String RED  = "\033[31m";
    static final String GRN  = "\033[32m";
    static final String YLW  = "\033[33m";
    static final String BLU  = "\033[34m";
    static final String MAG  = "\033[35m";
    static final String CYN  = "\033[36m";
    static final String WHT  = "\033[37m";
    static final String BLK  = "\033[30m";
    static final String BG_G = "\033[42m";
    static final String BG_R = "\033[41m";

    // ═══════════════════════ STATE ═════════════════════════════
    static final Scanner sc     = new Scanner(System.in);
    static ServerConnection     conn;
    static String  token        = null;
    static String  logUser      = null;
    static int     logId        = -1;
    static String  logRole      = null;
    static final Gson pretty    = new GsonBuilder().setPrettyPrinting().create();

    // ═══════════════════════ MAIN ══════════════════════════════

    public static void main(String[] args) {

        banner();

        conn = ServerConnection.getInstance();
        p(CYN + "\n  ⏳ Connexion au serveur..." + R);

        if (!conn.connect()) {
            err("Impossible de se connecter ! Lancez ServerMain d'abord.");
            return;
        }

        ok("Connecté à " + conn.getHost() + ":" + conn.getPort());
        pause();

        boolean run = true;
        while (run) {
            cls();
            statusBar();
            mainMenu();
            switch (readInt(CYN + "  ➤ " + R)) {
                case 1:  menuAuth();    break;
                case 2:  menuProd();    break;
                case 3:  menuCart();    break;
                case 4:  menuOrder();   break;
                case 5:  menuPay();     break;
                case 9:  quickRef();    break;
                case 0:  run = false;   break;
                default: err("Choix invalide !"); pause();
            }
        }

        // ─── Déconnexion propre ────────────────────────────
        if (token != null) {
            send(new Request(Action.LOGOUT, token, null));
        }
        conn.disconnect();

        p("");
        p(MAG + B + "  ╔═════════════════════════════════════════════════╗" + R);
        p(MAG + B + "  ║    👋  Merci d'avoir utilisé ChriOnline !       ║" + R);
        p(MAG + B + "  ╚═════════════════════════════════════════════════╝" + R);
        p("");
    }

    // ══════════════════════════════════════════════════════════
    //                      MAIN MENU
    // ══════════════════════════════════════════════════════════

    static void mainMenu() {
        p("");
        p(CYN+B+"  ╔═════════════════════════════════════════════════╗"+R);
        p(CYN+B+"  ║"+WHT+B+"            🏠  MENU PRINCIPAL                  "+CYN+B+"║"+R);
        p(CYN+B+"  ╠═════════════════════════════════════════════════╣"+R);
        p(CYN+B+"  ║                                                 ║"+R);
        p(CYN+B+"  ║"+GRN+B+"    [1]"+WHT+"  🔐  Authentification                 "+CYN+B+"║"+R);
        p(CYN+B+"  ║"+GRN+B+"    [2]"+WHT+"  📦  Produits                         "+CYN+B+"║"+R);
        p(CYN+B+"  ║"+GRN+B+"    [3]"+WHT+"  🛒  Panier                           "+CYN+B+"║"+R);
        p(CYN+B+"  ║"+GRN+B+"    [4]"+WHT+"  📋  Commandes                        "+CYN+B+"║"+R);
        p(CYN+B+"  ║"+GRN+B+"    [5]"+WHT+"  💳  Paiement                         "+CYN+B+"║"+R);
        p(CYN+B+"  ║                                                 ║"+R);
        p(CYN+B+"  ║"+YLW+B+"    [9]"+WHT+"  📖  Référence rapide (IDs DB)        "+CYN+B+"║"+R);
        p(CYN+B+"  ║"+RED+B+"    [0]"+WHT+"  🚪  Quitter                          "+CYN+B+"║"+R);
        p(CYN+B+"  ║                                                 ║"+R);
        p(CYN+B+"  ╚═════════════════════════════════════════════════╝"+R);
        p("");
    }

    // ══════════════════════════════════════════════════════════
    //                   1. AUTHENTIFICATION
    // ══════════════════════════════════════════════════════════

    static void menuAuth() {
        boolean back = false;
        while (!back) {
            cls();
            statusBar();
            p("");
            p(YLW+B+"  ╔═════════════════════════════════════════════════╗"+R);
            p(YLW+B+"  ║"+WHT+B+"            🔐  AUTHENTIFICATION                 "+YLW+B+"║"+R);
            p(YLW+B+"  ╠═════════════════════════════════════════════════╣"+R);
            p(YLW+B+"  ║                                                 ║"+R);
            p(YLW+B+"  ║"+GRN+B+"    [1]"+WHT+"  📝  Inscription (Register)          "+YLW+B+"║"+R);
            p(YLW+B+"  ║"+GRN+B+"    [2]"+WHT+"  🔑  Connexion  (Login)              "+YLW+B+"║"+R);
            p(YLW+B+"  ║"+GRN+B+"    [3]"+WHT+"  🚪  Déconnexion (Logout)            "+YLW+B+"║"+R);
            p(YLW+B+"  ║                                                 ║"+R);
            p(YLW+B+"  ║"+RED+B+"    [0]"+WHT+"  ↩   Retour                           "+YLW+B+"║"+R);
            p(YLW+B+"  ║                                                 ║"+R);
            p(YLW+B+"  ╚═════════════════════════════════════════════════╝"+R);
            p(DIM+"  ⚠ Les users seed (jdupont, mleroy...) ont un hash SHA-256"+R);
            p(DIM+"    → Créez un nouveau compte via Register pour tester Login"+R);
            p("");

            switch (readInt(YLW + "  ➤ " + R)) {
                case 1: doRegister(); break;
                case 2: doLogin();    break;
                case 3: doLogout();   break;
                case 0: back = true;  break;
                default: err("Choix invalide !"); pause();
            }
        }
    }

    static void doRegister() {
        subTitle("📝 INSCRIPTION");
        String username  = readStr("  Nom d'utilisateur");
        String email     = readStr("  Email");
        String password  = readStr("  Mot de passe");
        String firstName = readStr("  Prénom");
        String lastName  = readStr("  Nom de famille");
        String address   = readStr("  Adresse");
        String phone     = readStr("  Téléphone");

        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("email", email);
        data.put("password", password);
        data.put("firstName", firstName);
        data.put("lastName", lastName);
        data.put("address", address);
        data.put("phone", phone);

        Response r = send(new Request(Action.REGISTER, null, data));
        showResponse(r);
        pause();
    }

    @SuppressWarnings("unchecked")
    static void doLogin() {
        subTitle("🔑 CONNEXION");
        String username = readStr("  Nom d'utilisateur");
        String password = readStr("  Mot de passe");

        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);

        Response r = send(new Request(Action.LOGIN, null, data));
        showResponse(r);

        if (r.isSuccess() && r.getData() != null) {
            Map<String, Object> rd = (Map<String, Object>) r.getData();
            token   = str(rd.get("token"));
            logUser = str(rd.get("username"));
            logRole = str(rd.get("role"));
            logId   = toInt(rd.get("userId"));
            p("");
            ok("Session active !");
            p(GRN + "    Utilisateur : " + WHT + B + logUser + R);
            p(GRN + "    Rôle        : " + WHT + B + logRole + R);
            p(GRN + "    User ID     : " + WHT + B + logId + R);
            p(GRN + "    Token       : " + DIM + token.substring(0, 8) + "..." + R);
        }
        pause();
    }

    static void doLogout() {
        if (token == null) {
            err("Vous n'êtes pas connecté !");
            pause();
            return;
        }
        subTitle("🚪 DÉCONNEXION");
        Response r = send(new Request(Action.LOGOUT, token, null));
        showResponse(r);
        if (r.isSuccess()) {
            token = null;
            logUser = null;
            logId = -1;
            logRole = null;
        }
        pause();
    }

    // ══════════════════════════════════════════════════════════
    //                      2. PRODUITS
    // ══════════════════════════════════════════════════════════

    static void menuProd() {
        boolean back = false;
        while (!back) {
            cls();
            statusBar();
            p("");
            p(BLU+B+"  ╔═════════════════════════════════════════════════╗"+R);
            p(BLU+B+"  ║"+WHT+B+"              📦  PRODUITS                       "+BLU+B+"║"+R);
            p(BLU+B+"  ╠═════════════════════════════════════════════════╣"+R);
            p(BLU+B+"  ║                                                 ║"+R);
            p(BLU+B+"  ║"+GRN+B+"    [1]"+WHT+"  📋  Voir tous les produits          "+BLU+B+"║"+R);
            p(BLU+B+"  ║"+GRN+B+"    [2]"+WHT+"  🔍  Détail d'un produit (par ID)    "+BLU+B+"║"+R);
            p(BLU+B+"  ║"+GRN+B+"    [3]"+WHT+"  🔎  Rechercher (mot-clé)            "+BLU+B+"║"+R);
            p(BLU+B+"  ║                                                 ║"+R);
            p(BLU+B+"  ║"+RED+B+"    [0]"+WHT+"  ↩   Retour                           "+BLU+B+"║"+R);
            p(BLU+B+"  ║                                                 ║"+R);
            p(BLU+B+"  ╚═════════════════════════════════════════════════╝"+R);
            p("");

            switch (readInt(BLU + "  ➤ " + R)) {
                case 1: doListProducts();   break;
                case 2: doProductDetail();  break;
                case 3: doSearchProducts(); break;
                case 0: back = true;        break;
                default: err("Choix invalide !"); pause();
            }
        }
    }

    @SuppressWarnings("unchecked")
    static void doListProducts() {
        subTitle("📋 TOUS LES PRODUITS");
        Response r = send(new Request(Action.GET_PRODUCTS, token, null));
        if (r.isSuccess() && r.getData() != null) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) r.getData();
            printProductTable(list);
        } else {
            showResponse(r);
        }
        pause();
    }

    @SuppressWarnings("unchecked")
    static void doProductDetail() {
        subTitle("🔍 DÉTAIL PRODUIT");
        int id = readInt("  ID du produit : ");
        if (id <= 0) { err("ID invalide !"); pause(); return; }

        Response r = send(new Request(Action.GET_PRODUCT_DETAIL, token,
                Map.of("productId", id)));
        if (r.isSuccess() && r.getData() != null) {
            printProductDetail((Map<String, Object>) r.getData());
        } else {
            showResponse(r);
        }
        pause();
    }

    @SuppressWarnings("unchecked")
    static void doSearchProducts() {
        subTitle("🔎 RECHERCHE PRODUIT");
        String keyword = readStr("  Mot-clé de recherche");
        if (keyword.isEmpty()) { err("Mot-clé vide !"); pause(); return; }

        Response r = send(new Request(Action.SEARCH_PRODUCTS, token,
                Map.of("keyword", keyword)));
        if (r.isSuccess() && r.getData() != null) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) r.getData();
            if (list.isEmpty()) {
                info("Aucun résultat pour « " + keyword + " »");
            } else {
                p(GRN + "  " + list.size() + " résultat(s) pour « " + keyword + " »" + R);
                printProductTable(list);
            }
        } else {
            showResponse(r);
        }
        pause();
    }

    // ══════════════════════════════════════════════════════════
    //                       3. PANIER
    // ══════════════════════════════════════════════════════════

    static void menuCart() {
        boolean back = false;
        while (!back) {
            cls();
            statusBar();
            p("");
            p(GRN+B+"  ╔═════════════════════════════════════════════════╗"+R);
            p(GRN+B+"  ║"+WHT+B+"                🛒  PANIER                       "+GRN+B+"║"+R);
            p(GRN+B+"  ╠═════════════════════════════════════════════════╣"+R);
            p(GRN+B+"  ║                                                 ║"+R);
            p(GRN+B+"  ║"+CYN+B+"    [1]"+WHT+"  👁   Voir mon panier                "+GRN+B+"║"+R);
            p(GRN+B+"  ║"+CYN+B+"    [2]"+WHT+"  ➕  Ajouter un produit              "+GRN+B+"║"+R);
            p(GRN+B+"  ║"+CYN+B+"    [3]"+WHT+"  ➖  Retirer un produit              "+GRN+B+"║"+R);
            p(GRN+B+"  ║"+CYN+B+"    [4]"+WHT+"  🗑   Vider le panier                "+GRN+B+"║"+R);
            p(GRN+B+"  ║                                                 ║"+R);
            p(GRN+B+"  ║"+RED+B+"    [0]"+WHT+"  ↩   Retour                           "+GRN+B+"║"+R);
            p(GRN+B+"  ║                                                 ║"+R);
            p(GRN+B+"  ╚═════════════════════════════════════════════════╝"+R);
            p("");

            switch (readInt(GRN + "  ➤ " + R)) {
                case 1: doGetCart();    break;
                case 2: doAddToCart();  break;
                case 3: doRemoveCart(); break;
                case 4: doClearCart();  break;
                case 0: back = true;   break;
                default: err("Choix invalide !"); pause();
            }
        }
    }

    @SuppressWarnings("unchecked")
    static void doGetCart() {
        subTitle("👁  MON PANIER");
        int uid = getActiveUserId();
        if (uid < 0) { pause(); return; }

        Response r = send(new Request(Action.GET_CART, token,
                Map.of("userId", uid)));
        if (r.isSuccess() && r.getData() != null) {
            printCart((Map<String, Object>) r.getData());
        } else {
            showResponse(r);
        }
        pause();
    }

    static void doAddToCart() {
        subTitle("➕ AJOUTER AU PANIER");
        int uid = getActiveUserId();
        if (uid < 0) { pause(); return; }

        int productId = readInt("  ID du produit : ");
        int quantity  = readInt("  Quantité      : ");
        if (productId <= 0) { err("ID produit invalide !"); pause(); return; }
        if (quantity <= 0) quantity = 1;

        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("productId", productId);
        data.put("quantity", quantity);

        Response r = send(new Request(Action.ADD_TO_CART, token, data));
        showResponse(r);
        pause();
    }

    static void doRemoveCart() {
        subTitle("➖ RETIRER DU PANIER");
        int uid = getActiveUserId();
        if (uid < 0) { pause(); return; }

        int productId = readInt("  ID produit à retirer : ");
        if (productId <= 0) { err("ID invalide !"); pause(); return; }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("productId", productId);

        Response r = send(new Request(Action.REMOVE_FROM_CART, token, data));
        showResponse(r);
        pause();
    }

    static void doClearCart() {
        subTitle("🗑  VIDER LE PANIER");
        int uid = getActiveUserId();
        if (uid < 0) { pause(); return; }

        Response r = send(new Request(Action.CLEAR_CART, token,
                Map.of("userId", uid)));
        showResponse(r);
        pause();
    }

    // ══════════════════════════════════════════════════════════
    //                     4. COMMANDES
    // ══════════════════════════════════════════════════════════

    static void menuOrder() {
        boolean back = false;
        while (!back) {
            cls();
            statusBar();
            p("");
            p(MAG+B+"  ╔═════════════════════════════════════════════════╗"+R);
            p(MAG+B+"  ║"+WHT+B+"              📋  COMMANDES                      "+MAG+B+"║"+R);
            p(MAG+B+"  ╠═════════════════════════════════════════════════╣"+R);
            p(MAG+B+"  ║                                                 ║"+R);
            p(MAG+B+"  ║"+CYN+B+"    [1]"+WHT+"  🛍   Passer une commande             "+MAG+B+"║"+R);
            p(MAG+B+"  ║"+CYN+B+"    [2]"+WHT+"  📜  Historique des commandes         "+MAG+B+"║"+R);
            p(MAG+B+"  ║"+CYN+B+"    [3]"+WHT+"  🔍  Statut d'une commande            "+MAG+B+"║"+R);
            p(MAG+B+"  ║                                                 ║"+R);
            p(MAG+B+"  ║"+RED+B+"    [0]"+WHT+"  ↩   Retour                           "+MAG+B+"║"+R);
            p(MAG+B+"  ║                                                 ║"+R);
            p(MAG+B+"  ╚═════════════════════════════════════════════════╝"+R);
            p("");

            switch (readInt(MAG + "  ➤ " + R)) {
                case 1: doPlaceOrder();   break;
                case 2: doOrderHistory(); break;
                case 3: doOrderStatus();  break;
                case 0: back = true;      break;
                default: err("Choix invalide !"); pause();
            }
        }
    }

    @SuppressWarnings("unchecked")
    static void doPlaceOrder() {
        subTitle("🛍  PASSER UNE COMMANDE");
        int uid = getActiveUserId();
        if (uid < 0) { pause(); return; }

        String address = readStr("  Adresse de livraison");
        p(DIM + "    Méthodes : CARTE_BANCAIRE / PAIEMENT_FICTIF / VIREMENT" + R);
        String method = readStr("  Méthode de paiement");
        String card   = readStr("  4 derniers chiffres carte (vide si aucun)");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("shippingAddress", address);
        data.put("paymentMethod", method);
        if (!card.isEmpty()) data.put("cardLastFour", card);

        Response r = send(new Request(Action.PLACE_ORDER, token, data));
        if (r.isSuccess() && r.getData() != null) {
            printOrderConfirmation((Map<String, Object>) r.getData());
        } else {
            showResponse(r);
        }
        pause();
    }

    @SuppressWarnings("unchecked")
    static void doOrderHistory() {
        subTitle("📜 HISTORIQUE DES COMMANDES");
        int uid = getActiveUserId();
        if (uid < 0) { pause(); return; }

        Response r = send(new Request(Action.GET_ORDER_HISTORY, token,
                Map.of("userId", uid)));
        if (r.isSuccess() && r.getData() != null) {
            List<Map<String, Object>> orders = (List<Map<String, Object>>) r.getData();
            if (orders.isEmpty()) {
                info("Aucune commande pour cet utilisateur.");
            } else {
                printOrderTable(orders);
            }
        } else {
            showResponse(r);
        }
        pause();
    }

    @SuppressWarnings("unchecked")
    static void doOrderStatus() {
        subTitle("🔍 STATUT D'UNE COMMANDE");
        String orderId = readStr("  ID commande (ex: ORD-0001-2026)");
        if (orderId.isEmpty()) { err("ID vide !"); pause(); return; }

        Response r = send(new Request(Action.GET_ORDER_STATUS, token,
                Map.of("orderId", orderId)));
        if (r.isSuccess() && r.getData() != null) {
            printOrderDetail((Map<String, Object>) r.getData());
        } else {
            showResponse(r);
        }
        pause();
    }

    // ══════════════════════════════════════════════════════════
    //                      5. PAIEMENT
    // ══════════════════════════════════════════════════════════

    static void menuPay() {
        boolean back = false;
        while (!back) {
            cls();
            statusBar();
            p("");
            p(RED+B+"  ╔═════════════════════════════════════════════════╗"+R);
            p(RED+B+"  ║"+WHT+B+"               💳  PAIEMENT                      "+RED+B+"║"+R);
            p(RED+B+"  ╠═════════════════════════════════════════════════╣"+R);
            p(RED+B+"  ║                                                 ║"+R);
            p(RED+B+"  ║"+CYN+B+"    [1]"+WHT+"  💰  Traiter / retenter un paiement  "+RED+B+"║"+R);
            p(RED+B+"  ║                                                 ║"+R);
            p(RED+B+"  ║"+RED+B+"    [0]"+WHT+"  ↩   Retour                           "+RED+B+"║"+R);
            p(RED+B+"  ║                                                 ║"+R);
            p(RED+B+"  ╚═════════════════════════════════════════════════╝"+R);
            p("");

            switch (readInt(RED + "  ➤ " + R)) {
                case 1: doProcessPayment(); break;
                case 0: back = true;        break;
                default: err("Choix invalide !"); pause();
            }
        }
    }

    static void doProcessPayment() {
        subTitle("💰 TRAITER UN PAIEMENT");
        String orderId = readStr("  ID commande");
        p(DIM + "    Méthodes : CARTE_BANCAIRE / PAIEMENT_FICTIF / VIREMENT" + R);
        String method = readStr("  Méthode de paiement");
        String card   = readStr("  4 derniers chiffres (vide si aucun)");

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("paymentMethod", method);
        if (!card.isEmpty()) data.put("cardLastFour", card);

        Response r = send(new Request(Action.PROCESS_PAYMENT, token, data));
        showResponse(r);
        pause();
    }

    // ══════════════════════════════════════════════════════════
    //                   9. RÉFÉRENCE RAPIDE
    // ══════════════════════════════════════════════════════════

    static void quickRef() {
        cls();
        p("");
        p(YLW+B+"  ╔═══════════════════════════════════════════════════════════════════╗"+R);
        p(YLW+B+"  ║"+WHT+B+"                   📖  RÉFÉRENCE RAPIDE (BASE DE DONNÉES)          "+YLW+B+"║"+R);
        p(YLW+B+"  ╠═══════════════════════════════════════════════════════════════════╣"+R);
        p(YLW+B+"  ║                                                                   ║"+R);
        p(YLW+B+"  ║"+CYN+B+"  👤 UTILISATEURS :                                                "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=1  admin    (ADMIN)   admin@chrionline.com                   "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=2  jdupont  (CLIENT)  jean.dupont@mail.com                   "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=3  mleroy   (CLIENT)  marie.leroy@mail.com                   "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=4  cblanc   (CLIENT)  claire.blanc@mail.com                  "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=5  tnoir    (CLIENT)  thomas.noir@mail.com                   "+YLW+B+"║"+R);
        p(YLW+B+"  ║                                                                   ║"+R);
        p(YLW+B+"  ║"+CYN+B+"  📦 PRODUITS :                                                    "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=1   iPhone 15 Pro         1199.99€  (Smartphones)            "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=2   Samsung Galaxy S24      899.99€  (Smartphones)            "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=3   Google Pixel 8          699.99€  (Smartphones)            "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=4   MacBook Pro M3         2199.99€  (Laptops)                "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=5   Dell XPS 15            1749.99€  (Laptops)                "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=6   Lenovo ThinkPad X1     1299.99€  (Laptops)                "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=7   T-Shirt Coton Bio        24.99€  (T-Shirts)               "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=8   T-Shirt Nike Dri-FIT     34.99€  (T-Shirts)               "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=9   Jean Slim Noir           59.99€  (Pantalons)              "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=10  Chino Beige              49.99€  (Pantalons)              "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=11  Clean Code               35.00€  (Livres Tech)            "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=12  Design Patterns          42.00€  (Livres Tech)            "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=13  Java Complete Ref        55.00€  (Livres Tech)            "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=14  Le Petit Prince          12.99€  (Romans)                 "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=15  1984                     14.99€  (Romans)                 "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=16  Vélo de Route Carbon   1299.00€  (Sports)                 "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=17  Tapis de Yoga            29.90€  (Sports)                 "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ID=18  Raquette de Tennis      149.99€  (Sports)                 "+YLW+B+"║"+R);
        p(YLW+B+"  ║                                                                   ║"+R);
        p(YLW+B+"  ║"+CYN+B+"  📋 COMMANDES :                                                   "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ORD-0001-2026  (user 2, LIVREE,    899.99€)                     "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ORD-0002-2026  (user 2, EXPEDIEE,   70.00€)                     "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ORD-0003-2026  (user 3, VALIDEE,  2199.99€)                     "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ORD-0004-2026  (user 4, VALIDEE,    74.97€)                     "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    ORD-0005-2026  (user 5, ANNULEE,  1299.00€)                     "+YLW+B+"║"+R);
        p(YLW+B+"  ║                                                                   ║"+R);
        p(YLW+B+"  ║"+CYN+B+"  🛒 PANIERS :                                                     "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    cart 1 → user 2    cart 2 → user 3                              "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+WHT+"    cart 3 → user 4    cart 4 → user 5                              "+YLW+B+"║"+R);
        p(YLW+B+"  ║                                                                   ║"+R);
        p(YLW+B+"  ║"+RED+B+"  ⚠  Les users seed ont un hash SHA-256 (pas hashCode Java).       "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+RED+B+"     → Créez un nouveau compte via REGISTER pour tester LOGIN.     "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+RED+B+"     → Pour tester PANIER/COMMANDES sans login, entrez un userId   "+YLW+B+"║"+R);
        p(YLW+B+"  ║"+RED+B+"       manuellement (ex: 2, 3, 4, 5).                              "+YLW+B+"║"+R);
        p(YLW+B+"  ║                                                                   ║"+R);
        p(YLW+B+"  ╚═══════════════════════════════════════════════════════════════════╝"+R);
        p("");
        pause();
    }

    // ══════════════════════════════════════════════════════════
    //                  AFFICHAGE : PRODUITS
    // ══════════════════════════════════════════════════════════

    static void printProductTable(List<Map<String, Object>> list) {
        p("");
        p(BLU + B + "  ┌──────┬──────────────────────────────┬────────────┬────────┬──────────────────┐" + R);
        p(BLU + B + "  │" + WHT + B + "  ID  " + BLU + B + "│" + WHT + B + "         Nom                      " + BLU + B + "│" + WHT + B + "    Prix    " + BLU + B + "│" + WHT + B + " Stock  " + BLU + B + "│" + WHT + B + "   Catégorie       " + BLU + B + "│" + R);
        p(BLU + B + "  ├──────┼──────────────────────────────┼────────────┼────────┼──────────────────┤" + R);

        for (Map<String, Object> pr : list) {
            int id       = toInt(pr.get("productId"));
            String name  = truncate(str(pr.get("name")), 28);
            double price = toDbl(pr.get("price"));
            int stock    = toInt(pr.get("stockQuantity"));
            String cat   = truncate(str(pr.get("categoryName")), 16);

            String stockColor = stock > 10 ? GRN : (stock > 0 ? YLW : RED);
            System.out.printf(
                    BLU + B + "  │" + CYN + " %4d " +
                            BLU + B + "│" + WHT + " %-28s " +
                            BLU + B + "│" + GRN + " %10.2f " +
                            BLU + B + "│" + stockColor + " %6d " +
                            BLU + B + "│" + DIM + " %-16s " +
                            BLU + B + "│" + R + "%n",
                    id, name, price, stock, cat);
        }

        p(BLU + B + "  └──────┴──────────────────────────────┴────────────┴────────┴──────────────────┘" + R);
        p(DIM + "  Total : " + list.size() + " produit(s)" + R);
    }

    static void printProductDetail(Map<String, Object> pr) {
        p("");
        p(BLU + B + "  ╔════════════════════════════════════════════════════╗" + R);
        p(BLU + B + "  ║" + WHT + B + "               📦 DÉTAIL DU PRODUIT                " + BLU + B + "║" + R);
        p(BLU + B + "  ╠════════════════════════════════════════════════════╣" + R);
        fieldLine(BLU, "ID",           str(pr.get("productId")),                 CYN);
        fieldLine(BLU, "Nom",          str(pr.get("name")),                      WHT + B);
        fieldLine(BLU, "Description",  truncate(str(pr.get("description")),32),  WHT);
        fieldLine(BLU, "Prix",         String.format("%.2f €", toDbl(pr.get("price"))), GRN + B);
        int stock = toInt(pr.get("stockQuantity"));
        String sColor = stock > 10 ? GRN : (stock > 0 ? YLW : RED);
        fieldLine(BLU, "Stock",        stock + " unité(s)",                      sColor);
        fieldLine(BLU, "Catégorie",    str(pr.get("categoryName")),              MAG);
        boolean av = toBool(pr.get("isAvailable"));
        fieldLine(BLU, "Disponible",   av ? "✅ OUI" : "❌ NON",                av ? GRN : RED);
        p(BLU + B + "  ╚════════════════════════════════════════════════════╝" + R);
    }

    // ══════════════════════════════════════════════════════════
    //                   AFFICHAGE : PANIER
    // ══════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    static void printCart(Map<String, Object> cart) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) cart.get("items");
        double total = toDbl(cart.get("total"));

        if (items == null || items.isEmpty()) {
            p("");
            p(YLW + "  🛒  Votre panier est vide." + R);
            return;
        }

        p("");
        p(GRN+B+"  ┌──────┬──────────────────────────────┬────────┬────────────┬────────────┐"+R);
        p(GRN+B+"  │"+WHT+B+" P.ID "+GRN+B+"│"+WHT+B+"         Produit                "+GRN+B+"│"+WHT+B+"  Qté   "+GRN+B+"│"+WHT+B+" Prix Unit. "+GRN+B+"│"+WHT+B+" Sous-total "+GRN+B+"│"+R);
        p(GRN+B+"  ├──────┼──────────────────────────────┼────────┼────────────┼────────────┤"+R);

        for (Map<String, Object> it : items) {
            System.out.printf(
                    GRN + B + "  │" + CYN + " %4s " +
                            GRN + B + "│" + WHT + " %-28s " +
                            GRN + B + "│" + WHT + " %6d " +
                            GRN + B + "│" + WHT + " %10.2f " +
                            GRN + B + "│" + YLW + " %10.2f " +
                            GRN + B + "│" + R + "%n",
                    str(it.get("productId")),
                    truncate(str(it.get("productName")), 28),
                    toInt(it.get("quantity")),
                    toDbl(it.get("unitPrice")),
                    toDbl(it.get("subtotal")));
        }

        p(GRN+B+"  ├──────┴──────────────────────────────┴────────┴────────────┼────────────┤"+R);
        System.out.printf(
                GRN + B + "  │" + WHT + B + "                                         TOTAL          " +
                        GRN + B + "│" + GRN + B + " %10.2f " +
                        GRN + B + "│" + R + "%n", total);
        p(GRN+B+"  └────────────────────────────────────────────────────────────┴────────────┘"+R);
        p(DIM + "  " + items.size() + " article(s)" + R);
    }

    // ══════════════════════════════════════════════════════════
    //                  AFFICHAGE : COMMANDES
    // ══════════════════════════════════════════════════════════

    static void printOrderConfirmation(Map<String, Object> result) {
        p("");
        String orderStatus   = str(result.get("orderStatus"));
        String paymentStatus = str(result.get("paymentStatus"));

        p(GRN + B + "  ╔════════════════════════════════════════════════════╗" + R);
        p(GRN + B + "  ║" + WHT + B + "          ✅  COMMANDE CONFIRMÉE !                  " + GRN + B + "║" + R);
        p(GRN + B + "  ╠════════════════════════════════════════════════════╣" + R);
        fieldLine(GRN, "N° Commande",  str(result.get("orderId")),                     CYN + B);
        fieldLine(GRN, "Montant",      String.format("%.2f €", toDbl(result.get("totalAmount"))), GRN + B);
        fieldLine(GRN, "Statut cmd",   orderStatus,   "VALIDEE".equals(orderStatus) ? GRN : YLW);
        fieldLine(GRN, "ID Paiement",  str(result.get("paymentId")),                   WHT);
        fieldLine(GRN, "Paiement",     paymentStatus, "ACCEPTE".equals(paymentStatus) ? GRN + B : RED + B);
        fieldLine(GRN, "Réf. Trans.",  str(result.get("transactionRef")),               DIM);
        p(GRN + B + "  ╚════════════════════════════════════════════════════╝" + R);
    }

    static void printOrderTable(List<Map<String, Object>> orders) {
        p("");
        p(MAG+B+"  ┌────────────────────┬──────────────┬──────────────┬───────────────────────┐"+R);
        p(MAG+B+"  │"+WHT+B+"   N° Commande       "+MAG+B+"│"+WHT+B+"   Montant     "+MAG+B+"│"+WHT+B+"    Statut     "+MAG+B+"│"+WHT+B+"        Date           "+MAG+B+"│"+R);
        p(MAG+B+"  ├────────────────────┼──────────────┼──────────────┼───────────────────────┤"+R);

        for (Map<String, Object> o : orders) {
            String status = str(o.get("status"));
            String sColor = statusColor(status);
            System.out.printf(
                    MAG + B + "  │" + CYN + " %-18s " +
                            MAG + B + "│" + GRN + " %12.2f " +
                            MAG + B + "│" + sColor + " %-12s " +
                            MAG + B + "│" + DIM + " %-21s " +
                            MAG + B + "│" + R + "%n",
                    str(o.get("orderId")),
                    toDbl(o.get("totalAmount")),
                    status,
                    str(o.get("createdAt")));
        }

        p(MAG+B+"  └────────────────────┴──────────────┴──────────────┴───────────────────────┘"+R);
        p(DIM + "  " + orders.size() + " commande(s)" + R);
    }

    @SuppressWarnings("unchecked")
    static void printOrderDetail(Map<String, Object> order) {
        String status = str(order.get("status"));

        p("");
        p(MAG + B + "  ╔════════════════════════════════════════════════════╗" + R);
        p(MAG + B + "  ║" + WHT + B + "            📋  DÉTAILS DE LA COMMANDE              " + MAG + B + "║" + R);
        p(MAG + B + "  ╠════════════════════════════════════════════════════╣" + R);
        fieldLine(MAG, "N° Commande",  str(order.get("orderId")),                            CYN + B);
        fieldLine(MAG, "Statut",       status,                                               statusColor(status));
        fieldLine(MAG, "Montant",      String.format("%.2f €", toDbl(order.get("totalAmount"))), GRN + B);
        fieldLine(MAG, "Adresse",      truncate(str(order.get("shippingAddress")), 32),      WHT);
        fieldLine(MAG, "Créée le",     str(order.get("createdAt")),                          DIM);
        fieldLine(MAG, "Mise à jour",  str(order.get("updatedAt")),                          DIM);
        p(MAG + B + "  ╚════════════════════════════════════════════════════╝" + R);

        // ── Articles ────────────────────────────────────────
        if (order.get("items") != null) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
            if (!items.isEmpty()) {
                p("");
                p(DIM + "  📦 Articles :" + R);
                p(MAG + "  ┌──────────────────────────────┬────────┬────────────┐" + R);
                p(MAG + "  │" + WHT + B + "         Produit                " + MAG + "│" + WHT + B + "  Qté   " + MAG + "│" + WHT + B + " Sous-total " + MAG + "│" + R);
                p(MAG + "  ├──────────────────────────────┼────────┼────────────┤" + R);

                for (Map<String, Object> it : items) {
                    String name = str(it.get("productName"));
                    if ("null".equals(name) || "—".equals(name)) {
                        name = str(it.get("productSnapshot"));
                    }
                    System.out.printf(
                            MAG + "  │" + WHT + " %-28s " +
                                    MAG + "│" + WHT + " %6d " +
                                    MAG + "│" + YLW + " %10.2f " +
                                    MAG + "│" + R + "%n",
                            truncate(name, 28),
                            toInt(it.get("quantity")),
                            toDbl(it.get("subtotal")));
                }
                p(MAG + "  └──────────────────────────────┴────────┴────────────┘" + R);
            }
        }

        // ── Paiement ───────────────────────────────────────
        if (order.get("payment") != null) {
            Map<String, Object> pay = (Map<String, Object>) order.get("payment");
            String pStatus = str(pay.get("status"));
            p("");
            p(DIM + "  💳 Paiement :" + R);
            p("    " + CYN + "ID       : " + WHT + str(pay.get("paymentId")) + R);
            p("    " + CYN + "Méthode  : " + WHT + str(pay.get("method")) + R);
            p("    " + CYN + "Statut   : " + ("ACCEPTE".equals(pStatus) ? GRN + B : RED + B) + pStatus + R);
            p("    " + CYN + "Montant  : " + GRN + String.format("%.2f €", toDbl(pay.get("amount"))) + R);
            p("    " + CYN + "Réf.     : " + DIM + str(pay.get("transactionRef")) + R);
            p("    " + CYN + "Payé le  : " + DIM + str(pay.get("paidAt")) + R);
        }
    }

    // ══════════════════════════════════════════════════════════
    //                    RÉSEAU : ENVOI
    // ══════════════════════════════════════════════════════════

    static Response send(Request request) {
        p("");
        p(DIM + "  ╭─ 📤 REQUÊTE ──────────────────────────────────────" + R);
        p(DIM + "  │ " + request.toJson() + R);
        p(DIM + "  ╰────────────────────────────────────────────────────" + R);

        Response response = conn.send(request);

        String icon = response.isSuccess() ? "✅" : "❌";
        String color = response.isSuccess() ? GRN : RED;
        p(color + DIM + "  ╭─ 📥 RÉPONSE " + icon + " ────────────────────────────────" + R);
        p(color + DIM + "  │ " + response.toJson() + R);
        p(color + DIM + "  ╰────────────────────────────────────────────────────" + R);
        p("");

        return response;
    }

    // ══════════════════════════════════════════════════════════
    //                    HELPERS UI
    // ══════════════════════════════════════════════════════════

    static void banner() {
        cls();
        p("");
        p(CYN + B + "  ╔═══════════════════════════════════════════════════════════╗" + R);
        p(CYN + B + "  ║                                                           ║" + R);
        p(CYN + B + "  ║" + WHT + B + "     ██████╗██╗  ██╗██████╗ ██╗  ██████╗  ███╗   ██╗      " + CYN + B + "║" + R);
        p(CYN + B + "  ║" + WHT + B + "    ██╔════╝██║  ██║██╔══██╗██║ ██╔═══██╗ ████╗  ██║      " + CYN + B + "║" + R);
        p(CYN + B + "  ║" + WHT + B + "    ██║     ███████║██████╔╝██║ ██║   ██║ ██╔██╗ ██║      " + CYN + B + "║" + R);
        p(CYN + B + "  ║" + WHT + B + "    ██║     ██╔══██║██╔══██╗██║ ██║   ██║ ██║╚██╗██║      " + CYN + B + "║" + R);
        p(CYN + B + "  ║" + WHT + B + "    ╚██████╗██║  ██║██║  ██║██║ ╚██████╔╝ ██║ ╚████║      " + CYN + B + "║" + R);
        p(CYN + B + "  ║" + WHT + B + "     ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═════╝  ╚═╝  ╚═══╝   " + CYN + B + "║" + R);
        p(CYN + B + "  ║                                                           ║" + R);
        p(CYN + B + "  ║" + YLW + B + "          🛍  E-Commerce Console Client v1.0  🛍          " + CYN + B + "║" + R);
        p(CYN + B + "  ║" + DIM + "                TCP  →  localhost:9090                    " + CYN + B + "║" + R);
        p(CYN + B + "  ║                                                           ║" + R);
        p(CYN + B + "  ╚═══════════════════════════════════════════════════════════╝" + R);
        p("");
    }

    static void statusBar() {
        if (token != null) {
            p(BG_G + BLK + B + " ✅ Connecté : " + logUser + " (ID:" + logId + ") | Rôle : " + logRole + " | Token : " + token.substring(0, 8) + "... " + R);
        } else {
            p(BG_R + WHT + B + " ❌ Non connecté — Allez dans Auth > Login pour vous connecter                         " + R);
        }
    }

    static void subTitle(String title) {
        p("");
        p(WHT + B + "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + R);
        p(WHT + B + "    " + title + R);
        p(WHT + B + "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + R);
        p("");
    }

    static void fieldLine(String boxColor, String label, String value, String valueColor) {
        System.out.printf(
                boxColor + B + "  ║" + CYN + "  %-14s: " + valueColor + "%-32s" + boxColor + B + "║" + R + "%n",
                label, value);
    }

    static void showResponse(Response r) {
        p("");
        if (r.isSuccess()) {
            p(GRN + B + "  ✅ " + r.getMessage() + R);
            if (r.getData() != null) {
                p(DIM + pretty.toJson(r.getData()) + R);
            }
        } else {
            p(RED + B + "  ❌ " + r.getMessage() + R);
        }
    }

    static String statusColor(String status) {
        if (status == null) return YLW;
        switch (status) {
            case "LIVREE":     return GRN + B;
            case "EXPEDIEE":   return CYN + B;
            case "VALIDEE":    return BLU + B;
            case "ANNULEE":    return RED + B;
            case "EN_ATTENTE": return YLW;
            default:           return YLW;
        }
    }

    static void ok(String msg)   { p(GRN + B + "  ✅ " + msg + R); }
    static void err(String msg)  { p(RED + B + "  ❌ " + msg + R); }
    static void info(String msg) { p(CYN + "  ℹ  " + msg + R); }
    static void p(String s)      { System.out.println(s); }

    static void cls() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    static void pause() {
        p("");
        System.out.print(DIM + "  Appuyez sur Entrée pour continuer..." + R);
        sc.nextLine();
    }

    // ══════════════════════════════════════════════════════════
    //                      INPUT
    // ══════════════════════════════════════════════════════════

    static String readStr(String prompt) {
        System.out.print(WHT + prompt + " : " + CYN);
        String value = sc.nextLine().trim();
        System.out.print(R);
        return value;
    }

    static int readInt(String prompt) {
        System.out.print(prompt);
        String line = sc.nextLine().trim();
        System.out.print(R);
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static int getActiveUserId() {
        if (logId > 0) {
            p(DIM + "  (Utilisation du userId de la session : " + logId + ")" + R);
            return logId;
        }
        info("Pas de session active. Entrez un userId manuellement :");
        p(DIM + "  (Users DB : 2=jdupont, 3=mleroy, 4=cblanc, 5=tnoir)" + R);
        int uid = readInt("  userId : ");
        if (uid <= 0) {
            err("userId invalide !");
            return -1;
        }
        return uid;
    }

    // ══════════════════════════════════════════════════════════
    //                    CONVERTERS
    // ══════════════════════════════════════════════════════════

    static String str(Object o) {
        return o != null ? o.toString() : "—";
    }

    static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Double) return ((Double) o).intValue();
        if (o instanceof Integer) return (Integer) o;
        try { return Integer.parseInt(o.toString()); }
        catch (Exception e) { return 0; }
    }

    static double toDbl(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Double) return (Double) o;
        try { return Double.parseDouble(o.toString()); }
        catch (Exception e) { return 0.0; }
    }

    static boolean toBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        return Boolean.parseBoolean(o.toString());
    }

    static String truncate(String s, int max) {
        if (s == null) return "—";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}