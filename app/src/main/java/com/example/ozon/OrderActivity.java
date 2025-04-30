package com.example.ozon;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Класс OrderActivity представляет собой фрагмент для оформления заказа в приложении
 * "OZON". Позволяет пользователю выбрать способ оплаты, адрес доставки,
 * включить защиту заказа и подтвердить покупку. Использует Firebase Firestore для работы
 * с данными корзины, карт и заказов.
 */
public class OrderActivity extends Fragment {
    static final int MAP_SELECTION_REQUEST_CODE = 1001;
    private RecyclerView recyclerView;
    private CheckBox agreementCheckBox;
    private Button payButton;
    private TextView totalAmount;
    private Switch protectionSwitch;
    private Button addCardButton;
    private boolean isProtectionEnabled = false;
    private int total = 0;
    private int protectionCost = 149;
    private OrderAdapter orderAdapter;
    private Spinner bankCardSpinner;
    private TextView selectedCardInfo;
    private List<String> bankCards = new ArrayList<>();
    private TextView deliveryAddressText;
    private Button updateAddressButton;
    private ArrayAdapter<String> bankCardAdapter;
    private String userDocumentId, userRole;
    private String selectedCardNumber = null;
    private LinearLayout cardsContainer;
    private String deliveryAddress = null;
    private GeoPoint deliveryLocation;

    /**
     * Обрабатывает результат выбора адреса доставки из активности MapSelectionActivity.
     * Сохраняет выбранный адрес и координаты, обновляет UI и, при выполнении всех условий,
     * инициирует процесс оплаты.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAP_SELECTION_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
            if (data != null) {
                deliveryAddress = data.getStringExtra("SELECTED_ADDRESS");
                double lat = data.getDoubleExtra("LATITUDE", 0);
                double lng = data.getDoubleExtra("LONGITUDE", 0);
                deliveryLocation = new GeoPoint(lat, lng);
                updateDeliveryAddressText();
                showToast("Адрес выбран: " + deliveryAddress);
                if (checkOrderConditionsExceptAddress() && agreementCheckBox.isChecked() && selectedCardNumber != null) {
                    processPayment();
                }
            }
        }
    }

    /**
     * Создает и возвращает представление фрагмента. Инициализирует элементы UI, загружает
     * данные пользователя, корзины и банковских карт, а также настраивает обработчики событий.
     */
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

