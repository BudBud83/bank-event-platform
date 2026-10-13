package com.bankevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Entry point of the banking event platform.
 *
 * <p>{@code @SpringBootApplication} turns this class into a Spring Boot app:
 * it scans this package (and children) for {@code @Component}, {@code @Service},
 * {@code @RestController}, etc., and starts an embedded web server.
 *
 * <p>{@code @EnableKafka} activates {@code @KafkaListener} methods. Without it,
 * consumers would never subscribe to topics.
 *
 * <p>How the pieces fit together at runtime:
 * <ol>
 *   <li>You call REST endpoints (account open, payment initiate).</li>
 *   <li>Services write to the H2 "core ledger" database AND publish Kafka events.</li>
 *   <li>Independent listeners (fraud, ledger, notifications, audit) each consume
 *       those events in their own consumer group — like separate bank departments
 *       reading the same tape of activity.</li>
 * </ol>
 */
@EnableKafka
@SpringBootApplication
public class BankEventPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankEventPlatformApplication.class, args);
    }
}
