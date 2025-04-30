package com.example.ozon;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс OrderAdapter представляет собой адаптер для отображения списка товаров
 * в корзине при оформлении заказа в приложении "OZON". Использует
 * FirestoreRecyclerAdapter для синхронизации данных с Firebase Firestore и отображения
 * их в RecyclerView.
 */
public class OrderAdapter extends FirestoreRecyclerAdapter<Cart, OrderAdapter.OrderViewHolder> {

    /**
     * Конструктор адаптера. Инициализирует адаптер с заданными опциями FirestoreRecyclerOptions.
     */
    public OrderAdapter(@NonNull FirestoreRecyclerOptions<Cart> options) {
        super(options);
    }

    /**
     * Привязывает данные товара из корзины к элементам UI в ViewHolder. Устанавливает
     * название, цену и количество товара в соответствующие текстовые поля.
     */
    @Override
    protected void onBindViewHolder(@NonNull OrderViewHolder holder, int position, @NonNull Cart cart) {
        holder.productName.setText(cart.getName());
        holder.productPrice.setText(String.valueOf(cart.getPrice()) + " ₽");
        holder.productQuantity.setText("Количество: " + cart.getQuantity());
    }

    /**
     * Вызывается при изменении данных в Firestore. Уведомляет адаптер об изменении данных
     * для обновления отображения списка.
     */
    @Override
    public void onDataChanged() {
        super.onDataChanged();
        notifyDataSetChanged();
    }

    /**
     * Возвращает список всех товаров в корзине, отображаемых адаптером. Формирует список
     * на основе текущих данных Firestore.
     */
    public List<Cart> getCartItems() {
        List<Cart> items = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            items.add(getItem(i));
        }
        return items;
    }

    /**
     * Создает новый ViewHolder для элемента списка. Использует LayoutInflater для создания
     * представления из макета item_order.
     */
    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    /**
     * Внутренний класс OrderViewHolder представляет собой ViewHolder для элемента списка товаров.
     * Содержит ссылки на текстовые поля для отображения названия, цены и количества товара.
     */
    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, productQuantity;

        /**
         * Конструктор ViewHolder. Инициализирует текстовые поля для отображения данных товара.
         */
        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productQuantity = itemView.findViewById(R.id.productQuantity);
        }
    }
}