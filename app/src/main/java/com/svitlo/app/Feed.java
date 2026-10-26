package com.svitlo.app;

import android.text.Html;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Читає публічну веб-версію каналу: https://t.me/s/<канал>. */
final class Feed {
    private Feed() {}

    private static final Pattern TEXT = Pattern.compile(
            "<div class=\"tgme_widget_message_text[^>]*>(.*?)</div>", Pattern.DOTALL);
    private static final Pattern TIME = Pattern.compile("<time[^>]*datetime=\"([^\"]+)\"");
    private static final Pattern EMOJI = Pattern.compile(
            "<i class=\"emoji\"[^>]*><b>(.*?)</b></i>", Pattern.DOTALL);

    /** before = 0 — найновіші повідомлення, інакше — старіші за цей номер. */
    static List<Msg> load(int before) throws IOException {
        String url = "https://t.me/s/" + Cfg.CHANNEL + (before > 0 ? "?before=" + before : "");
        return parse(Http.get(url));
    }

    static List<Msg> parse(String html) {
        List<Msg> out = new ArrayList<Msg>();
        String key = "data-post=\"" + Cfg.CHANNEL + "/";
        int pos = html.indexOf(key);
        while (pos >= 0) {
            int next = html.indexOf(key, pos + key.length());
            String chunk = next < 0 ? html.substring(pos) : html.substring(pos, next);
            Msg m = one(chunk, key.length());
            if (m != null) out.add(m);
            pos = next;
        }
        Collections.sort(out, new Comparator<Msg>() {
            @Override public int compare(Msg a, Msg b) { return a.id - b.id; }
        });
        return out;
    }

    private static Msg one(String chunk, int keyLen) {
        int end = keyLen;
        while (end < chunk.length() && Character.isDigit(chunk.charAt(end))) end++;
        if (end == keyLen) return null;
        Msg m = new Msg();
        try {
            m.id = Integer.parseInt(chunk.substring(keyLen, end));
        } catch (NumberFormatException e) {
            return null;
        }
        Matcher t = TEXT.matcher(chunk);
        if (t.find()) {
            String inner = EMOJI.matcher(t.group(1)).replaceAll("$1");
            m.html = inner.trim();
            m.text = Html.fromHtml(m.html, Html.FROM_HTML_MODE_LEGACY).toString().trim();
        }
        if (m.text.length() == 0) return null; // фото/службові повідомлення без тексту пропускаємо
        Matcher tm = TIME.matcher(chunk);
        if (tm.find()) {
            try {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
                Date d = f.parse(tm.group(1));
                if (d != null) m.time = d.getTime();
            } catch (Exception ignored) {
            }
        }
        return m;
    }
}
