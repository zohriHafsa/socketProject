package com.chrionline.server.service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    // !! Remplace par tes credentials Gmail
    private static final String FROM     = "hafsazohri0@gmail.com";
    private static final String PASSWORD = "vgljnfvcwzcftoow"; // App Password

    public static boolean sendVerificationCode(String toEmail,
                                               String code) {
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
            msg.setFrom(new InternetAddress(FROM));
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(toEmail));
            msg.setSubject("ChriOnline — Code de verification");
            msg.setText(
                    "Bonjour,\n\n" +
                            "Votre code de verification ChriOnline est :\n\n" +
                            "    " + code + "\n\n" +
                            "Ce code expire dans 10 minutes.\n\n" +
                            "Si vous n'avez pas cree de compte, ignorez cet email.\n\n" +
                            "ChriOnline"
            );
            Transport.send(msg);
            return true;
        } catch (MessagingException e) {
            System.err.println("[EMAIL] Erreur : " + e.getMessage());
            return false;
        }
    }

    public static String generateCode() {
        // Génère un code 6 chiffres
        int code = (int)(Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }
}