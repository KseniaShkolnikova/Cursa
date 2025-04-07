package com.example.ozon;
import android.os.Bundle;
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
        View view = inflater.inflate(R.layout.cart_layout, container, false);
        cartRecyclerView = view.findViewById(R.id.cartRecyclerView);
        totalAmount = view.findViewById(R.id.totalAmount);
        clearCartButton = view.findViewById(R.id.clearCartButton);
        checkoutButton = view.findViewById(R.id.checkoutButton);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        if (getArguments() != null) {
            userDocumentId = getArguments().getString("USER_DOCUMENT_ID");
            userRole = getArguments().getString("USER_ROLE");
        } else {
            Toast.makeText(getContext(), "Не удалось загрузить корзину", Toast.LENGTH_SHORT).show();
        }
        clearCartButton.setOnClickListener(v -> clearCart());
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
        return view;
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadCarts();
    }
    private void loadCarts() {
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
                    return;
                }
                if (value != null) {
                    int total = 0;
                    boolean isEmpty = value.isEmpty();
                    for (DocumentSnapshot document : value.getDocuments()) {
                        Cart cart = document.toObject(Cart.class);
                        if (cart != null) {
                            int price = cart.getPrice();
                            int quantity = cart.getQuantity();
                            total += price * quantity;
                        }
                    }
                    totalAmount.setText("Итого: " + total + " ₽");
                    if (isAdded() && getContext() != null) {
                        if (isEmpty) {
                            checkoutButton.setEnabled(false);
                            checkoutButton.setBackgroundColor(getResources().getColor(R.color.light_gray)); // Замените на ваш цвет
                        } else {
                            checkoutButton.setEnabled(true);
                            checkoutButton.setBackgroundColor(getResources().getColor(R.color.highlight_color)); // Вернуть исходный цвет
                        }
                    }
                }
            });
        } else {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Не удалось загрузить корзину", Toast.LENGTH_SHORT).show();
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка при очистке корзины", Toast.LENGTH_SHORT).show();
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