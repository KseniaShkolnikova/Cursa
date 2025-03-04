package com.example.ozon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;

public class CartAdapter extends FirestoreRecyclerAdapter<Cart, CartAdapter.CartViewHolder> {

    public CartAdapter(@NonNull FirestoreRecyclerOptions<Cart> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull CartViewHolder holder, int position, @NonNull Cart cart) {
        holder.productName.setText(cart.getName());

        // Обработка цены с проверкой
        int price = cart.getPrice();
        holder.productPrice.setText(String.valueOf(price));

        // Количество товара
        int quantity = cart.getQuantity();
        holder.productQuantity.setText(String.valueOf(quantity));

        // Установка изображения
        if (cart.getImageBase64() != null && !cart.getImageBase64().isEmpty()) {
            Bitmap bitmap = base64ToBitmap(cart.getImageBase64());
            if (bitmap != null) {
                holder.productImage.setImageBitmap(bitmap);
            }
        }

        // Увеличение количества
        holder.increaseQuantity.setOnClickListener(v -> {
            int newQuantity = cart.getQuantity() + 1;
            updateQuantity(cart.getDocumentId(), newQuantity, holder.itemView.getContext());
        });

        // Уменьшение количества
        holder.decreaseQuantity.setOnClickListener(v -> {
            int newQuantity = cart.getQuantity() - 1;
            if (newQuantity > 0) {
                updateQuantity(cart.getDocumentId(), newQuantity, holder.itemView.getContext());
            } else {
                // Удаление товара, если количество = 0
                deleteCartItem(cart.getDocumentId(), holder.itemView.getContext());
            }
        });
    }

    private void updateQuantity(String documentId, int newQuantity, Context context) {
        FirebaseFirestore.getInstance().collection("cart")
                .document(documentId)
                .update("quantity", newQuantity)
                .addOnSuccessListener(aVoid -> {})
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Ошибка при обновлении количества", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteCartItem(String documentId, Context context) {
        FirebaseFirestore.getInstance().collection("cart")
                .document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Товар удален из корзины", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Ошибка при удалении товара", Toast.LENGTH_SHORT).show();
                });
    }

    private Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            Log.e("CartAdapter", "Ошибка при декодировании изображения", e);
            return null;
        }
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, productQuantity;
        ImageView productImage, increaseQuantity, decreaseQuantity;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productQuantity = itemView.findViewById(R.id.productQuantity);
            productImage = itemView.findViewById(R.id.productImage);
            increaseQuantity = itemView.findViewById(R.id.increaseQuantity);
            decreaseQuantity = itemView.findViewById(R.id.decreaseQuantity);
        }
    }
}
