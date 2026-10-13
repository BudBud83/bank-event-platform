package com.bankevent.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Persistent record of a transfer request.
 *
 * <p>{@code idempotencyKey} is how banks make "Pay" safe to retry. If the
 * mobile app double-taps, the second HTTP call with the same key returns
 * the original payment instead of debiting twice.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String sourceAccountId;

    @Column(nullable = false)
    private String destinationAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private String correlationId;

    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(
            String id,
            String idempotencyKey,
            String sourceAccountId,
            String destinationAccountId,
            BigDecimal amount,
            String currency,
            String reference,
            String correlationId,
            Instant createdAt
    ) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.reference = reference;
        this.status = PaymentStatus.INITIATED;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void markApproved() {
        this.status = PaymentStatus.APPROVED;
        this.updatedAt = Instant.now();
    }

    public void markSettled() {
        this.status = PaymentStatus.SETTLED;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void markFlagged(String reason) {
        this.status = PaymentStatus.FLAGGED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getSourceAccountId() {
        return sourceAccountId;
    }

    public String getDestinationAccountId() {
        return destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getReference() {
        return reference;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
