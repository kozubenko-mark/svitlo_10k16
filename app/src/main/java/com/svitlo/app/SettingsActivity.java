package com.svitlo.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private static final String[] THEMES = { "Як у системі", "Світла", "Темна" };

    private TextView themeValue;
    private Switch pushSwitch;
    private TextView pushHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Store.themeRes(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        themeValue = (TextView) findViewById(R.id.themeValue);
        themeValue.setText(THEMES[Store.theme(this)]);
        findViewById(R.id.rowTheme).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { chooseTheme(); }
        });

        pushSwitch = (Switch) findViewById(R.id.switchPush);
        pushHint = (TextView) findViewById(R.id.pushHint);
        showPush(Store.push(this));
        findViewById(R.id.rowPush).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean on = !Store.push(SettingsActivity.this);
                Store.setPush(SettingsActivity.this, on);
                showPush(on);
                if (on && Build.VERSION.SDK_INT >= 33
                        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 1);
                }
            }
        });

        findViewById(R.id.rowBattery).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openBatterySettings(); }
        });
    }

    private void showPush(boolean on) {
        pushSwitch.setChecked(on);
        pushHint.setText(on ? "Увімкнено: нові повідомлення каналу" : "Вимкнено");
    }

    private void chooseTheme() {
        new AlertDialog.Builder(this)
                .setTitle("Тема")
                .setSingleChoiceItems(THEMES, Store.theme(this), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        Store.setTheme(SettingsActivity.this, which);
                        d.dismiss();
                        recreate();
                    }
                })
                .show();
    }

    private void openBatterySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) {
            }
        }
    }
}
