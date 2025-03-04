package com.example.ozon;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
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

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProductAdapter extends FirestoreRecyclerAdapter<Product, ProductAdapter.ProductViewHolder> {

    private OnItemClickListener listener;
    private String userDocumentId;
    private String userRole; // Поле для хранения роли пользователя

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public ProductAdapter(@NonNull FirestoreRecyclerOptions<Product> options, OnItemClickListener listener, String userDocumentId, String userRole) {
        super(options);
        this.listener = listener;
        this.userDocumentId = userDocumentId;
        this.userRole = userRole; // Инициализируем поле userRole
    }

    @Override
    protected void onBindViewHolder(@NonNull ProductViewHolder holder, int position, @NonNull Product product) {
        String productId = getSnapshots().getSnapshot(position).getId();
        product.setId(productId);

        holder.productName.setText(product.getName());
        holder.productPrice.setText(String.valueOf(product.getPrice()));

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

        holder.addToCartButton.setOnClickListener(v -> {
            Cart cart = new Cart(productId, product.getName(), product.getPrice(), 1, product.getImageBase64(), userDocumentId);

            FirebaseFirestore.getInstance().collection("cart")
                    .document(productId)
                    .set(cart)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(holder.itemView.getContext(), "Товар добавлен в корзину", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(), "Ошибка при добавлении в корзину", Toast.LENGTH_SHORT).show();
                    });
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(product);

                // Создаем новый экземпляр фрагмента ProductDetail
                ProductDetail productDetailFragment = new ProductDetail();

                // Передаем documentId через Bundle
                Bundle bundle = new Bundle();
                bundle.putString("document_id", productId);
                bundle.putString("userDocumentId", userDocumentId); // Pass the userDocumentId here
                productDetailFragment.setArguments(bundle);
                FragmentManager fragmentManager = ((AppCompatActivity) holder.itemView.getContext()).getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, productDetailFragment) // Замените R.id.fragment_container на ваш контейнер для фрагментов
                        .addToBackStack(null) // Добавляем транзакцию в back stack, чтобы можно было вернуться назад
                        .commit();
            }
        });
    }

    private Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Выбираем разметку в зависимости от роли пользователя
        int layoutResId = userRole.equals("seller") ? R.layout.item_product_seller : R.layout.item_ptoduct;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        return new ProductViewHolder(view);
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice;
        ImageView productImage;
        Button addToCartButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productImage = itemView.findViewById(R.id.productImage);
            addToCartButton = itemView.findViewById(R.id.addToCartButton);
        }
    }
}