package com.example.ozon;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.TimeUnit;

public class OrderCheckWorker extends Worker {
    private static final String TAG = "OrderCheckWorker";
    private FirebaseFirestore db;
    private Context context;

    public OrderCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Worker started");

        try {
            // Используем getResult() с таймаутом
            QuerySnapshot snapshot = db.collection("orders")
                    .whereEqualTo("status", "создан")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Error getting documents", task.getException());
                        }
                    })
                    .getResult(); // Синхронное ожидание результата

            Log.d(TAG, "Found " + snapshot.size() + " active orders");
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                checkOrderDelivery(doc);
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in doWork", e);
            return Result.failure();
        }
    }

    private void checkOrderDelivery(DocumentSnapshot doc) {
        com.google.firebase.Timestamp orderDate = doc.getTimestamp("orderDate");
        Long daysToDelivery = doc.getLong("days");

        if (orderDate == null || daysToDelivery == null) {
            Log.e(TAG, "Order " + doc.getId() + " is missing required fields");
            return;
        }

        long millisecondsSinceOrder = System.currentTimeMillis() - orderDate.toDate().getTime();
        long daysSinceOrder = TimeUnit.MILLISECONDS.toDays(millisecondsSinceOrder);

        Log.d(TAG, String.format("Order %s: created %d days ago, delivery in %d days",
                doc.getId(), daysSinceOrder, daysToDelivery));

        if (daysSinceOrder >= daysToDelivery) {
            markOrderAsDelivered(doc.getId());
        } else {
            updateRemainingDays(doc.getId(), daysToDelivery - daysSinceOrder);
        }
    }

    private void markOrderAsDelivered(String orderId) {
        db.collection("orders").document(orderId)
                .update("status", "доставлен", "days", 0)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Order " + orderId + " marked as delivered");
                    sendDeliveryNotification(orderId);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error marking order as delivered", e));
    }

    private void updateRemainingDays(String orderId, long remainingDays) {
        db.collection("orders").document(orderId)
                .update("days", remainingDays)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated remaining days for order " + orderId))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating remaining days", e));
    }

    @SuppressLint("MissingPermission")
    private void sendDeliveryNotification(String orderId) {
        if (context == null) {
            Log.e(TAG, "Context is null - cannot send notification");
            return;
        }

        createNotificationChannel();

        Intent intent = new Intent(context, ProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("order_id", orderId);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "order_delivery_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Заказ доставлен!")
                .setContentText("Ваш заказ #" + orderId + " успешно доставлен!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(orderId.hashCode(), builder.build());
        Log.d(TAG, "Notification sent for order " + orderId);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "order_delivery_channel", "Order Delivery", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications about order delivery status");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }
}