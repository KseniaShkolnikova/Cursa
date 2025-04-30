package com.example.ozon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Класс DeliveryReminderReceiver представляет собой BroadcastReceiver для обработки
 * уведомлений о доставке в приложении "OZON". Получает данные о заказе
 * и вызывает метод отправки уведомления о статусе доставки.
 */
public class DeliveryReminderReceiver extends BroadcastReceiver {

    /**
     * Обрабатывает полученное широковещательное сообщение. Извлекает данные о заказе,
     * такие как идентификатор заказа, оставшиеся дни доставки, идентификатор пользователя
     * и его роль, и отправляет уведомление о статусе доставки через NotificationHelper.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String orderId = intent.getStringExtra("orderId");
        long days = intent.getLongExtra("days", 0);
        String userId = intent.getStringExtra("userId");
        String userRole = intent.getStringExtra("userRole");
        NotificationHelper.sendDeliveryNotification(context, orderId, false, days, userId, userRole);
    }
}