package com.example.ozon;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Класс NetworkUtil предоставляет утилитарный метод для проверки доступности
 * интернет-соединения в приложении "OZON". Использует
 * ConnectivityManager для определения состояния сети.
 */
public class NetworkUtil {

    /**
     * Проверяет наличие активного интернет-соединения. Возвращает true, если
     * устройство подключено к сети, и false в противном случае.
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}