package com.bankevent.config;

import com.bankevent.kafka.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares Kafka topics as Spring beans.
 *
 * <p>On startup, Spring Kafka's {@code KafkaAdmin} talks to the broker and
 * creates any missing topics. In production a bank's platform team usually
 * creates topics out-of-band (Terraform, Ansible) with stricter settings.
 * Auto-create is perfect for a learning laptop.
 *
 * <p>{@code partitions(3)}: each topic is split into 3 logs. Kafka assigns
 * records with the same key to the same partition, so all events for one
 * account stay in order. Consumers in a group share partitions for throughput.
 *
 * <p>{@code replicas(1)}: we have a single broker. A real cluster would use
 * 3 replicas so a broker crash does not lose the payment log.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic accountEvents() {
        return topic(Topics.ACCOUNT_EVENTS);
    }

    @Bean
    public NewTopic paymentsInitiated() {
        return topic(Topics.PAYMENTS_INITIATED);
    }

    @Bean
    public NewTopic paymentsApproved() {
        return topic(Topics.PAYMENTS_APPROVED);
    }

    @Bean
    public NewTopic paymentsSettled() {
        return topic(Topics.PAYMENTS_SETTLED);
    }

    @Bean
    public NewTopic paymentsFailed() {
        return topic(Topics.PAYMENTS_FAILED);
    }

    @Bean
    public NewTopic fraudAlerts() {
        return topic(Topics.FRAUD_ALERTS);
    }

    @Bean
    public NewTopic notifications() {
        return topic(Topics.NOTIFICATIONS);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
