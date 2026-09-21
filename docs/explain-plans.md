# Query plans

Record `EXPLAIN (ANALYZE, BUFFERS)` output here before and after each index, so
the index has a justification rather than a guess.

## Statement query: entries for one account

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM ledger_entry
WHERE account_id = '...'
ORDER BY created_at DESC
LIMIT 50;
```

Before `idx_entry_account_created`: sequential scan plus sort.
After: index scan, no sort.

Paste both plans here with the row counts and timings.

## Outbox drain

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM outbox_event
WHERE published_at IS NULL
ORDER BY created_at
LIMIT 100;
```

The partial index means this reads only the backlog. Show the plan with an empty
backlog and with 100k published rows present, which is the point of the index.
