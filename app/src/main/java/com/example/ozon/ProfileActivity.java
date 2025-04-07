package com.example.ozon;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.work.WorkManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
public class ProfileActivity extends Fragment {
    private static final String STATUS_CREATED = "создан";
    private static final String STATUS_DELIVERED = "доставлен";
    private SharedPreferences sharedPrefs;
    private Context context;
    private TextView tvUserName, tvUserLogin;
    private FirebaseFirestore db;
    private String userDocumentId;
    private String userRole;
    private LinearLayout completedOrdersContainer, inDeliveryOrdersContainer, cardsContainer;
    private ListenerRegistration userListener;
    private ListenerRegistration inDeliveryOrdersListener;
    private ListenerRegistration completedOrdersListener;
    private ListenerRegistration cardsListener;
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
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
        setupRealtimeListeners();

        return view;
    }
    @Override
    public void onStart() {
        super.onStart();
        setupRealtimeListeners();
    }
    @Override
    public void onStop() {
        super.onStop();
        removeRealtimeListeners();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        removeRealtimeListeners();
    }
    private void initializeViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserLogin = view.findViewById(R.id.tvUserLogin);
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
    private void setupRealtimeListeners() {
        removeRealtimeListeners();
        if (userDocumentId != null) {
            userListener = db.collection("users").document(userDocumentId)
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            return;
                        }
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            updateUserDataUI(documentSnapshot);
                        }
                    });
            inDeliveryOrdersListener = db.collection("orders")
                    .whereEqualTo("userId", userDocumentId)
                    .whereEqualTo("status", STATUS_CREATED)
                    .addSnapshotListener((querySnapshot, e) -> {
                        if (e != null) {
                            return;
                        }
                        if (querySnapshot != null) {
                            updateInDeliveryOrdersUI(querySnapshot.getDocuments());
                        }
                    });
            completedOrdersListener = db.collection("orders")
                    .whereEqualTo("userId", userDocumentId)
                    .whereEqualTo("status", STATUS_DELIVERED)
                    .addSnapshotListener((querySnapshot, e) -> {
                        if (e != null) {
                            return;
                        }
                        if (querySnapshot != null) {
                            updateCompletedOrdersUI(querySnapshot.getDocuments());
                        }
                    });
            cardsListener = db.collection("cards")
                    .whereEqualTo("userId", userDocumentId)
                    .addSnapshotListener((querySnapshot, e) -> {
                        if (e != null) {
                            return;
                        }
                        if (querySnapshot != null) {
                            updateCardsUI(querySnapshot.getDocuments());
                        }
                    });
        }
    }
    private void removeRealtimeListeners() {
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        if (inDeliveryOrdersListener != null) {
            inDeliveryOrdersListener.remove();
            inDeliveryOrdersListener = null;
        }
        if (completedOrdersListener != null) {
            completedOrdersListener.remove();
            completedOrdersListener = null;
        }
        if (cardsListener != null) {
            cardsListener.remove();
            cardsListener = null;
        }
    }
    private void updateUserDataUI(DocumentSnapshot document) {
        tvUserName.setText(document.getString("name"));
        tvUserLogin.setText("Логин: " + document.getString("email"));
    }
    private void updateInDeliveryOrdersUI(List<DocumentSnapshot> documents) {
        inDeliveryOrdersContainer.removeAllViews();
        if (documents.isEmpty()) {
            addEmptyMessage(inDeliveryOrdersContainer, "Нет заказов в доставке");
        } else {
            for (DocumentSnapshot document : documents) {
                addOrderCard(inDeliveryOrdersContainer, document, true);
            }
        }
    }
    private void updateCompletedOrdersUI(List<DocumentSnapshot> documents) {
        completedOrdersContainer.removeAllViews();
        if (documents.isEmpty()) {
            addEmptyMessage(completedOrdersContainer, "Нет выполненных заказов");
        } else {
            for (DocumentSnapshot document : documents) {
                addOrderCard(completedOrdersContainer, document, false);
            }
        }
    }
    private void updateCardsUI(List<DocumentSnapshot> documents) {
        cardsContainer.removeAllViews();
        for (DocumentSnapshot document : documents) {
            addCardToView(document);
        }
    }
    private void addOrderCard(LinearLayout container, DocumentSnapshot document, boolean showDeliveryDays) {
        View orderView = LayoutInflater.from(requireContext()).inflate(R.layout.item_order_profile, container, false);
        TextView tvOrderStatus = orderView.findViewById(R.id.tvOrderStatus);
        TextView tvOrderDetails = orderView.findViewById(R.id.tvOrderDetails);
        TextView tvDaysUntilDelivery = orderView.findViewById(R.id.tvDaysUntilDelivery);
        RecyclerView rvProducts = orderView.findViewById(R.id.productsRecyclerView);
        String deliveryAddress = document.getString("deliveryAddress") != null ?
                document.getString("deliveryAddress") : "Адрес не указан";
        Long totalAmount = document.getLong("totalAmount") != null ?
                document.getLong("totalAmount") : 0L;
        String status = document.getString("status") != null ?
                document.getString("status") : "Неизвестный статус";
        StringBuilder details = new StringBuilder()
                .append("Адрес: ").append(deliveryAddress).append("\n")
                .append("Сумма: ").append(totalAmount).append(" ₽");
        StringBuilder days = new StringBuilder();
        if (showDeliveryDays) {
            Long daysUntilDelivery = document.getLong("days");
            if (daysUntilDelivery != null) {
                days.append("\nДней до доставки: ").append(daysUntilDelivery);
            }
            tvDaysUntilDelivery.setVisibility(View.VISIBLE);
            tvDaysUntilDelivery.setText(days.toString());
        } else {
            tvDaysUntilDelivery.setVisibility(View.GONE);
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
        SharedPreferences sharedPrefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        sharedPrefs.edit().clear().apply();
        WorkManager.getInstance(requireContext()).cancelAllWork();
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
            String storeName = etStoreName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String firstName = etFirstName.getText().toString().trim();
            String middleName = etMiddleName.getText().toString().trim();
            String ogrnip = etOGRNIP.getText().toString().trim();
            String inn = etINN.getText().toString().trim();
            if (validateSellerInput(storeName, lastName, firstName, middleName, ogrnip, inn)) {
                registerSeller(storeName, lastName, firstName, middleName, ogrnip, inn);
                dialog.dismiss();
            }
        });

        dialog.show();
    }
    private boolean validateSellerInput(String storeName, String lastName, String firstName, String middleName, String ogrnip, String inn) {
        if (storeName.isEmpty()) {
            Toast.makeText(requireContext(), "Наименование магазина должно содержать хотя бы 1 символ", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (lastName.length() < 2 || !lastName.matches("[a-zA-Zа-яА-Я]+")) {
            Toast.makeText(requireContext(), "Фамилия должна содержать не менее 2 букв и только буквы", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (firstName.length() < 2 || !firstName.matches("[a-zA-Zа-яА-Я]+")) {
            Toast.makeText(requireContext(), "Имя должно содержать не менее 2 букв и только буквы", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!middleName.isEmpty() && (middleName.length() < 5 || !middleName.matches("[a-zA-Zа-яА-Я]+"))) {
            Toast.makeText(requireContext(), "Отчество должно содержать не менее 5 букв и только буквы", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!ogrnip.matches("\\d{15}")) {
            Toast.makeText(requireContext(), "ОГРНИП должен содержать ровно 15 цифр", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!inn.matches("\\d{10}")) {
            Toast.makeText(requireContext(), "ИНН должен содержать ровно 10 цифр", Toast.LENGTH_SHORT).show();
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
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(requireContext(), "Ошибка при регистрации продавца: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                });
    }
    private void navigateToSellerActivity(String sellerDocumentId) {
        SharedPreferences sharedPrefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear();
        editor.putString("userId", sellerDocumentId);
        editor.putString("userRole", "seller");
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
        Toast.makeText(requireContext(), "Аккаунт продавца успешно создан", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireContext(), SellerMainActivity.class);
        intent.putExtra("USER_DOCUMENT_ID", sellerDocumentId);
        intent.putExtra("USER_ROLE", "seller");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
    private void showEditAccountDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.edit_customer_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        EditText etEditName = dialogView.findViewById(R.id.etEditName);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        etEditName.setText(tvUserName.getText().toString());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String newName = etEditName.getText().toString().trim();
            if (validateAccountInput(newName)) {
                updateUserData(newName);
                dialog.dismiss();
            }
        });
        dialog.show();
    }
    private boolean validateAccountInput(String name) {
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Имя не может быть пустым", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    private void updateUserData(String newName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        db.collection("users").document(userDocumentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Данные успешно обновлены", Toast.LENGTH_SHORT).show();
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
        Button btnCancel = dialogView.findViewById(R.id.cancelButton);
        btnForgotPassword.setVisibility(View.GONE);
        btnConfirm.setOnClickListener(v -> {
            String oldPassword = etOldPassword.getText().toString().trim();
            if (oldPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Введите старый пароль", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyOldPassword(oldPassword, dialog, btnForgotPassword);
        });
        btnForgotPassword.setOnClickListener(v -> {
            dialog.dismiss();
            showForgotPasswordDialog(tvUserLogin.getText().toString().replace("Логин: ", ""));
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    private void verifyOldPassword(String oldPassword, AlertDialog dialog, Button btnForgotPassword) {
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
                            btnForgotPassword.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(requireContext(), "Ошибка проверки пароля", Toast.LENGTH_SHORT).show();
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
        String subject = "Восстановление пароля Ozon";
        String body = "<!DOCTYPE html>" +
                "<html lang='ru'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Восстановление пароля</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;'>" +
                "<table role='presentation' cellpadding='0' cellspacing='0' style='width: 100%; max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);'>" +
                "<tr>" +
                "<td style='background-color: #005BFF; padding: 20px; text-align: center; border-top-left-radius: 8px; border-top-right-radius: 8px;'>" +
                "<h1 style='color: #ffffff; margin: 0; font-size: 24px;'>Ozon</h1>" +
                "<h2 style='color: #ffffff; margin: 5px 0 0; font-size: 18px;'>Восстановление пароля</h2>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='padding: 30px; text-align: center;'>" +
                "<h3 style='color: #001A34; font-size: 20px; margin: 0 0 10px;'>Ваш код для восстановления пароля</h3>" +
                "<p style='color: #666666; font-size: 16px; margin: 0 0 20px;'>Используйте этот код для сброса пароля в приложении Ozon:</p>" +
                "<div style='background-color: #E6F0FF; padding: 15px; border-radius: 5px; display: inline-block;'>" +
                "<span style='font-size: 24px; font-weight: bold; color: #005BFF; letter-spacing: 2px;'>" + code + "</span>" +
                "</div>" +
                "<p style='color: #666666; font-size: 14px; margin: 20px 0 0;'>Код действителен в течение 10 минут.</p>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='padding: 20px; text-align: center; background-color: #f9f9f9; border-bottom-left-radius: 8px; border-bottom-right-radius: 8px;'>" +
                "<p style='color: #999999; font-size: 12px; margin: 0;'>Если вы не запрашивали восстановление пароля, проигнорируйте это сообщение.</p>" +
                "<p style='color: #999999; font-size: 12px; margin: 5px 0 0;'>Свяжитесь с нами: <a href='mailto:support@ozon.ru' style='color: #005BFF; text-decoration: none;'>support@ozon.ru</a></p>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</body>" +
                "</html>";
        new SendEmailTask(requireContext(), email, subject, body, true).execute();
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
        if (newPassword.length() < 7) {
            Toast.makeText(requireContext(), "Пароль должен содержать минимум 7 символов", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Pattern.compile("[A-Z]").matcher(newPassword).find()) {
            Toast.makeText(requireContext(), "Пароль должен содержать хотя бы одну заглавную букву", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Pattern.compile("[a-z]").matcher(newPassword).find()) {
            Toast.makeText(requireContext(), "Пароль должен содержать хотя бы одну строчную букву", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Pattern.compile("[0-9]").matcher(newPassword).find()) {
            Toast.makeText(requireContext(), "Пароль должен содержать хотя бы одну цифру", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(newPassword).find()) {
            Toast.makeText(requireContext(), "Пароль должен содержать хотя бы один специальный символ (!@#$%^&*(),.?\":{}|<>)", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void updatePassword(String newPassword) {
        db.collection("users").document(userDocumentId)
                .update("password", newPassword)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Пароль успешно изменен", Toast.LENGTH_SHORT).show();
                });
    }
    private void deleteAccount() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удаление аккаунта")
                .setMessage("Вы уверены, что хотите удалить аккаунт? Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteUserRelatedData(() -> {
                        db.collection("users").document(userDocumentId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(requireContext(), "Аккаунт успешно удален", Toast.LENGTH_SHORT).show();
                                    logoutUser();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(requireContext(), "Ошибка при удалении аккаунта: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    private void deleteUserRelatedData(Runnable onComplete) {
        db.collection("cards")
                .whereEqualTo("userId", userDocumentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete();
                    }
                    db.collection("orders")
                            .whereEqualTo("userId", userDocumentId)
                            .get()
                            .addOnSuccessListener(orderSnapshots -> {
                                for (DocumentSnapshot orderDoc : orderSnapshots) {
                                    orderDoc.getReference().delete();
                                }
                                onComplete.run();
                            })
                            .addOnFailureListener(e -> {
                                onComplete.run();
                            });
                })
                .addOnFailureListener(e -> {
                    onComplete.run();
                });
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
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnAdd.setOnClickListener(v -> {
            String cardNumber = etCardNumber.getText().toString().replaceAll(" ", "");
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
        if (cardNumber.length() != 16) {
            Toast.makeText(requireContext(), "Номер карты должен содержать 16 цифр", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (cardCVV.length() != 3) {
            Toast.makeText(requireContext(), "CVV код должен содержать 3 цифры", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (cardExpiry.length() != 5 || !cardExpiry.contains("/")) {
            Toast.makeText(requireContext(), "Введите дату в формате ММ/ГГ", Toast.LENGTH_SHORT).show();
            return false;
        }
        String[] expiryParts = cardExpiry.split("/");
        int month = Integer.parseInt(expiryParts[0]);
        int year = Integer.parseInt(expiryParts[1]);
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100;
        int maxYear = currentYear + 10;
        if (month < 1 || month > 12) {
            Toast.makeText(requireContext(), "Месяц должен быть от 01 до 12", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (year < currentYear) {
            Toast.makeText(requireContext(), "Год не может быть меньше текущего (" + currentYear + ")", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (year > maxYear) {
            Toast.makeText(requireContext(), "Год не может быть больше " + maxYear + " (текущий + 10 лет)", Toast.LENGTH_SHORT).show();
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

                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при добавлении карты: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при удалении карты: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}