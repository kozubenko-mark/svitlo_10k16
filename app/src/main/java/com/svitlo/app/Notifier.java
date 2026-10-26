package com.svitlo.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

final class Notifier {
    private Notifier() {}

    static final String CH_LOUD = "svitlo_loud";
    static final String CH_QUIET = "svitlo_quiet";
    static final String CH_SERVICE = "svitlo_service";

    static void ensureChannels(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel loud = new NotificationChannel(CH_LOUD, "Повідомлення зі звуком",
                NotificationManager.IMPORTANCE_HIGH);
        NotificationChannel quiet = new NotificationChannel(CH_QUIET, "Нічні повідомлення (без звуку)",
                NotificationManager.IMPORTANCE_LOW);
        NotificationChannel service = new NotificationChannel(CH_SERVICE, "Фонова робота",
                NotificationManager.IMPORTANCE_MIN);
        nm.createNotificationChannel(loud);
        nm.createNotificationChannel(quiet);
        nm.createNotificationChannel(service);
    }

    static PendingIntent open(Context c) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void show(Context c, Msg m) {
        if (!Store.push(c)) return; // сповіщення вимкнені в налаштуваннях
        ensureChannels(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        Notification.Builder b = new Notification.Builder(c, Quiet.now() ? CH_QUIET : CH_LOUD)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("Світло 10к16")
                .setContentText(m.text)
                .setStyle(new Notification.BigTextStyle().bigText(m.text))
                .setContentIntent(open(c))
                .setAutoCancel(true);
        if (m.time > 0) b.setWhen(m.time).setShowWhen(true);
        nm.notify(m.id, b.build());
    }
}
