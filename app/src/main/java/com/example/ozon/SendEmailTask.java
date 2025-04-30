package com.example.ozon;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 * Класс SendEmailTask представляет собой асинхронную задачу для отправки email-сообщений
 * в приложении "OZON". Использует MailSender для отправки писем
 * и уведомляет пользователя об успехе или неудаче.
 */
public class SendEmailTask extends AsyncTask<Void, Void, Boolean> {
    private String recipient;
    private String subject;
    private String body;
    private Context context;
    private boolean isHtml;

    /**
     * Конструктор класса SendEmailTask. Инициализирует задачу с параметрами для отправки email.
     */
    public SendEmailTask(Context context, String recipient, String subject, String body, boolean isHtml) {
        this.context = context;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.isHtml = isHtml;
    }

    /**
     * Выполняет отправку email в фоновом потоке. Использует MailSender для отправки письма
     * и возвращает результат операции.
     */
    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            MailSender.sendEmail(recipient, subject, body, isHtml);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Обрабатывает результат отправки email. Показывает уведомление об ошибке,
     * если отправка не удалась.
     */
    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if (success) {
        } else {
            Toast.makeText(context, "Ошибка при отправке письма", Toast.LENGTH_SHORT).show();
        }
    }
}