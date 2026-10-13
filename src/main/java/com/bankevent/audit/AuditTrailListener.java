package com.bankevent.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Compliance / audit tape.
 *
 * <p>This listener uses a <b>different consumer group</b> ({@code audit-service})
 * and subscribes to <b>every</b> domain topic. Kafka therefore delivers a
 * second copy of each record here, independent of fraud/ledger/notifications.
 *
 * <p>That is the teaching point: producers do not call audit. Audit just
 * joins the log. In production these records would land in an immutable
 * store (WORM disk, data lake) for regulators.
 *
 * <p>We consume as {@code Object} because the topics carry different Java types.
 * The {@code eventType} header tells us what we got.
 */
@Component
public class AuditTrailListener {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailListener.class);

    @KafkaListener(
            topics = {
                    "bank.account.events",
                    "bank.payments.initiated",
                    "bank.payments.approved",
                    "bank.payments.settled",
                    "bank.payments.failed",
                    "bank.fraud.alerts"
            },
            groupId = "audit-service"
    )
    public void onAnyEvent(
            @Payload Object event,
            @Header(name = "eventType", required = false) String eventType,
            @Header(name = "correlationId", required = false) String correlationId
    ) {
        log.info("AUDIT eventType={} correlationId={} payload={}", eventType, correlationId, event);
    }
}
