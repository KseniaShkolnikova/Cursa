package com.example.ozon;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CatalogActivity extends Fragment {

    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private FirebaseFirestore db;
    private EditText searchBar;
    private ImageButton searchButton;
    private Button filterButton;
    private String selectedCategory = "";
    private int maxPrice = 100000000;
    private boolean filterByPopularity = false;
    private String userDocumentId;
    private String userRole; // Добавляем поле для роли пользователя

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.catalog_layout, container, false);
        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerView);
        searchBar = view.findViewById(R.id.searchBar);
        searchButton = view.findViewById(R.id.searchButton);
        filterButton = view.findViewById(R.id.filterButton);

        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("USER_DOCUMENT_ID");
            userRole = bundle.getString("USER_ROLE"); // Получаем роль пользователя
        }

        // Передаем userRole в loadProducts
        loadProducts("", "", 0, maxPrice, false, userRole);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadProducts(s.toString().trim(), selectedCategory, 0, maxPrice, filterByPopularity, userRole);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchButton.setOnClickListener(v -> {
            loadProducts(searchBar.getText().toString().trim(), selectedCategory, 0, maxPrice, filterByPopularity, userRole);
        });

        filterButton.setOnClickListener(v -> showFiltersDialog());

        return view;
    }

    private void showFiltersDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.filters_dialog, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        SeekBar dialogPriceSeekBar = dialogView.findViewById(R.id.priceSeekBar);
        TextView dialogPriceRangeText = dialogView.findViewById(R.id.priceRangeText);
        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        CheckBox popularityCheckBox = dialogView.findViewById(R.id.popularityCheckBox);
        Button applyFiltersButton = dialogView.findViewById(R.id.applyFiltersButton);
        Button resetFiltersButton = dialogView.findViewById(R.id.resetFiltersButton);

        // Загружаем максимальную цену
        db.collection("products").orderBy("price", Query.Direction.DESCENDING).limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        int maxPrice = document.getLong("price").intValue();
                        dialogPriceSeekBar.setMax(maxPrice);
                        dialogPriceSeekBar.setProgress(maxPrice);
                        dialogPriceRangeText.setText(maxPrice + " ₽");
                    }
                })
                .addOnFailureListener(e -> Log.e("CatalogActivity", "Failed to load max price", e));

        // Устанавливаем слушатель для SeekBar
        dialogPriceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                dialogPriceRangeText.setText(progress + " ₽");
                maxPrice = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Загружаем категории в Spinner
        loadCategories(categorySpinner);

        // Применяем фильтры
        applyFiltersButton.setOnClickListener(v -> {
            selectedCategory = categorySpinner.getSelectedItem().toString();
            filterByPopularity = popularityCheckBox.isChecked();
            loadProducts(searchBar.getText().toString().trim(), selectedCategory, 0, maxPrice, filterByPopularity, userRole);
            dialog.dismiss();
        });

        // Сбрасываем фильтры
        resetFiltersButton.setOnClickListener(v -> {
            selectedCategory = "";
            filterByPopularity = false;

            // Сбрасываем цену и перезагружаем товары
            db.collection("products").orderBy("price", Query.Direction.DESCENDING).limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                            int maxPrice = document.getLong("price").intValue();
                            dialogPriceSeekBar.setMax(maxPrice);
                            dialogPriceSeekBar.setProgress(maxPrice);
                            dialogPriceRangeText.setText(maxPrice + " ₽");
                        }
                        loadProducts("", "", 0, maxPrice, false, userRole);  // Перезагружаем товары без фильтров
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Log.e("CatalogActivity", "Failed to load max price", e));
        });

        dialog.show();
    }

    private void loadCategories(Spinner categorySpinner) {
        List<String> categories = new ArrayList<>();
        categories.add("All Categories"); // Добавляем опцию по умолчанию

        // Загружаем категории из Firestore
        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> uniqueCategories = new HashSet<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String productType = document.getString("productType");
                        if (productType != null && !productType.isEmpty()) {
                            uniqueCategories.add(productType);
                        }
                    }

                    categories.addAll(uniqueCategories);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categorySpinner.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Log.e("CatalogActivity", "Failed to load categories", e));
    }

    private void loadProducts(String searchText, String selectedCategory, int minPrice, int maxPrice, boolean filterByPopularity, String userRole) {
        Query query = db.collection("products");

        // Фильтруем по тексту поиска
        if (!searchText.isEmpty()) {
            query = query.orderBy("name")
                    .startAt(searchText)
                    .endAt(searchText + "\uf8ff");
        }

        // Фильтруем по категории
        if (!selectedCategory.isEmpty() && !selectedCategory.equals("All Categories")) {
            query = query.whereEqualTo("productType", selectedCategory);
        }

        // Фильтруем по популярности
        if (filterByPopularity) {
            query = query.orderBy("popularity", Query.Direction.DESCENDING);
        }

        // Фильтруем по цене
        if (maxPrice > 0) {
            query = query.orderBy("price")
                    .whereLessThanOrEqualTo("price", maxPrice);
        }

        // Создаем FirestoreRecyclerOptions
        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setQuery(query, Product.class)
                .build();

        // Останавливаем текущий слушатель
        if (productAdapter != null) {
            productAdapter.stopListening();
        }

        // Инициализируем или обновляем адаптер
        productAdapter = new ProductAdapter(options, product -> {
            // Обработка клика на товар
        }, userDocumentId, userRole); // Передаем userDocumentId и userRole

        // Устанавливаем новый LayoutManager и адаптер
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(productAdapter);

        // Запускаем слушатель
        productAdapter.startListening();
    }
}