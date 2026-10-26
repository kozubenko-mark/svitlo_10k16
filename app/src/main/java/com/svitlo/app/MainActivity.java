package com.svitlo.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

public class MainActivity extends Activity {
    private final TreeMap<Integer, Msg> msgs = new TreeMap<Integer, Msg>();
    private final List<Msg> shown = new ArrayList<Msg>(); // найновіші зверху
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dayFmt = new SimpleDateFormat("dd.MM", Locale.US);
    private final SimpleDateFormat hmFmt = new SimpleDateFormat("HH:mm", Locale.US);

    private int appliedTheme;
    private ListView list;
    private MsgAdapter adapter;
    private View headerView;
    private TextView olderBtn;
    private boolean loadingOlder = false;
    private boolean noMoreOlder = false;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            reload();
            handler.postDelayed(this, 30000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Store.themeRes(this));
        appliedTheme = Store.theme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dayFmt.setTimeZone(Cfg.TZ);
        hmFmt.setTimeZone(Cfg.TZ);

        list = (ListView) findViewById(R.id.list);
        headerView = LayoutInflater.from(this).inflate(R.layout.header_blocks, list, false);
        View footer = LayoutInflater.from(this).inflate(R.layout.footer_older, list, false);
        olderBtn = (TextView) footer.findViewById(R.id.olderBtn);
        olderBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { loadOlder(); }
        });
        list.addHeaderView(headerView, null, false);
        list.addFooterView(footer, null, false);
        adapter = new MsgAdapter();
        list.setAdapter(adapter);

        findViewById(R.id.btnRefresh).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { reload(); }
        });
        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        applyStatus(Store.loadStatus(this));

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 1);
        }
        try {
            startForegroundService(new Intent(this, PollService.class));
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Store.theme(this) != appliedTheme) { // тему змінили в налаштуваннях
            recreate();
            return;
        }
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    // ---------- блоки 1 і 2 ----------

    private void applyStatus(Status s) {
        View card = headerView.findViewById(R.id.statusCard);
        card.setBackgroundResource(StatusWidget.bgRes(s.bg));
        ((ImageView) headerView.findViewById(R.id.sIcon)).setImageResource(StatusWidget.iconRes(s.icon));
        ((TextView) headerView.findViewById(R.id.sTitle)).setText(s.title);
        TextView sub = (TextView) headerView.findViewById(R.id.sSub);
        sub.setText(s.sub);
        sub.setVisibility(s.sub.length() > 0 ? View.VISIBLE : View.GONE);
        TextView upd = (TextView) headerView.findViewById(R.id.sUpdated);
        upd.setText(s.time > 0 ? "оновлено " + hmFmt.format(new Date(s.time)) : "");

        Calendar c = Calendar.getInstance(Cfg.TZ);
        int nowM = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);

        ((TextView) headerView.findViewById(R.id.todayDate)).setText(s.todayLabel);
        ((TextView) headerView.findViewById(R.id.tomorrowDate)).setText(s.tomorrowLabel);
        fillRows((LinearLayout) headerView.findViewById(R.id.todayRows), s.todayRows, true, nowM);
        fillRows((LinearLayout) headerView.findViewById(R.id.tomorrowRows), s.tomorrowRows, false, nowM);
        setNote((TextView) headerView.findViewById(R.id.todayNote), s.todayNote);
        setNote((TextView) headerView.findViewById(R.id.tomorrowNote), s.tomorrowNote);
    }

    private void setNote(TextView t, String text) {
        t.setText(text);
        t.setVisibility(text.length() > 0 ? View.VISIBLE : View.GONE);
    }

    private static String dur(int minutes) {
        int h = minutes / 60, m = minutes % 60;
        if (h > 0 && m > 0) return h + " год " + m + " хв";
        if (h > 0) return h + " год";
        return m + " хв";
    }

    private static String hm(int m) {
        m = ((m % 1440) + 1440) % 1440;
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60);
    }

    private void fillRows(LinearLayout box, List<int[]> rows, boolean isToday, int nowM) {
        box.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        for (int[] r : rows) {
            View v = inf.inflate(R.layout.row_slot, box, false);
            v.findViewById(R.id.dot).setBackgroundResource(r[2] == 1 ? R.drawable.dot_amber : R.drawable.dot_red);
            ((TextView) v.findViewById(R.id.range)).setText(
                    hm(r[0]) + " – " + (r[1] == 1440 ? "24:00" : hm(r[1])));
            ((TextView) v.findViewById(R.id.dur)).setText(
                    (r[2] == 1 ? "можливо · " : "") + dur(r[1] - r[0]));
            TextView tag = (TextView) v.findViewById(R.id.tag);
            if (isToday && r[2] == 0 && r[0] <= nowM && nowM < r[1]) {
                tag.setText("зараз");
                tag.setVisibility(View.VISIBLE);
            }
            if (isToday && r[1] <= nowM) v.setAlpha(0.45f); // минуле відключення
            box.addView(v);
        }
    }

    // ---------- блок 3: стрічка ----------

    private void reload() {
        new Thread(new Runnable() {
            @Override public void run() {
                List<Msg> got = null;
                try {
                    got = Feed.load(0);
                } catch (Exception ignored) {
                }
                final Status st = Updater.refreshStatus(getApplicationContext());
                final List<Msg> fin = got;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        applyStatus(st);
                        if (fin != null) merge(fin);
                    }
                });
            }
        }).start();
    }

    private void loadOlder() {
        if (loadingOlder || noMoreOlder || msgs.isEmpty()) return;
        loadingOlder = true;
        olderBtn.setText("Завантаження…");
        final int before = msgs.firstKey();
        new Thread(new Runnable() {
            @Override public void run() {
                List<Msg> got = null;
                try {
                    got = Feed.load(before);
                } catch (Exception ignored) {
                }
                final List<Msg> fin = got;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        loadingOlder = false;
                        olderBtn.setText("Завантажити старіші");
                        if (fin == null) return;
                        int oldCount = shown.size();
                        merge(fin);
                        if (shown.size() == oldCount) {
                            noMoreOlder = true;
                            olderBtn.setText("Старіших повідомлень немає");
                        }
                    }
                });
            }
        }).start();
    }

    /** Додає/оновлює повідомлення (за номером) і перемальовує стрічку. */
    private void merge(List<Msg> got) {
        for (Msg m : got) {
            if (m.id == Cfg.STATUS_POST) continue; // закріплений статус показано у блоці 1
            msgs.put(m.id, m);
        }
        shown.clear();
        shown.addAll(msgs.descendingMap().values());
        adapter.notifyDataSetChanged();
    }

    private String when(long t) {
        if (t <= 0) return "";
        Calendar now = Calendar.getInstance(Cfg.TZ);
        Calendar c = Calendar.getInstance(Cfg.TZ);
        c.setTimeInMillis(t);
        String day;
        if (sameDay(now, c)) {
            day = "Сьогодні";
        } else {
            now.add(Calendar.DAY_OF_MONTH, -1);
            day = sameDay(now, c) ? "Вчора" : dayFmt.format(new Date(t));
        }
        return day + ", " + hmFmt.format(new Date(t));
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private class MsgAdapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return shown.get(i).id; }

        @Override
        public View getView(int i, View convert, ViewGroup parent) {
            View v = convert;
            if (v == null) {
                v = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_message, parent, false);
            }
            Msg m = shown.get(i);
            TextView text = (TextView) v.findViewById(R.id.text);
            text.setText(Html.fromHtml(m.html, Html.FROM_HTML_MODE_LEGACY));
            text.setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) v.findViewById(R.id.time)).setText(when(m.time));
            return v;
        }
    }
}
