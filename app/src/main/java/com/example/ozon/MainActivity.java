package com.example.ozon;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Random;
import java.util.regex.Pattern;
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView registerLink, sellerAuthLink, forgotPasswordLink;
    private EditText loginField, passwordField;
    private FirebaseFirestore db;
    private SharedPreferences sharedPrefs;
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationHelper.createNotificationChannel(this);
        sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (sharedPrefs.getBoolean("isLoggedIn", false)) {
            String userId = sharedPrefs.getString("userId", "");
            String userRole = sharedPrefs.getString("userRole", "");
            if (userId == null || userId.isEmpty()) {
                return;
            }
            if ("customer".equals(userRole)) {
                startCustomerMainActivity(userId);
            } else if ("seller".equals(userRole)) {
                Intent intent = new Intent(this, AutorizationForSellerActivity.class);
                intent.putExtra("USER_DOCUMENT_ID", userId);
                intent.putExtra("USER_ROLE", "seller");
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Неизвестная роль пользователя: " + userRole, Toast.LENGTH_LONG).show();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.clear();
                editor.apply();
            }
            return;
        }
        setContentView(R.layout.activity_main);
        TextView logoText = findViewById(R.id.logoText);
        applyTextGradient(logoText);
        initializeViews();
        setupClickListeners();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }
    }
    private void applyTextGradient(TextView textView) {
        textView.post(() -> {
            float width = textView.getPaint().measureText(textView.getText().toString());
            Shader textShader = new LinearGradient(
                    0, 0, width, textView.getTextSize(),
                    new int[]{Color.parseColor("#2B67FF"), Color.parseColor("#EC407A")},
                    null, Shader.TileMode.CLAMP
            );
            textView.getPaint().setShader(textShader);
            textView.invalidate();
        });
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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Уведомления отключены. Включите их в настройках.", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void loginUser() {
        String email = loginField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NetworkUtil.isNetworkAvailable(this)) {
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_LONG).show();
            return;
        }
        findViewById(R.id.loginButton).setEnabled(false);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        new Thread(() -> {
            db.collection("users")
                    .whereEqualTo("email", email)
                    .whereEqualTo("password", password)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        runOnUiThread(() -> {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            findViewById(R.id.loginButton).setEnabled(true);
                            if (queryDocumentSnapshots.isEmpty()) {
                                Toast.makeText(this, "Неверные данные", Toast.LENGTH_SHORT).show();
                            } else {
                                DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                                String role = userDoc.getString("role");
                                String userId = userDoc.getId();
                                saveLoginData(userId, role);
                                if ("customer".equals(role)) {
                                    startCustomerMainActivity(userId);
                                } else {
                                    Toast.makeText(this, "Неизвестная роль пользователя", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            findViewById(R.id.loginButton).setEnabled(true);
                            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
                        });
                    });
        }).start();
    }
    private void saveLoginData(String userId, String role) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userId", userId);
        editor.putString("userRole", role);
        boolean success = editor.commit();
        if (success) {
        } else {
            Toast.makeText(this, "Ошибка сохранения данных", Toast.LENGTH_SHORT).show();
        }
    }
    private void startCustomerMainActivity(String userId) {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID пользователя не определен", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, CustomerMainActivity.class);
        intent.putExtra("USER_DOCUMENT_ID", userId);
        intent.putExtra("USER_ROLE", "customer");
        startActivity(intent);
        finish();
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
                        changePasswordButton.setEnabled(true);
                        changePasswordButton.setAlpha(1.0f);
                    } else {
                        Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка при проверке email" , Toast.LENGTH_SHORT).show();
                });
    }
    private void handleChangePassword(EditText emailField, EditText codeField, String[] generatedCode, AlertDialog dialog) {
        String email = emailField.getText().toString().trim();
        String enteredCode = codeField.getText().toString().trim();
        if (email.isEmpty() || enteredCode.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
        } else if (generatedCode[0] == null) {
            Toast.makeText(this, "Сначала отправьте код", Toast.LENGTH_SHORT).show();
        } else if (!enteredCode.equals(generatedCode[0])) {
            Toast.makeText(this, "Неверный код", Toast.LENGTH_SHORT).show();
        } else {
            dialog.dismiss();
            showChangePasswordDialog(email);
        }
    }
    private void sendPasswordRecoveryEmail(String email, String code) {
        String subject = "Восстановление пароля Ozon";
        String body = "<!DOCTYPE html>" +
                "<html lang='ru'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Восстановление пароля</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;'>" +
                "<table role='presentation' cellpadding='0' cellspacing='0' style='width: 100%; max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);'>" +
                "<tr>" +
                "<td style='background-color: #005BFF; padding: 20px; text-align: center; border-top-left-radius: 8px; border-top-right-radius: 8px;'>" +
                "<h1 style='color: #ffffff; margin: 0; font-size: 24px;'>Ozon</h1>" +
                "<h2 style='color: #ffffff; margin: 5px 0 0; font-size: 18px;'>Восстановление пароля</h2>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='padding: 30px; text-align: center;'>" +
                "<h3 style='color: #001A34; font-size: 20px; margin: 0 0 10px;'>Ваш код для восстановления пароля</h3>" +
                "<p style='color: #666666; font-size: 16px; margin: 0 0 20px;'>Используйте этот код для сброса пароля в приложении Ozon:</p>" +
                "<div style='background-color: #E6F0FF; padding: 15px; border-radius: 5px; display: inline-block;'>" +
                "<span style='font-size: 24px; font-weight: bold; color: #005BFF; letter-spacing: 2px;'>" + code + "</span>" +
                "</div>" +
                "<p style='color: #666666; font-size: 14px; margin: 20px 0 0;'>Код действителен в течение 10 минут.</p>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='padding: 20px; text-align: center; background-color: #f9f9f9; border-bottom-left-radius: 8px; border-bottom-right-radius: 8px;'>" +
                "<p style='color: #999999; font-size: 12px; margin: 0;'>Если вы не запрашивали восстановление пароля, проигнорируйте это сообщение.</p>" +
                "<p style='color: #999999; font-size: 12px; margin: 5px 0 0;'>Свяжитесь с нами: <a href='mailto:support@ozon.ru' style='color: #005BFF; text-decoration: none;'>support@ozon.ru</a></p>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</body>" +
                "</html>";
        new SendEmailTask(this, email, subject, body, true).execute();
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
        if (validateNewPassword(newPassword, confirmPassword)) {
            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            queryDocumentSnapshots.getDocuments().get(0).getReference()
                                    .update("password", newPassword)
                                    .addOnSuccessListener(aVoid -> {
                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Ошибка изменения пароля: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                    });
        }
    }
    private boolean validateNewPassword(String newPassword, String confirmPassword) {
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (newPassword.length() < 7) {
            Toast.makeText(this, "Пароль должен содержать минимум 7 символов", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Pattern.compile("[A-Z]").matcher(newPassword).find()) {
            Toast.makeText(this, "Пароль должен содержать хотя бы одну заглавную букву", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Pattern.compile("[a-z]").matcher(newPassword).find()) {
            Toast.makeText(this, "Пароль должен содержать хотя бы одну строчную букву", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Pattern.compile("[0-9]").matcher(newPassword).find()) {
            Toast.makeText(this, "Пароль должен содержать хотя бы одну цифру", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(newPassword).find()) {
            Toast.makeText(this, "Пароль должен содержать хотя бы один специальный символ (!@#$%^&*(),.?\":{}|<>)", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    private String generateVerificationCode() {
        return String.valueOf(10000 + new Random().nextInt(90000));
    }
}