package com.example.ozon;

import android.app.AlertDialog;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ProductDetailSeller extends Fragment {

    private Product product;
    private ImageView productImage;
    private TextView productName, productPrice, productType, productDescription, productQuantity;
    private Button editProductButton;
    private String userDocumentId;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.product_detail_seller_layout, container, false);

        productImage = view.findViewById(R.id.productImage);
        productName = view.findViewById(R.id.productName);
        productPrice = view.findViewById(R.id.productPrice);
        productType = view.findViewById(R.id.productType);
        productDescription = view.findViewById(R.id.productDescription);
        productQuantity = view.findViewById(R.id.productQuantity);
        editProductButton = view.findViewById(R.id.editProductButton);

        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("userDocumentId");
            String productId = bundle.getString("productId"); // Получаем ID товара

            if (productId == null) {
                Toast.makeText(requireContext(), "Ошибка: ID товара не найден", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack(); // Возвращаемся назад
                return view;
            }

            // Загружаем данные товара из Firestore
            FirebaseFirestore.getInstance().collection("products")
                    .document(productId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                product = document.toObject(Product.class);
                                if (product != null) {
                                    product.setId(document.getId());
                                    updateUI();
                                }
                            } else {
                                Toast.makeText(requireContext(), "Товар не найден", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(requireContext(), "Ошибка: данные не переданы", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack(); // Возвращаемся назад
        }

        editProductButton.setOnClickListener(v -> showOptionsMenu());

        return view;
    }

    private void updateUI() {
        if (product != null) {
            productName.setText(product.getName());
            productPrice.setText(String.format("%d ₽", product.getPrice()));
            productType.setText(String.format("Тип товара: %s", product.getProductType()));
            productDescription.setText(product.getDescription());
            productQuantity.setText(String.format("Количество: %d", product.getQuantity()));

            if (product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
                Bitmap bitmap = base64ToBitmap(product.getImageBase64());
                if (bitmap != null) {
                    Glide.with(this).load(bitmap).into(productImage);
                } else {
                    productImage.setImageResource(R.drawable.no_photo);
                }
            } else {
                productImage.setImageResource(R.drawable.no_photo);
            }
        }
    }

    private void showOptionsMenu() {
        String[] options = {"Пополнить количество", "Изменить информацию", "Удалить товар"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Выберите действие");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showAddQuantityDialog();
                    break;
                case 1:
                    showEditProductDialog();
                    break;
                case 2:
                    deleteProduct();
                    break;
            }
        });
        builder.show();
    }

    private void showAddQuantityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_quantity, null);
        builder.setView(dialogView);

        EditText quantityInput = dialogView.findViewById(R.id.quantityInput);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            String quantityStr = quantityInput.getText().toString();
            if (!quantityStr.isEmpty()) {
                int addedQuantity = Integer.parseInt(quantityStr);
                updateProductQuantity(addedQuantity);
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Введите количество", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void updateProductQuantity(int addedQuantity) {
        int newQuantity = product.getQuantity() + addedQuantity;
        FirebaseFirestore.getInstance().collection("products")
                .document(product.getId())
                .update("quantity", newQuantity)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Количество обновлено", Toast.LENGTH_SHORT).show();
                    product.setQuantity(newQuantity);
                    productQuantity.setText(String.format("Количество: %d", newQuantity));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при обновлении", Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_product, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.editName);
        EditText editPrice = dialogView.findViewById(R.id.editPrice);
        EditText editDescription = dialogView.findViewById(R.id.editDescription);
        Spinner editTypeSpinner = dialogView.findViewById(R.id.editTypeSpinner);
        Button uploadImageButton = dialogView.findViewById(R.id.uploadImageButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        loadProductTypes(editTypeSpinner);

        editName.setText(product.getName());
        editPrice.setText(String.valueOf(product.getPrice()));
        editDescription.setText(product.getDescription());

        uploadImageButton.setOnClickListener(v -> openImagePicker());

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString();
            String priceStr = editPrice.getText().toString();
            String description = editDescription.getText().toString();
            String type = editTypeSpinner.getSelectedItem().toString();

            if (!name.isEmpty() && !priceStr.isEmpty() && !description.isEmpty()) {
                int price = Integer.parseInt(priceStr);
                updateProductInfo(name, price, description, type, imageUri);
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void loadProductTypes(Spinner spinner) {
        FirebaseFirestore.getInstance().collection("productType")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> types = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            String typeName = document.getString("name");
                            if (typeName != null) {
                                types.add(typeName);
                            }
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, types);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(adapter);

                        if (product != null) {
                            int position = types.indexOf(product.getProductType());
                            if (position >= 0) {
                                spinner.setSelection(position);
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при загрузке типов товаров", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateProductInfo(String name, int price, String description, String type, Uri imageUri) {
        String imageBase64 = product.getImageBase64();

        if (imageUri != null) {
            imageBase64 = handleImage(imageUri);
            if (imageBase64 == null) {
                Toast.makeText(requireContext(), "Ошибка обработки изображения", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        final String finalImageBase64 = imageBase64;

        FirebaseFirestore.getInstance().collection("products")
                .document(product.getId())
                .update(
                        "name", name,
                        "price", price,
                        "description", description,
                        "productType", type,
                        "imageBase64", finalImageBase64
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Информация обновлена", Toast.LENGTH_SHORT).show();
                    if (finalImageBase64 != null) {
                        Bitmap bitmap = base64ToBitmap(finalImageBase64);
                        if (bitmap != null) {
                            Glide.with(this).load(bitmap).into(productImage);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при обновлении", Toast.LENGTH_SHORT).show();
                });
    }

    private String handleImage(Uri imageUri) {
        try {
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize and compress the image
            Bitmap resizedBitmap = resizeBitmap(bitmap, 800, 800);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;
        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
    }

    private void deleteProduct() {
        FirebaseFirestore.getInstance().collection("products")
                .document(product.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Товар удален", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                });
    }

    private Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
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

            // Отображаем выбранное изображение
            ImageView productImagePreview = getView().findViewById(R.id.productImagePreview);
            if (productImagePreview != null && imageUri != null) {
                productImagePreview.setImageURI(imageUri);
            }
        }
    }
}