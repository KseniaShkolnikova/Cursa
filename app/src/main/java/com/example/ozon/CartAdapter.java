package com.example.ozon;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
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
        holder.productPrice.setText(String.valueOf(cart.getPrice()) + " ₽");
        holder.productQuantity.setText(""+cart.getQuantity());
        if (cart.getImageBase64() != null && !cart.getImageBase64().isEmpty()) {
            Bitmap bitmap = base64ToBitmap(cart.getImageBase64());
            if (bitmap != null) {
                holder.productImage.setImageBitmap(bitmap);
            } else {
                holder.productImage.setImageResource(R.drawable.no_photo);
            }
        } else {
            holder.productImage.setImageResource(R.drawable.no_photo);
        }
        holder.increaseQuantity.setOnClickListener(v -> {
            int newQuantity = cart.getQuantity() + 1;
            updateQuantity(cart.getDocumentId(), newQuantity, holder.itemView.getContext());
        });
        holder.decreaseQuantity.setOnClickListener(v -> {
            int newQuantity = cart.getQuantity() - 1;
            if (newQuantity > 0) {
                updateQuantity(cart.getDocumentId(), newQuantity, holder.itemView.getContext());
            } else {
                deleteCartItem(cart.getDocumentId(), holder.itemView.getContext());
            }
        });
        holder.deleteItemButton.setOnClickListener(v -> {
            deleteCartItem(cart.getDocumentId(), holder.itemView.getContext());
        });
    }
    private void updateQuantity(String cartItemId, int newQuantity, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("cart")
                .document(cartItemId)
                .get()
                .addOnSuccessListener(cartDocument -> {
                    Cart cart = cartDocument.toObject(Cart.class);
                    if (cart != null) {
                        String productId = cart.getProductId();
                        db.collection("products")
                                .document(productId)
                                .get()
                                .addOnSuccessListener(productDocument -> {
                                    if (productDocument.exists()) {
                                        int availableQuantity = productDocument.getLong("quantity").intValue();
                                        if (newQuantity <= availableQuantity) {
                                            db.collection("cart")
                                                    .document(cartItemId)
                                                    .update("quantity", newQuantity)
                                                    .addOnSuccessListener(aVoid -> {
                                                    });
                                        } else {
                                            Toast.makeText(context,
                                                    "Недостаточно товара на складе", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(context,
                                                "Товар не найден", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }
    private void deleteCartItem(String documentId, Context context) {
        FirebaseFirestore.getInstance().collection("cart")
                .document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
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
        ImageView productImage, increaseQuantity, decreaseQuantity, deleteItemButton;
        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productQuantity = itemView.findViewById(R.id.productQuantity);
            productImage = itemView.findViewById(R.id.productImage);
            increaseQuantity = itemView.findViewById(R.id.increaseQuantity);
            decreaseQuantity = itemView.findViewById(R.id.decreaseQuantity);
            deleteItemButton = itemView.findViewById(R.id.deleteItemButton);
        }
    }
}