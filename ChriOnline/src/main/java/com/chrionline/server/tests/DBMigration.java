package com.chrionline.server.tests;

import com.chrionline.server.dao.DBConnection;
import com.chrionline.server.security.EncryptionHelper;

import java.sql.*;

/**
 * Run ONCE to encrypt existing plaintext rows in the DB.
 * Covers: users, orders, payments.
 */
public class DBMigration {
    public static void main(String[] args) throws Exception {
        Connection conn = DBConnection.getInstance().getConnection();

        migrateUsers(conn);
        migrateOrders(conn);
        migratePayments(conn);

        System.out.println("\n=== Migration complete ===");
    }

    private static void migrateUsers(Connection conn) throws Exception {
        System.out.println("\n--- Migrating users ---");
        ResultSet rs = conn.prepareStatement(
                "SELECT user_id, email, first_name, last_name, address, phone FROM users"
        ).executeQuery();

        PreparedStatement update = conn.prepareStatement(
                "UPDATE users SET email = ?, email_hash = ?, " +
                        "first_name = ?, last_name = ?, address = ?, phone = ? " +
                        "WHERE user_id = ?"
        );

        int count = 0;
        while (rs.next()) {
            int id       = rs.getInt("user_id");
            String email = rs.getString("email");
            String fname = rs.getString("first_name");
            String lname = rs.getString("last_name");
            String addr  = rs.getString("address");
            String phone = rs.getString("phone");

            update.setString(1, EncryptionHelper.encrypt(email));
            update.setString(2, EncryptionHelper.hashEmail(email));
            update.setString(3, EncryptionHelper.encrypt(fname));
            update.setString(4, EncryptionHelper.encrypt(lname));
            update.setString(5, EncryptionHelper.encrypt(addr));
            update.setString(6, EncryptionHelper.encrypt(phone));
            update.setInt(7, id);
            update.executeUpdate();
            count++;
            System.out.println("  Migrated user_id=" + id);
        }
        System.out.println("Done. " + count + " users migrated.");
    }

    private static void migrateOrders(Connection conn) throws Exception {
        System.out.println("\n--- Migrating orders ---");
        ResultSet rs = conn.prepareStatement(
                "SELECT order_id, shipping_address FROM orders"
        ).executeQuery();

        PreparedStatement update = conn.prepareStatement(
                "UPDATE orders SET shipping_address = ? WHERE order_id = ?"
        );

        int count = 0;
        while (rs.next()) {
            String orderId = rs.getString("order_id");
            String addr    = rs.getString("shipping_address");

            update.setString(1, EncryptionHelper.encrypt(addr));
            update.setString(2, orderId);
            update.executeUpdate();
            count++;
            System.out.println("  Migrated order_id=" + orderId);
        }
        System.out.println("Done. " + count + " orders migrated.");
    }

    private static void migratePayments(Connection conn) throws Exception {
        System.out.println("\n--- Migrating payments ---");
        ResultSet rs = conn.prepareStatement(
                "SELECT payment_id, card_last_four, transaction_ref FROM payments"
        ).executeQuery();

        PreparedStatement update = conn.prepareStatement(
                "UPDATE payments SET card_last_four = ?, transaction_ref = ? WHERE payment_id = ?"
        );

        int count = 0;
        while (rs.next()) {
            String payId = rs.getString("payment_id");
            String card  = rs.getString("card_last_four");
            String txRef = rs.getString("transaction_ref");

            update.setString(1, EncryptionHelper.encrypt(card));
            update.setString(2, EncryptionHelper.encrypt(txRef));
            update.setString(3, payId);
            update.executeUpdate();
            count++;
            System.out.println("  Migrated payment_id=" + payId);
        }
        System.out.println("Done. " + count + " payments migrated.");
    }
}

