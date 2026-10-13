package com.bankevent.fraud;

import com.bankevent.events.FraudAlertEvent;
import com.bankevent.events.PaymentApprovedEvent;
import com.bankevent.events.PaymentInitiatedEvent;
import com.bankevent.kafka.KafkaEventPublisher;
import com.bankevent.kafka.Topics;
import com.bankevent.payment.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The "fraud department" as a Kafka consumer.
 *
 * <p>{@code @KafkaListener} tells Spring: join consumer group
 * {@code fraud-service} and invoke this method for each record on
 * {@code bank.payments.initiated}.
 *
 * <p><b>Consumer groups are the scaling unit.</b> If we started a second
 * instance with the same group id, Kafka would split partitions between them
 * (competing consumers). A different group id (audit-service) would receive
 * a <i>copy</i> of every message. That is how one payment event fans out to
 * many bank systems without the producer knowing who is listening.
 *
 * <p>After screening we publish either APPROVED (ledger may book) or a
 * fraud alert (ledger never sees it on the approved topic).
 */
@Component
public class FraudDetectionListener {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionListener.class);

    private final FraudRuleEngine rules;
    private final PaymentRepository payments;
    private final KafkaEventPublisher publisher;

    public FraudDetectionListener(
            FraudRuleEngine rules,
            PaymentRepository payments,
            KafkaEventPublisher publisher
    ) {
        this.rules = rules;
        this.payments = payments;
        this.publisher = publisher;
    }

    @Transactional
    @KafkaListener(topics = Topics.PAYMENTS_INITIATED, groupId = "fraud-service")
    public void onPaymentInitiated(PaymentInitiatedEvent event) {
        log.info("Fraud screening payment {} amount={}", event.paymentId(), event.amount());

        FraudDecision decision = rules.screen(event);
        Instant now = Instant.now();

        payments.findById(event.paymentId()).ifPresent(payment -> {
            if (decision.allowed()) {
                payment.markApproved();
                publisher.publish(
                        Topics.PAYMENTS_APPROVED,
                        event.sourceAccountId(),
                        new PaymentApprovedEvent(
                                UUID.randomUUID().toString(),
                                now,
                                event.correlationId(),
                                event.paymentId(),
                                event.sourceAccountId(),
                                event.destinationAccountId(),
                                event.amount(),
                                event.currency()
                        )
                );
            } else {
                payment.markFlagged(decision.rule());
                publisher.publish(
                        Topics.FRAUD_ALERTS,
                        event.sourceAccountId(),
                        new FraudAlertEvent(
                                UUID.randomUUID().toString(),
                                now,
                                event.correlationId(),
                                event.paymentId(),
                                event.sourceAccountId(),
                                event.amount(),
                                decision.rule(),
                                decision.explanation()
                        )
                );
            }
        });
    }
}
