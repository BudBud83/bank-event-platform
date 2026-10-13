package com.bankevent.fraud;

import com.bankevent.events.PaymentInitiatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Toy fraud engine with two rules banks actually use (in far richer form):
 *
 * <ol>
 *   <li><b>High value</b> — amounts above a policy threshold need extra review.</li>
 *   <li><b>Velocity</b> — too many payments from one account in a short window
 *       looks like a stolen session or script.</li>
 * </ol>
 *
 * <p>State lives in memory ({@link ConcurrentHashMap}) so a restart forgets
 * velocity. A real fraud service would use Redis or a stream processor
 * (Kafka Streams / Flink) keyed by account id.
 *
 * <p>{@code @Value} injects numbers from {@code application.yml} under
 * {@code bank.fraud.*}.
 */
@Component
public class FraudRuleEngine {

    private final java.math.BigDecimal highValueThreshold;
    private final int velocityWindowSeconds;
    private final int velocityMaxPayments;

    /** accountId -> timestamps of recent initiated payments */
    private final Map<String, Deque<Instant>> recentByAccount = new ConcurrentHashMap<>();

    public FraudRuleEngine(
            @Value("${bank.fraud.high-value-threshold}") java.math.BigDecimal highValueThreshold,
            @Value("${bank.fraud.velocity-window-seconds}") int velocityWindowSeconds,
            @Value("${bank.fraud.velocity-max-payments}") int velocityMaxPayments
    ) {
        this.highValueThreshold = highValueThreshold;
        this.velocityWindowSeconds = velocityWindowSeconds;
        this.velocityMaxPayments = velocityMaxPayments;
    }

    public FraudDecision screen(PaymentInitiatedEvent event) {
        if (event.amount().compareTo(highValueThreshold) > 0) {
            return FraudDecision.flag(
                    "HIGH_VALUE",
                    "Amount " + event.amount() + " exceeds threshold " + highValueThreshold
            );
        }

        Deque<Instant> times = recentByAccount.computeIfAbsent(
                event.sourceAccountId(), id -> new ArrayDeque<>());

        Instant cutoff = event.occurredAt().minusSeconds(velocityWindowSeconds);
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
                times.removeFirst();
            }
            times.addLast(event.occurredAt());
            if (times.size() > velocityMaxPayments) {
                return FraudDecision.flag(
                        "VELOCITY",
                        times.size() + " payments from this account within "
                                + velocityWindowSeconds + " seconds"
                );
            }
        }

        return FraudDecision.clear();
    }
}
