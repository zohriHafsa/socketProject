package com.chrionline.server.service;

import com.chrionline.server.core.ServerLogger;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    // !! Remplace par tes credentials Gmail
    private static final String FROM     = "hafsazohri0@gmail.com";
    private static final String PASSWORD = "vgljnfvcwzcftoow"; // App Password

    private static boolean sendEmail(String toEmail, String subject, String title, String message, String code) {
        Properties props = new Properties();
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(FROM, PASSWORD);
                    }
                });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM, "ChriOnline"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            msg.setSubject(subject);

            String htmlContent = 
                "<div style='font-family: \"Inter\", Helvetica, Arial, sans-serif; background-color: #f4f6f8; padding: 40px 0;'>" +
                "  <div style='max-width: 550px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 16px rgba(0,0,0,0.05);'>" +
                "    <div style='background-color: #6C3483; padding: 30px; text-align: center; border-bottom: 3px solid #8E44AD;'>" +
                "      <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: 800; letter-spacing: -0.5px;'>" + title + "</h1>" +
                "    </div>" +
                "    <div style='padding: 40px 30px; color: #333333; line-height: 1.6;'>" +
                "      <h2 style='font-size: 20px; color: #2C3E50; margin-top: 0; font-weight: 600;'>Bonjour,</h2>" +
                "      <p style='font-size: 15px; color: #4a5568;'>" + message + "</p>" +
                "      <div style='text-align: center; margin: 35px 0;'>" +
                "        <span style='display: inline-block; background-color: #F8F9FA; border: 1px solid #E2E8F0; border-radius: 8px; padding: 18px 35px; font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #6C3483;'>" + code + "</span>" +
                "      </div>" +
                "      <p style='font-size: 14px; color: #718096; margin-bottom: 0;'>Ce code expirera dans 10 minutes. Si vous n'avez pas initie cette demande, vous pouvez ignorer cet email en toute securite.</p>" +
                "    </div>" +
                "    <div style='background-color: #f8fafc; padding: 20px; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "      <p style='margin: 0; font-size: 12px; color: #a0aec0;'>&copy; 2026 ChriOnline. Tous droits reserves.</p>" +
                "      <p style='margin: 5px 0 0 0; font-size: 11px; color: #cbd5e0;'>Ceci est un message automatique, merci de ne pas y repondre.</p>" +
                "    </div>" +
                "  </div>" +
                "</div>";

            msg.setContent(htmlContent, "text/html; charset=utf-8");
            Transport.send(msg);
            return true;
        } catch (Exception e) {
            System.err.println("[EMAIL] Erreur : " + e.getMessage());
            return false;
        }
    }

    public static boolean sendRegistrationEmail(String toEmail, String code) {
        return sendEmail(toEmail, 
            "Bienvenue sur ChriOnline - Code de verification", 
            "ChriOnline", 
            "Nous sommes ravis de vous compter parmi nous ! Pour securiser et valider la creation de votre compte, veuillez utiliser le code de verification ci-dessous :", 
            code);
    }

    public static boolean send2faEmail(String toEmail, String code) {
        return sendEmail(toEmail, 
            "ChriOnline - Authentification requise", 
            "Securite ChriOnline", 
            "Une tentative de connexion a ete detectee sur votre compte. Pour autoriser cet acces, veuillez utiliser le code d'authentification a deux facteurs (2FA) ci-dessous :", 
            code);
    }

    public static String generateCode() {
        // Génère un code 6 chiffres
        int code = (int)(Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }

    public static boolean sendResetPasswordEmail(String toEmail, String code) {
        return sendEmail(
                toEmail,
                "ChriOnline - Reinitialisation de votre mot de passe",
                "Recuperation de compte",
                "Nous avons recu une demande de reinitialisation de mot de passe pour votre compte. Veuillez utiliser le code de securite ci-dessous pour modifier votre mot de passe :",
                code
        );
    }
}