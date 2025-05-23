package com.example.ozon;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Класс SellerProfileActivity представляет собой фрагмент для отображения профиля продавца
 * в приложении "OZON". Позволяет продавцу просматривать свои данные,
 * график выручки по товарам, редактировать профиль, изменять пароль и удалять аккаунт.
 */
public class SellerProfileActivity extends Fragment {
    private TextView tvSellerName, tvSellerLogin, tvSellerShop, tvSellerOGRNIP, tvSellerINN;
    private ImageView btnMenu;
    private Spinner productSpinner;
    private LineChart revenueChart;
    private FirebaseFirestore db;
    private String userDocumentId, userRole;
    private List<String> productIds = new ArrayList<>();
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    private Map<String, String> productIdToNameMap = new HashMap<>();
    private Map<String, List<ProductRevenue>> productRevenueMap = new HashMap<>();
    private List<Order> cachedOrders;

    /**
     * Создает и возвращает представление фрагмента. Инициализирует элементы UI,
     * извлекает данные о пользователе из аргументов, загружает данные продавца и товаров,
     * а также настраивает меню действий.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
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
            productSpinner = view.findViewById(R.id.productSpinner);
            revenueChart = view.findViewById(R.id.revenueChart);
            loadSellerData();
            loadSellerProducts();
            btnMenu.setOnClickListener(v -> showSellerActionsMenu(v));
            return view;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Загружает список товаров продавца из Firebase Firestore. Подготавливает данные
     * для отображения в выпадающем списке и на графике выручки.
     */
    private void loadSellerProducts() {
        if (!isAdded() || userDocumentId == null) {
            return;
        }
        revenueChart.clear();
        revenueChart.setNoDataText("График выгружается...");
        revenueChart.invalidate();

        db.collection("products")
                .whereEqualTo("sellerId", userDocumentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) {
                        return;
                    }
                    productIds.clear();
                    productIdToNameMap.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(requireContext(), "У вас нет товаров", Toast.LENGTH_SHORT).show();
                        setupProductSpinner();
                        return;
                    }
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String productId = document.getId();
                        String productName = document.getString("name");
                        if (productId == null || productName == null) {
                            continue;
                        }
                        productIds.add(productId);
                        productIdToNameMap.put(productId, productName);
                    }
                    setupProductSpinner();
                    if (!productIds.isEmpty()) {
                        loadOrdersData();
                    } else {
                        revenueChart.clear();
                        revenueChart.setNoDataText("Нет товаров для отображения");
                        revenueChart.invalidate();
                        Toast.makeText(requireContext(), "Нет товаров для отображения", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Ошибка загрузки товаров: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        revenueChart.clear();
                        revenueChart.setNoDataText("Ошибка загрузки данных");
                        revenueChart.invalidate();
                    }
                });
    }

    /**
     * Загружает данные продавца из Firebase Firestore. Обновляет UI с информацией
     * о продавце, включая имя, email, название магазина, ОГРНИП и ИНН.
     */
    private void loadSellerData() {
        if (!isAdded() || userDocumentId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").document(userDocumentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            if (userRole == null || !userRole.equals("seller")) {
                                Toast.makeText(requireContext(), "Доступ запрещен: вы не продавец", Toast.LENGTH_SHORT).show();
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
                            if (firstName == null || lastName == null || email == null || storeName == null || ogrnip == null || inn == null) {
                                Toast.makeText(requireContext(), "Некоторые данные продавца отсутствуют", Toast.LENGTH_SHORT).show();
                                return;
                            }
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

    /**
     * Загружает данные о заказах за последние 14 дней из Firebase Firestore.
     * Использует кэшированные данные, если они доступны, и вызывает обработку заказов.
     */
    private void loadOrdersData() {
        if (!isAdded()) {
            return;
        }
        if (cachedOrders != null) {
            processOrdersData(cachedOrders);
            return;
        }
        revenueChart.clear();
        revenueChart.setNoDataText("График выгружается...");
        revenueChart.invalidate();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -14);
        Date startDate = calendar.getTime();
        db.collection("orders")
                .whereGreaterThanOrEqualTo("orderDate", startDate)
                .orderBy("orderDate", Query.Direction.ASCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) {
                        return;
                    }
                    productRevenueMap.clear();
                    cachedOrders = new ArrayList<>();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(requireContext(), "Нет заказов за последние 14 дней", Toast.LENGTH_SHORT).show();
                        if (!productIds.isEmpty()) {
                            updateChart(productIds.get(0));
                        }
                        return;
                    }
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Order order = document.toObject(Order.class);
                        order.setId(document.getId());
                        cachedOrders.add(order);
                    }
                    processOrdersData(cachedOrders);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Ошибка загрузки заказов: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        revenueChart.clear();
                        revenueChart.setNoDataText("Ошибка загрузки данных");
                        revenueChart.invalidate();
                    }
                });
    }

    /**
     * Обрабатывает данные о заказах. Вычисляет выручку по каждому товару и обновляет график
     * для первого товара в списке или выбранного товара.
     */
    private void processOrdersData(List<Order> orders) {
        if (!isAdded()) {
            return;
        }
        revenueChart.clear();
        revenueChart.setNoDataText("График выгружается...");
        revenueChart.invalidate();

        productRevenueMap.clear();
        for (Order order : orders) {
            List<Map<String, Object>> products = order.getProducts();
            if (products == null || products.isEmpty()) {
                continue;
            }
            Date orderDate = order.getOrderDate();
            if (orderDate == null) {
                continue;
            }
            for (Map<String, Object> product : products) {
                String productId = (String) product.get("productId");
                Object quantityObj = product.get("quantity");
                Object priceObj = product.get("price");
                if (productId == null || quantityObj == null || priceObj == null) {
                    continue;
                }
                if (!productIds.contains(productId)) {
                    continue;
                }
                long quantity = (quantityObj instanceof Long) ? (Long) quantityObj : Long.parseLong(quantityObj.toString());
                long price = (priceObj instanceof Long) ? (Long) priceObj : Long.parseLong(priceObj.toString());
                long revenue = quantity * price;
                productRevenueMap.putIfAbsent(productId, new ArrayList<>());
                productRevenueMap.get(productId).add(new ProductRevenue(orderDate, revenue));
            }
        }
        if (!productIds.isEmpty()) {
            updateChart(productIds.get(0));
        } else {
            updateChart(null);
        }
    }

    /**
     * Настраивает выпадающий список товаров. Заполняет его названиями товаров и
     * устанавливает обработчик выбора товара для обновления графика.
     */
    private void setupProductSpinner() {
        if (!isAdded()) {
            return;
        }
        List<String> spinnerItems = new ArrayList<>();
        if (productIds.isEmpty()) {
            spinnerItems.add("Нет");
            productSpinner.setEnabled(false);
            revenueChart.clear();
            revenueChart.setNoDataText("Нет товаров для отображения");
            revenueChart.invalidate();
        } else {
            for (String productId : productIds) {
                String productName = productIdToNameMap.get(productId);
                spinnerItems.add(productName != null ? productName : productId);
            }
            productSpinner.setEnabled(true);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, spinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        productSpinner.setAdapter(adapter);

        productSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isAdded()) {
                    return;
                }
                if (productIds.isEmpty()) {
                    updateChart(null);
                } else {
                    String selectedProductId = productIds.get(position);
                    updateChart(selectedProductId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * Обновляет график выручки для выбранного товара. Отображает данные о выручке
     * за последние 14 дней или сообщение об отсутствии данных.
     */
    private void updateChart(String productId) {
        if (!isAdded()) {
            return;
        }

        if (productId == null) {
            revenueChart.clear();
            revenueChart.setNoDataText("Нет товаров для отображения");
            revenueChart.invalidate();
            return;
        }

        List<ProductRevenue> revenues = productRevenueMap.get(productId);
        if (revenues == null || revenues.isEmpty()) {
            revenueChart.clear();
            revenueChart.setNoDataText("Загрузка");
            revenueChart.invalidate();

            Handler handler = new Handler(Looper.getMainLooper());
            Runnable timeoutRunnable = () -> {
                if (isAdded()) {
                    revenueChart.clear();
                    revenueChart.setNoDataText("Нет данных для товара");
                    revenueChart.invalidate();
                }
            };
            handler.postDelayed(timeoutRunnable, 10 * 1000);

            return;
        }

        List<Entry> entries = new ArrayList<>();
        Map<Integer, Long> dailyRevenue = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -14);
        Date startDate = calendar.getTime();
        int daysInPeriod = 14;

        for (int i = 0; i < daysInPeriod; i++) {
            dailyRevenue.put(i, 0L);
        }

        for (ProductRevenue revenue : revenues) {
            long diffInMillis = revenue.date.getTime() - startDate.getTime();
            int dayIndex = (int) (diffInMillis / (1000 * 60 * 60 * 24));
            if (dayIndex >= 0 && dayIndex < daysInPeriod) {
                dailyRevenue.put(dayIndex, dailyRevenue.get(dayIndex) + revenue.revenue);
            }
        }

        for (int i = 0; i < daysInPeriod; i++) {
            entries.add(new Entry(i, dailyRevenue.get(i)));
        }

        if (entries.isEmpty()) {
            revenueChart.clear();
            revenueChart.setNoDataText("Загрузка");
            revenueChart.invalidate();

            Handler handler = new Handler(Looper.getMainLooper());
            Runnable timeoutRunnable = () -> {
                if (isAdded()) {
                    revenueChart.clear();
                    revenueChart.setNoDataText("Нет данных для товара");
                    revenueChart.invalidate();
                }
            };
            handler.postDelayed(timeoutRunnable, 10 * 1000);

            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Выручка (руб.)");
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_dark));
        dataSet.setValueTextSize(10f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(getResources().getColor(android.R.color.holo_blue_dark));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        revenueChart.setData(lineData);

        XAxis xAxis = revenueChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(daysInPeriod, true);
        xAxis.setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                Calendar dateLabel = Calendar.getInstance();
                dateLabel.setTime(startDate);
                dateLabel.add(Calendar.DAY_OF_YEAR, (int) value);
                return new SimpleDateFormat("dd", Locale.getDefault()).format(dateLabel.getTime());
            }
        });

        revenueChart.getDescription().setEnabled(false);
        revenueChart.getAxisRight().setEnabled(false);
        revenueChart.getAxisLeft().setAxisMinimum(0f);
        revenueChart.setTouchEnabled(false);
        revenueChart.setDragEnabled(false);
        revenueChart.setScaleEnabled(false);
        revenueChart.setPinchZoom(false);
        revenueChart.invalidate();
    }

    /**
     * Отображает всплывающее меню с действиями продавца, такими как редактирование профиля,
     * смена пароля, удаление аккаунта и выход из системы.
     */
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

    /**
     * Отображает диалоговое окно для подтверждения старого пароля перед его изменением.
     * Проверяет введенный пароль и открывает диалог смены пароля при успехе.
     */
    private void showConfirmPasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.confirm_password_dialog, null);
        EditText etOldPassword = dialogView.findViewById(R.id.etOldPassword);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnForgotPassword = dialogView.findViewById(R.id.btnForgotPassword);
        Button btnCancel = dialogView.findViewById(R.id.cancelButton);
        btnForgotPassword.setVisibility(View.GONE);
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
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Отображает диалоговое окно для восстановления пароля. Отправляет код на email продавца
     * и позволяет ввести код для смены пароля.
     */
    private void showForgotPasswordDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.password_recovery, null);
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
                db.collection("users")
                        .whereEqualTo("email", enteredEmail)
                        .whereEqualTo("role", "seller")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                generatedCode[0] = generateVerificationCode();
                                sendPasswordRecoveryEmail(enteredEmail, generatedCode[0]);
                                changePasswordButton.setEnabled(true);
                                changePasswordButton.setAlpha(1.0f);
                            } else {
                                Toast.makeText(requireContext(), "Пользователь не найден или не является продавцом", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Ошибка при проверке email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
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

    /**
     * Генерирует случайный пятизначный код для восстановления пароля.
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 10000 + random.nextInt(90000);
        return String.valueOf(code);
    }

    /**
     * Отправляет email с кодом для восстановления пароля. Формирует HTML-сообщение с кодом
     * и отправляет его через SendEmailTask.
     */
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

    /**
     * Отображает диалоговое окно для смены пароля. Позволяет продавцу ввести новый пароль
     * и подтвердить его.
     */
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
            if (validateNewPassword(newPassword, confirmPassword)) {
                updatePassword(newPassword);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    /**
     * Проверяет корректность нового пароля. Убеждается, что пароль соответствует требованиям
     * по длине, наличию заглавных и строчных букв, цифр и специальных символов.
     */
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

    /**
     * Обновляет пароль продавца в Firebase Firestore. Сохраняет новый пароль.
     */
    private void updatePassword(String newPassword) {
        if (!isAdded() || userDocumentId == null) {
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

    /**
     * Отображает диалоговое окно для редактирования данных продавца. Позволяет изменить
     * ФИО, название магазина, ОГРНИП и ИНН.
     */
    private void showEditSellerDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.edit_sellet_profile_layout, null);
        EditText etEditLastName = dialogView.findViewById(R.id.etEditLastName);
        EditText etEditFirstName = dialogView.findViewById(R.id.etEditFirstName);
        EditText etEditMiddleName = dialogView.findViewById(R.id.etEditMiddleName);
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
        etEditStoreName.setText(tvSellerShop.getText().toString().replace("Магазин: ", ""));
        etEditOGRNIP.setText(tvSellerOGRNIP.getText().toString().replace("ОГРНИП: ", ""));
        etEditINN.setText(tvSellerINN.getText().toString().replace("ИНН: ", ""));
        btnSave.setOnClickListener(v -> {
            String newLastName = etEditLastName.getText().toString().trim();
            String newFirstName = etEditFirstName.getText().toString().trim();
            String newMiddleName = etEditMiddleName.getText().toString().trim();
            String newStoreName = etEditStoreName.getText().toString().trim();
            String newOGRNIP = etEditOGRNIP.getText().toString().trim();
            String newINN = etEditINN.getText().toString().trim();
            if (validateSellerInput(newLastName, newFirstName, newMiddleName, newStoreName, newOGRNIP, newINN)) {
                updateSellerData(newLastName, newFirstName, newMiddleName, newStoreName, newOGRNIP, newINN);
                dialog.dismiss();
            }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Проверяет корректность введенных данных для редактирования профиля продавца.
     * Убеждается, что все поля заполнены правильно, включая формат ОГРНИП и ИНН.
     */
    private boolean validateSellerInput(String lastName, String firstName, String middleName, String storeName, String ogrnip, String inn) {
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

    /**
     * Обновляет данные продавца в Firebase Firestore. Сохраняет новые значения ФИО,
     * названия магазина, ОГРНИП и ИНН, а затем обновляет UI.
     */
    private void updateSellerData(String lastName, String firstName, String middleName, String storeName, String ogrnip, String inn) {
        if (!isAdded() || userDocumentId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastName", lastName);
        updates.put("firstName", firstName);
        updates.put("middleName", middleName.isEmpty() ? null : middleName);
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

    /**
     * Удаляет аккаунт продавца. Сначала удаляет все товары продавца, затем удаляет
     * сам аккаунт и перенаправляет на экран входа.
     */
    private void deleteSellerAccount() {
        if (!isAdded() || userDocumentId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("products")
                .whereEqualTo("sellerId", userDocumentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (queryDocumentSnapshots.isEmpty()) {
                        deleteUserAccount();
                        return;
                    }
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String productId = document.getId();
                        db.collection("products").document(productId).delete();
                    }
                    deleteUserAccount();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Ошибка при удалении товаров: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Удаляет данные пользователя из Firebase Firestore. Очищает SharedPreferences
     * и перенаправляет на экран входа.
     */
    private void deleteUserAccount() {
        if (!isAdded()) {
            return;
        }
        db.collection("users").document(userDocumentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Аккаунт успешно удален", Toast.LENGTH_SHORT).show();
                    SharedPreferences sharedPrefs = requireContext().getSharedPreferences("AppPrefs", requireContext().MODE_PRIVATE);
                    sharedPrefs.edit()
                            .putBoolean("isLoggedIn", false)
                            .remove("userId")
                            .remove("userRole")
                            .apply();
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при удалении аккаунта: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Выполняет выход продавца из системы. Очищает SharedPreferences и перенаправляет
     * на экран авторизации продавца.
     */
    private void logout() {
        if (!isAdded()) {
            return;
        }
        SharedPreferences sharedPrefs = requireContext().getSharedPreferences("AppPrefs", requireContext().MODE_PRIVATE);
        sharedPrefs.edit()
                .putBoolean("isLoggedIn", false)
                .remove("userId")
                .remove("userRole")
                .apply();
        Toast.makeText(requireContext(), "Выход из аккаунта выполнен", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireContext(), AutorizationForSellerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    /**
     * Внутренний класс ProductRevenue представляет данные о выручке по товару за определенную дату.
     */
    private static class ProductRevenue {
        Date date;
        long revenue;

        ProductRevenue(Date date, long revenue) {
            this.date = date;
            this.revenue = revenue;
        }
    }

    /**
     * Внутренний класс Order представляет заказ с его идентификатором, датой и списком товаров.
     */
    private static class Order {
        private String id;
        private Date orderDate;
        private List<Map<String, Object>> products;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Date getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(Date orderDate) {
            this.orderDate = orderDate;
        }

        public List<Map<String, Object>> getProducts() {
            return products;
        }

        public void setProducts(List<Map<String, Object>> products) {
            this.products = products;
        }
    }
}