package com.example.ozon;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private TextView registerLink;
    private TextView sellerAuthLink;
    private TextView forgotPasswordLink;
    private EditText loginField, passwordField;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        loginField = findViewById(R.id.loginField);
        passwordField = findViewById(R.id.passwordField);

        registerLink = findViewById(R.id.registerLink);
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegistrationActivity.class);
            startActivity(intent);
        });

        sellerAuthLink = findViewById(R.id.sellerAuthLink);
        sellerAuthLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AutorizationForSellerActivity.class);
            startActivity(intent);
        });

        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        forgotPasswordLink.setOnClickListener(v -> showForgotPasswordDialog());

        findViewById(R.id.loginButton).setOnClickListener(v -> loginUser());
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this); // Используем this как контекст
        View view = getLayoutInflater().inflate(R.layout.password_recovery, null);
        builder.setView(view);

        EditText emailField = view.findViewById(R.id.loginField);
        Button sendEmailButton = view.findViewById(R.id.sendCodeButton);

        AlertDialog dialog = builder.create();

        sendEmailButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(MainActivity.this, "Введите email", Toast.LENGTH_SHORT).show(); // Используем MainActivity.this
            } else {
                sendPasswordRecoveryEmail(email);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void sendPasswordRecoveryEmail(String email) {
        String subject = "Восстановление пароля";
        String body = "Для восстановления пароля перейдите по ссылке: ...";
        new SendEmailTask(email, subject, body).execute();
        Toast.makeText(MainActivity.this, "Сообщение отправлено на " + email, Toast.LENGTH_SHORT).show();
    }

    private void loginUser() {
        String email = loginField.getText().toString();
        String password = passwordField.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("password", password)
                .whereEqualTo("role", "customer")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show();
                    } else {
                        String userDocumentId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        Intent intent = new Intent(MainActivity.this, CustomerMainActivity.class);
                        intent.putExtra("USER_DOCUMENT_ID", userDocumentId);
                        intent.putExtra("USER_ROLE", "customer");
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка при авторизации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}