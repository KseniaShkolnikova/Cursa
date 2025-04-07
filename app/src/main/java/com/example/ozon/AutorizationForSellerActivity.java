package com.example.ozon;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Random;
import java.util.regex.Pattern;
public class AutorizationForSellerActivity extends AppCompatActivity {
    private TextView buyerAuthLink;
    private TextView forgotPasswordLink;
    private EditText loginField, passwordField;
    private FirebaseFirestore db;
    private SharedPreferences sharedPrefs;
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (sharedPrefs.getBoolean("isLoggedIn", false)) {
            String userId = sharedPrefs.getString("userId", "");
            String userRole = sharedPrefs.getString("userRole", "");
            if ("seller".equals(userRole)) {
                startSellerMainActivity(userId);
            }
            return;
        }
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
        EditText codeField = view.findViewById(R.id.codeField);
        Button sendCodeButton = view.findViewById(R.id.sendCodeButton);
        Button changePasswordButton = view.findViewById(R.id.changePasswordButton);
        changePasswordButton.setEnabled(false);
        changePasswordButton.setAlpha(0.5f);
        AlertDialog dialog = builder.create();
        String[] generatedCode = {null};
        sendCodeButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(AutorizationForSellerActivity.this, "Введите email", Toast.LENGTH_SHORT).show();
            } else {
                db.collection("users")
                        .whereEqualTo("email", email)
                        .whereEqualTo("role", "seller")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (queryDocumentSnapshots.isEmpty()) {
                                Toast.makeText(AutorizationForSellerActivity.this, "Пользователь с таким email не найден или не является продавцом", Toast.LENGTH_SHORT).show();
                            } else {
                                generatedCode[0] = generateVerificationCode();
                                sendPasswordRecoveryEmail(email, generatedCode[0]);
                                changePasswordButton.setEnabled(true);
                                changePasswordButton.setAlpha(1.0f);
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
            if (validateNewPassword(newPassword, confirmPassword)) {
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
                        });
            }
        });

        dialog.show();
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
        Random random = new Random();
        int code = 10000 + random.nextInt(90000);
        return String.valueOf(code);
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
    private void loginUser() {
        String email = loginField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
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
                    .whereEqualTo("role", "seller")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        runOnUiThread(() -> {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            findViewById(R.id.loginButton).setEnabled(true);
                            if (queryDocumentSnapshots.isEmpty()) {
                                Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show();
                            } else {
                                DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                                String userDocumentId = userDoc.getId();
                                String sellerName = userDoc.getString("name");
                                String sellerShop = userDoc.getString("shop") ;
                                String sellerOGRNIP = userDoc.getString("ogrnip") ;
                                String sellerINN = userDoc.getString("inn") ;
                                SharedPreferences.Editor editor = sharedPrefs.edit();
                                editor.putBoolean("isLoggedIn", true);
                                editor.putString("userId", userDocumentId);
                                editor.putString("userRole", "seller");
                                editor.putString("sellerName", sellerName);
                                editor.putString("sellerLogin", email);
                                editor.putString("sellerShop", sellerShop);
                                editor.putString("sellerOGRNIP", sellerOGRNIP);
                                editor.putString("sellerINN", sellerINN);
                                boolean success = editor.commit();
                                if (success) {
                                    startSellerMainActivity(userDocumentId);
                                } else {
                                    Toast.makeText(this, "Ошибка сохранения данных", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(this, "Ошибка при авторизации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    });
        }).start();
    }
    private void startSellerMainActivity(String userId) {
        Intent intent = new Intent(this, SellerMainActivity.class);
        intent.putExtra("USER_DOCUMENT_ID", userId);
        intent.putExtra("USER_ROLE", "seller");
        startActivity(intent);
        finish();
    }
}