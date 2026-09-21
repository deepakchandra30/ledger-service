# Architecture

```
   client
     |  POST /api/transfers  (JWT, Idempotency-Key)
     v
+-------------------+        one database transaction
|   ledger-api      |  ---------------------------------------+
|  TransferService  |   debit + credit + transfer + outbox row |
+-------------------+  ---------------------------------------+
     |                                   |
     | Micrometer                        | outbox table (Postgres)
     v                                   v
 Prometheus <-- Grafana        OutboxPublisher (every 500ms)
                                         |
                                         v
                              Kafka: ledger.transfers.v1
                                         |
                                         v
                            +------------------------+
                            |  ledger-settlement     |
                            |  idempotent consumer   |
                            |  retry + circuit break |
                            +------------------------+
```

## Why double entry

Every posting writes a DEBIT and a CREDIT of the same amount, so the signed sum
of all entries is always zero. That invariant is asserted in `LedgerInvariantIT`
and is what makes reconciliation possible: a balance can always be rebuilt from
the entries, and a wrong balance is provably a bug rather than an opinion.

## Why an outbox instead of publishing from the service

Writing to Postgres and publishing to Kafka are two systems. Publishing inside
the transaction risks an event for a posting that later rolled back; publishing
after the commit risks a posting with no event if the process dies in between.
The outbox row is written in the same transaction as the entries, so the event
exists if and only if the money moved. The publisher then delivers at least
once, and the consumer is idempotent.

## Failure modes handled

| Failure | Behaviour |
|---|---|
| Two transfers race on one account | Optimistic lock, loser gets 409, no lost update |
| Client retries a request | Idempotency key returns the original transfer, posts nothing |
| Process dies after commit, before publish | Publisher picks the row up on the next tick |
| Event delivered twice | Consumer drops it, `settlement.duplicates` increments |
| Settlement store unavailable | Resilience4j retries, then the circuit opens |
| Insufficient funds | 422, no entries written |
