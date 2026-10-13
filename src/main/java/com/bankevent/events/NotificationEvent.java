package com.bankevent.events;

import java.time.Instant;

/**
 * A message we would send by SMS/email/push in production.
 * Here we only publish it to Kafka and log it — no real mail server.
 */
public record NotificationEvent(
        String eventId,
        Instant occurredAt,
        String correlationId,
        String accountId,
        String channel,
        String subject,
        String body
) implements BankEvent {
}
