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

/**
 * Класс CartAdapter является адаптером для отображения списка товаров в корзине пользователя
 * в приложении "OZON". Использует FirestoreRecyclerAdapter для динамического
 * обновления данных из Firebase Firestore. Предоставляет функционал для изменения количества
 * товаров, удаления товаров из корзины и отображения их изображений в формате Base64.
 */
public class CartAdapter extends FirestoreRecyclerAdapter<Cart, CartAdapter.CartViewHolder> {

    /**
     * Конструктор адаптера CartAdapter. Инициализирует адаптер с заданными параметрами запроса
     * для получения данных о товарах в корзине из Firebase Firestore.
     */
    public CartAdapter(@NonNull FirestoreRecyclerOptions<Cart> options) {
        super(options);
    }

    /**
     * Связывает данные о товаре из корзины с элементами представления в ViewHolder.
     * Устанавливает название, цену, количество и изображение товара. Настраивает обработчики
     * событий для кнопок увеличения/уменьшения количества и удаления товара из корзины.
     */
    @Override
    protected void onBindViewHolder(@NonNull CartViewHolder holder, int position, @NonNull Cart cart) {
        holder.productName.setText(cart.getName());
        holder.productPrice.setText(String.valueOf(cart.getPrice()) + " ₽");
        holder.productQuantity.setText("" + cart.getQuantity());
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

    /**
     * Обновляет количество товара в корзине в базе данных Firebase Firestore. Проверяет
     * доступное количество товара на складе перед обновлением. Если товара недостаточно,
     * отображает сообщение об ошибке.
     */
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

    /**
     * Удаляет товар из корзины, удаляя соответствующий документ из коллекции "cart" в Firebase Firestore.
     * Отображает сообщение об ошибке в случае неудачи.
     */
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

    /**
     * Преобразует строку в формате Base64 в объект Bitmap для отображения изображения товара.
     * Возвращает null в случае ошибки декодирования.
     */
    private Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Создает новый ViewHolder для элемента списка корзины. Использует разметку item_cart
     * для создания представления каждого товара.
     */
    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    /**
     * Вложенный класс CartViewHolder представляет ViewHolder для элемента списка корзины.
     * Содержит ссылки на элементы UI, такие как название, цена, количество, изображение товара
     * и кнопки для управления количеством и удаления товара.
     */
    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, productQuantity;
        ImageView productImage, increaseQuantity, decreaseQuantity, deleteItemButton;

        /**
         * Конструктор ViewHolder. Инициализирует элементы UI для отображения данных о товаре
         * в корзине, включая текстовые поля и кнопки управления.
         */
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