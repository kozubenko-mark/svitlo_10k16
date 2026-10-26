package com.svitlo.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Поточний стан для віджета та шапки: колір і заголовок — за фактичним станом
 * закріпленого поста каналу, підпис — за графіком Yasno.
 */
final class Status {
    int bg = 2;     // 0 зелений, 1 червоний, 2 сірий
    int icon = 2;   // 0 блискавка, 1 перекреслена, 2 питання
    String title = "Оновлення…";
    String sub = "";
    List<int[]> todayRows = new ArrayList<int[]>();     // {початок, кінець, тип: 0 точно / 1 можливо}
    List<int[]> tomorrowRows = new ArrayList<int[]>();
    String todayNote = "";
    String tomorrowNote = "";
    String todayLabel = "";
    String tomorrowLabel = "";
    long time = 0;

    private static String yasnoCache;
    private static long yasnoAt;

    // ---------- мережа ----------

    /** "on" / "off" / null — за текстом закріпленого поста. */
    static String fetchPower() {
        try {
            String html = Http.get("https://t.me/" + Cfg.CHANNEL + "/" + Cfg.STATUS_POST
                    + "?embed=1&mode=tme&_=" + System.currentTimeMillis());
            Matcher m = Pattern.compile("<div class=\"tgme_widget_message_text[^>]*>(.*?)</div>",
                    Pattern.DOTALL).matcher(html);
            if (!m.find()) return null;
            String t = m.group(1).replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");
            if (t.contains("Світла немає")) return "off";
            if (t.contains("Світло є")) return "on";
        } catch (Exception ignored) {
        }
        return null;
    }

    private static synchronized JSONObject fetchYasno() {
        long now = System.currentTimeMillis();
        String body = null;
        if (yasnoCache != null && now - yasnoAt < 4 * 60 * 1000L) {
            body = yasnoCache;
        } else {
            try {
                body = Http.get(Cfg.YASNO);
                yasnoCache = body;
                yasnoAt = now;
            } catch (Exception e) {
                if (yasnoCache != null && now - yasnoAt < 60 * 60 * 1000L) body = yasnoCache;
            }
        }
        if (body == null) return null;
        try {
            JSONObject g = new JSONObject(body).optJSONObject(Cfg.GROUP);
            if (g != null && g.optJSONObject("today") != null) return g;
        } catch (Exception ignored) {
        }
        return null;
    }

    // ---------- допоміжне ----------

