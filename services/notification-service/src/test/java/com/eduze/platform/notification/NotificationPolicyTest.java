package com.eduze.platform.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotificationPolicyTest {
    @Test
    void newerScheduleInvalidatesOldReminderEvenWhenOldEventArrivesLate() {
        assertThat(NotificationPolicy.acceptRevision(5, 4)).isFalse();
        assertThat(NotificationPolicy.acceptRevision(5, 5)).isFalse();
        assertThat(NotificationPolicy.acceptRevision(5, 6)).isTrue();
    }

    @Test
    void expiredClassReminderMustNotBeSent() {
        Instant now = Instant.parse("2026-09-08T08:00:00Z");
        assertThat(NotificationPolicy.canSendReminder(now.minusSeconds(1), now)).isFalse();
        assertThat(NotificationPolicy.canSendReminder(now.plusSeconds(60), now)).isTrue();
    }

    @Test
    void rejectedSubscriptionIsNotReportedAsSuccessfulDelivery() {
        assertThat(NotificationPolicy.providerOutcome(43101)).isEqualTo("SKIPPED");
        assertThat(NotificationPolicy.providerOutcome(-1)).isEqualTo("RETRY");
        assertThat(NotificationPolicy.providerOutcome(0)).isEqualTo("SENT");
    }
}
