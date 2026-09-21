package com.deepak.ledger.outbox;

import com.deepak.ledger.repository.OutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drains the outbox to Kafka. At-least-once by design: a crash between send
 * and mark-published republishes the event, and the settlement consumer is
 * keyed by transfer id so a duplicate is ignored.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final MeterRegistry meters;
    private final String topic;

    public OutboxPublisher(OutboxRepository outbox, KafkaTemplate<String, String> kafka,
                           MeterRegistry meters,
                           @Value("${ledger.topics.transfers}") String topic) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.meters = meters;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${ledger.outbox.interval-ms:500}")
    @Transactional
    public void publishPending() {
        var batch = outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (var event : batch) {
            kafka.send(topic, event.getAggregateId().toString(), event.getPayload());
            event.markPublished();
        }
        if (!batch.isEmpty()) {
            meters.counter("ledger.outbox.published").increment(batch.size());
            log.debug("published {} outbox events", batch.size());
        }
    }
}
