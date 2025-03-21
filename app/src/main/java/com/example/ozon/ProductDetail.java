package com.example.ozon;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProductDetail extends Fragment {

    private ImageView productImage;
    private TextView productName, productPrice, productTypes, productDescription, storeNameTextView;
    private Button addToCartButton;
    private FirebaseFirestore db;
    private String documentId;
    private String imageBase64;
    private String name;
    private Long price;
    private String userDocumentId;
    private String sellerId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.product_detail_layout, container, false);

        productImage = view.findViewById(R.id.productImage);
        productName = view.findViewById(R.id.productName);
        productPrice = view.findViewById(R.id.productPrice);
        productTypes = view.findViewById(R.id.productType);
        productDescription = view.findViewById(R.id.productDescription);
        addToCartButton = view.findViewById(R.id.addToCartButton);
        storeNameTextView = view.findViewById(R.id.storeNameTextView);

        db = FirebaseFirestore.getInstance();

        Bundle args = getArguments();
        if (args != null) {
            documentId = args.getString("productId"); // Исправлено на "productId"
            userDocumentId = args.getString("userDocumentId");
        }

        if (documentId == null) {
            Toast.makeText(getContext(), "Document ID is null", Toast.LENGTH_SHORT).show();
        } else {
            loadProductDetails(documentId);
        }

        addToCartButton.setOnClickListener(v -> addToCart());

        return view;
    }

    private void loadProductDetails(String documentId) {
        db.collection("products")
                .document(documentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            name = document.getString("name");
                            price = document.getLong("price");
                            String description = document.getString("description");
                            imageBase64 = document.getString("imageBase64");
                            String productType = document.getString("productType");
                            sellerId = document.getString("sellerId");

                            productName.setText(name);
                            productPrice.setText(price + " рублей");
                            productDescription.setText("Описание: " + description);
                            productTypes.setText("Тип: " + productType);

                            if (imageBase64 != null && !imageBase64.isEmpty()) {
                                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                                Glide.with(getContext())
                                        .load(decodedString)
                                        .into(productImage);
                            }

                            if (sellerId != null) {
                                loadStoreName(sellerId);
                            } else {
                                storeNameTextView.setText("Магазин: Неизвестно");
                            }
                        } else {
                            Toast.makeText(getContext(), "Product not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error getting product", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadStoreName(String sellerId) {
        db.collection("users")
                .document(sellerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String storeName = document.getString("storeName");
                            if (storeName != null) {
                                storeNameTextView.setText("Магазин: " + storeName);
                            } else {
                                storeNameTextView.setText("Магазин: Неизвестно");
                            }
                        } else {
                            storeNameTextView.setText("Магазин: Неизвестно");
                        }
                    } else {
                        Toast.makeText(getContext(), "Error getting store name", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addToCart() {
        if (userDocumentId == null) {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("cart")
                .document(documentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            int currentQuantity = document.getLong("quantity").intValue();
                            FirebaseFirestore.getInstance().collection("cart")
                                    .document(documentId)
                                    .update("quantity", currentQuantity + 1)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Количество товара увеличено", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Ошибка при обновлении корзины", Toast.LENGTH_SHORT).show();
                                        Log.e("ProductDetail", "Ошибка при обновлении корзины", e);
                                    });
                        } else {
                            Cart cart = new Cart(documentId, name, price.intValue(), 1, imageBase64, userDocumentId);
                            FirebaseFirestore.getInstance().collection("cart")
                                    .document(documentId)
                                    .set(cart)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Товар добавлен в корзину", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Ошибка при добавлении в корзину", Toast.LENGTH_SHORT).show();
                                        Log.e("ProductDetail", "Ошибка при добавлении в корзину", e);
                                    });
                        }
                    } else {
                        Toast.makeText(getContext(), "Ошибка при проверке корзины", Toast.LENGTH_SHORT).show();
                        Log.e("ProductDetail", "Ошибка при проверке корзины", task.getException());
                    }
                });
    }
}