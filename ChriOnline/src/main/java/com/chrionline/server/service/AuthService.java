package com.chrionline.server.service;


import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.dao.UserDAO;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    // ── REGISTER ──────────────────────────────────────────────
    public Response register(Request request) {
        try {
            // 1. Read fields from the JSON "data" object
            String username  = request.getString("username");
            String email     = request.getString("email");
            String password  = request.getString("password");
            String firstName = request.getString("firstName");
            String lastName  = request.getString("lastName");
            String address   = request.getString("address");
            String phone     = request.getString("phone");

            // 2. Validate required fields
            if (username == null || password == null || email == null)
                return Response.error("username, email, password required");

            // 3. Check username/email not already taken
            if (userDAO.findByUsername(username) != null)
                return Response.error("Username already exists");
            if (userDAO.findByEmail(email) != null)
                return Response.error("Email already exists");

            // 4. Hash the password (simple for now — could use BCrypt later)
            String hash = Integer.toHexString(password.hashCode());

            // 5. Insert into DB
            int userId = userDAO.createUser(username, email, hash,
                    firstName, lastName, address, phone);

            ServerLogger.info("REGISTER userId=" + userId + " username=" + username);
            return Response.ok("Registration successful",
                    Map.of("userId", userId, "username", username));

        } catch (Exception e) {
            ServerLogger.error("register: " + e.getMessage());
            return Response.error("Registration failed: " + e.getMessage());
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────
    public Response login(Request request) {
        try {
            String username = request.getString("username");
            String password = request.getString("password");

            if (username == null || password == null)
                return Response.error("username and password required");

            // Look up user in DB
            Map<String, Object> user = userDAO.findByUsername(username);
            if (user == null)
                return Response.error("User not found");

            // Compare hashed passwords
            String hash = Integer.toHexString(password.hashCode());
            if (!hash.equals(user.get("passwordHash")))
                return Response.error("Invalid password");

            // Check account not disabled
            if (!(boolean) user.get("isActive"))
                return Response.error("Account disabled");

            // Create session → generates a UUID token
            String token = SessionManager.createSession(
                    (int) user.get("userId"),
                    (String) user.get("role")
            );

            // Build response data
            Map<String, Object> data = new HashMap<>();
            data.put("token",    token);
            data.put("userId",   user.get("userId"));
            data.put("username", user.get("username"));
            data.put("role",     user.get("role"));

            ServerLogger.info("LOGIN userId=" + user.get("userId"));
            return Response.ok("Login successful", data);

        } catch (Exception e) {
            ServerLogger.error("login: " + e.getMessage());
            return Response.error("Login failed: " + e.getMessage());
        }
    }

    // ── LOGOUT ────────────────────────────────────────────────
    public Response logout(Request request) {
        try {
            String token = request.getToken();
            if (token == null)
                return Response.error("Token required");

            boolean ok = SessionManager.invalidate(token);
            if (!ok) return Response.error("Invalid token");

            ServerLogger.info("LOGOUT token=" + token);
            return Response.ok("Logged out", null);

        } catch (Exception e) {
            ServerLogger.error("logout: " + e.getMessage());
            return Response.error("Logout failed: " + e.getMessage());
        }
    }
}
