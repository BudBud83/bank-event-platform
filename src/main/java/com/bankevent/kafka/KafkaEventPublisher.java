package com.bankevent.kafka;

import com.bankevent.events.BankEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Spring's {@link KafkaTemplate}.
 *
 * <p>Every banking event we publish goes through this class so we always:
 * <ul>
 *   <li>use the same keying rule (order per account/payment)</li>
 *   <li>stamp a correlation id (trace a payment across topics)</li>
 *   <li>stamp the Java event type (helps operators and consumers)</li>
 * </ul>
 *
 * <p>{@code KafkaTemplate.send} is asynchronous: it returns a {@code CompletableFuture}.
 * We {@code join()} so the REST call does not return until the broker has accepted
 * the record ({@code acks=all} in application.yml). That is slower, but for money
 * movement "did Kafka get it?" matters more than a few milliseconds.
 */
@Component
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    /** KafkaTemplate is thread-safe. Spring injects the producer configured in YAML. */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, BankEvent event) {
        Message<BankEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader("eventType", event.getClass().getSimpleName())
                .setHeader("correlationId", event.correlationId())
                .build();

        kafkaTemplate.send(message).join();
        log.info("Published {} to topic={} key={} correlationId={}",
                event.getClass().getSimpleName(), topic, key, event.correlationId());
    }
}
