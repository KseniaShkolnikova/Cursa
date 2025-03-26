package com.example.ozon;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends Fragment {

    private static final String STATUS_CREATED = "создан";
    private static final String STATUS_DELIVERED = "доставлен";

    private TextView tvUserName, tvUserLogin, tvUserPassword;
    private FirebaseFirestore db;
    private String userDocumentId;
    private String userRole;
    private LinearLayout completedOrdersContainer, inDeliveryOrdersContainer, cardsContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_activity, container, false);

        db = FirebaseFirestore.getInstance();

        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("USER_DOCUMENT_ID");
            userRole = bundle.getString("USER_ROLE");
        }

        initializeViews(view);
        setupMenuButton(view);
        setupAddCardButton(view);
        loadUserData();
        loadUserCards();
        loadUserOrders();

        return view;
    }

    private void initializeViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserLogin = view.findViewById(R.id.tvUserLogin);
        tvUserPassword = view.findViewById(R.id.tvUserPassword);
        completedOrdersContainer = view.findViewById(R.id.completedOrdersContainer);
        inDeliveryOrdersContainer = view.findViewById(R.id.inDeliveryOrdersContainer);
        cardsContainer = view.findViewById(R.id.cardsContainer);
    }

    private void setupMenuButton(View view) {
        ImageView btnMenu = view.findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> showPopupMenu(v));
    }

    private void setupAddCardButton(View view) {
        Button btnAddCard = view.findViewById(R.id.btnAddCard);
        btnAddCard.setOnClickListener(v -> showAddCardDialog());
    }

    private void loadUserData() {
        if (userDocumentId == null) return;

        db.collection("users").document(userDocumentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            tvUserName.setText(document.getString("name"));
                            tvUserLogin.setText("Логин: " + document.getString("email"));
                            tvUserPassword.setText("Пароль: " + document.getString("password"));
                        }
                    }
                });
    }

    private void loadUserOrders() {
        if (userDocumentId == null) return;

        // Заказы в доставке
        db.collection("orders")
                .whereEqualTo("userId", userDocumentId)
                .whereEqualTo("status", STATUS_CREATED)
                .get()
                .addOnCompleteListener(this::processInDeliveryOrders);

        // Завершенные заказы
        db.collection("orders")
                .whereEqualTo("userId", userDocumentId)
                .whereEqualTo("status", STATUS_DELIVERED)
                .get()
                .addOnCompleteListener(this::processCompletedOrders);
    }

    private void processInDeliveryOrders(com.google.android.gms.tasks.Task<QuerySnapshot> task) {
        if (task.isSuccessful()) {
            inDeliveryOrdersContainer.removeAllViews();
            if (task.getResult().isEmpty()) {
                addEmptyMessage(inDeliveryOrdersContainer, "Нет заказов в доставке");
            } else {
                for (DocumentSnapshot document : task.getResult()) {
                    addOrderCard(inDeliveryOrdersContainer, document, true);
                }
            }
        }
    }

    private void processCompletedOrders(com.google.android.gms.tasks.Task<QuerySnapshot> task) {
        if (task.isSuccessful()) {
            completedOrdersContainer.removeAllViews();
            if (task.getResult().isEmpty()) {
                addEmptyMessage(completedOrdersContainer, "Нет выполненных заказов");
            } else {
                for (DocumentSnapshot document : task.getResult()) {
                    addOrderCard(completedOrdersContainer, document, false);
                }
            }
        }
    }

    private void addOrderCard(LinearLayout container, DocumentSnapshot document, boolean showDeliveryDays) {
        View orderView = LayoutInflater.from(requireContext()).inflate(R.layout.item_order_profile, container, false);

        TextView tvOrderStatus = orderView.findViewById(R.id.tvOrderStatus);
        TextView tvOrderDetails = orderView.findViewById(R.id.tvOrderDetails);
        RecyclerView rvProducts = orderView.findViewById(R.id.productsRecyclerView);

        String deliveryAddress = document.getString("deliveryAddress") != null ?
                document.getString("deliveryAddress") : "Адрес не указан";
        Long totalAmount = document.getLong("totalAmount") != null ?
                document.getLong("totalAmount") : 0L;
        Long days = document.getLong("days");
        String status = document.getString("status") != null ?
                document.getString("status") : "Неизвестный статус";

        StringBuilder details = new StringBuilder()
                .append("Адрес: ").append(deliveryAddress).append("\n")
                .append("Сумма: ").append(totalAmount).append(" ₽");

        if (showDeliveryDays && days != null) {
            details.append("\nДней до доставки: ").append(days);
        }

        tvOrderStatus.setText(status);
        tvOrderDetails.setText(details.toString());

        rvProducts.setLayoutManager(new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        ));

        List<Map<String, Object>> productMaps = (List<Map<String, Object>>) document.get("products");
        List<Product> productList = new ArrayList<>();

        if (productMaps != null) {
            for (Map<String, Object> productMap : productMaps) {
                try {
                    Product product = new Product(
                            (String) productMap.get("name"),
                            ((Long) productMap.getOrDefault("price", 0L)).intValue(),
                            null,
                            (String) productMap.get("imageBase64"),
                            null,
                            ((Long) productMap.getOrDefault("quantity", 1L)).intValue()
                    );
                    productList.add(product);
                } catch (Exception e) {
                    Log.e("OrderError", "Error parsing product", e);
                }
            }
        }

        ProductAdapter productAdapter = new ProductAdapter(userDocumentId);
        productAdapter.updateData(productList, null);
        rvProducts.setAdapter(productAdapter);

        container.addView(orderView);
    }

    private void addEmptyMessage(LinearLayout container, String message) {
        TextView emptyView = new TextView(requireContext());
        emptyView.setText(message);
        emptyView.setTextSize(16);
        emptyView.setPadding(16, 16, 16, 16);
        container.addView(emptyView);
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_account, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_logout) {
                logoutUser();
                return true;
            } else if (id == R.id.action_register_seller) {
                checkIfSellerExistsBeforeRegistration();
                return true;
            } else if (id == R.id.action_edit_account) {
                showEditAccountDialog();
                return true;
            } else if (id == R.id.action_change_password) {
                showConfirmPasswordDialog();
                return true;
            } else if (id == R.id.action_delete_account) {
                deleteAccount();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void logoutUser() {
        Toast.makeText(requireContext(), "Выход из аккаунта", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireContext(), MainActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    private void checkIfSellerExistsBeforeRegistration() {
        db.collection("users")
                .whereEqualTo("userId", userDocumentId)
                .whereEqualTo("role", "seller")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            showRegisterSellerDialog();
                        } else {
                            Toast.makeText(requireContext(), "Вы уже зарегистрированы как продавец", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showRegisterSellerDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.seller_registration_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        EditText etStoreName = dialogView.findViewById(R.id.etStoreName);
        EditText etLastName = dialogView.findViewById(R.id.etLastName);
        EditText etFirstName = dialogView.findViewById(R.id.etFirstName);
        EditText etMiddleName = dialogView.findViewById(R.id.etMiddleName);
        EditText etOGRNIP = dialogView.findViewById(R.id.etOGRNIP);
        EditText etINN = dialogView.findViewById(R.id.etINN);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnCreateAccount = dialogView.findViewById(R.id.btnCreateAccount);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnCreateAccount.setOnClickListener(v -> {
            String storeName = etStoreName.getText().toString();
            String lastName = etLastName.getText().toString();
            String firstName = etFirstName.getText().toString();
            String middleName = etMiddleName.getText().toString();
            String ogrnip = etOGRNIP.getText().toString();
            String inn = etINN.getText().toString();

            if (validateSellerInput(storeName, lastName, firstName, ogrnip, inn)) {
                registerSeller(storeName, lastName, firstName, middleName, ogrnip, inn);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private boolean validateSellerInput(String storeName, String lastName,
                                        String firstName, String ogrnip, String inn) {
        if (storeName.isEmpty() || lastName.isEmpty() ||
                firstName.isEmpty() || ogrnip.isEmpty() || inn.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните все обязательные поля", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void registerSeller(String storeName, String lastName, String firstName,
                                String middleName, String ogrnip, String inn) {
        db.collection("users").document(userDocumentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> sellerData = new HashMap<>();
                            sellerData.put("email", document.getString("email"));
                            sellerData.put("password", document.getString("password"));
                            sellerData.put("role", "seller");
                            sellerData.put("storeName", storeName);
                            sellerData.put("lastName", lastName);
                            sellerData.put("firstName", firstName);
                            sellerData.put("middleName", middleName.isEmpty() ? null : middleName);
                            sellerData.put("ogrnip", ogrnip);
                            sellerData.put("inn", inn);
                            sellerData.put("userId", userDocumentId);

                            db.collection("users")
                                    .add(sellerData)
                                    .addOnSuccessListener(documentReference -> {
                                        navigateToSellerActivity(documentReference.getId());
                                    });
                        }
                    }
                });
    }

    private void navigateToSellerActivity(String sellerDocumentId) {
        Toast.makeText(requireContext(), "Аккаунт продавца успешно создан", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireContext(), SellerMainActivity.class);
        intent.putExtra("USER_DOCUMENT_ID", sellerDocumentId);
        intent.putExtra("USER_ROLE", "seller");
        startActivity(intent);
        requireActivity().finish();
    }

    private void showEditAccountDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.edit_customer_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        EditText etEditName = dialogView.findViewById(R.id.etEditName);
        EditText etEditEmail = dialogView.findViewById(R.id.etEditEmail);
        EditText etEditPassword = dialogView.findViewById(R.id.etEditPassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        etEditName.setText(tvUserName.getText().toString());
        etEditEmail.setText(tvUserLogin.getText().toString().replace("Логин: ", ""));
        etEditPassword.setText(tvUserPassword.getText().toString().replace("Пароль: ", ""));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String newName = etEditName.getText().toString();
            String newEmail = etEditEmail.getText().toString();
            String newPassword = etEditPassword.getText().toString();

            if (validateAccountInput(newName, newEmail, newPassword)) {
                updateUserData(newName, newEmail, newPassword);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private boolean validateAccountInput(String name, String email, String password) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void updateUserData(String newName, String newEmail, String newPassword) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("email", newEmail);
        updates.put("password", newPassword);

        db.collection("users").document(userDocumentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Данные успешно обновлены", Toast.LENGTH_SHORT).show();
                    loadUserData();
                });
    }

    private void showConfirmPasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.confirm_password_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        EditText etOldPassword = dialogView.findViewById(R.id.etOldPassword);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnForgotPassword = dialogView.findViewById(R.id.btnForgotPassword);

        btnConfirm.setOnClickListener(v -> {
            String oldPassword = etOldPassword.getText().toString().trim();
            if (oldPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Введите старый пароль", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyOldPassword(oldPassword, dialog);
        });

        btnForgotPassword.setOnClickListener(v -> {
            dialog.dismiss();
            showForgotPasswordDialog(tvUserLogin.getText().toString().replace("Логин: ", ""));
        });

        dialog.show();
    }

    private void verifyOldPassword(String oldPassword, AlertDialog dialog) {
        db.collection("users").document(userDocumentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists() && document.getString("password").equals(oldPassword)) {
                            dialog.dismiss();
                            showChangePasswordDialog();
                        } else {
                            Toast.makeText(requireContext(), "Неверный пароль", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showForgotPasswordDialog(String email) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.password_recovery, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        EditText emailField = view.findViewById(R.id.loginField);
        EditText codeField = view.findViewById(R.id.codeField);
        Button sendCodeButton = view.findViewById(R.id.sendCodeButton);
        Button changePasswordButton = view.findViewById(R.id.changePasswordButton);

        emailField.setText(email);
        emailField.setEnabled(false);
        changePasswordButton.setEnabled(false);
        changePasswordButton.setAlpha(0.5f);

        String[] generatedCode = {null};

        sendCodeButton.setOnClickListener(v -> {
            generatedCode[0] = String.valueOf(10000 + (int)(Math.random() * 90000));
            sendPasswordRecoveryEmail(email, generatedCode[0]);
            Toast.makeText(requireContext(), "Код отправлен на " + email, Toast.LENGTH_SHORT).show();
            changePasswordButton.setEnabled(true);
            changePasswordButton.setAlpha(1.0f);
        });

        changePasswordButton.setOnClickListener(v -> {
            if (generatedCode[0] != null && generatedCode[0].equals(codeField.getText().toString())) {
                dialog.dismiss();
                showChangePasswordDialog();
            } else {
                Toast.makeText(requireContext(), "Неверный код", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void sendPasswordRecoveryEmail(String email, String code) {
        // Реализация отправки email с кодом
        // Можно использовать JavaMail API или Firebase Auth sendPasswordResetEmail
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.change_password_layout, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        EditText newPasswordField = dialogView.findViewById(R.id.newPasswordField);
        EditText confirmPasswordField = dialogView.findViewById(R.id.confirmPasswordField);
        Button btnSave = dialogView.findViewById(R.id.savePasswordButton);

        btnSave.setOnClickListener(v -> {
            String newPassword = newPasswordField.getText().toString().trim();
            String confirmPassword = confirmPasswordField.getText().toString().trim();

            if (validateNewPassword(newPassword, confirmPassword)) {
                updatePassword(newPassword);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private boolean validateNewPassword(String newPassword, String confirmPassword) {
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(requireContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void updatePassword(String newPassword) {
        db.collection("users").document(userDocumentId)
                .update("password", newPassword)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Пароль успешно изменен", Toast.LENGTH_SHORT).show();
                    loadUserData();
                });
    }

    private void deleteAccount() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удаление аккаунта")
                .setMessage("Вы уверены, что хотите удалить аккаунт? Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    db.collection("users").document(userDocumentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(requireContext(), "Аккаунт успешно удален", Toast.LENGTH_SHORT).show();
                                logoutUser();
                            });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showAddCardDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.add_card_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        EditText etCardNumber = dialogView.findViewById(R.id.etCardNumber);
        EditText etCardCVV = dialogView.findViewById(R.id.etCardCVV);
        EditText etCardExpiry = dialogView.findViewById(R.id.etCardExpiry);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnAdd.setOnClickListener(v -> {
            String cardNumber = etCardNumber.getText().toString();
            String cardCVV = etCardCVV.getText().toString();
            String cardExpiry = etCardExpiry.getText().toString();

            if (validateCardInput(cardNumber, cardCVV, cardExpiry)) {
                saveCardToFirestore(cardNumber, cardCVV, cardExpiry);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private boolean validateCardInput(String cardNumber, String cardCVV, String cardExpiry) {
        if (cardNumber.isEmpty() || cardCVV.isEmpty() || cardExpiry.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveCardToFirestore(String cardNumber, String cardCVV, String cardExpiry) {
        Map<String, Object> cardData = new HashMap<>();
        cardData.put("cardNumber", cardNumber);
        cardData.put("cvv", cardCVV);
        cardData.put("expiryDate", cardExpiry);
        cardData.put("userId", userDocumentId);

        db.collection("cards")
                .add(cardData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "Карта успешно добавлена", Toast.LENGTH_SHORT).show();
                    loadUserCards();
                });
    }

    private void loadUserCards() {
        if (userDocumentId == null) return;

        db.collection("cards")
                .whereEqualTo("userId", userDocumentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cardsContainer.removeAllViews();
                        for (DocumentSnapshot document : task.getResult()) {
                            addCardToView(document);
                        }
                    }
                });
    }

    private void addCardToView(DocumentSnapshot document) {
        String cardNumber = document.getString("cardNumber");
        String cardId = document.getId();

        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_card, null);
        TextView tvCardNumber = cardView.findViewById(R.id.tvCardNumber);
        tvCardNumber.setText("Карта **** " + cardNumber.substring(cardNumber.length() - 4));

        cardView.setOnClickListener(v -> showCardOptionsMenu(v, cardId));
        cardsContainer.addView(cardView);
    }

    private void showCardOptionsMenu(View view, String cardId) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.card_delete_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_card) {
                deleteCard(cardId);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void deleteCard(String cardId) {
        db.collection("cards").document(cardId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Карта успешно удалена", Toast.LENGTH_SHORT).show();
                    loadUserCards();
                });
    }
}