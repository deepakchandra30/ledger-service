package com.deepak.ledger.service;

import com.deepak.ledger.domain.*;
import com.deepak.ledger.repository.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Posts transfers as balanced double-entry pairs.
 *
 * Isolation: READ_COMMITTED plus optimistic locking on Account. The balance
 * update is a compare-and-set on the version column, so a concurrent transfer
 * on the same account fails fast with an optimistic lock exception instead of
 * producing a lost update. SERIALIZABLE would also be correct but serialises
 * unrelated accounts; see docs/adr/0002-isolation-level.md.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final LedgerEntryRepository entries;
    private final OutboxRepository outbox;
    private final Counter posted;
    private final Timer postTimer;

    public TransferService(AccountRepository accounts, TransferRepository transfers,
                           LedgerEntryRepository entries, OutboxRepository outbox,
                           MeterRegistry meters) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.entries = entries;
        this.outbox = outbox;
        this.posted = Counter.builder("ledger.transfers.posted").register(meters);
        this.postTimer = Timer.builder("ledger.transfers.latency").register(meters);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transfer post(String idempotencyKey, UUID from, UUID to, BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("cannot transfer to the same account");
        }

        // Replayed request: return the original result, post nothing.
        var existing = transfers.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("idempotent replay key={} transfer={}", idempotencyKey, existing.get().getId());
            return existing.get();
        }

        return postTimer.record(() -> {
            Account debited = accounts.findById(from).orElseThrow(() -> new AccountNotFoundException(from));
            Account credited = accounts.findById(to).orElseThrow(() -> new AccountNotFoundException(to));

            if (!debited.getCurrency().equals(credited.getCurrency())) {
                throw new IllegalArgumentException("currency mismatch");
            }

            debited.debit(amount);
            credited.credit(amount);

            Transfer transfer = transfers.save(new Transfer(idempotencyKey, from, to, amount));
            entries.save(new LedgerEntry(transfer.getId(), from, EntryDirection.DEBIT, amount));
            entries.save(new LedgerEntry(transfer.getId(), to, EntryDirection.CREDIT, amount));

            outbox.save(new OutboxEvent(transfer.getId(), "TransferPosted",
                    """
                    {"transferId":"%s","from":"%s","to":"%s","amount":"%s"}"""
                            .formatted(transfer.getId(), from, to, amount)));

            posted.increment();
            return transfer;
        });
    }

    @Transactional(readOnly = true)
    public BigDecimal ledgerImbalance() {
        return entries.signedTotal();
    }
}
