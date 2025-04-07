package com.example.ozon;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
public class RegistrationActivity extends AppCompatActivity {
    private TextView loginLink;
    private TextView sellerAuthLink;
    private EditText loginField, passwordField, nameField;
    private Button registerButton;
    private FirebaseFirestore db;
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);
        loginField = findViewById(R.id.loginField);
        passwordField = findViewById(R.id.passwordField);
        registerButton = findViewById(R.id.registerButton);
        nameField = findViewById(R.id.nameField);
        loginLink = findViewById(R.id.loginLink);
        sellerAuthLink = findViewById(R.id.sellerAuthLink);
        db = FirebaseFirestore.getInstance();
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
            startActivity(intent);
        });
        sellerAuthLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegistrationActivity.this, AutorizationForSellerActivity.class);
            startActivity(intent);
        });
        registerButton.setOnClickListener(v -> {
            registerUser(loginField.getText().toString(), passwordField.getText().toString(), nameField.getText().toString());
        });
        TextView logoText = findViewById(R.id.logoText);
        applyTextGradient(logoText);
    }
    private void applyTextGradient(TextView textView) {
        textView.post(() -> {
            float width = textView.getPaint().measureText(textView.getText().toString());
            Shader textShader = new LinearGradient(
                    0, 0, width, textView.getTextSize(),
                    new int[]{
                            Color.parseColor("#2B67FF"),
                            Color.parseColor("#EC407A")
                    },
                    null, Shader.TileMode.CLAMP
            );
            textView.getPaint().setShader(textShader);
            textView.invalidate();
        });
    }
    private void registerUser(String login, String password, String name) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Заполните имя", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!validateLogin(login)) {
            return;
        }
        if (!validatePassword(password)) {
            return;
        }
        db.collection("users")
                .whereEqualTo("email", login)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            Toast.makeText(this, "Этот email уже зарегистрирован", Toast.LENGTH_SHORT).show();
                        } else {
                            performRegistration(login, password, name);
                        }
                    } else {
                        Toast.makeText(this, "Ошибка проверки email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private boolean validateLogin(String login) {
        if (login.isEmpty()) {
            Toast.makeText(this, "Заполните логин (email)", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (login.length() < 5) {
            Toast.makeText(this, "Email должен содержать минимум 8 символов", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(login).matches()){
            Toast.makeText(this, "Неверный формат почты", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            Toast.makeText(this, "Заполните пароль", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 7) {
            Toast.makeText(this, "Пароль должен содержать минимум 7 символов", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            Toast.makeText(this, "Пароль должен содержать хотя бы одну заглавную букву", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Pattern.compile("[a-z]").matcher(password).find()) {
            Toast.makeText(this, "Пароль должен содержать хотя бы одну строчную букву", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            Toast.makeText(this, "Пароль должен содержать хотя бы одну цифру", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            Toast.makeText(this, "Пароль должен содержать хотя бы один специальный символ (!@#$%^&*(),.?\":{}|<>)", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    private void performRegistration(String login, String password, String name) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", login);
        user.put("password", password);
        user.put("role", "customer");
        db.collection("users").add(user)
                .addOnSuccessListener(documentReference -> {
                    String userDocumentId = documentReference.getId();
                    saveUserAuthData(userDocumentId, "customer");
                    Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegistrationActivity.this, CustomerMainActivity.class);
                    intent.putExtra("USER_DOCUMENT_ID", userDocumentId);
                    intent.putExtra("USER_ROLE", "customer");
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка при регистрации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void saveUserAuthData(String userDocumentId, String userRole) {
        SharedPreferences sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userId", userDocumentId);
        editor.putString("userRole", userRole);
        editor.apply();
    }
}