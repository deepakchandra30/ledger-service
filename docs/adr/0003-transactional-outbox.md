# ADR 0003: Transactional outbox for event publishing

Status: accepted

## Context

Settlement reacts to posted transfers. The posting is a database transaction;
the notification is a Kafka publish. There is no transaction spanning both.

## Decision

Write an `outbox_event` row inside the posting transaction and publish it from a
scheduled drainer.

## Consequences

- Delivery is at least once. The consumer must be idempotent, and it is: it is
  keyed by transfer id and drops a repeat.
- Latency is bounded by the drain interval (500ms), not by the posting path.
- A partial index on unpublished rows keeps the drain query proportional to the
  backlog rather than to the table.
