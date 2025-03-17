package com.example.ozon;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SellerMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.seller_layout); // Используем seller_layout для продавца

        // Получаем userDocumentId и userRole из Intent
        String userDocumentId = getIntent().getStringExtra("USER_DOCUMENT_ID");
        String userRole = getIntent().getStringExtra("USER_ROLE"); // Получаем роль пользователя

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_catalog) {
                selectedFragment = new SellerCatalogActivity();
                Bundle bundle = new Bundle();
                bundle.putString("USER_DOCUMENT_ID", userDocumentId);
                bundle.putString("USER_ROLE", userRole); // Передаем роль пользователя
                selectedFragment.setArguments(bundle);
            } else if (item.getItemId() == R.id.nav_cart) {
                selectedFragment = new CreateProductActivity();
                Bundle bundle = new Bundle();
                bundle.putString("USER_DOCUMENT_ID", userDocumentId);
                bundle.putString("USER_ROLE", userRole); // Передаем роль пользователя
                selectedFragment.setArguments(bundle);
            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new SellerProfileActivity();
                Bundle bundle = new Bundle();
                bundle.putString("USER_DOCUMENT_ID", userDocumentId);
                bundle.putString("USER_ROLE", userRole); // Передаем роль пользователя
                selectedFragment.setArguments(bundle);
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frameLayout, selectedFragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_catalog);
        }
    }
}