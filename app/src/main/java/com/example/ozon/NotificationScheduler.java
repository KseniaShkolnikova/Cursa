package com.example.ozon;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
public class NotificationScheduler {
    private static final String TAG = "NotificationScheduler";
    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleDeliveryReminder(Context context, Order order, String orderId, String userId, String userRole) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders").document(orderId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean notificationSent = documentSnapshot.getBoolean("notificationSent");
                    if (notificationSent != null && notificationSent) {
                        return;
                    }
                    Calendar deliveryDate = Calendar.getInstance();
                    deliveryDate.setTime(order.getOrderDate().toDate());
                    deliveryDate.add(Calendar.DAY_OF_MONTH, order.getDays().intValue());
                    deliveryDate.add(Calendar.DAY_OF_MONTH, -1);
                    if (deliveryDate.getTimeInMillis() <= System.currentTimeMillis()) {
                        NotificationHelper.sendOrderNotification(context, order, orderId, userId, userRole);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("notificationSent", true);
                        db.collection("orders")
                                .document(orderId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                })
                                .addOnFailureListener(e -> {
                                });
                        return;
                    }
                    Intent intent = new Intent(context, DeliveryReminderReceiver.class);
                    intent.putExtra("orderId", orderId);
                    intent.putExtra("days", order.getDays());
                    intent.putExtra("userId", userId);
                    intent.putExtra("userRole", userRole);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            orderId.hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
                    );
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    if (alarmManager != null) {
                        alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                deliveryDate.getTimeInMillis(),
                                pendingIntent
                        );
                    }
                })
                .addOnFailureListener(e -> {
                });
    }
}