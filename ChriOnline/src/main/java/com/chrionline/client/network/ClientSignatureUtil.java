package com.chrionline.client.network;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utilitaire côté client pour vérifier les signatures numériques
 * des réponses du serveur (ex: confirmations de paiement).
 *
 * Utilise la clé publique du serveur (transmise en Base64)
 * pour vérifier l'authenticité et l'intégrité des données reçues.
 */
public class ClientSignatureUtil {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * Vérifie la signature numérique d'une donnée.
     *
     * @param data            Les données originales (ex: JSON du paiement)
     * @param signatureBase64 La signature en Base64 reçue du serveur
     * @param publicKeyBase64 La clé publique du serveur en Base64
     * @return true si la signature est valide, false sinon
     */
    public static boolean verifySignature(String data, String signatureBase64,
                                          String publicKeyBase64) {
        try {
            // Reconstruire la clé publique depuis Base64
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // Vérifier la signature
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes("UTF-8"));

            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            boolean isValid = signature.verify(signatureBytes);

            if (isValid) {
                System.out.println("[SIGNATURE] ✓ Signature du serveur vérifiée — Données authentiques");
            } else {
                System.err.println("[SIGNATURE] ✗ Signature invalide — Données potentiellement altérées !");
            }

            return isValid;

        } catch (Exception e) {
            System.err.println("[SIGNATURE] Erreur de vérification : " + e.getMessage());
            return false;
        }
    }
}
