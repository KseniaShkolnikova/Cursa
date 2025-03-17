package com.example.ozon;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> products = new ArrayList<>();
    private OnItemClickListener listener;
    private String userDocumentId;
    private String userRole;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public ProductAdapter(OnItemClickListener listener, String userDocumentId, String userRole) {
        this.listener = listener;
        this.userDocumentId = userDocumentId;
        this.userRole = userRole;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Выбор макета в зависимости от роли пользователя
        int layoutResId = userRole.equals("seller") ? R.layout.item_product_seller : R.layout.item_ptoduct;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.productName.setText(product.getName());
        holder.productPrice.setText(String.valueOf(product.getPrice()));

        // Загрузка изображения товара
        if (product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
            Bitmap bitmap = base64ToBitmap(product.getImageBase64());
            if (bitmap != null) {
                holder.productImage.setImageBitmap(bitmap);
            } else {
                holder.productImage.setImageResource(R.drawable.no_photo);
            }
        } else {
            holder.productImage.setImageResource(R.drawable.no_photo);
        }

        // Обработка клика на товар
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(product);
            }
        });

        // Если это продавец, добавляем кнопки редактирования и удаления
        if (userRole.equals("seller")) {
            if (holder.editButton != null) {
                holder.editButton.setOnClickListener(v -> {
                    editProduct(product, holder);
                });
            }

            if (holder.deleteButton != null) {
                holder.deleteButton.setOnClickListener(v -> {
                    deleteProduct(product, holder);
                });
            }
        }

        // Обработка клика на кнопку "Добавить в корзину"
        if (holder.addToCartButton != null) {
            holder.addToCartButton.setOnClickListener(v -> {
                addToCart(product, holder);
            });
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    // Метод для обновления данных в адаптере
    public void updateData(List<Product> newProducts, Map<String, Integer> productPopularityMap) {
        this.products.clear();
        this.products.addAll(newProducts);

        // Сортировка по популярности (если данные о популярности переданы)
        if (productPopularityMap != null) {
            products.sort((p1, p2) -> {
                int popularity1 = productPopularityMap.getOrDefault(p1.getName(), 0);
                int popularity2 = productPopularityMap.getOrDefault(p2.getName(), 0);
                return Integer.compare(popularity2, popularity1); // Сортировка по убыванию
            });
        }

        notifyDataSetChanged(); // Уведомляем адаптер об изменении данных
    }

    // Метод для добавления товара в корзину
    private void addToCart(Product product, ProductViewHolder holder) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String productId = product.getId();

        // Проверяем, существует ли товар в корзине
        db.collection("cart")
                .document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Товар уже есть в корзине, увеличиваем quantity на 1
                        Cart existingCartItem = documentSnapshot.toObject(Cart.class);
                        if (existingCartItem != null) {
                            existingCartItem.setQuantity(existingCartItem.getQuantity() + 1);

                            // Обновляем документ в Firestore
                            db.collection("cart")
                                    .document(productId)
                                    .set(existingCartItem)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(holder.itemView.getContext(), "Количество товара увеличено", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(holder.itemView.getContext(), "Ошибка при обновлении корзины", Toast.LENGTH_SHORT).show();
                                        Log.e("ProductAdapter", "Ошибка при обновлении корзины", e);
                                    });
                        }
                    } else {
                        // Товара нет в корзине, добавляем новый
                        Cart cartItem = new Cart(
                                productId,
                                product.getName(),
                                product.getPrice(),
                                1, // Количество по умолчанию
                                product.getImageBase64(),
                                userDocumentId
                        );

                        db.collection("cart")
                                .document(productId)
                                .set(cartItem)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(holder.itemView.getContext(), "Товар добавлен в корзину", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(holder.itemView.getContext(), "Ошибка при добавлении в корзину", Toast.LENGTH_SHORT).show();
                                    Log.e("ProductAdapter", "Ошибка при добавлении в корзину", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(holder.itemView.getContext(), "Ошибка при проверке корзины", Toast.LENGTH_SHORT).show();
                    Log.e("ProductAdapter", "Ошибка при проверке корзины", e);
                });
    }

    // Метод для редактирования товара
    private void editProduct(Product product, ProductViewHolder holder) {
        // Открываем фрагмент редактирования товара
        ProductDetailSeller productDetailSeller = new ProductDetailSeller();
        Bundle bundle = new Bundle();
        bundle.putString("productId", product.getId());
        bundle.putString("userDocumentId", userDocumentId);
        bundle.putString("userRole", userRole);
        productDetailSeller.setArguments(bundle);

        FragmentManager fragmentManager = ((AppCompatActivity) holder.itemView.getContext()).getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.frameLayout, productDetailSeller)
                .addToBackStack(null)
                .commit();
    }

    // Метод для удаления товара
    private void deleteProduct(Product product, ProductViewHolder holder) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products")
                .document(product.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(holder.itemView.getContext(), "Товар удален", Toast.LENGTH_SHORT).show();
                    // Удаляем товар из списка и обновляем адаптер
                    products.remove(product);
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(holder.itemView.getContext(), "Ошибка при удалении товара", Toast.LENGTH_SHORT).show();
                    Log.e("ProductAdapter", "Ошибка при удалении товара", e);
                });
    }

    // Метод для преобразования Base64 в Bitmap
    private Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ViewHolder для товара
    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice;
        ImageView productImage;
        Button editButton, deleteButton, addToCartButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productImage = itemView.findViewById(R.id.productImage);


            addToCartButton = itemView.findViewById(R.id.addToCartButton);

            // Логирование для отладки
            Log.d("ProductViewHolder", "editButton: " + editButton);
            Log.d("ProductViewHolder", "deleteButton: " + deleteButton);
            Log.d("ProductViewHolder", "addToCartButton: " + addToCartButton);
        }
    }
}