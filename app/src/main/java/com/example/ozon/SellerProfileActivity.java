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

public class SellerProfileActivity extends Fragment {

    private TextView tvSellerName, tvSellerLogin, tvSellerShop, tvSellerOGRNIP, tvSellerINN;
    private ImageView btnMenu;
    private FirebaseFirestore db;
    private String userDocumentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.seller_profile_layout, container, false);

        db = FirebaseFirestore.getInstance();

        Bundle bundle = getArguments();
        if (bundle != null) {
            userDocumentId = bundle.getString("USER_DOCUMENT_ID");
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
                            String firstName = document.getString("firstName");
                            String lastName = document.getString("lastName");
                            String middleName = document.getString("middleName");
                            String email = document.getString("email");
                            String password = document.getString("password");
                            String storeName = document.getString("storeName");
                            String ogrnip = document.getString("ogrnip");
                            String inn = document.getString("inn");

                            // Форматируем ФИО
                            String fullName = lastName + " " + firstName + " " + middleName;
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
            }
            return false;
        });

        popupMenu.show();
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

        // Заполняем поля текущими данными
        String[] fullNameParts = tvSellerName.getText().toString().split(" ");
        etEditLastName.setText(fullNameParts[0]); // Фамилия
        etEditFirstName.setText(fullNameParts[1]); // Имя
        etEditMiddleName.setText(fullNameParts[2]); // Отчество
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
                    loadSellerData(); // Перезагружаем данные
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