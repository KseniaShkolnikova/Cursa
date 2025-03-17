package com.example.ozon;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class CartActivity extends Fragment {

    private RecyclerView cartRecyclerView;
    private CartAdapter cartAdapter;
    private TextView totalAmount;
    private Button clearCartButton, checkoutButton;
    private String userDocumentId, userRole;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("CartActivity", "onCreateView started");
        View view = inflater.inflate(R.layout.cart_layout, container, false);

        cartRecyclerView = view.findViewById(R.id.cartRecyclerView);
        totalAmount = view.findViewById(R.id.totalAmount);
        clearCartButton = view.findViewById(R.id.clearCartButton);
        checkoutButton = view.findViewById(R.id.checkoutButton);

        // Настройка RecyclerView
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Get the user's ID and role from the arguments passed to the fragment
        if (getArguments() != null) {
            userDocumentId = getArguments().getString("USER_DOCUMENT_ID");
            userRole = getArguments().getString("USER_ROLE");
        } else {
            Log.e("CartActivity", "No arguments passed to fragment.");
            Toast.makeText(getContext(), "Не удалось загрузить корзину: аргументы не переданы", Toast.LENGTH_SHORT).show();
        }

        // Очистка корзины
        clearCartButton.setOnClickListener(v -> clearCart());

        // Переход к оплате
        checkoutButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("USER_DOCUMENT_ID", userDocumentId);
            bundle.putString("USER_ROLE", userRole);

            OrderActivity checkoutFragment = new OrderActivity();
            checkoutFragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, checkoutFragment)
                    .addToBackStack(null)
                    .commit();
        });

        Log.d("CartActivity", "onCreateView finished");
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadCarts();
    }

    private void loadCarts() {
        Log.d("CartActivity", "Loading carts from Firestore");

        if (userDocumentId != null) {
            Query query = FirebaseFirestore.getInstance().collection("cart")
                    .whereEqualTo("userId", userDocumentId);

            FirestoreRecyclerOptions<Cart> options = new FirestoreRecyclerOptions.Builder<Cart>()
                    .setQuery(query, Cart.class)
                    .build();

            cartAdapter = new CartAdapter(options);
            cartRecyclerView.setAdapter(cartAdapter);
            cartAdapter.startListening();

            query.addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e("CartActivity", "Ошибка при загрузке корзины: " + error.getMessage());
                    return;
                }

                if (value != null) {
                    int total = 0;
                    boolean isEmpty = value.isEmpty(); // Проверка на пустоту корзины

                    for (DocumentSnapshot document : value.getDocuments()) {
                        Cart cart = document.toObject(Cart.class);
                        if (cart != null) {
                            int price = cart.getPrice();
                            int quantity = cart.getQuantity();
                            total += price * quantity;
                        }
                    }
                    totalAmount.setText("Итого: " + total + " ₽");

                    // Проверка, прикреплен ли фрагмент к контексту
                    if (isAdded() && getContext() != null) {
                        // Блокировка кнопки и изменение цвета, если корзина пуста
                        if (isEmpty) {
                            checkoutButton.setEnabled(false);
                            checkoutButton.setBackgroundColor(getResources().getColor(R.color.light_gray)); // Замените на ваш цвет
                        } else {
                            checkoutButton.setEnabled(true);
                            checkoutButton.setBackgroundColor(getResources().getColor(R.color.button_color)); // Вернуть исходный цвет
                        }
                    }
                }
            });
        } else {
            Log.e("CartActivity", "User ID is null. Cannot load cart.");
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Не удалось загрузить корзину: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void clearCart() {
        FirebaseFirestore.getInstance().collection("cart")
                .whereEqualTo("userId", userDocumentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        document.getReference().delete();
                    }
                    Toast.makeText(getContext(), "Корзина очищена", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка при очистке корзины", Toast.LENGTH_SHORT).show();
                    Log.e("CartActivity", "Clear cart failed: " + e.getMessage());
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (cartAdapter != null) {
            cartAdapter.stopListening();
        }
    }
}