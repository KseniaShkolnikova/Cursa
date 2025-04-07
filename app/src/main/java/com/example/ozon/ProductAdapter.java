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
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> products = new ArrayList<>();
    private OnItemClickListener listener;
    private String userDocumentId;
    private String userRole;
    private boolean isForOrder;
    public interface OnItemClickListener {
        void onItemClick(Product product);
    }
    public ProductAdapter(OnItemClickListener listener, String userDocumentId, String userRole) {
        this.listener = listener;
        this.userDocumentId = userDocumentId;
        this.userRole = userRole;
        this.isForOrder = false;
    }
    public ProductAdapter(String userDocumentId) {
        this.userDocumentId = userDocumentId;
        this.isForOrder = true;
    }
    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutResId = isForOrder ? R.layout.item_product_seller :
                userRole != null && userRole.equals("seller") ? R.layout.item_product_seller : R.layout.item_ptoduct;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        if (isForOrder) {
            int fixedWidthPx = (int) (160 * parent.getContext().getResources().getDisplayMetrics().density);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    fixedWidthPx,
                    RecyclerView.LayoutParams.WRAP_CONTENT
            );
            view.setLayoutParams(params);
        }
        return new ProductViewHolder(view, isForOrder);
    }
    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product);
        if (holder.isForOrderMode) {
            return;
        }
        if (holder.addToCartButton != null) {
            if (product.getQuantity() <= 0) {
                holder.addToCartButton.setEnabled(false);
                holder.addToCartButton.setBackgroundColor(
                        holder.itemView.getContext().getResources().getColor(R.color.light_gray));
                holder.addToCartButton.setText("Нет в наличии");
            } else {
                holder.addToCartButton.setEnabled(true);
                holder.addToCartButton.setOnClickListener(v -> addToCart(product, holder));
            }
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(product);
            }
        });
        if (userRole != null && userRole.equals("seller")) {
            if (holder.editButton != null) {
                holder.editButton.setOnClickListener(v -> editProduct(product, holder));
            }
            if (holder.deleteButton != null) {
                holder.deleteButton.setOnClickListener(v -> deleteProduct(product, holder));
            }
        }
    }
    @Override
    public int getItemCount() {
        return products.size();
    }
    public void updateData(List<Product> newProducts, Map<String, Integer> productPopularityMap) {
        this.products.clear();
        this.products.addAll(newProducts);
        if (productPopularityMap != null) {
            products.sort((p1, p2) -> {
                int popularity1 = productPopularityMap.getOrDefault(p1.getName(), 0);
                int popularity2 = productPopularityMap.getOrDefault(p2.getName(), 0);
                return Integer.compare(popularity2, popularity1);
            });
        }
        notifyDataSetChanged();
    }
    private void addToCart(Product product, ProductViewHolder holder) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String productId = product.getId();
        String cartItemId = userDocumentId + "_" + productId;
        db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(productDocument -> {
                    if (productDocument.exists()) {
                        int availableQuantity = productDocument.getLong("quantity").intValue();
                        if (availableQuantity <= 0) {
                            holder.addToCartButton.setEnabled(false);
                            holder.addToCartButton.setBackgroundColor(
                                    holder.itemView.getContext().getResources().getColor(R.color.light_gray));
                            showToast(holder, "Товар отсутствует на складе", false);
                            return;
                        }
                        db.collection("cart").document(cartItemId)
                                .get()
                                .addOnSuccessListener(cartDocument -> {
                                    if (cartDocument.exists()) {
                                        Cart existingCart = cartDocument.toObject(Cart.class);
                                        if (existingCart != null) {
                                            int newQuantity = existingCart.getQuantity() + 1;
                                            if (newQuantity <= availableQuantity) {
                                                db.collection("cart").document(cartItemId)
                                                        .update("quantity", newQuantity);
                                            } else {
                                                showToast(holder, "Недостаточно товара на складе", false);
                                            }
                                        }
                                    } else {
                                        Cart newCartItem = new Cart(
                                                productId,
                                                product.getName(),
                                                product.getPrice(),
                                                1,
                                                product.getImageBase64(),
                                                userDocumentId
                                        );
                                        db.collection("cart").document(cartItemId)
                                                .set(newCartItem);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    showToast(holder, "Ошибка при проверке товара", false);
                });
    }
    private void editProduct(Product product, ProductViewHolder holder) {
        ProductDetailSeller fragment = new ProductDetailSeller();
        Bundle args = new Bundle();
        args.putString("productId", product.getId());
        args.putString("userDocumentId", userDocumentId);
        args.putString("userRole", userRole);
        fragment.setArguments(args);
        FragmentManager fm = ((AppCompatActivity) holder.itemView.getContext()).getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .addToBackStack(null)
                .commit();
    }
    private void deleteProduct(Product product, ProductViewHolder holder) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products").document(product.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    products.remove(product);
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    showToast(holder, "Ошибка при удалении", false);
                });
    }
    private void showToast(ProductViewHolder holder, String message, boolean isSuccess) {
        Toast.makeText(holder.itemView.getContext(), message, Toast.LENGTH_SHORT).show();
    }
    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice;
        ImageView productImage;
        Button addToCartButton, editButton, deleteButton;
        boolean isForOrderMode;
        public ProductViewHolder(@NonNull View itemView, boolean isForOrder) {
            super(itemView);
            this.isForOrderMode = isForOrder;
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productImage = itemView.findViewById(R.id.productImage);
            if (!isForOrderMode) {
                addToCartButton = itemView.findViewById(R.id.addToCartButton);
                editButton = itemView.findViewById(R.id.editProductButton);
            }
        }
        public void bind(Product product) {
            if (productName != null) {
                productName.setText(product.getName() != null ? product.getName() : "");
            }
            if (productPrice != null) {
                if (isForOrderMode) {
                    productPrice.setText(String.format("%d ₽ × %d",
                            product.getPrice(),
                            product.getQuantity()));
                } else {
                    productPrice.setText(String.format("%d ₽", product.getPrice()));
                }
            }
            if (productImage != null) {
                if (product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
                    try {
                        byte[] decodedBytes = Base64.decode(product.getImageBase64(), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        productImage.setImageBitmap(bitmap);
                    } catch (IllegalArgumentException e) {
                        productImage.setImageResource(R.drawable.no_photo);
                    }
                } else {
                    productImage.setImageResource(R.drawable.no_photo);
                }
            }
        }
    }
}