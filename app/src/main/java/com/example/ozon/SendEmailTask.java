package com.example.ozon;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
public class SendEmailTask extends AsyncTask<Void, Void, Boolean> {
    private String recipient;
    private String subject;
    private String body;
    private Context context;
    private boolean isHtml;
    public SendEmailTask(Context context, String recipient, String subject, String body, boolean isHtml) {
        this.context = context;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.isHtml = isHtml;
    }
    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            MailSender.sendEmail(recipient, subject, body, isHtml);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if (success) {
        } else {
            Toast.makeText(context, "Ошибка при отправке письма", Toast.LENGTH_SHORT).show();
        }
    }
}