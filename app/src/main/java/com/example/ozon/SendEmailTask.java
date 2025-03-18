package com.example.ozon;

import android.os.AsyncTask;

public class SendEmailTask extends AsyncTask<Void, Void, Void> {
    private String recipient;
    private String subject;
    private String body;

    public SendEmailTask(String recipient, String subject, String body) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        MailSender.sendEmail(recipient, subject, body);
        return null;
    }
}