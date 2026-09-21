# ADR 0002: READ COMMITTED with optimistic locking

Status: accepted

## Context

Concurrent transfers on the same account can produce a lost update: both read
balance 100, both write 90, one transfer vanishes. Postgres defaults to READ
COMMITTED, which does not prevent this on its own.

Options considered:

1. SERIALIZABLE for every transaction.
2. Pessimistic row locks (`SELECT ... FOR UPDATE`) on both accounts.
3. READ COMMITTED plus a version column (optimistic locking).

## Decision

Option 3. `Account.version` makes the balance update a compare-and-set. A
concurrent writer fails with an optimistic lock exception, which the API maps to
409 with a retryable message.

## Consequences

- Uncontended transfers, which is nearly all of them, pay nothing.
- Hot accounts see conflicts, and the client retries. `TransferConcurrencyIT`
  measures the conflict rate under 200 concurrent transfers on one pair.
- SERIALIZABLE would also be correct but serialises unrelated accounts and
  raises the failure rate under load. Pessimistic locks need a consistent lock
  ordering across both accounts to avoid deadlock; that constraint is easy to
  break later, so it is left to the benchmark path only.
