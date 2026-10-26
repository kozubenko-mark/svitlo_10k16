package com.svitlo.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatusWidget extends AppWidgetProvider {
    static final String ACTION_REFRESH = "com.svitlo.app.REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        // одразу показуємо збережене, а свіже підтягує сервіс або кнопка
        renderAll(context, Store.loadStatus(context));
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            final PendingResult pr = goAsync();
            Status s = Store.loadStatus(context);
            s.sub = "Оновлення…";
            renderAll(context, s);
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        Updater.refreshStatus(context.getApplicationContext());
                    } catch (Throwable ignored) {
                    } finally {
                        pr.finish();
                    }
                }
            }).start();
        }
    }

    static int bgRes(int bg) {
        return bg == 0 ? R.drawable.bg_green : bg == 1 ? R.drawable.bg_red : R.drawable.bg_grey;
    }

    static int iconRes(int icon) {
        return icon == 0 ? R.drawable.ic_bolt : icon == 1 ? R.drawable.ic_bolt_off : R.drawable.ic_help;
    }

    static void renderAll(Context c, Status s) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        render(c, mgr, s, StatusWidget.class, false);
        render(c, mgr, s, StatusWidgetWide.class, true);
    }

    private static void render(Context c, AppWidgetManager mgr, Status s, Class<?> cls, boolean wide) {
        int[] ids = mgr.getAppWidgetIds(new ComponentName(c, cls));
        if (ids == null || ids.length == 0) return;
        RemoteViews v = views(c, s, wide);
        for (int id : ids) mgr.updateAppWidget(id, v);
    }

    static RemoteViews views(Context c, Status s, boolean wide) {
        RemoteViews v = new RemoteViews(c.getPackageName(), wide ? R.layout.widget_wide : R.layout.widget);
        v.setInt(R.id.root, "setBackgroundResource", bgRes(s.bg));
        v.setImageViewResource(R.id.icon, iconRes(s.icon));
        v.setTextViewText(R.id.title, s.title);
        v.setTextViewText(R.id.sub, s.sub);
        String upd = "";
        if (s.time > 0) {
            SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.US);
            f.setTimeZone(Cfg.TZ);
            upd = "оновлено " + f.format(new Date(s.time));
        }
        v.setTextViewText(R.id.updated, upd);

        if (wide) {
            v.setTextViewText(R.id.todayTitle, "Сьогодні" + (s.todayLabel.length() > 0 ? ", " + s.todayLabel : ""));
            v.setTextViewText(R.id.todayRows,
                    s.todayRows.isEmpty() ? s.todayNote : Status.rowsText(s.todayRows, 4));
            v.setTextViewText(R.id.tomorrowTitle, "Завтра" + (s.tomorrowLabel.length() > 0 ? ", " + s.tomorrowLabel : ""));
            v.setTextViewText(R.id.tomorrowRows,
                    s.tomorrowRows.isEmpty() ? s.tomorrowNote : Status.rowsText(s.tomorrowRows, 3));
        }

        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        Intent r = new Intent(c, StatusWidget.class);
        r.setAction(ACTION_REFRESH);
        v.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(c, 1, r,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        return v;
    }
}
