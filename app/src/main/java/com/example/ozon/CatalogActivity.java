package com.example.ozon;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
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
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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

    private static final String TAG = "CatalogActivity";
    private static final String PREF_MAX_PRICE = "max_price";
    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private FirebaseFirestore db;
    private EditText searchBar;
    private ImageButton searchButton;
    private Button filterButton;
    private String selectedCategory = "";
    private int maxPrice = 1000000;
    private int currentPrice;
    private boolean filterByPopularity = false;
    private String userDocumentId;
    private String userRole;
    private TextView emptyView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.catalog_layout, container, false);
        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerView);
        searchBar = view.findViewById(R.id.searchBar);
        searchButton = view.findViewById(R.id.searchButton);
        filterButton = view.findViewById(R.id.filterButton);
        emptyView = view.findViewById(R.id.emptyView);
        decreaseOrderDays("7sEQnIlfOhlJFX1V0ndg");


        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("USER_DOCUMENT_ID");
            userRole = bundle.getString("USER_ROLE");
        }

        productAdapter = new ProductAdapter(product -> {
            openProductDetail(product.getId(), userDocumentId);
        }, userDocumentId, userRole);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(productAdapter);

        loadMaxPriceFromDB(() -> {
            currentPrice = maxPrice;
            setupRecyclerView("", selectedCategory, 0, currentPrice, filterByPopularity, userRole);
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d(TAG, "beforeTextChanged: " + s.toString());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged: " + s.toString());
                setupRecyclerView(s.toString().trim(), selectedCategory, 0, currentPrice, filterByPopularity, userRole);
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, "afterTextChanged: " + s.toString());
            }
        });

        searchButton.setOnClickListener(v -> {
            setupRecyclerView(searchBar.getText().toString().trim(), selectedCategory, 0, currentPrice, filterByPopularity, userRole);
        });

        filterButton.setOnClickListener(v -> showFiltersDialog());

        return view;
    }

    private void decreaseOrderDays(String orderId) {
        db.collection("orders").document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long currentDays = documentSnapshot.getLong("days");
                        if (currentDays != null && currentDays > 0) {
                            long newDays = currentDays - 1;
                            db.collection("orders").document(orderId)
                                    .update("days", newDays)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Days decreased for order " + orderId + ". New value: " + newDays);
                                        if (newDays == 0) {
                                            db.collection("orders").document(orderId)
                                                    .update("status", "доставлен")
                                                    .addOnSuccessListener(aVoid2 -> {
                                                        Log.d(TAG, "Order " + orderId + " marked as delivered");
                                                    })
                                                    .addOnFailureListener(e -> Log.e(TAG, "Error updating status", e));
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Error updating days", e));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting order document", e));
    }





    private void loadMaxPriceFromDB(Runnable onComplete) {
        db.collection("products").orderBy("price", Query.Direction.DESCENDING).limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        maxPrice = document.getLong("price").intValue();
                        Log.d(TAG, "Max price loaded from DB: " + maxPrice);
                    } else {
                        Log.w(TAG, "No products found to determine max price. Using default.");
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load max price from DB", e);
                    onComplete.run();
                });
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

        loadMaxPriceFromDB(() -> {
            dialogPriceSeekBar.setMax(maxPrice);
            dialogPriceSeekBar.setProgress(currentPrice);
            dialogPriceRangeText.setText(currentPrice + " ₽");
            popularityCheckBox.setChecked(filterByPopularity);
            loadCategories(categorySpinner, selectedCategory);

            dialogPriceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    currentPrice = progress;
                    dialogPriceRangeText.setText(currentPrice + " ₽");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            applyFiltersButton.setOnClickListener(v -> {
                selectedCategory = categorySpinner.getSelectedItem().toString();
                filterByPopularity = popularityCheckBox.isChecked();
                setupRecyclerView(searchBar.getText().toString().trim(), selectedCategory, 0, currentPrice, filterByPopularity, userRole);
                dialog.dismiss();
            });

            resetFiltersButton.setOnClickListener(v -> {
                selectedCategory = "";
                filterByPopularity = false;
                loadMaxPriceFromDB(() -> {
                    dialogPriceSeekBar.setMax(maxPrice);
                    dialogPriceSeekBar.setProgress(maxPrice);
                    dialogPriceRangeText.setText(maxPrice + " ₽");
                    currentPrice = maxPrice;
                    setupRecyclerView("", selectedCategory, 0, currentPrice, filterByPopularity, userRole);
                    dialog.dismiss();
                });
            });

            dialog.show();
        });
    }

    private void loadCategories(Spinner categorySpinner, String selectedCategory) {
        List<String> categories = new ArrayList<>();
        categories.add("Все категории");

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

                    if (selectedCategory != null && !selectedCategory.isEmpty()) {
                        int position = categories.indexOf(selectedCategory);
                        if (position >= 0) {
                            categorySpinner.setSelection(position);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load categories", e));
    }

    private void setupRecyclerView(String searchText, String selectedCategory, int minPrice, int maxPrice, boolean filterByPopularity, String userRole) {
        Log.d(TAG, "setupRecyclerView called with: searchText=" + searchText + ", selectedCategory=" + selectedCategory + ", maxPrice=" + maxPrice + ", filterByPopularity=" + filterByPopularity);

        Query query = db.collection("products");

        if (maxPrice > 0) {
            query = query.whereLessThanOrEqualTo("price", maxPrice);
        }

        if (!searchText.isEmpty()) {
            query = query.whereGreaterThanOrEqualTo("name", searchText)
                    .whereLessThanOrEqualTo("name", searchText + "\uf8ff");
        }

        if (!selectedCategory.isEmpty() && !selectedCategory.equals("Все категории")) {
            query = query.whereEqualTo("productType", selectedCategory);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Product> products = new ArrayList<>();
            for (DocumentSnapshot document : queryDocumentSnapshots) {
                Product product = document.toObject(Product.class);
                if (product != null) {
                    product.setId(document.getId());
                    products.add(product);
                }
            }

            if (filterByPopularity) {
                fetchPopularityData(productPopularityMap -> {
                    products.sort((p1, p2) -> {
                        int popularity1 = productPopularityMap.getOrDefault(p1.getName(), 0);
                        int popularity2 = productPopularityMap.getOrDefault(p2.getName(), 0);
                        return Integer.compare(popularity2, popularity1);
                    });
                    productAdapter.updateData(products, productPopularityMap);
                });
            } else {
                productAdapter.updateData(products, null);
            }

            if (products.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading products", e));
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
                                String productName = (String) product.get("name");
                                int quantity = ((Long) product.get("quantity")).intValue();
                                productPopularityMap.put(productName, productPopularityMap.getOrDefault(productName, 0) + quantity);
                            }
                        }
                    }
                    listener.onPopularityDataFetched(productPopularityMap);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch popularity data", e));
    }

    interface OnPopularityDataFetchedListener {
        void onPopularityDataFetched(Map<String, Integer> productPopularityMap);
    }

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