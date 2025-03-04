package com.example.ozon;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

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
        forgotPasswordLink.setOnClickListener(v -> {
            Intent intent = new Intent(AutorizationForSellerActivity.this, PasswordRecoveryActivity.class);
            startActivity(intent);
        });
        db = FirebaseFirestore.getInstance();

        loginField = findViewById(R.id.loginField);
        passwordField = findViewById(R.id.passwordField);
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
