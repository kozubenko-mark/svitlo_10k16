package com.svitlo.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private static final String[] THEMES = { "Як у системі", "Світла", "Темна" };

    private TextView themeValue;

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

        Switch push = (Switch) findViewById(R.id.switchPush);
        push.setChecked(Store.push(this));
        push.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean checked) {
                Store.setPush(SettingsActivity.this, checked);
            }
        });

        findViewById(R.id.rowBattery).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openBatterySettings(); }
        });
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
