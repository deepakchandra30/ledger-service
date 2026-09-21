package com.deepak.ledger.service;

import com.deepak.ledger.domain.Account;
import com.deepak.ledger.repository.AccountRepository;
import com.deepak.ledger.repository.LedgerEntryRepository;
import com.deepak.ledger.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The lost-update test. 200 concurrent transfers hit the same pair of accounts.
 * Optimistic locking means some lose the race and are rejected with a conflict;
 * what must never happen is a balance that does not match the entries written.
 *
 */
@IntegrationTest
@Import(IntegrationTest.Containers.class)
class TransferConcurrencyIT {

    @Autowired TransferService transfers;
    @Autowired AccountRepository accounts;
    @Autowired LedgerEntryRepository entries;

    @Test
    void concurrent_transfers_never_lose_an_update() throws Exception {
        var from = accounts.save(new Account(UUID.randomUUID(), "SRC-" + UUID.randomUUID(), "EUR",
                new BigDecimal("100000.0000")));
        var to = accounts.save(new Account(UUID.randomUUID(), "DST-" + UUID.randomUUID(), "EUR",
                BigDecimal.ZERO));

        int attempts = 200;
        var amount = new BigDecimal("10.0000");
        var succeeded = new AtomicInteger();
        var contended = new AtomicInteger();

        try (var pool = Executors.newFixedThreadPool(16)) {
            var latch = new CountDownLatch(1);
            var futures = new CompletableFuture[attempts];
            for (int i = 0; i < attempts; i++) {
                final String key = "load-" + i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    try {
                        latch.await();
                        transfers.post(key, from.getId(), to.getId(), amount);
                        succeeded.incrementAndGet();
                    } catch (OptimisticLockingFailureException e) {
                        contended.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, pool);
            }
            latch.countDown();
            CompletableFuture.allOf(futures).join();
        }

        var creditedAccount = accounts.findById(to.getId()).orElseThrow();
        var expected = amount.multiply(BigDecimal.valueOf(succeeded.get()));

        System.out.printf("succeeded=%d contended=%d balance=%s%n",
                succeeded.get(), contended.get(), creditedAccount.getBalance());

        assertThat(succeeded.get() + contended.get()).isEqualTo(attempts);
        assertThat(creditedAccount.getBalance()).isEqualByComparingTo(expected);
        assertThat(entries.signedTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
