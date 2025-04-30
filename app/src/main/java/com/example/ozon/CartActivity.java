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

/**
 * Класс CartActivity представляет собой фрагмент для отображения корзины пользователя
 * в приложении "OZON". Отображает список товаров в корзине, общую сумму,
 * предоставляет возможность очистить корзину или перейти к оформлению заказа.
 * Использует Firebase Firestore для загрузки данных и FirestoreRecyclerAdapter для динамического
 * обновления списка товаров.
 */
public class CartActivity extends Fragment {
    private RecyclerView cartRecyclerView;
    private CartAdapter cartAdapter;
    private TextView totalAmount;
    private Button clearCartButton, checkoutButton;
    private String userDocumentId, userRole;

    /**
     * Создает и возвращает представление фрагмента корзины. Инициализирует элементы UI,
     * такие как RecyclerView для списка товаров, текстовое поле для общей суммы и кнопки
     * для очистки корзины и перехода к оформлению заказа. Устанавливает обработчики событий
     * для кнопок и извлекает данные пользователя из аргументов фрагмента.
     */
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

    /**
     * Вызывается после создания активности, содержащей фрагмент. Инициирует загрузку данных
     * корзины пользователя через вызов метода loadCarts().
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadCarts();
    }

    /**
     * Загружает данные корзины пользователя из Firebase Firestore. Создает запрос для получения
     * товаров, связанных с текущим пользователем, и настраивает FirestoreRecyclerAdapter для
     * отображения списка товаров в RecyclerView. Также обновляет общую сумму корзины в реальном
     * времени и управляет состоянием кнопки оформления заказа в зависимости от наличия товаров.
     */
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
                            checkoutButton.setBackgroundColor(getResources().getColor(R.color.light_gray));
                        } else {
                            checkoutButton.setEnabled(true);
                            checkoutButton.setBackgroundColor(getResources().getColor(R.color.highlight_color));
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

    /**
     * Очищает корзину пользователя, удаляя все связанные записи из коллекции "cart" в Firebase Firestore.
     * Выполняет запрос на удаление всех документов, соответствующих текущему пользователю, и отображает
     * сообщение об ошибке в случае неудачи.
     */
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

    /**
     * Вызывается при остановке фрагмента. Останавливает прослушивание изменений в данных корзины
     * через FirestoreRecyclerAdapter, чтобы избежать утечек памяти.
     */
    @Override
    public void onStop() {
        super.onStop();
        if (cartAdapter != null) {
            cartAdapter.stopListening();
        }
    }
}