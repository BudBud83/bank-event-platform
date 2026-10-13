package com.bankevent.kafka;

/**
 * Central list of Kafka topic names.
 *
 * <p>A topic is an append-only log of events. Producers write; consumers read
 * at their own pace. We use several topics (a "pipeline") instead of one giant
 * topic so each department only receives the events it is allowed to act on.
 *
 * <p>Example: the ledger must NEVER post money on a payment that fraud has not
 * cleared. So the ledger listens to {@link #PAYMENTS_APPROVED}, not
 * {@link #PAYMENTS_INITIATED}.
 */
public final class Topics {

    /** Account opened / closed / frozen — reference data for the rest of the bank. */
    public static final String ACCOUNT_EVENTS = "bank.account.events";

    /** Customer (or another channel) asked us to move money. Not yet booked. */
    public static final String PAYMENTS_INITIATED = "bank.payments.initiated";

    /** Fraud rules passed. Ledger is allowed to debit/credit. */
    public static final String PAYMENTS_APPROVED = "bank.payments.approved";

    /** Both sides of the double-entry posting succeeded. */
    public static final String PAYMENTS_SETTLED = "bank.payments.settled";

    /** Could not settle (e.g. insufficient funds) after approval. */
    public static final String PAYMENTS_FAILED = "bank.payments.failed";

    /** Suspicious payment. Ledger will not book it. */
    public static final String FRAUD_ALERTS = "bank.fraud.alerts";

    /** Human-readable notices derived from the events above. */
    public static final String NOTIFICATIONS = "bank.notifications";

    private Topics() {
        // utility class — no instances
    }
}
