package com.example.ozon;

import android.content.Intent;
import android.os.Bundle;
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
        forgotPasswordLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PasswordRecoveryActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.loginButton).setOnClickListener(v -> loginUser());
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
                        // Получаем ID документа пользователя
                        String userDocumentId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        // Передаем userDocumentId в CustomerMainActivity
                        Intent intent = new Intent(MainActivity.this, CustomerMainActivity.class);
                        intent.putExtra("USER_DOCUMENT_ID", userDocumentId);
                        intent.putExtra("USER_ROLE", "customer"); // Передаем роль пользователя
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка при авторизации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
