package com.bankevent.events;

import java.math.BigDecimal;
import java.time.Instant;

/** Fact: fraud screening passed. The ledger may now attempt double-entry booking. */
public record PaymentApprovedEvent(
        String eventId,
        Instant occurredAt,
        String correlationId,
        String paymentId,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency
) implements BankEvent {
}
