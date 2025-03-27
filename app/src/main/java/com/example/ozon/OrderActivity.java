package com.example.ozon;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class OrderActivity extends Fragment {

    static final int MAP_SELECTION_REQUEST_CODE = 1001;

    private RecyclerView recyclerView;
    private CheckBox agreementCheckBox;
    private Button payButton;
    private TextView totalAmount;
    private Button protectionButton;
    private Button addCardButton;
    private boolean isProtectionEnabled = false;
    private int total = 0;
    private int protectionCost = 149;
    private OrderAdapter orderAdapter;
    private Spinner bankCardSpinner;
    private TextView selectedCardInfo;
    private List<String> bankCards = new ArrayList<>();
    private ArrayAdapter<String> bankCardAdapter;
    private String userDocumentId, userRole;
    private String selectedCardNumber = null;
    private LinearLayout cardsContainer;
    private String deliveryAddress = null;
    private GeoPoint deliveryLocation;

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAP_SELECTION_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
            if (data != null) {
                deliveryAddress = data.getStringExtra("SELECTED_ADDRESS");
                double lat = data.getDoubleExtra("LATITUDE", 0);
                double lng = data.getDoubleExtra("LONGITUDE", 0);
                deliveryLocation = new GeoPoint(lat, lng);

                showToast("Адрес выбран: " + deliveryAddress);
                if (checkOrderConditionsExceptAddress() && agreementCheckBox.isChecked() && selectedCardNumber != null) {
                    processPayment(); // Proceed to payment after address selection
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.order_layout, container, false);
        initViews(view);
        getUserData();
        setupBankCardSpinner();
        loadCartItems();
        loadBankCards();
        setupEventListeners();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        agreementCheckBox = view.findViewById(R.id.agreementCheckBox);
        payButton = view.findViewById(R.id.payButton);
        totalAmount = view.findViewById(R.id.totalAmount);
        protectionButton = view.findViewById(R.id.protectionButton);
        bankCardSpinner = view.findViewById(R.id.bankCardSpinner);
        selectedCardInfo = view.findViewById(R.id.selectedCardInfo);
        addCardButton = view.findViewById(R.id.addCardButton);
        cardsContainer = view.findViewById(R.id.cardsContainer);

        payButton.setOnClickListener(v -> {
            if (checkOrderConditionsExceptAddress()) {
                if (deliveryAddress == null || deliveryAddress.isEmpty()) {
                    showDeliveryAddressDialog(); // Show address dialog if address is missing
                } else {
                    processPayment(); // Proceed to payment if address is already set
                }
            }
        });
        payButton.setEnabled(false);
        payButton.setAlpha(0.5f);
    }

    private void getUserData() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("USER_DOCUMENT_ID");
            userRole = bundle.getString("USER_ROLE");
        } else {
            showToast("Ошибка: данные пользователя не переданы");
        }
    }

    private void setupBankCardSpinner() {
        bankCardAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, bankCards);
        bankCardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bankCardSpinner.setAdapter(bankCardAdapter);
    }

    private void setupEventListeners() {
        protectionButton.setOnClickListener(v -> toggleProtection());

        agreementCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> updatePayButtonState());

        bankCardSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCardNumber = bankCards.get(position);
                selectedCardInfo.setText("Выбранная карта: ****" + selectedCardNumber.substring(selectedCardNumber.length() - 4));
                updatePayButtonState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCardNumber = null;
                selectedCardInfo.setText("Карта не выбрана");
                updatePayButtonState();
            }
        });

        addCardButton.setOnClickListener(v -> showAddCardDialog());
    }

    private void loadCartItems() {
        Query query = FirebaseFirestore.getInstance()
                .collection("cart")
                .whereEqualTo("userId", getCurrentUserId());

        FirestoreRecyclerOptions<Cart> options = new FirestoreRecyclerOptions.Builder<Cart>()
                .setQuery(query, Cart.class)
                .setLifecycleOwner(this)
                .build();

        orderAdapter = new OrderAdapter(options);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(orderAdapter);

        orderAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                updateTotalFromAdapter();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateTotalFromAdapter();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateTotalFromAdapter();
            }
        });
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
                            String cardNumber = document.getString("cardNumber");
                            if (cardNumber != null) {
                                bankCards.add(cardNumber);
                            }
                        }

                        if (bankCards.isEmpty()) {
                            selectedCardInfo.setText("Нет сохраненных карт");
                            addCardButton.setVisibility(View.VISIBLE);
                            bankCardSpinner.setVisibility(View.GONE);
                            payButton.setEnabled(false);
                            payButton.setAlpha(0.5f);
                        } else {
                            addCardButton.setVisibility(View.GONE);
                            bankCardSpinner.setVisibility(View.VISIBLE);
                            bankCardAdapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> showToast("Ошибка при загрузке карт"));
        }
    }

    private void showDeliveryAddressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_map, null);
        builder.setView(dialogView);

        Button btnSelectOnMap = dialogView.findViewById(R.id.btnSelectOnMap);
        Button btnManualInput = dialogView.findViewById(R.id.btnManualInput);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = builder.create();

        btnSelectOnMap.setOnClickListener(v -> {
            dialog.dismiss();
            startMapSelection();
        });

        btnManualInput.setOnClickListener(v -> {
            dialog.dismiss();
            showManualAddressInputDialog();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void startMapSelection() {
        Intent intent = new Intent(getActivity(), MapSelectionActivity.class);
        startActivityForResult(intent, MAP_SELECTION_REQUEST_CODE);
    }

    private void showManualAddressInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manual_addres, null);
        builder.setView(dialogView);

        EditText etAddress = dialogView.findViewById(R.id.etAddress);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = builder.create();

        btnConfirm.setOnClickListener(v -> {
            String address = etAddress.getText().toString().trim();
            if (!address.isEmpty()) {
                deliveryAddress = address;
                deliveryLocation = null;
                showToast("Адрес сохранен: " + deliveryAddress);
                dialog.dismiss();
                if (checkOrderConditionsExceptAddress()) {
                    processPayment(); // Proceed to payment after address input
                }
            } else {
                showToast("Введите адрес");
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showAddCardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.add_card_dialog, null);
        builder.setView(dialogView);

        EditText etCardNumber = dialogView.findViewById(R.id.etCardNumber);
        EditText etCardCVV = dialogView.findViewById(R.id.etCardCVV);
        EditText etCardExpiry = dialogView.findViewById(R.id.etCardExpiry);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        etCardNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().replaceAll(" ", "");
                if (input.length() > 0 && (input.length() % 4) == 0 && input.length() <= 16) {
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < input.length(); i++) {
                        if (i > 0 && i % 4 == 0) {
                            formatted.append(" ");
                        }
                        formatted.append(input.charAt(i));
                    }
                    etCardNumber.removeTextChangedListener(this);
                    etCardNumber.setText(formatted.toString());
                    etCardNumber.setSelection(formatted.length());
                    etCardNumber.addTextChangedListener(this);
                }
            }
        });

        etCardExpiry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 2 && !s.toString().contains("/")) {
                    s.append("/");
                }
            }
        });

        AlertDialog dialog = builder.create();

        btnAdd.setOnClickListener(v -> {
            String cardNumber = etCardNumber.getText().toString().replaceAll(" ", "");
            String cvv = etCardCVV.getText().toString().trim();
            String expiryDate = etCardExpiry.getText().toString().trim();

            if (validateCardInput(cardNumber, cvv, expiryDate)) {
                saveCardToFirestore(cardNumber, cvv, expiryDate);
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private boolean validateCardInput(String cardNumber, String cvv, String expiryDate) {
        if (cardNumber.isEmpty() || cvv.isEmpty() || expiryDate.isEmpty()) {
            showToast("Заполните все поля");
            return false;
        }

        if (cardNumber.length() != 16) {
            showToast("Номер карты должен содержать 16 цифр");
            return false;
        }

        if (cvv.length() != 3) {
            showToast("CVV код должен содержать 3 цифры");
            return false;
        }

        if (expiryDate.length() != 5 || !expiryDate.contains("/")) {
            showToast("Введите дату в формате ММ/ГГ");
            return false;
        }

        return true;
    }

    private void saveCardToFirestore(String cardNumber, String cvv, String expiryDate) {
        String userId = getCurrentUserId();
        if (userId == null) {
            showToast("Ошибка: пользователь не авторизован");
            return;
        }

        Map<String, Object> cardData = new HashMap<>();
        cardData.put("cardNumber", cardNumber);
        cardData.put("cvv", cvv);
        cardData.put("expiryDate", expiryDate);
        cardData.put("userId", userId);

        FirebaseFirestore.getInstance().collection("cards")
                .add(cardData)
                .addOnSuccessListener(documentReference -> {
                    showToast("Карта успешно добавлена");
                    loadBankCards();
                })
                .addOnFailureListener(e -> showToast("Ошибка при добавлении карты: " + e.getMessage()));
    }

    private boolean checkOrderConditions() {
        if (deliveryAddress == null || deliveryAddress.isEmpty()) {
            showToast("Адрес доставки не выбран");
            return false;
        }

        if (!agreementCheckBox.isChecked()) {
            showToast("Подтвердите согласие с условиями");
            return false;
        }

        if (selectedCardNumber == null) {
            showToast("Выберите карту для оплаты");
            return false;
        }

        if (total <= 0) {
            showToast("Корзина пуста");
            return false;
        }

        return true;
    }

    private boolean checkOrderConditionsExceptAddress() {
        if (!agreementCheckBox.isChecked()) {
            showToast("Подтвердите согласие с условиями");
            return false;
        }

        if (selectedCardNumber == null) {
            showToast("Выберите карту для оплаты");
            return false;
        }

        if (total <= 0) {
            showToast("Корзина пуста");
            return false;
        }

        return true;
    }

    private void updateTotalFromAdapter() {
        total = 0;
        for (Cart cart : orderAdapter.getCartItems()) {
            total += cart.getPrice() * cart.getQuantity();
        }
        updateTotalAmount();
    }

    private void updatePayButtonState() {
        boolean isEnabled = agreementCheckBox.isChecked() && selectedCardNumber != null && total > 0;
        payButton.setEnabled(isEnabled);
        payButton.setAlpha(isEnabled ? 1.0f : 0.5f);
    }

    private void toggleProtection() {
        isProtectionEnabled = !isProtectionEnabled;
        protectionButton.setText(isProtectionEnabled ? "Отключить защиту имущества" : "Подключить защиту имущества");
        updateTotalAmount();
    }

    private void updateTotalAmount() {
        int finalTotal = total + (isProtectionEnabled ? protectionCost : 0);
        totalAmount.setText("Итого: " + finalTotal + " ₽");
    }

    private void processPayment() {
        String userId = getCurrentUserId();
        if (userId == null) {
            showToast("Ошибка: пользователь не авторизован");
            return;
        }

        List<Cart> cartItemsSnapshot = new ArrayList<>(orderAdapter.getCartItems());
        if (cartItemsSnapshot.isEmpty()) {
            showToast("Корзина пуста");
            return;
        }

        int finalTotal = total + (isProtectionEnabled ? protectionCost : 0);
        orderAdapter.stopListening();
        showConfirmationDialog(userId, cartItemsSnapshot, finalTotal);
    }

    private void showConfirmationDialog(String userId, List<Cart> cartItems, int finalTotal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Подтверждение заказа");
        builder.setMessage("Адрес доставки: " + deliveryAddress + "\n\nСумма: " + finalTotal + " ₽");

        builder.setPositiveButton("Подтвердить", (dialog, which) -> {
            updateProductQuantities(cartItems, new ProductUpdateCallback() {
                @Override
                public void onAllProductsUpdated() {
                    createOrder(userId, cartItems, finalTotal, selectedCardNumber);
                }

                @Override
                public void onUpdateFailed(Exception e) {
                    showToast("Ошибка: " + e.getMessage());
                    orderAdapter.startListening();
                }
            });
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> orderAdapter.startListening());
        builder.show();
    }

    private void createOrder(String userId, List<Cart> cartItems, int totalAmount, String cardNumber) {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", userId);
        orderData.put("products", cartItems);
        orderData.put("totalAmount", totalAmount);
        orderData.put("paymentMethod", "Карта ****" + cardNumber.substring(cardNumber.length() - 4));
        orderData.put("deliveryAddress", deliveryAddress);
        orderData.put("protectionEnabled", isProtectionEnabled);

        // Добавляем текущую дату и время заказа
        orderData.put("orderDate", com.google.firebase.Timestamp.now());

        // Добавляем случайное количество дней доставки (1-7) и статус "создан"
        Random random = new Random();
        int days = ThreadLocalRandom.current().nextInt(1, 7);
        orderData.put("days", days);
        orderData.put("status", "создан");

        if (deliveryLocation != null) {
            orderData.put("deliveryLocation", deliveryLocation);
        }

        FirebaseFirestore.getInstance().collection("orders")
                .add(orderData)
                .addOnSuccessListener(documentReference -> {
                    clearCart(userId);
                    navigateToCatalog();
                    showToast("Заказ успешно оформлен");
                    orderAdapter.startListening();
                })
                .addOnFailureListener(e -> {
                    showToast("Ошибка при оформлении заказа: " + e.getMessage());
                    orderAdapter.startListening();
                });
    }

    private interface ProductUpdateCallback {
        void onAllProductsUpdated();
        void onUpdateFailed(Exception e);
    }

    private void updateProductQuantities(List<Cart> cartItems, ProductUpdateCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final int[] processedItems = {0};
        final boolean[] hasError = {false};

        for (Cart cartItem : cartItems) {
            String productId = cartItem.getProductId();
            int orderedQuantity = cartItem.getQuantity();

            db.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentReference productRef = db.collection("products").document(productId);
                DocumentSnapshot productSnapshot = transaction.get(productRef);

                if (!productSnapshot.exists()) {
                    try {
                        throw new Exception("Товар с ID " + productId + " не найден");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                Long quantity = productSnapshot.getLong("quantity");
                if (quantity == null) {
                    try {
                        throw new Exception("Для товара " + productSnapshot.getString("name") + " не указано количество");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                Long newQuantity = quantity - orderedQuantity;
                if (newQuantity < 0) {
                    try {
                        throw new Exception("Недостаточно товара: " + productSnapshot.getString("name") +
                                ". Доступно: " + quantity + ", требуется: " + orderedQuantity);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                transaction.update(productRef, "quantity", newQuantity);
                return null;
            }).addOnSuccessListener(aVoid -> {
                processedItems[0]++;
                if (processedItems[0] == cartItems.size() && !hasError[0]) {
                    callback.onAllProductsUpdated();
                }
            }).addOnFailureListener(e -> {
                hasError[0] = true;
                callback.onUpdateFailed(e);
                Log.e("OrderActivity", "Ошибка обновления количества: " + e.getMessage());
            });
        }
    }

    private void clearCart(String userId) {
        FirebaseFirestore.getInstance().collection("cart")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        document.getReference().delete();
                    }
                })
                .addOnFailureListener(e -> showToast("Ошибка при очистке корзины"));
    }

    private void navigateToCatalog() {
        Bundle bundle = new Bundle();
        bundle.putString("USER_DOCUMENT_ID", userDocumentId);
        bundle.putString("USER_ROLE", userRole);

        CatalogActivity catalogFragment = new CatalogActivity();
        catalogFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, catalogFragment)
                .commit();
    }

    private String getCurrentUserId() {
        return userDocumentId;
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
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