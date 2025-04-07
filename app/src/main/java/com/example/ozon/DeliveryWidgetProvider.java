package com.example.ozon;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
public class DeliveryWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "DeliveryWidgetProvider";
    private static final String ACTION_UPDATE = "com.example.ozon.UPDATE_WIDGET";
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static Runnable updateRunnable;
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
        startUpdating(context);
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, DeliveryWidgetProvider.class));
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }
    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.delivery_widget);
        if (!isUserAuthenticated(context)) {
            views.setTextViewText(R.id.delivery_date, "Не авторизован");
            views.setTextViewText(R.id.time_remaining, "Войдите в приложение");
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }
        SharedPreferences sharedPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String userId = sharedPrefs.getString("userId", null);
        String userRole = sharedPrefs.getString("userRole", null);
        if (userId == null || userId.isEmpty()) {
            views.setTextViewText(R.id.delivery_date, "Ошибка");
            views.setTextViewText(R.id.time_remaining, "Не удалось определить пользователя");
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }
        if (!NetworkUtil.isNetworkAvailable(context)) {
            views.setTextViewText(R.id.delivery_date, "Нет интернета");
            views.setTextViewText(R.id.time_remaining, "Проверьте подключение");
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders")
                .whereEqualTo("userId", userId)
                .whereIn("status", Arrays.asList("создан", "в процессе"))
                .orderBy("days", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        views.setTextViewText(R.id.delivery_date, "Нет активных заказов");
                        views.setTextViewText(R.id.time_remaining, "");
                    } else {
                        Order order = queryDocumentSnapshots.getDocuments().get(0).toObject(Order.class);
                        if (order == null || order.getDays() == null) {
                            views.setTextViewText(R.id.delivery_date, "Ошибка данных заказа");
                            views.setTextViewText(R.id.time_remaining, "");
                        } else {
                            Calendar deliveryDate = Calendar.getInstance();
                            deliveryDate.add(Calendar.DAY_OF_MONTH, order.getDays().intValue());
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
                            String dateString = "Дата доставки: " + dateFormat.format(deliveryDate.getTime());
                            views.setTextViewText(R.id.delivery_date, dateString);
                            long daysRemaining = order.getDays();
                            if (daysRemaining > 0) {
                                String remaining = String.format(Locale.getDefault(), "Осталось: %d д", daysRemaining);
                                views.setTextViewText(R.id.time_remaining, remaining);
                            } else {
                                views.setTextViewText(R.id.time_remaining, "Доставлено!");
                            }
                        }
                    }
                    Intent intent = new Intent(context, OrderActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("USER_DOCUMENT_ID", userId);
                    intent.putExtra("USER_ROLE", userRole);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(R.id.delivery_date, pendingIntent);
                    views.setOnClickPendingIntent(R.id.time_remaining, pendingIntent);

                    appWidgetManager.updateAppWidget(appWidgetId, views);
                })
                .addOnFailureListener(e -> {
                    views.setTextViewText(R.id.delivery_date, "Ошибка");
                    views.setTextViewText(R.id.time_remaining, "Не удалось загрузить данные: " + e.getMessage());
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                });
    }
    private void startUpdating(final Context context) {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(context, DeliveryWidgetProvider.class);
                intent.setAction(ACTION_UPDATE);
                context.sendBroadcast(intent);
                handler.postDelayed(this, 60 * 1000);
            }
        };
        handler.post(updateRunnable);
    }
    @Override
    public void onDisabled(Context context) {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }
    private boolean isUserAuthenticated(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = sharedPrefs.getBoolean("isLoggedIn", false);
        return isLoggedIn;
    }
}