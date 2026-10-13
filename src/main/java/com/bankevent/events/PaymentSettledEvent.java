package com.bankevent.events;

import java.math.BigDecimal;
import java.time.Instant;

/** Fact: debit and credit both posted. This is the "money moved" moment. */
public record PaymentSettledEvent(
        String eventId,
        Instant occurredAt,
        String correlationId,
        String paymentId,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        BigDecimal sourceBalanceAfter,
        BigDecimal destinationBalanceAfter
) implements BankEvent {
}
