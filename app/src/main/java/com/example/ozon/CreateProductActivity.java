package com.example.ozon;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateProductActivity extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText productNameInput, productPriceInput, productDescriptionInput;
    private ImageView productImagePreview;
    private Uri imageUri;
    private FirebaseFirestore db;
    private String userDocumentId;
    private Spinner productTypeInput;
    private ArrayAdapter<String> spinnerAdapter;
    private List<String> productTypes = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_product_layout, container, false);
        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("USER_DOCUMENT_ID");
        }
        db = FirebaseFirestore.getInstance();

        productNameInput = view.findViewById(R.id.productNameInput);
        productPriceInput = view.findViewById(R.id.productPriceInput);
        productImagePreview = view.findViewById(R.id.productImagePreview);
        productDescriptionInput = view.findViewById(R.id.productDescriptionInput);
        productTypeInput = view.findViewById(R.id.productTypeSpinner); // Инициализация Spinner

        Button selectImageButton = view.findViewById(R.id.selectImageButton);
        Button saveProductButton = view.findViewById(R.id.saveProductButton);
        selectImageButton.setOnClickListener(v -> openImagePicker());
        saveProductButton.setOnClickListener(v -> saveProduct());

        // Инициализация адаптера для Spinner
        spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, productTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        productTypeInput.setAdapter(spinnerAdapter);

        // Загрузка типов товаров из Firestore
        loadProductTypes();

        return view;
    }
    private void loadProductTypes() {
        db.collection("productType")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        productTypes.clear(); // Очистка списка перед загрузкой новых данных
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String typeName = document.getString("name"); // Предположим, что поле с названием типа называется "name"
                            if (typeName != null) {
                                productTypes.add(typeName);
                            }
                        }
                        spinnerAdapter.notifyDataSetChanged(); // Обновление адаптера
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки типов товаров", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
            imageUri = data.getData();
            productImagePreview.setImageURI(imageUri);
        }
    }

    private void saveProduct() {
        String name = productNameInput.getText().toString();
        String price = productPriceInput.getText().toString();
        String type = productTypeInput.getSelectedItem().toString(); // Получаем выбранный тип товара
        String description = productDescriptionInput.getText().toString();
        String imageBase64;

        // Проверка на заполнение обязательных полей
        if (name.isEmpty() || price.isEmpty() || type.isEmpty() || description.isEmpty()) {
            Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null) {
            // Преобразуем стандартное изображение в Base64
            imageBase64 = drawableToBase64(R.drawable.no_photo);
        } else {
            // Преобразуем выбранное изображение в Base64
            imageBase64 = uriToBase64(imageUri);
        }

        // Проверка на ошибку преобразования изображения
        if (imageBase64 == null) {
            Toast.makeText(getContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
            return;
        }

        // Сохраняем продукт в Firestore
        int price2 = Integer.parseInt(price);
        saveProductToFirestore(name, price2, type, imageBase64, description);
    }

    private String drawableToBase64(int drawableResId) {
        try {
            // Получаем Bitmap из ресурса drawable
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), drawableResId);

            // Преобразуем Bitmap в Base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String uriToBase64(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveProductToFirestore(String name, int price, String type, String imageBase64, String description) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("price", price);
        product.put("productType", type);
        product.put("imageBase64", imageBase64);
        product.put("description", description);
        product.put("sellerId", userDocumentId);

        db.collection("products").add(product)
                .addOnSuccessListener(documentReference -> {
                    String productId = documentReference.getId();
                    Toast.makeText(getContext(), "Продукт добавлен с ID: " + productId, Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Ошибка при добавлении продукта", Toast.LENGTH_SHORT).show());
    }
}