package com.example.ozon;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderActivity extends Fragment {
    private RecyclerView recyclerView;
    private CheckBox agreementCheckBox;
    private Button payButton;
    private TextView totalAmount;
    private Button protectionButton;
    private boolean isProtectionEnabled = false;
    private int total = 0;
    private int protectionCost = 149; // Стоимость защиты имущества
    private OrderAdapter orderAdapter;
    private Spinner bankCardSpinner;
    private TextView selectedCardInfo;
    private List<String> bankCards = new ArrayList<>(); // Список карт
    private ArrayAdapter<String> bankCardAdapter;
    private String userDocumentId, userRole; // Добавляем переменные для хранения данных

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.order_layout, container, false);

        // Получаем данные из Bundle
        if (getArguments() != null) {
            userDocumentId = getArguments().getString("USER_DOCUMENT_ID");
            userRole = getArguments().getString("USER_ROLE");
        } else {
            Toast.makeText(getContext(), "Ошибка: данные пользователя не переданы", Toast.LENGTH_SHORT).show();
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        agreementCheckBox = view.findViewById(R.id.agreementCheckBox);
        payButton = view.findViewById(R.id.payButton);
        totalAmount = view.findViewById(R.id.totalAmount);
        protectionButton = view.findViewById(R.id.protectionButton);

        // Загрузка данных из корзины
        loadCartItems();

        // Кнопка для подключения/отключения "Защиты имущества"
        protectionButton.setOnClickListener(v -> toggleProtection());

        // Чекбокс согласия
        agreementCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            payButton.setEnabled(isChecked);
            payButton.setAlpha(isChecked ? 1.0f : 0.5f);
        });

        // Кнопка оплаты
        payButton.setOnClickListener(v -> processPayment());

        // Инициализация Spinner и TextView
        bankCardSpinner = view.findViewById(R.id.bankCardSpinner);
        selectedCardInfo = view.findViewById(R.id.selectedCardInfo);

        // Настройка адаптера для Spinner
        bankCardAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, bankCards);
        bankCardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bankCardSpinner.setAdapter(bankCardAdapter);

        // Загрузка карт
        loadBankCards();

        // Слушатель выбора карты
        bankCardSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCard = bankCards.get(position);
                selectedCardInfo.setText("Выбранная карта: " + selectedCard);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCardInfo.setText("Карта не выбрана");
            }
        });

        return view;
    }

    private void toggleProtection() {
        isProtectionEnabled = !isProtectionEnabled;
        if (isProtectionEnabled) {
            protectionButton.setText("Отключить защиту имущества");
        } else {
            protectionButton.setText("Подключить защиту имущества");
        }
        updateTotalAmount();
    }

    private void loadBankCards() {
        String userId = getCurrentUserId();
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("cards")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        bankCards.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            // Предположим, что карта хранится в поле "cardNumber"
                            String cardNumber = document.getString("cardNumber");
                            if (cardNumber != null) {
                                bankCards.add(cardNumber);
                            }
                        }
                        bankCardAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Ошибка при загрузке карт", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadCartItems() {
        Query query = FirebaseFirestore.getInstance()
                .collection("cart")
                .whereEqualTo("userId", getCurrentUserId()); // Фильтр по ID пользователя

        FirestoreRecyclerOptions<Cart> options = new FirestoreRecyclerOptions.Builder<Cart>()
                .setQuery(query, Cart.class)
                .build();

        orderAdapter = new OrderAdapter(options);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(orderAdapter);

        // Подсчет общей суммы
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            total = 0;
            for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                Cart cart = document.toObject(Cart.class);
                if (cart != null) {
                    total += cart.getPrice() * cart.getQuantity();
                }
            }
            updateTotalAmount();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Ошибка при загрузке корзины", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateTotalAmount() {
        int finalTotal = total;
        if (isProtectionEnabled) {
            finalTotal += protectionCost;
        }
        totalAmount.setText("Итого: " + finalTotal + " ₽");
    }

    private void processPayment() {
        if (agreementCheckBox.isChecked()) {
            if (total > 0) {
                // Получаем текущего пользователя
                String userId = getCurrentUserId();
                if (userId == null) {
                    Toast.makeText(getContext(), "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Получаем товары из корзины
                List<Cart> cartItems = orderAdapter.getCartItems();

                // Рассчитываем общую сумму с учетом защиты
                int finalTotal = total;
                if (isProtectionEnabled) {
                    finalTotal += protectionCost;
                }

                // Создаем данные для заказа
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("userId", userId);
                orderData.put("products", cartItems);
                orderData.put("totalAmount", finalTotal);
                // Записываем заказ в Firestore
                FirebaseFirestore.getInstance().collection("orders")
                        .add(orderData)
                        .addOnSuccessListener(documentReference -> {
                            // Очищаем корзину
                            clearCart(userId);

                            // Переходим на страницу каталога
                            navigateToCatalog();

                            Toast.makeText(getContext(), "Заказ успешно оформлен", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Ошибка при оформлении заказа", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(getContext(), "Корзина пуста", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Подтвердите согласие с условиями", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearCart(String userId) {
        FirebaseFirestore.getInstance().collection("cart")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        document.getReference().delete(); // Удаляем каждый товар из корзины
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка при очистке корзины", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToCatalog() {
        // Создаем Bundle для передачи данных
        Bundle bundle = new Bundle();
        bundle.putString("USER_DOCUMENT_ID", userDocumentId);
        bundle.putString("USER_ROLE", userRole);

        // Создаем экземпляр CatalogActivity и передаем данные
        CatalogActivity catalogFragment = new CatalogActivity();
        catalogFragment.setArguments(bundle);

        // Переходим на страницу каталога
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, catalogFragment) // Замените на ваш контейнер
                .commit();
    }

    private String getCurrentUserId() {
        if (getArguments() != null) {
            return getArguments().getString("USER_DOCUMENT_ID");
        }
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (orderAdapter != null) {
            orderAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (orderAdapter != null) {
            orderAdapter.stopListening();
        }
    }
}