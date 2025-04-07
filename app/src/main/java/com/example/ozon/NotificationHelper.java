package com.example.ozon;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "order_delivery_channel";
    private static final String CHANNEL_NAME = "Order Delivery";
    private static final String CHANNEL_DESCRIPTION = "Notifications about order delivery status";
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    public static void sendOrderNotification(Context context, Order order, String orderId, String userId, String userRole) {
        try {
            Intent intent = new Intent(context, CustomerMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("USER_DOCUMENT_ID", userId);
            intent.putExtra("USER_ROLE", userRole);
            intent.putExtra("order_id", orderId);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    orderId.hashCode(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
            String title = "Обновление статуса заказа";
            String message = "Заказ #" + orderId + "\n" +
                    "Статус: " + order.getStatus() + "\n" +
                    "Сумма: " + order.getTotalAmount() + " руб.\n" +
                    "Способ оплаты: " + order.getPaymentMethod() + "\n" +
                    "Срок доставки: " + order.getDays() + " дней";
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            try {
                notificationManager.notify(orderId.hashCode(), builder.build());
            } catch (SecurityException e) {}
        } catch (Exception e) {
        }
    }
    public static void sendDeliveryNotification(Context context, String orderId, boolean isDelivered, long remainingDays, String userId, String userRole) {
        try {
            Intent intent = new Intent(context, CustomerMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("USER_DOCUMENT_ID", userId);
            intent.putExtra("USER_ROLE", userRole);
            intent.putExtra("order_id", orderId);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    orderId.hashCode(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            if (isDelivered) {
                builder.setContentTitle("Заказ доставлен!")
                        .setContentText("Ваш заказ #" + orderId + " успешно доставлен!");
            } else {
                builder.setContentTitle("Напоминание о доставке")
                        .setContentText("До доставки заказа #" + orderId + " осталось " + remainingDays + " дней");
            }
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            try {
                notificationManager.notify(orderId.hashCode(), builder.build());
            } catch (SecurityException e) {
            }
        } catch (Exception e) {
        }
    }
}