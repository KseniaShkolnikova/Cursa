package com.example.ozon;
import android.util.Log;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
public class MailSender {
    private static final String EMAIL = "sesha_shk@mail.ru";
    private static final String PASSWORD = "jrnM9p0yFSvy3qE0GhTi";
    public static void sendEmail(String recipient, String subject, String body, boolean isHtml) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.mail.ru");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL, PASSWORD);
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            if (isHtml) {
                message.setContent(body, "text/html; charset=UTF-8");
            } else {
                message.setText(body);
            }
            Transport.send(message);
            Log.d("MailSender", "Письмо успешно отправлено на: " + recipient);
        } catch (MessagingException e) {
            Log.e("MailSender", "Ошибка отправки письма: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}