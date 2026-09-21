package com.deepak.ledger.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox row. Written in the same transaction as the ledger
 * entries, so an event exists if and only if the posting committed. A
 * scheduled publisher moves rows to Kafka and marks them published, which
 * gives at-least-once delivery without a distributed transaction.
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() { }

    public OutboxEvent(UUID aggregateId, String type, String payload) {
        this.id = UUID.randomUUID();
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public void markPublished() { this.publishedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public Instant getPublishedAt() { return publishedAt; }
}
