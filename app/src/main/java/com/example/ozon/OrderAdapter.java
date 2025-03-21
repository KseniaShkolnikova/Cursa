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

public class OrderAdapter extends FirestoreRecyclerAdapter<Cart, OrderAdapter.OrderViewHolder> {
    private List<Cart> cartItems = new ArrayList<>();

    public OrderAdapter(@NonNull FirestoreRecyclerOptions<Cart> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull OrderViewHolder holder, int position, @NonNull Cart cart) {
        cartItems.add(cart); // Добавляем товар в список
        holder.productName.setText(cart.getName());
        holder.productPrice.setText(String.valueOf(cart.getPrice()) + " ₽");
        holder.productQuantity.setText("Количество: " + cart.getQuantity());
    }

    public List<Cart> getCartItems() {
        return cartItems;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, productQuantity;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productQuantity = itemView.findViewById(R.id.productQuantity);
        }
    }
}