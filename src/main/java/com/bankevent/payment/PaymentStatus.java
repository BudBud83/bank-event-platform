package com.bankevent.payment;

/**
 * Lifecycle of a payment in this pipeline.
 *
 * <pre>
 * INITIATED --(fraud ok)--> APPROVED --(ledger ok)--> SETTLED
 *           \                  \
 *            \                  +--(no funds)--> FAILED
 *             +--(fraud hit)--> FLAGGED
 * </pre>
 */
public enum PaymentStatus {
    INITIATED,
    APPROVED,
    SETTLED,
    FAILED,
    FLAGGED
}