    private static String hm(int m) {
        m = ((m % 1440) + 1440) % 1440;
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60);
    }

    private static String upto(int m) {
        return (m >= 1440 ? "завтра " : "") + hm(m);
    }

    private static String dayStr(JSONObject d) {
        if (d == null) return "";
        String s = d.optString("date", "");
        return s.length() >= 10 ? s.substring(0, 10) : s;
    }

    private static boolean isEmergency(JSONObject d) {
        if (d == null) return false;
        String s = d.optString("status", "");
        return s.equals("EmergencyShutdowns") || s.equals("EmergencyOutages");
    }

    private static boolean waiting(JSONObject d) {
        return d != null && "WaitingForSchedule".equals(d.optString("status", ""));
    }

    /** Відключення сьогодні+завтра у хвилинах від початку сьогодні, суміжні зливаються. */
    private static List<int[]> blocksOf(JSONObject today, JSONObject tomorrow) {
        List<int[]> raw = new ArrayList<int[]>();
        JSONObject[] days = new JSONObject[] { today, tomorrow };
        for (int i = 0; i < 2; i++) {
            JSONObject d = days[i];
            if (d == null || isEmergency(d) || waiting(d)) continue;
            JSONArray slots = d.optJSONArray("slots");
            if (slots == null) continue;
            for (int k = 0; k < slots.length(); k++) {
                JSONObject s = slots.optJSONObject(k);
                if (s != null && "Definite".equals(s.optString("type"))) {
                    raw.add(new int[] { s.optInt("start") + i * 1440, s.optInt("end") + i * 1440 });
                }
            }
        }
        Collections.sort(raw, new Comparator<int[]>() {
            @Override public int compare(int[] a, int[] b) { return a[0] - b[0]; }
        });
        List<int[]> out = new ArrayList<int[]>();
        for (int[] b : raw) {
            int[] last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && b[0] <= last[1]) last[1] = Math.max(last[1], b[1]);
            else out.add(new int[] { b[0], b[1] });
        }
        return out;
    }

    /** Рядки графіка одного дня: точні відключення (суміжні зливаються) і «можливі». */
    private static List<int[]> rowsOf(JSONObject d) {
        List<int[]> out = new ArrayList<int[]>();
        if (d == null || isEmergency(d) || waiting(d)) return out;
        JSONArray slots = d.optJSONArray("slots");
        if (slots == null) return out;
        List<int[]> def = new ArrayList<int[]>();
        List<int[]> pos = new ArrayList<int[]>();
        for (int k = 0; k < slots.length(); k++) {
            JSONObject sl = slots.optJSONObject(k);
            if (sl == null) continue;
            String type = sl.optString("type");
            if ("Definite".equals(type)) def.add(new int[] { sl.optInt("start"), sl.optInt("end"), 0 });
            else if ("Possible".equals(type)) pos.add(new int[] { sl.optInt("start"), sl.optInt("end"), 1 });
        }
        Comparator<int[]> byStart = new Comparator<int[]>() {
            @Override public int compare(int[] a, int[] b) { return a[0] - b[0]; }
        };
        Collections.sort(def, byStart);
        for (int[] b : def) {
            int[] last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && last[2] == 0 && b[0] <= last[1]) last[1] = Math.max(last[1], b[1]);
            else out.add(b);
        }
        out.addAll(pos);
        Collections.sort(out, byStart);
        return out;
    }

    static String rowsText(List<int[]> rows, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size() && i < max; i++) {
            if (i > 0) sb.append('\n');
            int[] r = rows.get(i);
            sb.append(hm(r[0])).append(" – ").append(r[1] == 1440 ? "24:00" : hm(r[1]));
        }
        return sb.toString();
    }

    // ---------- збереження ----------

    private static JSONArray rowsJson(List<int[]> rows) {
        JSONArray a = new JSONArray();
        for (int[] r : rows) {
            JSONArray x = new JSONArray();
            x.put(r[0]);
            x.put(r[1]);
            x.put(r[2]);
            a.put(x);
        }
        return a;
    }

    private static List<int[]> rowsFrom(JSONArray a) {
        List<int[]> out = new ArrayList<int[]>();
        if (a == null) return out;
        for (int i = 0; i < a.length(); i++) {
            JSONArray x = a.optJSONArray(i);
            if (x != null && x.length() >= 3) out.add(new int[] { x.optInt(0), x.optInt(1), x.optInt(2) });
        }
        return out;
    }

    String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("bg", bg);
            o.put("icon", icon);
            o.put("title", title);
            o.put("sub", sub);
            o.put("time", time);
            o.put("tr", rowsJson(todayRows));
            o.put("mr", rowsJson(tomorrowRows));
            o.put("tn", todayNote);
            o.put("mn", tomorrowNote);
            o.put("tl", todayLabel);
            o.put("ml", tomorrowLabel);
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    static Status fromJson(String js) {
        Status s = new Status();
        if (js == null) return s;
        try {
            JSONObject o = new JSONObject(js);
            if (!o.has("title")) return s;
            s.bg = o.optInt("bg", 2);
            s.icon = o.optInt("icon", 2);
            s.title = o.optString("title", s.title);
            s.sub = o.optString("sub", "");
            s.time = o.optLong("time", 0);
            s.todayRows = rowsFrom(o.optJSONArray("tr"));
            s.tomorrowRows = rowsFrom(o.optJSONArray("mr"));
            s.todayNote = o.optString("tn", "");
            s.tomorrowNote = o.optString("mn", "");
            s.todayLabel = o.optString("tl", "");
            s.tomorrowLabel = o.optString("ml", "");
        } catch (Exception ignored) {
        }
        return s;
    }

    // ---------- розрахунок ----------

    static Status compute() {
        String power = fetchPower();
        JSONObject g = fetchYasno();

        Status s = new Status();
        if ("off".equals(power)) { s.bg = 1; s.icon = 1; s.title = "Світла немає"; }
        else if ("on".equals(power)) { s.bg = 0; s.icon = 0; s.title = "Світло є"; }
        else { s.bg = 2; s.icon = 2; s.title = "Статус невідомий"; }

        JSONObject today = null, tomorrow = null;
        Calendar cal = Calendar.getInstance(Cfg.TZ);
        if (g != null) {
            today = g.optJSONObject("today");
            tomorrow = g.optJSONObject("tomorrow");
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setTimeZone(Cfg.TZ);
            String ts = f.format(cal.getTime());
            if (!ts.equals(dayStr(today)) && ts.equals(dayStr(tomorrow))) {
                today = tomorrow;
                tomorrow = null;
            }
        }
        int nowM = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        boolean emergency = g != null && isEmergency(today);
        List<int[]> blocks = g != null ? blocksOf(today, tomorrow) : new ArrayList<int[]>();
        int[] cur = null, next = null;
        for (int[] b : blocks) {
            if (cur == null && b[0] <= nowM && nowM < b[1]) cur = b;
            if (next == null && b[0] > nowM) next = b;
        }
        boolean unpublished = g != null && !emergency && (tomorrow == null || waiting(tomorrow));
        boolean noToday = g != null && !emergency && waiting(today) && blocks.isEmpty();

        List<String> lines = new ArrayList<String>();
        if (g == null) {
            lines.add("Не вдалося отримати графік");
        } else if (emergency) {
            lines.add("Діють екстрені відключення — графіки не діють");
        } else if ("off".equals(power)) {
            if (cur != null) {
                lines.add("Увімкнення за графіком о " + upto(cur[1]));
                int[] nx = null;
                for (int[] b : blocks) {
                    if (b != cur && b[0] >= cur[1]) { nx = b; break; }
                }
                if (nx != null) {
                    lines.add("Далі світло до " + upto(nx[0]) + ", потім відключення до " + upto(nx[1]));
                } else if (unpublished) {
                    lines.add("Графік на завтра ще не сформований");
                } else {
                    lines.add("Далі відключень не заплановано");
                }
            } else {
                lines.add("Відключення не за графіком");
                if (next != null) lines.add("За графіком далі: " + upto(next[0]) + " – " + upto(next[1]));
            }
        } else {
            if (cur != null) {
                lines.add("Світло є, хоча за графіком мало б не бути до " + upto(cur[1]));
                if (next != null) lines.add("Наступне відключення: " + upto(next[0]) + " – " + upto(next[1]));
                else if (unpublished) lines.add("Графік на завтра ще не сформований");
                else lines.add("Далі відключень не заплановано");
            } else if (next != null) {
                lines.add("До " + upto(next[0]) + ", а далі відключення до " + upto(next[1]));
            } else if (noToday) {
                lines.add("Графіки ще не опубліковані");
            } else {
                lines.add(unpublished ? "Відключень сьогодні більше не заплановано"
                        : "Відключень більше не заплановано");
                if (unpublished) lines.add("Графік на завтра ще не сформований");
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines.get(i));
        }
        s.sub = sb.toString();

        // графік по днях для блоку «Графік» і широкого віджета
        SimpleDateFormat lf = new SimpleDateFormat("dd.MM", Locale.US);
        lf.setTimeZone(Cfg.TZ);
        s.todayLabel = lf.format(cal.getTime());
        Calendar tc = (Calendar) cal.clone();
        tc.add(Calendar.DAY_OF_MONTH, 1);
        s.tomorrowLabel = lf.format(tc.getTime());
        if (g == null) {
            s.todayNote = "Немає даних";
            s.tomorrowNote = "Немає даних";
        } else {
            s.todayRows = rowsOf(today);
            s.tomorrowRows = rowsOf(tomorrow);
            if (emergency) s.todayNote = "Екстрені відключення — графік не діє";
            else if (waiting(today) && s.todayRows.isEmpty()) s.todayNote = "Графік ще не опублікований";
            else if (s.todayRows.isEmpty()) s.todayNote = "Відключень не заплановано";
            if (tomorrow == null || waiting(tomorrow)) s.tomorrowNote = "Графік на завтра ще не сформований";
            else if (emergency || isEmergency(tomorrow)) s.tomorrowNote = "Графік не діє";
            else if (s.tomorrowRows.isEmpty()) s.tomorrowNote = "Відключень не заплановано";
        }
        s.time = System.currentTimeMillis();
        return s;
    }
}
