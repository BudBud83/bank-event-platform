package com.bankevent.events;

import java.time.Instant;

/**
 * Marker for every message we put on Kafka.
 *
 * <p>Banks rarely send "raw" payloads. They wrap facts in an envelope so every
 * consumer can log {@code eventId} (idempotency), {@code occurredAt} (when it
 * happened in the domain), and {@code correlationId} (tie a REST call to every
 * downstream topic hop: initiated → approved → settled).
 */
public interface BankEvent {

    String eventId();

    Instant occurredAt();

    /** Same id from the original HTTP request, copied onto every follow-up event. */
    String correlationId();
}
