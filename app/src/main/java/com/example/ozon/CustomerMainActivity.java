package com.example.ozon;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.TimeUnit;

public class CustomerMainActivity extends AppCompatActivity {
    private static final String TAG = "CustomerMainActivity";
    private FirebaseFirestore db;
    private String userDocumentId;
    private static final String CHANNEL_ID = "order_delivery_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customer_layout);

        db = FirebaseFirestore.getInstance();

        // Получаем userDocumentId и userRole из Intent
        userDocumentId = getIntent().getStringExtra("USER_DOCUMENT_ID");
        String userRole = getIntent().getStringExtra("USER_ROLE");

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

        // Перерасчет заказов пользователя при запуске активности
        recalculateUserOrders();
    }

    private void recalculateUserOrders() {
        if (userDocumentId == null) {
            Log.e(TAG, "userDocumentId is null, skipping recalculation");
            return;
        }

        Log.d(TAG, "Starting recalculation of orders for user: " + userDocumentId);
        db.collection("orders")
                .whereEqualTo("userId", userDocumentId)
                .whereEqualTo("status", "создан")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " active orders for user " + userDocumentId);
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        checkAndUpdateOrder(doc);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error recalculating orders for user " + userDocumentId, e);
                });
    }

    private void checkAndUpdateOrder(DocumentSnapshot doc) {
        com.google.firebase.Timestamp orderDate = doc.getTimestamp("orderDate");
        Long daysToDelivery = doc.getLong("days");

        if (orderDate == null || daysToDelivery == null) {
            Log.e(TAG, "Order " + doc.getId() + " is missing required fields");
            return;
        }

        long millisecondsSinceOrder = System.currentTimeMillis() - orderDate.toDate().getTime();
        long daysSinceOrder = TimeUnit.MILLISECONDS.toDays(millisecondsSinceOrder);
        long remainingDays = daysToDelivery - daysSinceOrder;

        Log.d(TAG, "Order " + doc.getId() + ": daysSinceOrder=" + daysSinceOrder + ", daysToDelivery=" + daysToDelivery + ", remainingDays=" + remainingDays);

        if (remainingDays <= 0) {
            // Заказ доставлен
            db.collection("orders").document(doc.getId())
                    .update("status", "доставлен", "days", 0)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Order " + doc.getId() + " marked as delivered");
                        sendDeliveryNotification(doc.getId());
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error marking order " + doc.getId() + " as delivered", e));
        } else {
            // Обновляем оставшиеся дни
            db.collection("orders").document(doc.getId())
                    .update("days", remainingDays)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Order " + doc.getId() + " days updated to " + remainingDays))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating days for order " + doc.getId(), e));
        }
    }

    private void sendDeliveryNotification(String orderId) {
        createNotificationChannel();

        Intent intent = new Intent(this, CustomerMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("USER_DOCUMENT_ID", userDocumentId);
        intent.putExtra("USER_ROLE", "customer");
        intent.putExtra("order_id", orderId); // Для отображения конкретного заказа в профиле, если нужно
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Убедитесь, что у вас есть этот ресурс
                .setContentTitle("Заказ доставлен!")
                .setContentText("Ваш заказ #" + orderId + " успешно доставлен!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(orderId.hashCode(), builder.build());
            Log.d(TAG, "Notification sent for order " + orderId);
        } else {
            Log.w(TAG, "No permission to send notification for order " + orderId);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Order Delivery",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications about order delivery status");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }
}