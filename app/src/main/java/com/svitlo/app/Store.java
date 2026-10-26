package com.svitlo.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

final class Store {
    private Store() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("svitlo", Context.MODE_PRIVATE);
    }

    static int lastSeen(Context c) { return p(c).getInt("last", 0); }

    static void setLastSeen(Context c, int v) { p(c).edit().putInt("last", v).apply(); }

    static void saveStatus(Context c, Status s) {
        p(c).edit().putString("status", s.toJson()).apply();
    }

    static Status loadStatus(Context c) {
        return Status.fromJson(p(c).getString("status", null));
    }

    // ---------- налаштування ----------

    /** 0 — як у системі, 1 — світла, 2 — темна. */
    static int theme(Context c) { return p(c).getInt("theme", 0); }

    static void setTheme(Context c, int v) { p(c).edit().putInt("theme", v).apply(); }

    static boolean push(Context c) { return p(c).getBoolean("push", true); }

    static void setPush(Context c, boolean v) { p(c).edit().putBoolean("push", v).apply(); }

    static int themeRes(Context c) {
        int t = theme(c);
        if (t == 1) return R.style.AppTheme_Light;
        if (t == 2) return R.style.AppTheme_Dark;
        boolean night = (c.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return night ? R.style.AppTheme_Dark : R.style.AppTheme_Light;
    }
}
