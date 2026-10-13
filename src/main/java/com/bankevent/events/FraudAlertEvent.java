package com.bankevent.events;

import java.math.BigDecimal;
import java.time.Instant;

/** Fact: rules flagged a payment. Downstream must not settle it automatically. */
public record FraudAlertEvent(
        String eventId,
        Instant occurredAt,
        String correlationId,
        String paymentId,
        String sourceAccountId,
        BigDecimal amount,
        String rule,
        String explanation
) implements BankEvent {
}
