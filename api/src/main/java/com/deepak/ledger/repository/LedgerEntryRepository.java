package com.deepak.ledger.repository;

import com.deepak.ledger.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransferId(UUID transferId);

    /** Ledger invariant: the signed sum of every entry must be zero. */
    @Query("select coalesce(sum(case when e.direction = com.deepak.ledger.domain.EntryDirection.CREDIT "
         + "then e.amount else -e.amount end), 0) from LedgerEntry e")
    BigDecimal signedTotal();
}
