package com.deepak.ledger.service;

import com.deepak.ledger.domain.Account;
import com.deepak.ledger.repository.AccountRepository;
import com.deepak.ledger.repository.LedgerEntryRepository;
import com.deepak.ledger.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-style check: whatever sequence of valid transfers runs, the signed
 * sum of all ledger entries stays zero and no account goes negative.
 */
@IntegrationTest
@Import(IntegrationTest.Containers.class)
class LedgerInvariantIT {

    @Autowired TransferService transfers;
    @Autowired AccountRepository accounts;
    @Autowired LedgerEntryRepository entries;

    @Test
    void books_always_balance_under_random_traffic() {
        var ids = List.of(newAccount(), newAccount(), newAccount(), newAccount());
        var random = new Random(42);

        for (int i = 0; i < 300; i++) {
            var from = ids.get(random.nextInt(ids.size()));
            var to = ids.get(random.nextInt(ids.size()));
            if (from.equals(to)) continue;
            var amount = new BigDecimal(random.nextInt(1, 50) + ".0000");
            try {
                transfers.post("prop-" + UUID.randomUUID(), from, to, amount);
            } catch (RuntimeException expectedSometimes) {
                // insufficient funds and contention are valid outcomes
            }
        }

        assertThat(entries.signedTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        accounts.findAllById(ids).forEach(a ->
                assertThat(a.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO));
    }

    private UUID newAccount() {
        return accounts.save(new Account(UUID.randomUUID(), "PROP-" + UUID.randomUUID(), "EUR",
                new BigDecimal("1000.0000"))).getId();
    }
}
