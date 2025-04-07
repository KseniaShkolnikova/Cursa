package com.example.ozon;
import android.content.Context;
import android.content.SharedPreferences;
public class OrderManager {
    private Context context;
    private SharedPreferences sharedPrefs;
    public OrderManager(Context context) {
        this.context = context;
        this.sharedPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        NotificationHelper.createNotificationChannel(context);
    }
    public void createOrUpdateOrder(Order order, String orderId) {
        String currentUserId = sharedPrefs.getString("userId", null);
        String userRole = sharedPrefs.getString("userRole", null);
        if (currentUserId != null && order.getUserId().equals(currentUserId)) {
            NotificationHelper.sendOrderNotification(context, order, orderId, currentUserId, userRole);
            NotificationScheduler.scheduleDeliveryReminder(context, order, orderId, currentUserId, userRole);
        }
    }
}