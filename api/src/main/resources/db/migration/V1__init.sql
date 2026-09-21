create table account (
    id          uuid primary key,
    reference   varchar(64)  not null unique,
    currency    char(3)      not null,
    balance     numeric(19,4) not null default 0,
    version     bigint       not null default 0
);

create table transfer (
    id              uuid primary key,
    idempotency_key varchar(128) not null unique,
    from_account    uuid not null references account(id),
    to_account      uuid not null references account(id),
    amount          numeric(19,4) not null check (amount > 0),
    status          varchar(10) not null,
    created_at      timestamptz not null
);

create table ledger_entry (
    id          uuid primary key,
    transfer_id uuid not null references transfer(id),
    account_id  uuid not null references account(id),
    direction   varchar(6) not null check (direction in ('DEBIT','CREDIT')),
    amount      numeric(19,4) not null check (amount > 0),
    created_at  timestamptz not null
);

create table outbox_event (
    id           uuid primary key,
    aggregate_id uuid not null,
    type         varchar(64) not null,
    payload      text not null,
    created_at   timestamptz not null,
    published_at timestamptz
);

-- Statement account lookups: entries for one account, newest first.
create index idx_entry_account_created on ledger_entry (account_id, created_at desc);

-- Reconciliation joins entries back to their transfer.
create index idx_entry_transfer on ledger_entry (transfer_id);

-- Publisher scans only unpublished rows; partial index keeps it small as the
-- table grows, so the scan stays O(backlog) rather than O(all events).
create index idx_outbox_unpublished on outbox_event (created_at) where published_at is null;
