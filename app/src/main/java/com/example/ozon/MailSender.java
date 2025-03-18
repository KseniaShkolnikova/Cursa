package com.example.ozon;

import android.util.Log;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailSender {

    private static final String EMAIL = "sesha_shk@mail.ru"; // Ваш email
    private static final String PASSWORD = "jrnM9p0yFSvy3qE0GhTi"; // Ваш пароль

    private static final Executor executor = Executors.newSingleThreadExecutor();

    public static void sendEmail(String recipient, String subject, String body) {
        executor.execute(() -> {
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.mail.ru"); // SMTP сервер (например, Gmail)
            props.put("mail.smtp.port", "587"); // Порт SMTP
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true"); // Использование TLS

            // Создаем сессию с аутентификацией
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL, PASSWORD);
                }
            });

            try {
                // Создаем объект сообщения
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL)); // Отправитель
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient)); // Получатель
                message.setSubject(subject); // Тема письма
                message.setText(body); // Текст письма

                // Отправляем письмо
                Transport.send(message);
                Log.d("MailSender", "Письмо отправлено!");
            } catch (MessagingException e) {
                Log.e("MailSender", "Ошибка отправки письма: " + e.getMessage(), e);
                e.printStackTrace(); // Детали ошибки
            }
        });
    }
}