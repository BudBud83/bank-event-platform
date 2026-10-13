package com.bankevent.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fact: a transfer was requested. Money has NOT moved yet.
 *
 * <p>This is a "command result" event. Fraud listens here. Ledger does not —
 * posting on initiation would skip the control that every real bank has.
 */
public record PaymentInitiatedEvent(
        String eventId,
        Instant occurredAt,
        String correlationId,
        String paymentId,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        String reference
) implements BankEvent {
}
