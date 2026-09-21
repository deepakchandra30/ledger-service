# ADR 0001: Model balances as a double-entry ledger

Status: accepted

## Context

A transfer service can keep balances as a mutable column and update both sides,
or it can record immutable entries and treat the balance as a projection.

## Decision

Double entry. Each transfer writes two immutable `ledger_entry` rows, and
`account.balance` is a cached projection updated in the same transaction.

## Consequences

- Any balance can be rebuilt from the entries, so corruption is detectable.
- Corrections are reversing entries, which leaves an audit trail. Regulated
  environments need that trail.
- The balance column is redundant by design; it is kept because reading a
  balance is far more frequent than posting, and an aggregate over every entry
  would grow unbounded. The invariant test covers the risk of the two drifting.
