package com.bankevent.fraud;

import com.bankevent.events.PaymentInitiatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests do not need Kafka. We construct the engine with known thresholds
 * and feed it events. This is how you should test business rules: fast, no broker.
 */
class FraudRuleEngineTest {

    private final FraudRuleEngine engine = new FraudRuleEngine(
            new BigDecimal("10000"),
            60,
            3
    );

    @Test
    void smallPaymentClears() {
        FraudDecision decision = engine.screen(payment("acc-1", "50.00", Instant.now()));
        assertTrue(decision.allowed());
        assertEquals("CLEAR", decision.rule());
    }

    @Test
    void highValueIsFlagged() {
        FraudDecision decision = engine.screen(payment("acc-1", "10000.01", Instant.now()));
        assertFalse(decision.allowed());
        assertEquals("HIGH_VALUE", decision.rule());
    }

    @Test
    void fourthPaymentInWindowIsFlagged() {
        Instant t = Instant.parse("2026-01-01T12:00:00Z");
        assertTrue(engine.screen(payment("acc-2", "10", t)).allowed());
        assertTrue(engine.screen(payment("acc-2", "10", t.plusSeconds(1))).allowed());
        assertTrue(engine.screen(payment("acc-2", "10", t.plusSeconds(2))).allowed());
        FraudDecision fourth = engine.screen(payment("acc-2", "10", t.plusSeconds(3)));
        assertFalse(fourth.allowed());
        assertEquals("VELOCITY", fourth.rule());
    }

    private PaymentInitiatedEvent payment(String account, String amount, Instant when) {
        return new PaymentInitiatedEvent(
                UUID.randomUUID().toString(),
                when,
                "corr",
                UUID.randomUUID().toString(),
                account,
                "dest",
                new BigDecimal(amount),
                "USD",
                "test"
        );
    }
}
