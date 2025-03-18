package com.example.ozon;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;

public class AutorizationForSellerActivity extends AppCompatActivity {
    private TextView buyerAuthLink;
    private TextView forgotPasswordLink;
    private EditText loginField, passwordField;
    private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.autorization_for_seller);
        buyerAuthLink = findViewById(R.id.buyerAuthLink);
        buyerAuthLink.setOnClickListener(v -> {
            Intent intent = new Intent(AutorizationForSellerActivity.this, MainActivity.class);
            startActivity(intent);
        });
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        forgotPasswordLink.setOnClickListener(v -> showForgotPasswordDialog());

        db = FirebaseFirestore.getInstance();

        loginField = findViewById(R.id.loginField);
        passwordField = findViewById(R.id.passwordField);
        findViewById(R.id.loginButton).setOnClickListener(v -> loginUser());
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.password_recovery, null);
        builder.setView(view);

        EditText emailField = view.findViewById(R.id.loginField);
        EditText codeField = view.findViewById(R.id.codeField); // Поле для ввода кода
        Button sendCodeButton = view.findViewById(R.id.sendCodeButton); // Кнопка "Отправить код"
        Button changePasswordButton = view.findViewById(R.id.changePasswordButton); // Кнопка "Изменить пароль"

        // Изначально кнопка "Изменить пароль" заблокирована
        changePasswordButton.setEnabled(false);
        changePasswordButton.setAlpha(0.5f); // Визуально делаем кнопку полупрозрачной

        AlertDialog dialog = builder.create();

        // Генерация кода восстановления
        String[] generatedCode = {null}; // Массив для хранения сгенерированного кода

        sendCodeButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(AutorizationForSellerActivity.this, "Введите email", Toast.LENGTH_SHORT).show();
            } else {
                // Проверяем, существует ли пользователь с таким email и ролью seller
                db.collection("users")
                        .whereEqualTo("email", email)
                        .whereEqualTo("role", "seller")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (queryDocumentSnapshots.isEmpty()) {
                                Toast.makeText(AutorizationForSellerActivity.this, "Пользователь с таким email не найден или не является продавцом", Toast.LENGTH_SHORT).show();
                            } else {
                                // Генерация и отправка кода восстановления
                                generatedCode[0] = generateVerificationCode();
                                sendPasswordRecoveryEmail(email, generatedCode[0]);
                                Toast.makeText(AutorizationForSellerActivity.this, "Код отправлен на " + email, Toast.LENGTH_SHORT).show();

                                // Разблокируем кнопку "Изменить пароль"
                                changePasswordButton.setEnabled(true);
                                changePasswordButton.setAlpha(1.0f); // Визуально делаем кнопку активной
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AutorizationForSellerActivity.this, "Ошибка при проверке email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        changePasswordButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String enteredCode = codeField.getText().toString().trim();

            if (email.isEmpty() || enteredCode.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            } else if (generatedCode[0] == null) {
                Toast.makeText(this, "Сначала отправьте код", Toast.LENGTH_SHORT).show();
            } else if (!enteredCode.equals(generatedCode[0])) {
                Toast.makeText(this, "Неверный код", Toast.LENGTH_SHORT).show();
            } else {
                // Если код верный, открываем окно для смены пароля
                dialog.dismiss();
                showChangePasswordDialog(email);
            }
        });

        dialog.show();
    }

    private void showChangePasswordDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.change_password_layout, null);
        builder.setView(view);

        EditText newPasswordField = view.findViewById(R.id.newPasswordField);
        EditText confirmPasswordField = view.findViewById(R.id.confirmPasswordField);
        Button savePasswordButton = view.findViewById(R.id.savePasswordButton);

        AlertDialog dialog = builder.create();

        savePasswordButton.setOnClickListener(v -> {
            String newPassword = newPasswordField.getText().toString().trim();
            String confirmPassword = confirmPasswordField.getText().toString().trim();

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            } else if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            } else {
                // Обновляем пароль пользователя в Firestore, только если роль seller
                db.collection("users")
                        .whereEqualTo("email", email)
                        .whereEqualTo("role", "seller")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                String userDocumentId = queryDocumentSnapshots.getDocuments().get(0).getId();
                                db.collection("users")
                                        .document(userDocumentId)
                                        .update("password", newPassword)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Пароль успешно изменен", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Ошибка при изменении пароля: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Toast.makeText(this, "Пользователь не найден или не является продавцом", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Ошибка при поиске пользователя: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        dialog.show();
    }
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 10000 + random.nextInt(90000);
        return String.valueOf(code);
    }

    private void sendPasswordRecoveryEmail(String email, String code) {
        String subject = "Восстановление пароля";
        String body = "Код для восстановления пароля: " + code;
        new SendEmailTask(email, subject, body).execute();
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
                .whereEqualTo("role", "seller")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show();
                    } else {
                        String userDocumentId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        Intent intent = new Intent(AutorizationForSellerActivity.this, SellerMainActivity.class);
                        intent.putExtra("USER_DOCUMENT_ID", userDocumentId);
                        intent.putExtra("USER_ROLE", "seller"); // Передаем роль пользователя

                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка при авторизации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