    /**
     * Инициализирует элементы UI фрагмента, такие как список товаров, кнопки, переключатели
     * и текстовые поля. Настраивает начальное состояние кнопки оплаты и обработчик для выбора
     * адреса доставки.
     */
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        agreementCheckBox = view.findViewById(R.id.agreementCheckBox);
        payButton = view.findViewById(R.id.payButton);
        totalAmount = view.findViewById(R.id.totalAmount);
        protectionSwitch = view.findViewById(R.id.protectionSwitch);
        bankCardSpinner = view.findViewById(R.id.bankCardSpinner);
        selectedCardInfo = view.findViewById(R.id.selectedCardInfo);
        addCardButton = view.findViewById(R.id.addCardButton);
        cardsContainer = view.findViewById(R.id.cardsContainer);
        deliveryAddressText = view.findViewById(R.id.deliveryAddressText);
        updateAddressButton = view.findViewById(R.id.updateAddressButton);
        payButton.setOnClickListener(v -> {
            if (checkOrderConditionsExceptAddress()) {
                if (deliveryAddress == null || deliveryAddress.isEmpty()) {
                    showDeliveryAddressDialog();
                } else {
                    processPayment();
                }
            }
        });
        payButton.setEnabled(false);
        payButton.setAlpha(0.5f);
        protectionSwitch.setChecked(isProtectionEnabled);
        updateDeliveryAddressText();
        updateAddressButton.setOnClickListener(v -> showDeliveryAddressDialog());
    }

    /**
     * Обновляет текстовое поле с адресом доставки. Отображает выбранный адрес или сообщение
     * о том, что адрес не выбран.
     */
    private void updateDeliveryAddressText() {
        if (deliveryAddress != null && !deliveryAddress.isEmpty()) {
            deliveryAddressText.setText(deliveryAddress);
        } else {
            deliveryAddressText.setText("Адрес доставки не выбран");
        }
    }

    /**
     * Извлекает данные пользователя (идентификатор и роль) из переданных аргументов фрагмента.
     * Если данные отсутствуют, отображает сообщение об ошибке.
     */
    private void getUserData() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("USER_DOCUMENT_ID");
            userRole = bundle.getString("USER_ROLE");
        } else {
            showToast("Ошибка: данные пользователя не переданы");
        }
    }

    /**
     * Настраивает выпадающий список для выбора банковской карты. Инициализирует адаптер
     * для списка карт и задает стиль отображения.
     */
    private void setupBankCardSpinner() {
        bankCardAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, bankCards);
        bankCardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bankCardSpinner.setAdapter(bankCardAdapter);
    }

    /**
     * Настраивает обработчики событий для элементов UI, таких как переключатель защиты заказа,
     * чекбокс согласия, выбор карты и кнопка добавления новой карты.
     */
    private void setupEventListeners() {
        protectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isProtectionEnabled = isChecked;
            updateTotalAmount();
        });
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

    /**
     * Загружает товары из корзины пользователя из Firebase Firestore. Настраивает адаптер
     * для отображения списка товаров и обновляет общую сумму при изменении данных.
     */
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

    /**
     * Загружает список банковских карт пользователя из Firebase Firestore. Обновляет UI
     * в зависимости от наличия карт: показывает выпадающий список или кнопку добавления карты.
     */
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

    /**
     * Отображает диалоговое окно для выбора способа ввода адреса доставки. Предлагает
     * пользователю выбрать адрес на карте или ввести его вручную.
     */
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

    /**
     * Запускает активность MapSelectionActivity для выбора адреса доставки на карте.
     */
    private void startMapSelection() {
        Intent intent = new Intent(getActivity(), MapSelectionActivity.class);
        startActivityForResult(intent, MAP_SELECTION_REQUEST_CODE);
    }

    /**
     * Отображает диалоговое окно для ручного ввода адреса доставки. Позволяет пользователю
     * ввести адрес, проверяет его корректность и сохраняет при подтверждении.
     */
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
            if (validateAddress(address)) {
                deliveryAddress = address;
                deliveryLocation = null;
                updateDeliveryAddressText();
                dialog.dismiss();
                if (checkOrderConditionsExceptAddress()) {
                    processPayment();
                }
            }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Проверяет корректность введенного адреса доставки. Убеждается, что адрес содержит
     * город, улицу, дом и квартиру, и соответствует заданному формату.
     */
    private boolean validateAddress(String address) {
        if (address.isEmpty()) {
            showToast("Введите адрес");
            return false;
        }
        String[] parts = address.split(",");
        if (parts.length < 4) {
            showToast("Адрес должен содержать город, улицу, дом и квартиру, разделённые запятыми");
            return false;
        }
        boolean hasCity = false;
        boolean hasStreet = false;
        boolean hasHouse = false;
        boolean hasApartment = false;
        for (String part : parts) {
            String trimmedPart = part.trim();
            if (trimmedPart.startsWith("г.")) {
                hasCity = trimmedPart.length() > 2;
            } else if (trimmedPart.startsWith("ул.")) {
                hasStreet = trimmedPart.length() > 3;
            } else if (trimmedPart.matches(".*\\d+.*") && !trimmedPart.contains("кв.") && !trimmedPart.contains("квартира")) {
                hasHouse = true;
            } else if (trimmedPart.contains("кв.") || trimmedPart.contains("квартира") || trimmedPart.matches("\\d+")) {
                hasApartment = trimmedPart.matches(".*\\d+.*");
            }
        }
        if (!hasCity) {
            showToast("Укажите город в формате 'г. Название'");
            return false;
        }
        if (!hasStreet) {
            showToast("Укажите улицу в формате 'ул. Название'");
            return false;
        }
        if (!hasHouse) {
            showToast("Укажите номер дома");
            return false;
        }
        if (!hasApartment) {
            showToast("Укажите номер квартиры");
            return false;
        }
        return true;
    }

    /**
     * Отображает диалоговое окно для добавления новой банковской карты. Позволяет пользователю
     * ввести номер карты, CVV и срок действия, форматирует ввод и сохраняет карту при подтверждении.
     */
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
            private boolean isFormatting;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                String input = s.toString().replaceAll("[^0-9]", "");
                if (input.length() > 16) {
                    input = input.substring(0, 16);
                }
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < input.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(input.charAt(i));
                }
                etCardNumber.setText(formatted.toString());
                etCardNumber.setSelection(formatted.length());
                isFormatting = false;
            }
        });
        etCardExpiry.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                String input = s.toString().replaceAll("[^0-9]", "");
                if (input.length() > 4) {
                    input = input.substring(0, 4);
                }
                StringBuilder formatted = new StringBuilder(input);
                if (input.length() >= 2 && !s.toString().contains("/")) {
                    String monthStr = input.substring(0, 2);
                    int month = Integer.parseInt(monthStr);
                    if (month > 12) {
                        formatted = new StringBuilder("12");
                    } else {
                        formatted = new StringBuilder(monthStr);
                    }
                    if (input.length() > 2) {
                        formatted.append("/").append(input.substring(2));
                    } else {
                        formatted.append("/");
                    }
                }
                etCardExpiry.setText(formatted.toString());
                etCardExpiry.setSelection(formatted.length());
                isFormatting = false;
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

    /**
     * Проверяет корректность введенных данных банковской карты. Убеждается, что номер карты
     * содержит 16 цифр, CVV — 3 цифры, а срок действия соответствует формату и находится
     * в допустимом диапазоне.
     */
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
        String[] expiryParts = expiryDate.split("/");
        int month = Integer.parseInt(expiryParts[0]);
        int year = Integer.parseInt(expiryParts[1]);
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100;
        int maxYear = currentYear + 10;
        if (month < 1 || month > 12) {
            showToast("Месяц должен быть от 01 до 12");
            return false;
        }
        if (year < currentYear) {
            showToast("Год не может быть меньше текущего (" + currentYear + ")");
            return false;
        }
        if (year > maxYear) {
            showToast("Год не может быть больше " + maxYear + " (текущий + 10 лет)");
            return false;
        }
        return true;
    }

    /**
     * Сохраняет данные банковской карты в Firebase Firestore. Добавляет карту в коллекцию
     * "cards" и обновляет список карт в UI после успешного сохранения.
     */
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
                    loadBankCards();
                })
                .addOnFailureListener(e -> showToast("Ошибка при добавлении карты: " + e.getMessage()));
    }

    /**
     * Проверяет выполнение условий для оформления заказа, кроме адреса доставки. Убеждается,
     * что пользователь согласился с условиями, выбрал карту и корзина не пуста.
     */
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

    /**
     * Обновляет общую сумму заказа на основе данных из адаптера корзины. Учитывает стоимость
     * товаров и дополнительную защиту заказа.
     */
    private void updateTotalFromAdapter() {
        total = 0;
        for (Cart cart : orderAdapter.getCartItems()) {
            total += cart.getPrice() * cart.getQuantity();
        }
        updateTotalAmount();
    }

    /**
     * Обновляет состояние кнопки оплаты. Активирует кнопку, если выполнены все условия:
     * выбрана карта, согласовано с условиями и корзина не пуста.
     */
    private void updatePayButtonState() {
        boolean isEnabled = agreementCheckBox.isChecked() && selectedCardNumber != null && total > 0;
        payButton.setEnabled(isEnabled);
        payButton.setAlpha(isEnabled ? 1.0f : 0.5f);
    }

    /**
     * Обновляет отображение общей суммы заказа. Учитывает стоимость товаров и дополнительную
     * защиту заказа, если она включена.
     */
    private void updateTotalAmount() {
        int finalTotal = total + (isProtectionEnabled ? protectionCost : 0);
        totalAmount.setText("Итого: " + finalTotal + " ₽");
    }

    /**
     * Инициирует процесс оплаты. Проверяет авторизацию пользователя, делает снимок текущей
     * корзины, останавливает слушатель адаптера и отображает диалог подтверждения заказа.
     */
    private void processPayment() {
        String userId = getCurrentUserId();
        if (userId == null) {
            showToast("Ошибка: пользователь не авторизован");
            return;
        }
        List<Cart> cartItemsSnapshot = new ArrayList<>(orderAdapter.getCartItems());
        if (cartItemsSnapshot.isEmpty()) {
            return;
        }
        int finalTotal = total + (isProtectionEnabled ? protectionCost : 0);
        orderAdapter.stopListening();
        showConfirmationDialog(userId, cartItemsSnapshot, finalTotal);
    }

    /**
     * Отображает диалог подтверждения заказа. Показывает адрес доставки и итоговую сумму,
     * позволяя пользователю подтвердить или отменить заказ.
     */
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

    /**
     * Создает заказ в Firebase Firestore. Формирует данные заказа, включая список товаров,
     * сумму, способ оплаты, адрес доставки и случайный срок доставки, после чего очищает
     * корзину и перенаправляет пользователя в каталог.
     */
    private void createOrder(String userId, List<Cart> cartItems, int totalAmount, String cardNumber) {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", userId);
        orderData.put("products", cartItems);
        orderData.put("totalAmount", totalAmount);
        orderData.put("paymentMethod", "Карта ****" + cardNumber.substring(cardNumber.length() - 4));
        orderData.put("deliveryAddress", deliveryAddress);
        orderData.put("protectionEnabled", isProtectionEnabled);
        orderData.put("orderDate", com.google.firebase.Timestamp.now());
        int days = ThreadLocalRandom.current().nextInt(1, 7);
        orderData.put("days", (long) days);
        orderData.put("initialDays", (long) days);
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

    /**
     * Интерфейс для обработки результатов обновления количества товаров в процессе оформления заказа.
     */
    private interface ProductUpdateCallback {
        void onAllProductsUpdated();
        void onUpdateFailed(Exception e);
    }

    /**
     * Обновляет количество товаров в Firebase Firestore в рамках транзакции. Уменьшает
     * количество доступных товаров на основе заказанного количества и вызывает callback
     * по завершении.
     */
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
            });
        }
    }

    /**
     * Очищает корзину пользователя в Firebase Firestore после успешного оформления заказа.
     * Удаляет все записи корзины, связанные с текущим пользователем.
     */
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

    /**
     * Перенаправляет пользователя в фрагмент каталога после успешного оформления заказа.
     * Передает данные пользователя в новый фрагмент.
     */
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

    /**
     * Возвращает идентификатор текущего пользователя.
     */
    private String getCurrentUserId() {
        return userDocumentId;
    }

    /**
     * Отображает всплывающее сообщение с заданным текстом.
     */
    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Вызывается при запуске фрагмента. Запускает слушатель адаптера для обновления данных корзины.
     */
    @Override
    public void onStart() {
        super.onStart();
        if (orderAdapter != null) {
            orderAdapter.startListening();
        }
    }

    /**
     * Вызывается при остановке фрагмента. Останавливает слушатель адаптера для предотвращения
     * утечек памяти.
     */
    @Override
    public void onStop() {
        super.onStop();
        if (orderAdapter != null) {
            orderAdapter.stopListening();
        }
    }
}