package com.keyvault.controllers;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class MailController extends Thread{
    private String email, authCode;
    public MailController(String authCode, String email)
    {
        this.authCode = authCode;
        this.email = email;
    }

    @Override
    public void run()
    {
        try {
            sendAuthMail();
        } catch (UnsupportedEncodingException | MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendAuthMail() throws UnsupportedEncodingException, MessagingException {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "213.37.28.35");
        //prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.socketFactory.port", "587");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
        prop.put("mail.smtp.socketFactory.fallback", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.auth", "false");

        Session session = Session.getInstance(prop);

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("auth@sagaraque.com.es", "KeyVault"));
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
        msg.setSubject("Código de verificación");
        msg.setContent(getEmailTemplate(), "text/html");

        Transport.send(msg);
    }

    private String getEmailTemplate()
    {
       return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <meta http-equiv=\"Content-Type\" content=\"text/html charset=UTF-8\" />\n" +
               "    <title>Código de verificación</title>\n" +
               "</head>\n" +
               "<body style=\"background-color: #e7e7e7; margin: 0; padding: 20px 0; font-family: Arial, sans-serif\">\n" +
               "<table style=\"width: 100%; max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 2px; border-top: 3px solid #2a333f;\">\n" +
               "    <tr>\n" +
               "        <td style=\"padding: 40px; box-sizing: border-box; text-align: center\">\n" +
               "            <img src=\"https://raw.githubusercontent.com/SagAraque/KeyVault-Server/master/src/main/resources/logo.png\" alt=\"KeyVault Logo\">\n" +
               "            <h1 style=\"font-size: 25px; text-align: center;\">Verificación de dispositivo</h1>\n" +
               "            <p style=\"margin-bottom: 0; text-align: center;\">Se detectó un nuevo inicio de sesión en un dispositivo no autorizado.</p>\n" +
               "            <p style=\"margin-bottom: 20px; text-align: center;\">Copie el siguiente código de verificación:</p>\n" +
               "            <p style=\"font-size: 25px; margin-bottom: 20px; font-weight: bold; text-align: center;\">" + authCode + "</p>\n" +
               "        </td>\n" +
               "    </tr>\n" +
               "</table>\n" +
               "</body>\n" +
               "</html>";
    }
}
