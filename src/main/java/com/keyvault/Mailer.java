package com.keyvault;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class Mailer extends Thread{
    private String email;
    private int authCode;
    public Mailer(int authCode, String email)
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
        msg.setText(String.valueOf(authCode));

        Transport.send(msg);
    }
}
