package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import com.chrionline.server.dao.BankDAO;
import com.chrionline.shared.dto.Response;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class PaymentValidationService {

    private final BankDAO bankDAO = new BankDAO();

    /**
     * Valide les informations de carte bancaire.
     *
     * @return Response avec :
     *   - success=true + data={bankName, bankLogo, maskedCard} si OK
     *   - success=false + message si erreur
     */
    public Response validateCard(int userId, String cardNumber, String expiry, String cvv, boolean isRealtime) {
        // 1. Vérifier les tentatives récentes
        if (!isRealtime) {
            try {
                int attempts = bankDAO.getRecentFailedAttempts(userId);
                if (attempts >= 2) {
                    return Response.error(
                            "Compte temporairement bloqué après 2 tentatives échouées. " +
                                    "Veuillez réessayer dans 30 minutes."
                    );
                }
            } catch (SQLException e) {
                ServerLogger.error("Erreur vérification tentatives : " + e.getMessage());
            }
        }

        // 2. Nettoyer et valider le format
        String cleaned = cardNumber.replaceAll("\\s+", "");

        if (!isRealtime) {
            if (cleaned.length() < 13 || cleaned.length() > 19) {
                recordFailure(userId, cardNumber, "Numéro invalide (longueur)");
                return Response.error("Numéro de carte invalide.");
            }

            if (!cleaned.matches("\\d+")) {
                recordFailure(userId, cardNumber, "Numéro invalide (non numérique)");
                return Response.error("Le numéro de carte ne doit contenir que des chiffres.");
            }

            // 3. Algorithme de Luhn
            if (!luhnCheck(cleaned)) {
                recordFailure(userId, cardNumber, "Échec validation Luhn");
                return Response.error("Numéro de carte invalide.");
            }

            // 4. Vérifier la date d'expiration
            Response expiryCheck = validateExpiry(expiry);
            if (!expiryCheck.isSuccess()) {
                recordFailure(userId, cardNumber, "Date expirée : " + expiry);
                return expiryCheck;
            }

            // 5. Valider le CVV
            if (cvv == null || !cvv.matches("\\d{3,4}")) {
                recordFailure(userId, cardNumber, "CVV invalide");
                return Response.error("Le CVV doit contenir 3 ou 4 chiffres.");
            }
        }

        // 6. Identifier la banque via BIN
        Map<String, Object> bankInfo = null;
        try {
            bankInfo = bankDAO.getBankByBIN(cleaned);
        } catch (SQLException e) {
            ServerLogger.error("Erreur recherche BIN : " + e.getMessage());
        }

        // 7. Construire la réponse
        Map<String, Object> result = new HashMap<>();
        result.put("maskedCard", maskCard(cleaned));
        result.put("last4", cleaned.length() >= 4 ? cleaned.substring(cleaned.length() - 4) : cleaned);
        result.put("cardType", detectCardType(cleaned));

        if (bankInfo != null) {
            result.put("bankName", bankInfo.get("bankName"));
            result.put("bankLogo", "/images/banks/" + bankInfo.get("logoFilename"));
            result.put("isMoroccan", true);
        } else {
            result.put("bankName", "Banque internationale");
            result.put("bankLogo", "/images/banks/default_bank.png");
            result.put("isMoroccan", false);
        }

        if (!isRealtime) {
            ServerLogger.info("Carte validée : " + maskCard(cleaned)
                    + " (" + result.get("bankName") + ")");
        }

        return Response.ok("Carte valide", result);
    }

    /**
     * Valide la date d'expiration (format MM/AA ou MM/AAAA).
     */
    private Response validateExpiry(String expiry) {
        if (expiry == null || expiry.trim().isEmpty()) {
            return Response.error("Date d'expiration requise.");
        }

        String cleaned = expiry.trim().replace(" ", "");

        // Accepter MM/AA ou MM/AAAA
        if (!cleaned.matches("\\d{2}/\\d{2,4}")) {
            return Response.error("Format invalide. Utilisez MM/AA (ex: 12/25).");
        }

        String[] parts = cleaned.split("/");
        int month = Integer.parseInt(parts[0]);
        int year  = Integer.parseInt(parts[1]);

        // Normaliser l'année (25 → 2025)
        if (year < 100) {
            year += 2000;
        }

        if (month < 1 || month > 12) {
            return Response.error("Mois invalide (doit être entre 01 et 12).");
        }

        // Vérifier si la carte est expirée
        LocalDate now = LocalDate.now();
        LocalDate cardExpiry = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);

        if (cardExpiry.isBefore(now)) {
            return Response.error(
                    "❌ Carte expirée depuis " +
                            cardExpiry.format(DateTimeFormatter.ofPattern("MM/yyyy")) +
                            ". Veuillez utiliser une autre carte."
            );
        }

        return Response.ok("Date valide", null);
    }

    /**
     * Algorithme de Luhn pour validation du numéro de carte.
     */
    private boolean luhnCheck(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

    /**
     * Détecte le type de carte (Visa, Mastercard, etc.).
     */
    private String detectCardType(String cardNumber) {
        char first = cardNumber.charAt(0);

        if (first == '4') return "Visa";
        if (first == '5') return "Mastercard";
        if (first == '3') {
            if (cardNumber.length() >= 2) {
                String prefix = cardNumber.substring(0, 2);
                if ("34".equals(prefix) || "37".equals(prefix)) return "Amex";
            }
            return "Diners/JCB";
        }
        if (first == '6') return "Discover";

        return "Autre";
    }

    /**
     * Masque un numéro de carte : 1234567890123456 → **** **** **** 3456
     */
    private String maskCard(String cardNumber) {
        if (cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Enregistre une tentative échouée.
     */
    private void recordFailure(int userId, String cardNumber, String reason) {
        try {
            bankDAO.recordFailedAttempt(userId, cardNumber, reason);
        } catch (SQLException e) {
            ServerLogger.error("Erreur enregistrement échec : " + e.getMessage());
        }
    }

    /**
     * Génère et envoie un code 2FA pour paiement.
     */
    public Response send2FACode(int userId, double amount, String orderId, String email) {
        String code = EmailService.generateCode();

        try {
            bankDAO.save2FACode(userId, code, amount, orderId);

            boolean sent = EmailService.sendPayment2FAEmail(email, code, amount);

            if (sent) {
                ServerLogger.info("Code 2FA paiement envoyé à userId=" + userId);
                return Response.ok("Code envoyé à votre email",
                        Map.of("requiresCode", true));
            } else {
                return Response.error("Erreur envoi email. Contactez le support.");
            }

        } catch (SQLException e) {
            ServerLogger.error("Erreur création code 2FA : " + e.getMessage());
            return Response.error("Erreur serveur.");
        }
    }

    /**
     * Vérifie le code 2FA de paiement.
     */
    public Response verify2FACode(int userId, String code) {
        try {
            boolean valid = bankDAO.verify2FACode(userId, code);

            if (valid) {
                ServerLogger.info("Code 2FA paiement validé pour userId=" + userId);
                return Response.ok("Code valide", null);
            } else {
                return Response.error("Code incorrect ou expiré.");
            }

        } catch (SQLException e) {
            ServerLogger.error("Erreur vérification 2FA : " + e.getMessage());
            return Response.error("Erreur serveur.");
        }
    }
}