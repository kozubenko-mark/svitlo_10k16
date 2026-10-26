package com.svitlo.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

final class Updater {
    private Updater() {}

    /** Один цикл: нові повідомлення → сповіщення, потім оновлення статусу й віджета. */
    static void cycle(Context c) {
        try {
            handleNew(c, Feed.load(0));
        } catch (Exception ignored) {
        }
        refreshStatus(c);
    }

    static Status refreshStatus(Context c) {
        Status s = Status.compute();
        Store.saveStatus(c, s);
        StatusWidget.renderAll(c, s);
        return s;
    }

    private static void handleNew(Context c, List<Msg> msgs) {
        int last = Store.lastSeen(c);
        int max = last;
        List<Msg> fresh = new ArrayList<Msg>();
        for (Msg m : msgs) {
            if (m.id > max) max = m.id;
            if (last > 0 && m.id > last && m.id != Cfg.STATUS_POST) fresh.add(m);
        }
        if (last == 0) { // перший запуск: нічого не показуємо, просто запам'ятовуємо
            if (max > 0) Store.setLastSeen(c, max);
            return;
        }
        long now = System.currentTimeMillis();
        for (int i = Math.max(0, fresh.size() - 5); i < fresh.size(); i++) {
            Msg m = fresh.get(i);
            if (m.time > 0 && now - m.time > 30 * 60 * 1000L) continue; // застаріле
            Notifier.show(c, m);
        }
        if (max > last) Store.setLastSeen(c, max);
    }
}
