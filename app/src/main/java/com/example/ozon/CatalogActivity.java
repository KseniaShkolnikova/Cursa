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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private String userRole;
    private TextView emptyView; // TextView для отображения сообщения "Ничего не найдено"

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.catalog_layout, container, false);
        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerView);
        searchBar = view.findViewById(R.id.searchBar);
        searchButton = view.findViewById(R.id.searchButton);
        filterButton = view.findViewById(R.id.filterButton);
        emptyView = view.findViewById(R.id.emptyView);

        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("USER_DOCUMENT_ID");
            userRole = bundle.getString("USER_ROLE");
        }

        // Инициализация адаптера
        productAdapter = new ProductAdapter(product -> {
            openProductDetail(product.getId(), userDocumentId);
        }, userDocumentId, userRole);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(productAdapter);

        setupRecyclerView("", "", 0, maxPrice, false, userRole);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d("CatalogActivity", "beforeTextChanged: " + s.toString());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("CatalogActivity", "onTextChanged: " + s.toString());
                setupRecyclerView(s.toString().trim(), selectedCategory, 0, maxPrice, filterByPopularity, userRole);
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d("CatalogActivity", "afterTextChanged: " + s.toString());
            }
        });

        searchButton.setOnClickListener(v -> {
            setupRecyclerView(searchBar.getText().toString().trim(), selectedCategory, 0, maxPrice, filterByPopularity, userRole);
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

        loadCategories(categorySpinner);

        applyFiltersButton.setOnClickListener(v -> {
            selectedCategory = categorySpinner.getSelectedItem().toString();
            Log.d("CatalogActivity", "Выбрана категория: " + selectedCategory);
            filterByPopularity = popularityCheckBox.isChecked();
            setupRecyclerView(searchBar.getText().toString().trim(), selectedCategory, 0, maxPrice, filterByPopularity, userRole);
            dialog.dismiss();
        });

        resetFiltersButton.setOnClickListener(v -> {
            selectedCategory = "";
            filterByPopularity = false;

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
                        setupRecyclerView("", "", 0, maxPrice, false, userRole);
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Log.e("CatalogActivity", "Failed to load max price", e));
        });

        dialog.show();
    }

    private void loadCategories(Spinner categorySpinner) {
        List<String> categories = new ArrayList<>();
        categories.add("All Categories");

        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> uniqueCategories = new HashSet<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String productType = document.getString("productType"); // Используем поле "productType"
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

    private void setupRecyclerView(String searchText, String selectedCategory, int minPrice, int maxPrice, boolean filterByPopularity, String userRole) {
        Log.d("CatalogActivity", "setupRecyclerView вызван с параметрами:");
        Log.d("CatalogActivity", "searchText: " + searchText);
        Log.d("CatalogActivity", "selectedCategory: " + selectedCategory);
        Log.d("CatalogActivity", "maxPrice: " + maxPrice);
        Log.d("CatalogActivity", "filterByPopularity: " + filterByPopularity);

        Query query = db.collection("products");

        // Фильтр по цене
        if (maxPrice > 0) {
            query = query.whereLessThanOrEqualTo("price", maxPrice);
        }

        // Фильтр по поисковому запросу
        if (!searchText.isEmpty()) {
            query = query.whereGreaterThanOrEqualTo("name", searchText)
                    .whereLessThanOrEqualTo("name", searchText + "\uf8ff");
            Log.d("CatalogActivity", "Применен фильтр по поисковому запросу: " + searchText);
        }

        // Фильтр по категории
        if (!selectedCategory.isEmpty() && !selectedCategory.equals("All Categories")) {
            query = query.whereEqualTo("productType", selectedCategory); // Используем поле "productType"
            Log.d("CatalogActivity", "Применен фильтр по категории: " + selectedCategory);
        }

        // Загружаем данные вручную
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Product> products = new ArrayList<>();
            for (DocumentSnapshot document : queryDocumentSnapshots) {
                Product product = document.toObject(Product.class);
                if (product != null) {
                    product.setId(document.getId()); // Устанавливаем ID продукта
                    products.add(product);
                }
            }

            // Логируем количество загруженных товаров
            Log.d("CatalogActivity", "Загружено товаров: " + products.size());

            if (filterByPopularity) {
                // Загружаем данные о популярности и сортируем
                fetchPopularityData(productPopularityMap -> {
                    // Сортируем продукты по популярности
                    products.sort((p1, p2) -> {
                        int popularity1 = productPopularityMap.getOrDefault(p1.getName(), 0);
                        int popularity2 = productPopularityMap.getOrDefault(p2.getName(), 0);
                        return Integer.compare(popularity2, popularity1); // Сортировка по убыванию
                    });

                    // Обновляем адаптер с отсортированными данными
                    productAdapter.updateData(products, productPopularityMap);
                });
            } else {
                // Обновляем адаптер без сортировки
                productAdapter.updateData(products, null);
            }

            // Показываем или скрываем пустое состояние
            if (products.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            Log.e("CatalogActivity", "Ошибка при загрузке продуктов", e);
        });
    }

    private void fetchPopularityData(OnPopularityDataFetchedListener listener) {
        db.collection("orders")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> productPopularityMap = new HashMap<>();

                    for (DocumentSnapshot orderDocument : queryDocumentSnapshots) {
                        List<Map<String, Object>> products = (List<Map<String, Object>>) orderDocument.get("products");
                        if (products != null) {
                            for (Map<String, Object> product : products) {
                                String productName = (String) product.get("name"); // Используем name как идентификатор
                                int quantity = ((Long) product.get("quantity")).intValue();
                                productPopularityMap.put(productName, productPopularityMap.getOrDefault(productName, 0) + quantity);
                            }
                        }
                    }

                    listener.onPopularityDataFetched(productPopularityMap);
                })
                .addOnFailureListener(e -> Log.e("CatalogActivity", "Failed to fetch popularity data", e));
    }

    interface OnPopularityDataFetchedListener {
        void onPopularityDataFetched(Map<String, Integer> productPopularityMap);
    }

    // Метод для открытия ProductDetail фрагмента
    private void openProductDetail(String productId, String userDocumentId) {
        ProductDetail productDetailFragment = new ProductDetail();
        Bundle bundle = new Bundle();
        bundle.putString("productId", productId);
        bundle.putString("userDocumentId", userDocumentId);
        productDetailFragment.setArguments(bundle);

        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frameLayout, productDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}