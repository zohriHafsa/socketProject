package com.chrionline.server.tests;

import com.chrionline.client.network.ServerConnection;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;

public class RateLimitTester {
    public static void main(String[] args) throws Exception {
        // Enforce TLS Truststore location properties if not set
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            System.setProperty("javax.net.ssl.trustStore", "chrionline-client/src/main/resources/security/client_truststore.p12");
            System.setProperty("javax.net.ssl.trustStorePassword", "chrionline2026");
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
        }

        ServerConnection conn = ServerConnection.getInstance();
        System.out.println("[TEST] Connecting to server (localhost:9090)...");
        if (!conn.connect()) {
            System.err.println("[TEST] ERROR: Could not establish a secure connection to the server!");
            System.err.println("[TEST] Ensure the Server application is running before executing this test.");
            return;
        }

        System.out.println("\n=== Starting Rate Limit Test (Limit = 10 requests per second) ===");

        // Send 15 messages rapidly (Limit is 10)
        for (int i = 1; i <= 15; i++) {
            Request req = new Request(Action.GET_PRODUCTS, null, null);
            Response resp = conn.send(req);
            System.out.printf("Request %2d: Success = %b, Response Message = %s%n", 
                    i, resp.isSuccess(), resp.getMessage() != null ? resp.getMessage() : "OK");
        }

        System.out.println("\n[TEST] Waiting 1.5 seconds for token bucket refill...");
        Thread.sleep(1500);

        System.out.println("\n[TEST] Sending post-wait message (Refill test)...");
        Request refillReq = new Request(Action.GET_PRODUCTS, null, null);
        Response refillResp = conn.send(refillReq);
        System.out.printf("Post-wait Request: Success = %b, Response Message = %s%n", 
                refillResp.isSuccess(), refillResp.getMessage() != null ? refillResp.getMessage() : "OK");

        System.out.println("\n[TEST] Sending another burst of 15 requests...");
        for (int i = 1; i <= 15; i++) {
            Request req = new Request(Action.GET_PRODUCTS, null, null);
            Response resp = conn.send(req);
            System.out.printf("Request %2d: Success = %b, Response Message = %s%n", 
                    i, resp.isSuccess(), resp.getMessage() != null ? resp.getMessage() : "OK");
        }

        conn.disconnect();
        System.out.println("\n=== Rate Limit Test Completed ===");
    }
}
