package com.svitlo.app;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

public class PollService extends Service {
    private volatile boolean running;
    private Thread worker;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notifier.ensureChannels(this);
        Notification n = new Notification.Builder(this, Notifier.CH_SERVICE)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("Світло 10к16")
                .setContentText("Стежимо за каналом")
                .setContentIntent(Notifier.open(this))
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1, n);
        }
        if (worker == null || !worker.isAlive()) {
            running = true;
            worker = new Thread(new Runnable() {
                @Override public void run() {
                    while (running) {
                        try {
                            Updater.cycle(getApplicationContext());
                        } catch (Throwable ignored) {
                        }
                        try {
                            Thread.sleep(Cfg.POLL_MS);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }, "svitlo-poll");
            worker.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (worker != null) worker.interrupt();
        super.onDestroy();
    }
}
