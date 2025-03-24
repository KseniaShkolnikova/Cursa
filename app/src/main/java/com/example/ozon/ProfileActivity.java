package com.example.ozon;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ProfileActivity extends Fragment {

    private TextView tvUserName, tvUserLogin, tvUserPassword;
    private FirebaseFirestore db;
    private String userDocumentId;
    private String userRole;

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

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserLogin = view.findViewById(R.id.tvUserLogin);
        tvUserPassword = view.findViewById(R.id.tvUserPassword);
        ImageView btnMenu = view.findViewById(R.id.btnMenu);
        Button btnAddCard = view.findViewById(R.id.btnAddCard);

        loadUserData();
        loadUserCards();

        btnMenu.setOnClickListener(v -> showPopupMenu(v));
        btnAddCard.setOnClickListener(v -> showAddCardDialog());

        return view;
    }

    private void loadUserData() {
        if (userDocumentId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userDocumentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String name = document.getString("name");
                            String email = document.getString("email");
                            String password = document.getString("password");

                            tvUserName.setText(name);
                            tvUserLogin.setText("Логин: " + email);
                            tvUserPassword.setText("Пароль: " + password);
                        } else {
                            Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_account, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                Toast.makeText(requireContext(), "Выход из аккаунта", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(requireContext(), MainActivity.class);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
                return true;
            } else if (item.getItemId() == R.id.action_register_seller) {
                checkIfSellerExistsBeforeRegistration();
                return true;
            } else if (item.getItemId() == R.id.action_edit_account) {
                showEditAccountDialog();
                return true;
            } else if (item.getItemId() == R.id.action_change_password) {
                showConfirmPasswordDialog();
                return true;
            } else if (item.getItemId() == R.id.action_delete_account) {
                deleteAccount();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void showConfirmPasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.confirm_password_dialog, null);

        EditText etOldPassword = dialogView.findViewById(R.id.etOldPassword);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnForgotPassword = dialogView.findViewById(R.id.btnForgotPassword);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnConfirm.setOnClickListener(v -> {
            String oldPassword = etOldPassword.getText().toString().trim();
            if (oldPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Введите старый пароль", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").document(userDocumentId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String currentPassword = document.getString("password");
                                if (currentPassword.equals(oldPassword)) {
                                    dialog.dismiss();
                                    showChangePasswordDialog();
                                } else {
                                    Toast.makeText(requireContext(), "Неверный пароль", Toast.LENGTH_SHORT).show();
                                    btnForgotPassword.setVisibility(View.VISIBLE);
                                }
                            } else {
                                Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireContext(), "Ошибка при проверке пароля", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnForgotPassword.setOnClickListener(v -> {
            dialog.dismiss();
            showForgotPasswordDialog(tvUserLogin.getText().toString().replace("Логин: ", ""));
        });

        dialog.show();
    }

    private void showForgotPasswordDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.password_recovery, null);
        builder.setView(view);

        EditText emailField = view.findViewById(R.id.loginField);
        EditText codeField = view.findViewById(R.id.codeField);
        Button sendCodeButton = view.findViewById(R.id.sendCodeButton);
        Button changePasswordButton = view.findViewById(R.id.changePasswordButton);

        emailField.setText(email); // Автоматическая подстановка email
        emailField.setEnabled(false); // Запрет изменения email

        // Изначально кнопка "Изменить пароль" заблокирована
        changePasswordButton.setEnabled(false);
        changePasswordButton.setAlpha(0.5f); // Визуально делаем кнопку полупрозрачной

        AlertDialog dialog = builder.create();

        // Генерация кода восстановления
        String[] generatedCode = {null}; // Массив для хранения сгенерированного кода

        sendCodeButton.setOnClickListener(v -> {
            String enteredEmail = emailField.getText().toString().trim();
            if (enteredEmail.isEmpty()) {
                Toast.makeText(requireContext(), "Введите email", Toast.LENGTH_SHORT).show();
            } else {
                // Генерация и отправка кода восстановления
                generatedCode[0] = generateVerificationCode();
                sendPasswordRecoveryEmail(enteredEmail, generatedCode[0]);
                Toast.makeText(requireContext(), "Код отправлен на " + enteredEmail, Toast.LENGTH_SHORT).show();

                // Разблокируем кнопку "Изменить пароль"
                changePasswordButton.setEnabled(true);
                changePasswordButton.setAlpha(1.0f); // Визуально делаем кнопку активной
            }
        });

        changePasswordButton.setOnClickListener(v -> {
            String enteredCode = codeField.getText().toString().trim();

            if (enteredCode.isEmpty()) {
                Toast.makeText(requireContext(), "Введите код", Toast.LENGTH_SHORT).show();
            } else if (generatedCode[0] == null) {
                Toast.makeText(requireContext(), "Сначала отправьте код", Toast.LENGTH_SHORT).show();
            } else if (!enteredCode.equals(generatedCode[0])) {
                Toast.makeText(requireContext(), "Неверный код", Toast.LENGTH_SHORT).show();
            } else {
                // Если код верный, открываем окно для смены пароля
                dialog.dismiss();
                showChangePasswordDialog();
            }
        });

        dialog.show();
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 10000 + random.nextInt(90000);
        return String.valueOf(code);
    }

    private void sendPasswordRecoveryEmail(String email, String code) {
        String subject = "Восстановление пароля";
        String body = "Код для восстановления пароля: " + code;
        new SendEmailTask(email, subject, body).execute();
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.change_password_layout, null);

        EditText newPasswordField = dialogView.findViewById(R.id.newPasswordField);
        EditText confirmPasswordField = dialogView.findViewById(R.id.confirmPasswordField);
        Button btnSave = dialogView.findViewById(R.id.savePasswordButton);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String newPassword = newPasswordField.getText().toString().trim();
            String confirmPassword = confirmPasswordField.getText().toString().trim();

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            } else if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(requireContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            } else {
                updatePassword(newPassword);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void updatePassword(String newPassword) {
        if (userDocumentId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userDocumentId)
                .update("password", newPassword)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Пароль успешно изменен", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при изменении пароля: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при проверке регистрации", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegisterSellerDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.seller_registration_dialog, null);

        EditText etStoreName = dialogView.findViewById(R.id.etStoreName);
        EditText etLastName = dialogView.findViewById(R.id.etLastName);
        EditText etFirstName = dialogView.findViewById(R.id.etFirstName);
        EditText etMiddleName = dialogView.findViewById(R.id.etMiddleName);
        EditText etOGRNIP = dialogView.findViewById(R.id.etOGRNIP);
        EditText etINN = dialogView.findViewById(R.id.etINN);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnCreateAccount = dialogView.findViewById(R.id.btnCreateAccount);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCreateAccount.setOnClickListener(v -> {
            String storeName = etStoreName.getText().toString();
            String lastName = etLastName.getText().toString();
            String firstName = etFirstName.getText().toString();
            String middleName;
            if (etMiddleName.getText().toString() != "") {
                middleName = etMiddleName.getText().toString();
            } else {
                middleName = null;
            }
            String ogrnip = etOGRNIP.getText().toString();
            String inn = etINN.getText().toString();

            if (storeName.isEmpty() || lastName.isEmpty() || firstName.isEmpty() || ogrnip.isEmpty() || inn.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните все обязательные поля", Toast.LENGTH_SHORT).show();
                return;
            }

            registerSeller(storeName, lastName, firstName, middleName, ogrnip, inn);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void registerSeller(String storeName, String lastName, String firstName, String middleName, String ogrnip, String inn) {
        if (userDocumentId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userDocumentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String email = document.getString("email");
                            String password = document.getString("password");

                            Map<String, Object> sellerData = new HashMap<>();
                            sellerData.put("email", email);
                            sellerData.put("password", password);
                            sellerData.put("role", "seller");
                            sellerData.put("storeName", storeName);
                            sellerData.put("lastName", lastName);
                            sellerData.put("firstName", firstName);
                            sellerData.put("middleName", middleName);
                            sellerData.put("ogrnip", ogrnip);
                            sellerData.put("inn", inn);
                            sellerData.put("userId", userDocumentId);

                            db.collection("users")
                                    .add(sellerData)
                                    .addOnSuccessListener(documentReference -> {
                                        String newSellerDocumentId = documentReference.getId(); // Получаем ID нового документа
                                        Toast.makeText(requireContext(), "Аккаунт продавца успешно создан", Toast.LENGTH_SHORT).show();

                                        // Запуск SellerMainActivity и передача данных
                                        Intent intent = new Intent(requireContext(), SellerMainActivity.class);
                                        intent.putExtra("USER_DOCUMENT_ID", newSellerDocumentId); // Передаем новый ID
                                        intent.putExtra("USER_ROLE", "seller"); // Роль продавца
                                        startActivity(intent);

                                        // Закрытие текущего активити, если необходимо
                                        if (getActivity() != null) {
                                            getActivity().finish();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(requireContext(), "Ошибка при создании аккаунта продавца: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при загрузке данных пользователя", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditAccountDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.edit_customer_dialog, null);

        EditText etEditName = dialogView.findViewById(R.id.etEditName);
        EditText etEditEmail = dialogView.findViewById(R.id.etEditEmail);
        EditText etEditPassword = dialogView.findViewById(R.id.etEditPassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        etEditName.setText(tvUserName.getText().toString());
        etEditEmail.setText(tvUserLogin.getText().toString().replace("Логин: ", ""));
        etEditPassword.setText(tvUserPassword.getText().toString().replace("Пароль: ", ""));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etEditName.getText().toString();
            String newEmail = etEditEmail.getText().toString();
            String newPassword = etEditPassword.getText().toString();

            if (newName.isEmpty() || newEmail.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            updateUserData(newName, newEmail, newPassword);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateUserData(String newName, String newEmail, String newPassword) {
        if (userDocumentId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("email", newEmail);
        updates.put("password", newPassword);

        db.collection("users").document(userDocumentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Данные успешно обновлены", Toast.LENGTH_SHORT).show();
                    tvUserName.setText(newName);
                    tvUserLogin.setText("Логин: " + newEmail);
                    tvUserPassword.setText("Пароль: " + newPassword);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при обновлении данных: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteAccount() {
        if (userDocumentId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").document(userDocumentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Аккаунт успешно удален", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при удалении аккаунта: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddCardDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.add_card_dialog, null);

        EditText etCardNumber = dialogView.findViewById(R.id.etCardNumber);
        EditText etCardCVV = dialogView.findViewById(R.id.etCardCVV);
        EditText etCardExpiry = dialogView.findViewById(R.id.etCardExpiry);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String cardNumber = etCardNumber.getText().toString();
            String cardCVV = etCardCVV.getText().toString();
            String cardExpiry = etCardExpiry.getText().toString();

            if (cardNumber.isEmpty() || cardCVV.isEmpty() || cardExpiry.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            saveCardToFirestore(cardNumber, cardCVV, cardExpiry);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveCardToFirestore(String cardNumber, String cardCVV, String cardExpiry) {
        if (userDocumentId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            return;
        }

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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при добавлении карты: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserCards() {
        if (userDocumentId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("cards")
                .whereEqualTo("userId", userDocumentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        LinearLayout cardsContainer = getView().findViewById(R.id.cardsContainer);
                        cardsContainer.removeAllViews();

                        for (DocumentSnapshot document : task.getResult()) {
                            String cardNumber = document.getString("cardNumber");
                            String cardId = document.getId();

                            View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_card, null);
                            TextView tvCardNumber = cardView.findViewById(R.id.tvCardNumber);
                            tvCardNumber.setText("Карта **** " + cardNumber.substring(cardNumber.length() - 4));

                            cardView.setOnClickListener(v -> showCardOptionsMenu(v, cardId));

                            cardsContainer.addView(cardView);
                        }
                    } else {
                        Toast.makeText(requireContext(), "Ошибка загрузки карт", Toast.LENGTH_SHORT).show();
                    }
                });
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при удалении карты: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}