package com.chrionline;

import com.chrionline.server.core.RequestRouter;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;

public class MainTest {
    public static void main(String[] args) {
        String raw = new Request(Action.GET_PRODUCTS, null, null).toJson();
        System.out.println("Request JSON  : " + raw);

        Response res = RequestRouter.route(raw);
        System.out.println("Response JSON : " + res.toJson());
    }
}
