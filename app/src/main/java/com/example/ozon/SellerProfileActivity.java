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
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SellerProfileActivity extends Fragment {

    private TextView tvSellerName, tvSellerLogin, tvSellerShop, tvSellerOGRNIP, tvSellerINN;
    private ImageView btnMenu;
    private FirebaseFirestore db;
    private String userDocumentId, userRole;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.seller_profile_layout, container, false);

        db = FirebaseFirestore.getInstance();

        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("USER_DOCUMENT_ID");
            userRole = bundle.getString("USER_ROLE");
        }

        tvSellerName = view.findViewById(R.id.tvSellerName);
        tvSellerLogin = view.findViewById(R.id.tvSellerLogin);
        tvSellerShop = view.findViewById(R.id.tvSellerShop);
        tvSellerOGRNIP = view.findViewById(R.id.tvSellerOGRNIP);
        tvSellerINN = view.findViewById(R.id.tvSellerINN);
        btnMenu = view.findViewById(R.id.btnMenu);

        loadSellerData();

        btnMenu.setOnClickListener(v -> showSellerActionsMenu(v));

        return view;
    }

    private void loadSellerData() {
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
                            // Логируем все данные документа
                            for (Map.Entry<String, Object> entry : document.getData().entrySet()) {
                                Log.d("FirestoreData", entry.getKey() + ": " + entry.getValue());
                            }

                            // Проверяем, что роль пользователя - seller
                            if (userRole == null || !userRole.equals("seller")) {
                                Toast.makeText(requireContext(), "Доступ запрещен: вы не продавец", Toast.LENGTH_SHORT).show();
                                // Перенаправляем пользователя на главный экран или экран входа
                                Intent intent = new Intent(requireContext(), MainActivity.class);
                                startActivity(intent);
                                if (getActivity() != null) {
                                    getActivity().finish();
                                }
                                return;
                            }

                            String firstName = document.getString("firstName");
                            String lastName = document.getString("lastName");
                            String middleName = document.getString("middleName");
                            String email = document.getString("email");
                            String storeName = document.getString("storeName");
                            String ogrnip = document.getString("ogrnip");
                            String inn = document.getString("inn");

                            // Проверяем, что данные не null
                            if (firstName == null || lastName == null || email == null || storeName == null || ogrnip == null || inn == null) {
                                Toast.makeText(requireContext(), "Некоторые данные продавца отсутствуют", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Форматируем ФИО
                            String fullName = lastName + " " + firstName + " " + (middleName != null ? middleName : "");
                            tvSellerName.setText(fullName);
                            tvSellerLogin.setText("Логин: " + email);
                            tvSellerShop.setText("Магазин: " + storeName);
                            tvSellerOGRNIP.setText("ОГРНИП: " + ogrnip);
                            tvSellerINN.setText("ИНН: " + inn);
                        } else {
                            Toast.makeText(requireContext(), "Данные продавца не найдены", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showSellerActionsMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.seller_profile_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit_account) {
                showEditSellerDialog();
                return true;
            } else if (item.getItemId() == R.id.action_delete_account) {
                deleteSellerAccount();
                return true;
            } else if (item.getItemId() == R.id.action_logout) {
                logout();
                return true;
            } else if (item.getItemId() == R.id.action_change_password) {
                showConfirmPasswordDialog();
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
                                if (currentPassword != null && currentPassword.equals(oldPassword)) {
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
            showForgotPasswordDialog(tvSellerLogin.getText().toString().replace("Логин: ", ""));
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

        emailField.setText(email);
        emailField.setEnabled(false);

        changePasswordButton.setEnabled(false);
        changePasswordButton.setAlpha(0.5f);

        AlertDialog dialog = builder.create();

        String[] generatedCode = {null};

        sendCodeButton.setOnClickListener(v -> {
            String enteredEmail = emailField.getText().toString().trim();
            if (enteredEmail.isEmpty()) {
                Toast.makeText(requireContext(), "Введите email", Toast.LENGTH_SHORT).show();
            } else {
                generatedCode[0] = generateVerificationCode();
                sendPasswordRecoveryEmail(enteredEmail, generatedCode[0]);
                Toast.makeText(requireContext(), "Код отправлен на " + enteredEmail, Toast.LENGTH_SHORT).show();

                changePasswordButton.setEnabled(true);
                changePasswordButton.setAlpha(1.0f);
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

    private void showEditSellerDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.edit_sellet_profile_layout, null);

        EditText etEditLastName = dialogView.findViewById(R.id.etEditLastName);
        EditText etEditFirstName = dialogView.findViewById(R.id.etEditFirstName);
        EditText etEditMiddleName = dialogView.findViewById(R.id.etEditMiddleName);
        EditText etEditEmail = dialogView.findViewById(R.id.etEditEmail);
        EditText etEditStoreName = dialogView.findViewById(R.id.etEditStoreName);
        EditText etEditOGRNIP = dialogView.findViewById(R.id.etEditOGRNIP);
        EditText etEditINN = dialogView.findViewById(R.id.etEditINN);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        String[] fullNameParts = tvSellerName.getText().toString().split(" ");
        if (fullNameParts.length >= 1) {
            etEditLastName.setText(fullNameParts[0]);
        }
        if (fullNameParts.length >= 2) {
            etEditFirstName.setText(fullNameParts[1]);
        }
        if (fullNameParts.length >= 3) {
            etEditMiddleName.setText(fullNameParts[2]);
        } else {
            etEditMiddleName.setText("");
        }
        etEditEmail.setText(tvSellerLogin.getText().toString().replace("Логин: ", ""));
        etEditStoreName.setText(tvSellerShop.getText().toString().replace("Магазин: ", ""));
        etEditOGRNIP.setText(tvSellerOGRNIP.getText().toString().replace("ОГРНИП: ", ""));
        etEditINN.setText(tvSellerINN.getText().toString().replace("ИНН: ", ""));

        btnSave.setOnClickListener(v -> {
            String newLastName = etEditLastName.getText().toString();
            String newFirstName = etEditFirstName.getText().toString();
            String newMiddleName = etEditMiddleName.getText().toString();
            String newEmail = etEditEmail.getText().toString();
            String newStoreName = etEditStoreName.getText().toString();
            String newOGRNIP = etEditOGRNIP.getText().toString();
            String newINN = etEditINN.getText().toString();

            if (newLastName.isEmpty() || newFirstName.isEmpty() || newEmail.isEmpty()  || newStoreName.isEmpty() || newOGRNIP.isEmpty() || newINN.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните все обязательные поля", Toast.LENGTH_SHORT).show();
                return;
            }

            updateSellerData(newLastName, newFirstName, newMiddleName, newEmail, newStoreName, newOGRNIP, newINN);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateSellerData(String lastName, String firstName, String middleName, String email,  String storeName, String ogrnip, String inn) {
        if (userDocumentId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastName", lastName);
        updates.put("firstName", firstName);
        updates.put("middleName", middleName);
        updates.put("email", email);
        updates.put("storeName", storeName);
        updates.put("ogrnip", ogrnip);
        updates.put("inn", inn);

        db.collection("users").document(userDocumentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Данные успешно обновлены", Toast.LENGTH_SHORT).show();
                    loadSellerData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при обновлении данных: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteSellerAccount() {
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

    private void logout() {
        Toast.makeText(requireContext(), "Выход из аккаунта", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireContext(), MainActivity.class);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}