package com.mobwal.walker.beacon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.mobwal.walker.beautil.v1.wBeacon;

public class ForegroundServiceUtil {

    /**
     * Старт фонового сервиса
     *
     * @param context
     * @param service сервис
     */
    public static void startForegroundService(@NonNull Context context, Class<?> service, wBeacon[] beacons) {
        Intent intent = new Intent(context, service);
        intent.putExtra("beacons", beacons);

        context.startForegroundService(intent);
    }

    /**
     * Остановка фонового сервиса
     *
     * @param context
     */
    public static void stopForegroundService(@NonNull Context context, Class<?> service) {
        Intent intent = new Intent(context, service);
        context.stopService(intent);
    }

    public static NotificationChannel getNotificationChannel(@NonNull Context context, @NonNull String channelId, @NonNull String channelName) {
        return new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
    }

    public static Notification getNotification(@NonNull Context context, @NonNull String channelId, int notificationIconId, @NonNull String title, @NonNull String contentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(contentText)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), notificationIconId))
                .setSmallIcon(notificationIconId)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        return builder.build();
    }
}
