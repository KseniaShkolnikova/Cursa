package com.example.ozon;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
public class CustomerMainActivity extends AppCompatActivity {
    private static final String TAG = "CustomerMainActivity";
    private FirebaseFirestore db;
    private String userDocumentId;
    private OrderManager orderManager;
    private Handler handler;
    private Runnable recalculateRunnable;
    private static final long RECALCULATE_INTERVAL = 6 * 1000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customer_layout);
        NotificationHelper.createNotificationChannel(this);
        db = FirebaseFirestore.getInstance();
        userDocumentId = getIntent().getStringExtra("USER_DOCUMENT_ID");
        String userRole = getIntent().getStringExtra("USER_ROLE");
        if (userDocumentId == null || userDocumentId.isEmpty()) {
            Toast.makeText(this, "Ошибка: идентификатор пользователя не найден", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        orderManager = new OrderManager(this);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            if (item.getItemId() == R.id.nav_catalog) {
                selectedFragment = new CatalogActivity();
            } else if (item.getItemId() == R.id.nav_cart) {
                selectedFragment = new CartActivity();
            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new ProfileActivity();
            }
            if (selectedFragment != null) {
                Bundle bundle = new Bundle();
                bundle.putString("USER_DOCUMENT_ID", userDocumentId);
                bundle.putString("USER_ROLE", userRole);
                selectedFragment.setArguments(bundle);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frameLayout, selectedFragment)
                        .commit();
            }
            return true;
        });
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_catalog);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
        if (NetworkUtil.isNetworkAvailable(this)) {
            recalculateUserOrders();
            startPeriodicRecalculation();
        } else {
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_LONG).show();
        }
    }
    private void startPeriodicRecalculation() {
        handler = new Handler(Looper.getMainLooper());
        recalculateRunnable = new Runnable() {
            @Override
            public void run() {
                if (NetworkUtil.isNetworkAvailable(CustomerMainActivity.this)) {
                    recalculateUserOrders();
                }
                handler.postDelayed(this, RECALCULATE_INTERVAL);
            }
        };
        handler.post(recalculateRunnable);
    }
    private void recalculateUserOrders() {
        if (userDocumentId == null || userDocumentId.isEmpty()) {
            return;
        }
        db.collection("orders")
                .whereEqualTo("userId", userDocumentId)
                .whereIn("status", Arrays.asList("создан", "в процессе", "доставлен"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        return;
                    }
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            checkAndUpdateOrder(doc);
                        }catch (Exception e){}
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка загрузки заказов: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    @SuppressLint("MissingPermission")
    private void checkAndUpdateOrder(DocumentSnapshot doc) {
        try {
            Order order = doc.toObject(Order.class);
            if (order == null) {
                return;
            }
            Date orderDate = order.getOrderDate().toDate();
            Long initialDays = order.getInitialDays();
            if (orderDate == null) {
                return;
            }
            if (initialDays == null) {
                initialDays = 0L;
                order.setInitialDays(initialDays);
            }
            Calendar currentDate = Calendar.getInstance();
            Calendar orderCalendar = Calendar.getInstance();
            orderCalendar.setTime(orderDate);
            long millisecondsDiff = currentDate.getTimeInMillis() - orderCalendar.getTimeInMillis();
            long daysSinceCreation = TimeUnit.MILLISECONDS.toDays(millisecondsDiff);
            long remainingDays = Math.max(0, initialDays - daysSinceCreation - 1);
            Long originalDays = order.getDays();
            order.setDays(remainingDays);
            Boolean notificationSent = order.getNotificationSent() != null ? order.getNotificationSent() : false;
            Long lastNotifiedDays = order.getLastNotifiedDays();
            if (remainingDays <= 0) {
                if (!"доставлен".equals(order.getStatus())) {
                    order.setStatus("доставлен");
                    order.setNotificationSent(true);
                    order.setLastNotifiedDays(remainingDays);
                    updateOrderInFirestore(order, doc.getId(), true, remainingDays);
                } else if (!notificationSent) {
                    order.setNotificationSent(true);
                    order.setLastNotifiedDays(remainingDays);
                    updateOrderInFirestore(order, doc.getId(), true, remainingDays);
                }
            } else {
                if (!notificationSent || (lastNotifiedDays == null || lastNotifiedDays != remainingDays) ||
                        (originalDays != null && !originalDays.equals(remainingDays))) {
                    order.setNotificationSent(true);
                    order.setLastNotifiedDays(remainingDays);
                    updateOrderInFirestore(order, doc.getId(), false, remainingDays);
                }
            }
        } catch (Exception e) {
        }
    }
    private void updateOrderInFirestore(Order order, String orderId, boolean isDelivered, long remainingDays) {
        db.collection("orders").document(orderId)
                .set(order)
                .addOnSuccessListener(aVoid -> {
                    orderManager.createOrUpdateOrder(order, orderId);
                    NotificationHelper.sendDeliveryNotification(this, orderId, isDelivered, remainingDays, userDocumentId, "customer");
                });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recalculateUserOrders();
            } else {
                Toast.makeText(this, "Уведомления не прийдут без разрешения", Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && recalculateRunnable != null) {
            handler.removeCallbacks(recalculateRunnable);
        }
    }
}