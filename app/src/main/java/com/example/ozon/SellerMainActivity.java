package com.example.ozon;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Класс SellerMainActivity представляет собой основную активность для продавца
 * в приложении "OZON". Управляет навигацией между фрагментами
 * каталога, создания товара и профиля продавца через нижнюю панель навигации.
 */
public class SellerMainActivity extends AppCompatActivity {

    /**
     * Инициализирует активность. Устанавливает layout, извлекает данные о пользователе
     * из Intent, настраивает нижнюю панель навигации и отображает фрагмент каталога
     * по умолчанию при первом запуске.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.seller_layout);
        String userDocumentId = getIntent().getStringExtra("USER_DOCUMENT_ID");
        String userRole = getIntent().getStringExtra("USER_ROLE");
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            if (item.getItemId() == R.id.nav_catalog) {
                selectedFragment = new SellerCatalogActivity();
                Bundle bundle = new Bundle();
                bundle.putString("USER_DOCUMENT_ID", userDocumentId);
                bundle.putString("USER_ROLE", userRole);
                selectedFragment.setArguments(bundle);
            } else if (item.getItemId() == R.id.nav_cart) {
                selectedFragment = new CreateProductActivity();
                Bundle bundle = new Bundle();
                bundle.putString("USER_DOCUMENT_ID", userDocumentId);
                bundle.putString("USER_ROLE", userRole);
                selectedFragment.setArguments(bundle);
            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new SellerProfileActivity();
                Bundle bundle = new Bundle();
                bundle.putString("USER_DOCUMENT_ID", userDocumentId);
                bundle.putString("USER_ROLE", userRole);
                selectedFragment.setArguments(bundle);
            }
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frameLayoutseller, selectedFragment)
                        .commit();
            }
            return true;
        });
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_catalog);
        }
    }
}