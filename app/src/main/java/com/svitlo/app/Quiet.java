package com.svitlo.app;

import java.util.Calendar;

/** Тихі години: 23:00–07:00; якщо ранок субота/неділя — 23:00–09:00. */
final class Quiet {
    private Quiet() {}

    static boolean now() {
        Calendar c = Calendar.getInstance(Cfg.TZ);
        int h = c.get(Calendar.HOUR_OF_DAY);
        if (h >= 23) return true;
        int d = c.get(Calendar.DAY_OF_WEEK);
        boolean weekend = d == Calendar.SATURDAY || d == Calendar.SUNDAY;
        return h < (weekend ? 9 : 7);
    }
}
