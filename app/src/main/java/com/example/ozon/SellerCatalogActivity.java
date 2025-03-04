package com.example.ozon;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class SellerCatalogActivity extends Fragment {

    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private FirebaseFirestore db;
    private String userDocumentId;
    private String userRole; // Добавляем поле для роли пользователя

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.catalog_seller_layout, container, false);
        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerView);

        // Получаем ID пользователя и его роль из аргументов
        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("USER_DOCUMENT_ID");
            userRole = bundle.getString("USER_ROLE"); // Получаем роль пользователя
        }

        // Загружаем товары, принадлежащие текущему пользователю (продавцу)
        loadProducts(userDocumentId, userRole); // Передаем userRole

        return view;
    }

    private void loadProducts(String sellerId, String userRole) {
        // Создаем запрос к Firestore, чтобы получить только товары с sellerId, равным ID пользователя
        Query query = db.collection("products")
                .whereEqualTo("sellerId", sellerId);

        // Создаем FirestoreRecyclerOptions
        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setQuery(query, Product.class)
                .build();

        // Останавливаем текущий слушатель, если он был
        if (productAdapter != null) {
            productAdapter.stopListening();
        }

        // Инициализируем адаптер с передачей userRole
        productAdapter = new ProductAdapter(options, product -> {
            // Обработка клика на товар (если нужно)
        }, userDocumentId, userRole); // Передаем userRole

        // Устанавливаем LayoutManager и адаптер
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(productAdapter);

        // Запускаем слушатель
        productAdapter.startListening();
    }
}