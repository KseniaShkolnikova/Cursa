package com.example.ozon;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Класс OrderManager управляет процессом создания и обновления заказов в приложении
 * "OZON". Отвечает за отправку уведомлений о статусе заказа и
 * планирование напоминаний о доставке.
 */
public class OrderManager {
    private Context context;
    private SharedPreferences sharedPrefs;

    /**
     * Конструктор класса. Инициализирует контекст и SharedPreferences для доступа
     * к данным пользователя, а также создает канал уведомлений через NotificationHelper.
     */
    public OrderManager(Context context) {
        this.context = context;
        this.sharedPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        NotificationHelper.createNotificationChannel(context);
    }

    /**
     * Создает или обновляет заказ. Проверяет, принадлежит ли заказ текущему пользователю,
     * и, если да, отправляет уведомление о статусе заказа и планирует напоминание о доставке.
     */
    public void createOrUpdateOrder(Order order, String orderId) {
        String currentUserId = sharedPrefs.getString("userId", null);
        String userRole = sharedPrefs.getString("userRole", null);
        if (currentUserId != null && order.getUserId().equals(currentUserId)) {
            NotificationHelper.sendOrderNotification(context, order, orderId, currentUserId, userRole);
            NotificationScheduler.scheduleDeliveryReminder(context, order, orderId, currentUserId, userRole);
        }
    }
}