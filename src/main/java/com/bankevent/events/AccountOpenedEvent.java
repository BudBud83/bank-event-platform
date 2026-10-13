package com.bankevent.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fact: a customer account now exists in the core books.
 *
 * <p>Java {@code record} is a compact immutable class. Kafka JSON serializer
 * writes the record components as JSON fields. Consumers deserialize back
 * into this type (via the type header we configured).
 */
public record AccountOpenedEvent(
        String eventId,
        Instant occurredAt,
        String correlationId,
        String accountId,
        String ownerName,
        String currency,
        BigDecimal openingBalance
) implements BankEvent {
}
