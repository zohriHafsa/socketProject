package com.chrionline;

import com.chrionline.server.core.RequestRouter;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;

import java.util.Map;

public class MainTest {
    public static void main(String[] args) {

        System.out.println("========== T08 : PANIER ==========");

        test("GET_CART (user 2)",
                new Request(Action.GET_CART, null, Map.of("userId", 2)));

        test("ADD_TO_CART (user 2, product 5)",
                new Request(Action.ADD_TO_CART, null,
                        Map.of("userId", 2, "productId", 5, "quantity", 1)));

        test("GET_CART after add",
                new Request(Action.GET_CART, null, Map.of("userId", 2)));

        test("REMOVE_FROM_CART (user 2, product 5)",
                new Request(Action.REMOVE_FROM_CART, null,
                        Map.of("userId", 2, "productId", 5)));

        test("GET_CART after remove",
                new Request(Action.GET_CART, null, Map.of("userId", 2)));

        test("CLEAR_CART (user 3)",
                new Request(Action.CLEAR_CART, null, Map.of("userId", 3)));

        test("GET_CART after clear (user 3)",
                new Request(Action.GET_CART, null, Map.of("userId", 3)));

        System.out.println("\n========== T09 : COMMANDES ==========");

        test("PLACE_ORDER (user 4)",
                new Request(Action.PLACE_ORDER, null, Map.of(
                        "userId", 4,
                        "shippingAddress", "5 Rue du Moulin, Bordeaux",
                        "paymentMethod", "CARTE_BANCAIRE",
                        "cardLastFour", "9999")));

        test("GET_ORDER_HISTORY (user 4)",
                new Request(Action.GET_ORDER_HISTORY, null, Map.of("userId", 4)));

        test("GET_ORDER_STATUS (ORD-0001-2026)",
                new Request(Action.GET_ORDER_STATUS, null,
                        Map.of("orderId", "ORD-0001-2026")));

        System.out.println("\n========== T09 : PAIEMENT ==========");

        test("PROCESS_PAYMENT retry (ORD-0004-2026)",
                new Request(Action.PROCESS_PAYMENT, null, Map.of(
                        "orderId", "ORD-0004-2026",
                        "paymentMethod", "CARTE_BANCAIRE",
                        "cardLastFour", "1111")));
    }

    private static void test(String label, Request request) {
        System.out.println("\n--- " + label + " ---");
        System.out.println("REQ  : " + request.toJson());
        Response res = RequestRouter.route(request.toJson());
        System.out.println("RESP : " + res.toJson());
    }
}