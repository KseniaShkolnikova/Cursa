package com.example.ozon;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {
    private TextView loginLink;
    private TextView sellerAuthLink;
    private EditText loginField, passwordField, nameField;
    private Button registerButton;
    private FirebaseFirestore db;

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
    }

    private void registerUser(String login, String password, String name) {
        if (login.isEmpty()) {
            Toast.makeText(RegistrationActivity.this, "Заполните логин", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(RegistrationActivity.this, "Заполните пароль", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty()) {
            Toast.makeText(RegistrationActivity.this, "Заполните имя", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            Map<String, Object> user = new HashMap<>();
            user.put("name", name);
            user.put("email", login);
            user.put("password", password);
            user.put("role", "customer");

            db.collection("users").add(user)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Запись создана", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Запись не создана: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}