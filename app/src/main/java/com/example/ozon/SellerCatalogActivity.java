package com.example.ozon;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import java.util.List;
public class SellerCatalogActivity extends Fragment {
    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private FirebaseFirestore db;
    private String userDocumentId;
    private String userRole;
    private TextView emptyView;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.catalog_seller_layout, container, false);
        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerView);
        if (recyclerView == null) {
            return view;
        }
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        emptyView = view.findViewById(R.id.emptyView);
        if (emptyView == null) {
            return view;
        }
        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("USER_DOCUMENT_ID");
            userRole = bundle.getString("USER_ROLE");
        } else {
            return view;
        }
        productAdapter = new ProductAdapter(product -> {
            openProductDetailSeller(product.getId(), userDocumentId, userRole);
        }, userDocumentId, userRole);
        recyclerView.setAdapter(productAdapter);
        loadProducts(userDocumentId);
        return view;
    }

    private void loadProducts(String sellerId) {
        if (db == null || sellerId == null) {
            return;
        }
        Query query = db.collection("products")
                .whereEqualTo("sellerId", sellerId);
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Product> products = new ArrayList<>();
            for (DocumentSnapshot document : queryDocumentSnapshots) {
                Product product = document.toObject(Product.class);
                if (product != null) {
                    product.setId(document.getId());
                    products.add(product);
                }
            }
            if (productAdapter != null) {
                productAdapter.updateData(products, null);
            }
            if (emptyView != null && recyclerView != null) {
                if (products.isEmpty()) {
                    emptyView.setText("У вас еще нет товаров");
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        }).addOnFailureListener(e -> {
        });
    }
    private void openProductDetailSeller(String productId, String userDocumentId, String userRole) {
        if (productId == null || userDocumentId == null || userRole == null) {
            return;
        }
        ProductDetailSeller productDetailSeller = new ProductDetailSeller();
        Bundle bundle = new Bundle();
        bundle.putString("productId", productId);
        bundle.putString("userDocumentId", userDocumentId);
        bundle.putString("userRole", userRole);
        productDetailSeller.setArguments(bundle);

        FragmentManager fragmentManager = getParentFragmentManager();
        if (fragmentManager != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.frameLayoutseller, productDetailSeller);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
}