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

/**
 * Класс AutorizationForSellerActivity представляет собой активность для авторизации продавца
 * в приложении "OZON". Он предоставляет интерфейс для ввода логина и пароля,
 * проверки учетных данных через Firebase Firestore, а также функционал восстановления пароля
 * с отправкой кода подтверждения на email. Класс поддерживает переключение на авторизацию
 * покупателя и перенаправление авторизованного продавца на главный экран продавца (SellerMainActivity).
 */
public class AutorizationForSellerActivity extends AppCompatActivity {
    private TextView buyerAuthLink;
    private TextView forgotPasswordLink;
    private EditText loginField, passwordField;
    private FirebaseFirestore db;
    private SharedPreferences sharedPrefs;
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    /**
     * Инициализирует активность AutorizationForSellerActivity. Проверяет, авторизован ли пользователь,
     * и если да, перенаправляет его на главный экран продавца. Иначе загружает интерфейс авторизации,
     * настраивает элементы UI (поля ввода, ссылки, кнопки) и устанавливает обработчики событий для
     * перехода к авторизации покупателя, восстановления пароля и входа в систему.
     *
     * savedInstanceState Сохраненное состояние активности.
     */
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

    /**
     * Отображает диалоговое окно для восстановления пароля продавца. Позволяет пользователю ввести
     * email, отправить код подтверждения и ввести полученный код для перехода к смене пароля.
     * Проверяет существование пользователя в базе данных Firebase Firestore и активирует кнопку
     * смены пароля после успешной отправки кода.
     */
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

    /**
     * Отображает диалоговое окно для смены пароля продавца. Пользователь вводит новый пароль
     * и подтверждает его, после чего данные обновляются в базе Firebase Firestore. Метод вызывается
     * после успешной проверки кода подтверждения, отправленного на email.
     *
     *  email Email пользователя, для которого выполняется смена пароля.
     */
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
                                            Toast.makeText(this, "Ошибка при изменении пароля", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                        });
            }
        });

        dialog.show();
    }

    /**
     * Проверяет корректность нового пароля при его смене. Убеждается, что пароль и его подтверждение
     * совпадают, а также соответствуют требованиям: минимум 7 символов, наличие заглавной и строчной
     * букв, цифры и специального символа. Возвращает true, если пароль валиден, иначе отображает
     * сообщение об ошибке и возвращает false.
     *
     *  newPassword Новый пароль.
     *  confirmPassword Подтверждение нового пароля.
     * @return true, если пароль валиден, иначе false.
     */
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

    /**
     * Генерирует случайный пятизначный код подтверждения для восстановления пароля. Использует
     * класс Random для создания числа в диапазоне от 10000 до 99999. Возвращает код в виде строки.
     *
     * @return Сгенерированный код подтверждения.
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 10000 + random.nextInt(90000);
        return String.valueOf(code);
    }

    /**
     * Формирует и отправляет email с кодом подтверждения для восстановления пароля. Создает
     * HTML-шаблон письма с кодом, темой "Восстановление пароля Ozon" и информацией о сроке действия
     * кода (10 минут). Использует асинхронную задачу SendEmailTask для отправки письма.
     *
     *  email Email пользователя, на который отправляется код.
     *  code Код подтверждения для восстановления пароля.
     */
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

    /**
     * Обрабатывает процесс авторизации продавца. Проверяет введенные email и пароль, отправляет
     * запрос в Firebase Firestore для поиска пользователя с указанными данными и ролью "seller".
     * При успешной авторизации сохраняет данные пользователя в SharedPreferences и перенаправляет
     * на главный экран продавца (SellerMainActivity). Отображает индикатор загрузки и сообщения
     * об ошибках при необходимости.
     */
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
                                String sellerShop = userDoc.getString("shop");
                                String sellerOGRNIP = userDoc.getString("ogrnip");
                                String sellerINN = userDoc.getString("inn");
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
                            Toast.makeText(this, "Ошибка при авторизации", Toast.LENGTH_SHORT).show();
                        });
                    });
        }).start();
    }

    /**
     * Запускает главный экран продавца (SellerMainActivity) после успешной авторизации. Передает
     * идентификатор пользователя (userId) и роль ("seller") через Intent, завершая текущую активность.
     *
     *  userId Идентификатор пользователя в базе данных.
     */
    private void startSellerMainActivity(String userId) {
        Intent intent = new Intent(this, SellerMainActivity.class);
        intent.putExtra("USER_DOCUMENT_ID", userId);
        intent.putExtra("USER_ROLE", "seller");
        startActivity(intent);
        finish();
    }
}