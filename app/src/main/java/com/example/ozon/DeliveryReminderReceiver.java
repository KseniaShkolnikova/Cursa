package com.example.ozon;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class DeliveryReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String orderId = intent.getStringExtra("orderId");
        long days = intent.getLongExtra("days", 0);
        String userId = intent.getStringExtra("userId");
        String userRole = intent.getStringExtra("userRole");
        NotificationHelper.sendDeliveryNotification(context, orderId, false, days, userId, userRole);
    }
}