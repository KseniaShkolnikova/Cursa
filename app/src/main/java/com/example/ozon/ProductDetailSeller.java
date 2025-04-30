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
import java.util.Map;

/**
 * Класс ProductDetailSeller представляет собой фрагмент для отображения и редактирования
 * детальной информации о товаре продавцом в приложении "OZON". Позволяет
 * продавцу изменять информацию о товаре, пополнять количество, удалять товар и просматривать
 * выручку от продаж.
 */
public class ProductDetailSeller extends Fragment {
    private Product product;
    private ImageView productImage;
    private TextView productName, productPrice, productType, productDescription, productQuantity;
    private Button editProductButton;
    private TextView revenueTextView;
    private String userDocumentId;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;

    /**
     * Создает и возвращает представление фрагмента. Инициализирует элементы UI, загружает
     * данные о товаре из Firebase Firestore и настраивает обработчик для кнопки редактирования.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.product_detail_seller_layout, container, false);
        productImage = view.findViewById(R.id.productImage);
        productName = view.findViewById(R.id.productName);
        productPrice = view.findViewById(R.id.productPrice);
        productType = view.findViewById(R.id.productType);
        revenueTextView = view.findViewById(R.id.productRevenue);
        productDescription = view.findViewById(R.id.productDescription);
        productQuantity = view.findViewById(R.id.productQuantity);
        editProductButton = view.findViewById(R.id.editProductButton);
        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("userDocumentId");
            String productId = bundle.getString("productId");
            if (productId == null) {
                requireActivity().getSupportFragmentManager().popBackStack();
                return view;
            }
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
            requireActivity().getSupportFragmentManager().popBackStack();
        }
        editProductButton.setOnClickListener(v -> showOptionsMenu());
        return view;
    }

    /**
     * Вычисляет выручку от продаж товара. Анализирует все заказы в Firebase Firestore,
     * подсчитывает количество проданных единиц и общую сумму, затем обновляет UI.
     */
    private void calculateRevenue() {
        if (product == null) return;
        FirebaseFirestore.getInstance().collection("orders")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double totalRevenue = 0;
                        int totalQuantitySold = 0;
                        for (DocumentSnapshot order : task.getResult()) {
                            List<Map<String, Object>> products = (List<Map<String, Object>>) order.get("products");
                            if (products != null) {
                                for (Map<String, Object> productInOrder : products) {
                                    String productName = (String) productInOrder.get("name");
                                    Long priceLong = (Long) productInOrder.get("price");
                                    int price = priceLong != null ? priceLong.intValue() : 0;
                                    if (product.getName().equals(productName) && product.getPrice() == price) {
                                        Long quantityLong = (Long) productInOrder.get("quantity");
                                        int quantity = quantityLong != null ? quantityLong.intValue() : 0;
                                        totalRevenue += quantity * price;
                                        totalQuantitySold += quantity;
                                    }
                                }
                            }
                        }
                        revenueTextView.setText(String.format("Выручка: %.2f ₽ (продано %d шт.)", totalRevenue, totalQuantitySold));
                    } else {
                        revenueTextView.setText("Выручка: данные недоступны");
                    }
                });
    }

    /**
     * Обновляет элементы UI данными о товаре. Устанавливает название, цену, тип, описание,
     * количество и изображение товара, а также вызывает метод для расчета выручки.
     */
    private void updateUI() {
        if (product != null) {
            productName.setText(product.getName());
            productPrice.setText(String.format("%d ₽", product.getPrice()));
            productType.setText(String.format("Тип товара: %s", product.getProductType()));
            productDescription.setText(product.getDescription());
            productQuantity.setText(String.format("Количество: %d", product.getQuantity()));
            calculateRevenue();
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

    /**
     * Отображает меню с опциями для редактирования товара. Предлагает пользователю
     * пополнить количество, изменить информацию о товаре или удалить товар.
     */
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

    /**
     * Отображает диалоговое окно для пополнения количества товара. Позволяет продавцу
     * ввести количество для добавления и обновляет данные в Firebase Firestore.
     */
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
                if (addedQuantity < 1) {
                    Toast.makeText(requireContext(), "Количество должно быть не менее 1", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateProductQuantity(addedQuantity);
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Введите количество", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    /**
     * Обновляет количество товара в Firebase Firestore. Увеличивает количество на указанное
     * значение и обновляет UI после успешного обновления.
     */
    private void updateProductQuantity(int addedQuantity) {
        int newQuantity = product.getQuantity() + addedQuantity;
        FirebaseFirestore.getInstance().collection("products")
                .document(product.getId())
                .update("quantity", newQuantity)
                .addOnSuccessListener(aVoid -> {
                    product.setQuantity(newQuantity);
                    productQuantity.setText(String.format("Количество: %d", newQuantity));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при обновлении", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Отображает диалоговое окно для редактирования информации о товаре. Позволяет продавцу
     * изменить название, цену, описание, тип и изображение товара, а также удалить изображение.
     */
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
        Button removeImageButton = dialogView.findViewById(R.id.removeImageButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        if (editName == null || editPrice == null || editDescription == null || editTypeSpinner == null ||
                uploadImageButton == null || cancelButton == null || saveButton == null || removeImageButton == null) {
            Toast.makeText(requireContext(), "Ошибка интерфейса", Toast.LENGTH_SHORT).show();
            return;
        }
        loadProductTypes(editTypeSpinner);
        editName.setText(product != null ? product.getName() : "");
        editPrice.setText(product != null ? String.valueOf(product.getPrice()) : "");
        editDescription.setText(product != null ? product.getDescription() : "");
        AlertDialog dialog = builder.create();
        uploadImageButton.setOnClickListener(v -> openImagePicker());
        removeImageButton.setOnClickListener(v -> {
            imageUri = null;
            updateProductInfo(editName.getText().toString().trim(),
                    Integer.parseInt(editPrice.getText().toString().trim()),
                    editDescription.getText().toString().trim(),
                    editTypeSpinner.getSelectedItem().toString(),
                    null);
            dialog.dismiss();
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String priceStr = editPrice.getText().toString().trim();
            String description = editDescription.getText().toString().trim();
            Object selectedItem = editTypeSpinner.getSelectedItem();
            if (!validateProductInput(name, priceStr, description, selectedItem)) {
                return;
            }
            int price = Integer.parseInt(priceStr);
            String type = selectedItem.toString();
            updateProductInfo(name, price, description, type, imageUri);
            dialog.dismiss();
        });
        dialog.show();
    }

    /**
     * Проверяет корректность введенных данных о товаре. Убеждается, что название, цена,
     * описание и тип товара соответствуют заданным требованиям.
     */
    private boolean validateProductInput(String name, String priceStr, String description, Object selectedItem) {
        if (name.length() < 4) {
            Toast.makeText(requireContext(), "Название должно содержать минимум 4 символа", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (priceStr.isEmpty()) {
            Toast.makeText(requireContext(), "Введите цену", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            int price = Integer.parseInt(priceStr);
            if (price < 1) {
                Toast.makeText(requireContext(), "Цена должна быть не менее 1 рубля", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Цена должна быть числом", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (description.length() < 10) {
            Toast.makeText(requireContext(), "Описание должно содержать минимум 10 символов", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedItem == null) {
            Toast.makeText(requireContext(), "Выберите категорию товара", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Загружает список типов товаров из Firebase Firestore и заполняет выпадающий список.
     * Устанавливает текущий тип товара, если он уже задан.
     */
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

    /**
     * Обновляет информацию о товаре в Firebase Firestore. Сохраняет новые данные о товаре,
     * включая название, цену, описание, тип и изображение, и обновляет UI после успешного сохранения.
     */
    private void updateProductInfo(String name, int price, String description, String type, Uri imageUri) {
        String imageBase64 = product.getImageBase64();
        if (imageUri != null) {
            imageBase64 = handleImage(imageUri);
        } else if (imageUri == null && product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
            imageBase64 = null;
        }
        if (imageBase64 == null && imageUri != null) {
            Toast.makeText(requireContext(), "Ошибка обработки изображения", Toast.LENGTH_SHORT).show();
            return;
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
                    product.setName(name);
                    product.setPrice(price);
                    product.setDescription(description);
                    product.setProductType(type);
                    product.setImageBase64(finalImageBase64);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при обновлении", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Обрабатывает выбранное изображение. Преобразует изображение в формат Base64 после
     * изменения размера и сжатия.
     */
    private String handleImage(Uri imageUri) {
        try {
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
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

    /**
     * Изменяет размер изображения, сохраняя пропорции, чтобы ширина и высота не превышали
     * заданных максимальных значений.
     */
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

    /**
     * Удаляет товар из Firebase Firestore. После успешного удаления возвращает пользователя
     * на предыдущий экран.
     */
    private void deleteProduct() {
        FirebaseFirestore.getInstance().collection("products")
                .document(product.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Преобразует строку Base64 в объект Bitmap. Используется для отображения изображения товара.
     */
    private Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Открывает системный выбор изображения для загрузки нового изображения товара.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * Обрабатывает результат выбора изображения. Загружает выбранное изображение в UI
     * с помощью Glide.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(productImage);
        }
    }
}