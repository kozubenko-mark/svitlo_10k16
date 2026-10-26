package com.svitlo.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            context.startForegroundService(new Intent(context, PollService.class));
        } catch (Exception ignored) {
        }
    }
}
