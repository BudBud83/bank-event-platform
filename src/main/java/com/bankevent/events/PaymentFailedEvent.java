package com.bankevent.events;

import java.time.Instant;

/**
 * Fact: we could not complete the payment. {@code reason} is a stable code
 * (INSUFFICIENT_FUNDS, ACCOUNT_NOT_FOUND) that UIs and ops can switch on.
 */
public record PaymentFailedEvent(
        String eventId,
        Instant occurredAt,
        String correlationId,
        String paymentId,
        String reason,
        String detail
) implements BankEvent {
}
