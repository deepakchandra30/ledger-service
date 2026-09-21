package com.deepak.ledger.settlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Applies posted transfers to the settlement store.
 *
 * Delivery is at-least-once (the producer side is a transactional outbox), so
 * this consumer is idempotent: settlement is keyed by transfer id and a repeat
 * of an already settled id is dropped. A permanently failing message goes to
 * the dead letter topic rather than blocking the partition.
 */
@Component
public class SettlementConsumer {

    private static final Logger log = LoggerFactory.getLogger(SettlementConsumer.class);

    private final SettlementRepository settlements;
    private final ObjectMapper json;
    private final MeterRegistry meters;

    public SettlementConsumer(SettlementRepository settlements, ObjectMapper json, MeterRegistry meters) {
        this.settlements = settlements;
        this.json = json;
        this.meters = meters;
    }

    @KafkaListener(topics = "${ledger.topics.transfers}", groupId = "settlement")
    @Retry(name = "settlement")
    @CircuitBreaker(name = "settlement")
    public void onTransferPosted(String payload) throws Exception {
        var node = json.readTree(payload);
        var transferId = UUID.fromString(node.get("transferId").asText());

        if (settlements.existsById(transferId)) {
            meters.counter("settlement.duplicates").increment();
            log.debug("transfer {} already settled, dropping duplicate", transferId);
            return;
        }

        settlements.save(new Settlement(transferId, node.get("amount").asText()));
        meters.counter("settlement.applied").increment();
        log.info("settled transfer {}", transferId);
    }
}
