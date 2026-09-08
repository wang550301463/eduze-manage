package com.eduze.platform.notification;

import java.time.Instant;

public final class NotificationPolicy {
    private NotificationPolicy() {}

    public static boolean acceptRevision(long current, long incoming) {
        return incoming > current;
    }

    public static boolean canSendReminder(Instant lessonStart, Instant now) {
        return lessonStart.isAfter(now);
    }

    public static String providerOutcome(int code) {
        if (code == 0) {
            return "SENT";
        }
        if (code == 43101 || code == 40003 || code == 47003) {
            return "SKIPPED";
        }
        return code == -1 || code == 45009 || code == 40001 || code == 42001 ? "RETRY" : "FAILED";
    }
}
