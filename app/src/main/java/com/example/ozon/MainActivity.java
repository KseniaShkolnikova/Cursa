package com.example.ozon;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private TextView registerLink;
    private TextView sellerAuthLink;
    private TextView forgotPasswordLink;
    private EditText loginField, passwordField;
    private FirebaseFirestore db;
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Проверяем авторизацию перед установкой layout
        sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (sharedPrefs.getBoolean("isLoggedIn", false)) {
            String userId = sharedPrefs.getString("userId", "");
            String userRole = sharedPrefs.getString("userRole", "");

            if ("customer".equals(userRole)) {
                startCustomerMainActivity(userId); // Передаем сохраненный ID
            }
            return; // Важно - завершаем метод
        }

        setContentView(R.layout.activity_main);
        initializeViews();
        setupClickListeners();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }

        recalculateAllOrders();
    }

    private void initializeViews() {
        db = FirebaseFirestore.getInstance();
        loginField = findViewById(R.id.loginField);
        passwordField = findViewById(R.id.passwordField);
        registerLink = findViewById(R.id.registerLink);
        sellerAuthLink = findViewById(R.id.sellerAuthLink);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
    }

    private void setupClickListeners() {
        registerLink.setOnClickListener(v -> startActivity(new Intent(this, RegistrationActivity.class)));
        sellerAuthLink.setOnClickListener(v -> startActivity(new Intent(this, AutorizationForSellerActivity.class)));
        forgotPasswordLink.setOnClickListener(v -> showForgotPasswordDialog());
        findViewById(R.id.loginButton).setOnClickListener(v -> loginUser());
    }

    private void requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на уведомления получено", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void recalculateAllOrders() {
        db.collection("orders")
                .whereEqualTo("status", "создан")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.google.firebase.Timestamp orderDate = doc.getTimestamp("orderDate");
                        Long daysToDelivery = doc.getLong("days");

                        if (orderDate != null && daysToDelivery != null) {
                            updateOrderStatus(doc.getId(), orderDate, daysToDelivery);
                        }
                    }
                });
    }

    private void updateOrderStatus(String orderId, com.google.firebase.Timestamp orderDate, long daysToDelivery) {
        long daysSinceOrder = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - orderDate.toDate().getTime());
        long remainingDays = daysToDelivery - daysSinceOrder;

        if (remainingDays <= 0) {
            db.collection("orders").document(orderId)
                    .update("status", "доставлен", "days", 0);
        } else {
            db.collection("orders").document(orderId)
                    .update("days", remainingDays);
        }
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.password_recovery, null);
        builder.setView(view);

        EditText emailField = view.findViewById(R.id.loginField);
        EditText codeField = view.findViewById(R.id.codeField);
        Button sendCodeButton = view.findViewById(R.id.sendCodeButton);
        Button changePasswordButton = view.findViewById(R.id.changePasswordButton);

        changePasswordButton.setEnabled(false);
        changePasswordButton.setAlpha(0.5f);

        AlertDialog dialog = builder.create();
        String[] generatedCode = {null};

        sendCodeButton.setOnClickListener(v -> handleSendCode(emailField, changePasswordButton, generatedCode));
        changePasswordButton.setOnClickListener(v -> handleChangePassword(emailField, codeField, generatedCode, dialog));

        dialog.show();
    }

    private void handleSendCode(EditText emailField, Button changePasswordButton, String[] generatedCode) {
        String email = emailField.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("role", "customer")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        generatedCode[0] = generateVerificationCode();
                        sendPasswordRecoveryEmail(email, generatedCode[0]);
                        Toast.makeText(this, "Код отправлен на " + email, Toast.LENGTH_SHORT).show();
                        changePasswordButton.setEnabled(true);
                        changePasswordButton.setAlpha(1.0f);
                    } else {
                        Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleChangePassword(EditText emailField, EditText codeField, String[] generatedCode, AlertDialog dialog) {
        String email = emailField.getText().toString().trim();
        String enteredCode = codeField.getText().toString().trim();

        if (email.isEmpty() || enteredCode.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
        } else if (!enteredCode.equals(generatedCode[0])) {
            Toast.makeText(this, "Неверный код", Toast.LENGTH_SHORT).show();
        } else {
            dialog.dismiss();
            showChangePasswordDialog(email);
        }
    }

    private void sendPasswordRecoveryEmail(String email, String code) {
        new SendEmailTask(email, "Восстановление пароля", "Код для восстановления пароля: " + code).execute();
    }

    private void showChangePasswordDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.change_password_layout, null);
        builder.setView(view);

        EditText newPasswordField = view.findViewById(R.id.newPasswordField);
        EditText confirmPasswordField = view.findViewById(R.id.confirmPasswordField);
        Button savePasswordButton = view.findViewById(R.id.savePasswordButton);

        AlertDialog dialog = builder.create();

        savePasswordButton.setOnClickListener(v -> updatePassword(email, newPasswordField, confirmPasswordField, dialog));

        dialog.show();
    }

    private void updatePassword(String email, EditText newPasswordField, EditText confirmPasswordField, AlertDialog dialog) {
        String newPassword = newPasswordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
        } else if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
        } else {
            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            queryDocumentSnapshots.getDocuments().get(0).getReference()
                                    .update("password", newPassword)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Пароль изменен", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    });
                        }
                    });
        }
    }

    private String generateVerificationCode() {
        return String.valueOf(10000 + new Random().nextInt(90000));
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
                        Toast.makeText(this, "Неверные данные", Toast.LENGTH_SHORT).show();
                    } else {
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        saveLoginData(userDoc.getId(), "customer"); // Сохраняем данные
                        startOrderCheckWorker(userDoc.getId());
                        startCustomerMainActivity(userDoc.getId()); // Передаем ID
                    }
                });
    }

    private void saveLoginData(String userId, String role) {
        sharedPrefs.edit()
                .putBoolean("isLoggedIn", true)
                .putString("userId", userId)
                .putString("userRole", role)
                .apply();
    }

    private void startOrderCheckWorker(String userId) {
        Data inputData = new Data.Builder().putString("userId", userId).build();
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                OrderCheckWorker.class, 24, TimeUnit.HOURS)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "orderCheckWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest);
    }

    private void startCustomerMainActivity(String userId) {
        Intent intent = new Intent(this, CustomerMainActivity.class);
        intent.putExtra("USER_DOCUMENT_ID", userId);
        intent.putExtra("USER_ROLE", "customer");
        startActivity(intent);
        finish(); // Закрываем MainActivity
    }

}