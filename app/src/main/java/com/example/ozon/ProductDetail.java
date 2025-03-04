package com.example.ozon;

import android.os.Bundle;
import android.util.Base64;
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
    private TextView productName, productPrice, productTypes, productDescription;
    private Button addToCartButton;  // Add this line
    private FirebaseFirestore db;
    private String documentId; // Store the documentId here
    private String imageBase64; // Store imageBase64
    private String name;       // Store the product name
    private Long price; // Store the price
    private String userDocumentId;  // Store userDocumentId
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.product_detail_layout, container, false);

        productImage = view.findViewById(R.id.productImage);
        productName = view.findViewById(R.id.productName);
        productPrice = view.findViewById(R.id.productPrice);
        productTypes = view.findViewById(R.id.productType);
        productDescription = view.findViewById(R.id.productDescription);
        addToCartButton = view.findViewById(R.id.addToCartButton); // Initialize the button

        db = FirebaseFirestore.getInstance();

        Bundle args = getArguments();

        if (args != null){
            documentId = args.getString("document_id");
            userDocumentId = args.getString("userDocumentId");
        }

        if (documentId == null) {
            Toast.makeText(getContext(), "Document ID is null", Toast.LENGTH_SHORT).show();
        } else {
            loadProductDetails(documentId); // Загружаем детали продукта
        }

        // Set onClickListener for add to cart button.
        addToCartButton.setOnClickListener(v -> addToCart());

        return view;
    }

    private void loadProductDetails(String documentId) {
        db.collection("products")
                .document(documentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            // Извлекаем данные из документа
                            name = document.getString("name");
                            price = document.getLong("price"); // Получаем price как Long
                            String description = document.getString("description");
                            imageBase64 = document.getString("imageBase64");
                            String productType = document.getString("productType");

                            // Устанавливаем данные в UI
                            productName.setText(name);
                            productPrice.setText(price + " рублей"); // Преобразуем Long в строку
                            productDescription.setText("Описание: " + description);
                            productTypes.setText("Тип: " + productType);

                            // Загружаем изображение с помощью Glide, если оно есть
                            if (imageBase64 != null && !imageBase64.isEmpty()) {
                                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                                Glide.with(getContext())
                                        .load(decodedString)
                                        .into(productImage);
                            }
                        } else {
                            Toast.makeText(getContext(), "Product not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error getting product", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addToCart() {

        if (userDocumentId == null){
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Cart cart = new Cart(documentId, name, price.intValue(), 1, imageBase64, userDocumentId);

        FirebaseFirestore.getInstance().collection("cart")
                .document(documentId)
                .set(cart)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Товар добавлен в корзину", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка при добавлении в корзину", Toast.LENGTH_SHORT).show();
                });
    }
}