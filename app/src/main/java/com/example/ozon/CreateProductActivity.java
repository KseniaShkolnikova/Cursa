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
    private EditText productNameInput, productPriceInput, productDescriptionInput, productQuantityInput;
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
        productQuantityInput = view.findViewById(R.id.productQuantityInput);
        productImagePreview = view.findViewById(R.id.productImagePreview);
        productDescriptionInput = view.findViewById(R.id.productDescriptionInput);
        productTypeInput = view.findViewById(R.id.productTypeSpinner);
        Button selectImageButton = view.findViewById(R.id.selectImageButton);
        Button saveProductButton = view.findViewById(R.id.saveProductButton);
        selectImageButton.setOnClickListener(v -> openImagePicker());
        saveProductButton.setOnClickListener(v -> saveProduct());
        spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, productTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        productTypeInput.setAdapter(spinnerAdapter);
        loadProductTypes();
        return view;
    }

    private void loadProductTypes() {
        db.collection("productType")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        productTypes.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String typeName = document.getString("name");
                            if (typeName != null) {
                                productTypes.add(typeName);
                            }
                        }
                        spinnerAdapter.notifyDataSetChanged();
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
        String name = productNameInput.getText().toString().trim();
        String price = productPriceInput.getText().toString().trim();
        String quantity = productQuantityInput.getText().toString().trim();
        String type = productTypeInput.getSelectedItem() != null ? productTypeInput.getSelectedItem().toString() : "";
        String description = productDescriptionInput.getText().toString().trim();
        String imageBase64;
        if (!validateProductInput(name, price, quantity, type, description)) {
            return;
        }
        if (imageUri == null) {
            imageBase64 = drawableToBase64(R.drawable.no_photo);
        } else {
            imageBase64 = uriToBase64(imageUri);
        }
        if (imageBase64 == null) {
            Toast.makeText(getContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
            return;
        }
        int priceInt = Integer.parseInt(price);
        int quantityInt = Integer.parseInt(quantity);
        saveProductToFirestore(name, priceInt, type, imageBase64, description, quantityInt);
    }
    private boolean validateProductInput(String name, String price, String quantity, String type, String description) {
        if (name.length() < 4) {
            Toast.makeText(getContext(), "Название должно содержать минимум 4 символа", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (price.isEmpty()) {
            Toast.makeText(getContext(), "Введите цену", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            int priceInt = Integer.parseInt(price);
            if (priceInt < 1) {
                Toast.makeText(getContext(), "Цена должна быть не менее 1 рубля", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Цена должна быть числом", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (quantity.isEmpty()) {
            Toast.makeText(getContext(), "Введите количество", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            int quantityInt = Integer.parseInt(quantity);
            if (quantityInt < 1) {
                Toast.makeText(getContext(), "Количество должно быть не менее 1 штуки", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Количество должно быть числом", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (type.isEmpty() || productTypes.isEmpty()) {
            Toast.makeText(getContext(), "Выберите тип товара", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (description.length() < 10) {
            Toast.makeText(getContext(), "Описание должно содержать минимум 10 символов", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    private String drawableToBase64(int drawableResId) {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), drawableResId);
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
    private void saveProductToFirestore(String name, int price, String type, String imageBase64, String description, int quantity) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("price", price);
        product.put("productType", type);
        product.put("imageBase64", imageBase64);
        product.put("description", description);
        product.put("sellerId", userDocumentId);
        product.put("quantity", quantity);

        db.collection("products").add(product)
                .addOnSuccessListener(documentReference -> {
                    clearInputFields();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Ошибка при добавлении продукта", Toast.LENGTH_SHORT).show());
    }
    private void clearInputFields() {
        productNameInput.setText("");
        productPriceInput.setText("");
        productQuantityInput.setText("");
        productDescriptionInput.setText("");
        productTypeInput.setSelection(0);
        productImagePreview.setImageResource(R.drawable.no_photo);
        imageUri = null;
    }
}