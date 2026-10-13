package com.bankevent.fraud;

/**
 * Result of screening one payment. {@code allowed=true} means the ledger may book it.
 */
public record FraudDecision(boolean allowed, String rule, String explanation) {

    public static FraudDecision clear() {
        return new FraudDecision(true, "CLEAR", "No fraud rule matched");
    }

    public static FraudDecision flag(String rule, String explanation) {
        return new FraudDecision(false, rule, explanation);
    }
}
