package com.svitlo.app;

import java.util.TimeZone;

final class Cfg {
    private Cfg() {}

    /** Публічний канал, який читає застосунок (без підписки і без Telegram). */
    static final String CHANNEL = "svitlo_10k16";
    /** Номер закріпленого посту зі статусом («🟢 Світло є» / «🔴 Світла немає»). */
    static final int STATUS_POST = 27;

    /** Група/черга у Yasno. */
    static final String GROUP = "10.1";
    static final int REGION = 25;
    static final int DSO = 902;
    static final String YASNO = "https://app.yasno.ua/api/blackout-service/public/shutdowns/regions/"
            + REGION + "/dsos/" + DSO + "/planned-outages";

    /** Як часто сервіс перевіряє канал. */
    static final long POLL_MS = 60_000L;

    static final TimeZone TZ = TimeZone.getTimeZone("Europe/Kiev");
}
