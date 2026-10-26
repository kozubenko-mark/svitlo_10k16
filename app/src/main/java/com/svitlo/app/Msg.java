package com.svitlo.app;

final class Msg {
    int id;
    /** HTML-текст повідомлення (вже без обгорток для емодзі). */
    String html = "";
    /** Звичайний текст — для сповіщень. */
    String text = "";
    /** Час публікації, мс (0 — невідомо). */
    long time;
}
