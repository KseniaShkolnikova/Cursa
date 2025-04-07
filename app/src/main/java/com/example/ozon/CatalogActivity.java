package com.example.ozon;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
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
    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private FirebaseFirestore db;
    private EditText searchBar;
    private MaterialButton searchButton;
    private MaterialButton filterButton;
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
        searchBar = view.findViewById(R.id.searchBar);
        searchButton = view.findViewById(R.id.searchButton);
        filterButton = view.findViewById(R.id.filterButton);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
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
            performSearch("");
        });
        setupSearch();
        setupFilters();
        return view;
    }
    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        searchButton.setOnClickListener(v -> {
            performSearch(searchBar.getText().toString());
            hideKeyboard();
        });
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchBar.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });
    }
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
    }
    private void setupFilters() {
        filterButton.setOnClickListener(v -> showFiltersDialog());
    }
    private void performSearch(String query) {
        setupRecyclerView(query, selectedCategory, currentPrice, filterByPopularity);
    }
    private void loadMaxPriceFromDB(Runnable onComplete) {
        db.collection("products").orderBy("price", Query.Direction.DESCENDING).limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        maxPrice = document.getLong("price").intValue();
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    onComplete.run();
                });
    }
    private void showFiltersDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.filters_dialog, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        SeekBar priceSeekBar = dialogView.findViewById(R.id.priceSeekBar);
        TextView priceRangeText = dialogView.findViewById(R.id.priceRangeText);
        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        CheckBox popularityCheckBox = dialogView.findViewById(R.id.popularityCheckBox);
        Button applyFiltersButton = dialogView.findViewById(R.id.applyFiltersButton);
        Button resetFiltersButton = dialogView.findViewById(R.id.resetFiltersButton);
        priceSeekBar.setMax(maxPrice);
        priceSeekBar.setProgress(currentPrice);
        priceRangeText.setText(currentPrice + " ₽");
        popularityCheckBox.setChecked(filterByPopularity);
        loadCategories(categorySpinner, selectedCategory);
        priceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentPrice = progress;
                priceRangeText.setText(currentPrice + " ₽");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        applyFiltersButton.setOnClickListener(v -> {
            selectedCategory = categorySpinner.getSelectedItem().toString();
            filterByPopularity = popularityCheckBox.isChecked();
            performSearch(searchBar.getText().toString());
            dialog.dismiss();
        });
        resetFiltersButton.setOnClickListener(v -> {
            selectedCategory = "";
            filterByPopularity = false;
            currentPrice = maxPrice;
            priceSeekBar.setProgress(maxPrice);
            priceRangeText.setText(maxPrice + " ₽");
            categorySpinner.setSelection(0);
            popularityCheckBox.setChecked(false);
            performSearch(searchBar.getText().toString());
        });
        dialog.show();
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
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_spinner_item,
                            categories
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categorySpinner.setAdapter(adapter);
                    if (!selectedCategory.isEmpty()) {
                        int position = categories.indexOf(selectedCategory);
                        if (position >= 0) {
                            categorySpinner.setSelection(position);
                        }
                    }
                });
    }
    private void setupRecyclerView(String searchText, String selectedCategory,
                                    int maxPrice,
                                   boolean filterByPopularity) {
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
            if (products.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
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
            }
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
                                String productName = (String) product.get("name");
                                int quantity = ((Long) product.get("quantity")).intValue();
                                productPopularityMap.put(productName, productPopularityMap.getOrDefault(productName, 0) + quantity);
                            }
                        }
                    }
                    listener.onPopularityDataFetched(productPopularityMap);
                });
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
    interface OnPopularityDataFetchedListener {
        void onPopularityDataFetched(Map<String, Integer> productPopularityMap);
    }
}