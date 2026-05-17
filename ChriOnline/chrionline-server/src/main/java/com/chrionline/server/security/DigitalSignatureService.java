package com.chrionline.server.security;

import com.chrionline.server.core.ServerLogger;

import java.io.File;
import java.io.FileInputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Base64;

/**
 * Service de signature numérique SHA256withRSA.
 *
 * Utilise le Keystore serveur (server.p12) pour :
 * - Signer les données de paiement avec la clé privée
 * - Vérifier les signatures avec la clé publique
 *
 * Garanties :
 * - Intégrité des données (les données n'ont pas été altérées)
 * - Authentification de l'auteur (seul le serveur possède la clé privée)
 * - Non-répudiation (le serveur ne peut pas nier avoir signé)
 */
public class DigitalSignatureService {

    private static DigitalSignatureService instance;

    private static final String KEYSTORE_PATH     = "src/main/resources/security/server.p12";
    private static final String KEYSTORE_PASSWORD  = "chrionline2026";
    private static final String KEY_ALIAS          = "chrionline";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private PrivateKey privateKey;
    private PublicKey  publicKey;

    private DigitalSignatureService() {
        loadKeys();
    }

    public static synchronized DigitalSignatureService getInstance() {
        if (instance == null) {
            instance = new DigitalSignatureService();
        }
        return instance;
    }

    /**
     * Charge la paire de clés depuis le Keystore PKCS12.
     */
    private void loadKeys() {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            File keystoreFile = new File(KEYSTORE_PATH);
            if (!keystoreFile.exists()) {
                keystoreFile = new File("chrionline-server/" + KEYSTORE_PATH);
            }

            try (FileInputStream fis = new FileInputStream(keystoreFile.getAbsolutePath())) {
                keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
            }

            // Récupérer la clé privée
            Key key = keyStore.getKey(KEY_ALIAS, KEYSTORE_PASSWORD.toCharArray());
            if (key instanceof PrivateKey) {
                this.privateKey = (PrivateKey) key;
            } else {
                throw new SecurityException("Clé privée introuvable pour l'alias : " + KEY_ALIAS);
            }

            // Récupérer la clé publique via le certificat
            Certificate cert = keyStore.getCertificate(KEY_ALIAS);
            this.publicKey = cert.getPublicKey();

            ServerLogger.info("[SIGNATURE] Clés RSA chargées depuis " + KEYSTORE_PATH);
            ServerLogger.info("[SIGNATURE] Algorithme : " + SIGNATURE_ALGORITHM);

        } catch (Exception e) {
            ServerLogger.error("[SIGNATURE] Erreur chargement des clés : " + e.getMessage());
            throw new RuntimeException("Impossible de charger les clés de signature", e);
        }
    }

    /**
     * Signe les données avec la clé privée du serveur.
     *
     * @param data Les données à signer (ex: JSON du paiement)
     * @return La signature encodée en Base64
     */
    public String sign(String data) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes("UTF-8"));

            byte[] signatureBytes = signature.sign();
            String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

            ServerLogger.info("[SIGNATURE] Données signées (" + data.length() + " octets)");
            return signatureBase64;

        } catch (Exception e) {
            ServerLogger.error("[SIGNATURE] Erreur de signature : " + e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie une signature avec la clé publique.
     *
     * @param data            Les données originales
     * @param signatureBase64 La signature en Base64 à vérifier
     * @return true si la signature est valide
     */
    public boolean verify(String data, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes("UTF-8"));

            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            boolean isValid = signature.verify(signatureBytes);

            ServerLogger.info("[SIGNATURE] Vérification : " + (isValid ? "VALIDE ✓" : "INVALIDE ✗"));
            return isValid;

        } catch (Exception e) {
            ServerLogger.error("[SIGNATURE] Erreur de vérification : " + e.getMessage());
            return false;
        }
    }

    /**
     * Retourne la clé publique encodée en Base64.
     * Peut être partagée avec le client pour vérifier les signatures.
     */
    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
}
