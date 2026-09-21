package com.deepak.ledger.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * An account in the ledger. The balance field is a cached projection of the
 * account's ledger entries; {@link com.deepak.ledger.service.TransferService}
 * keeps the two consistent inside one database transaction.
 *
 * Concurrency: the {@code version} column gives optimistic locking. Two
 * transfers touching the same account race on this column, and the loser
 * retries rather than silently overwriting the winner (lost update).
 */
@Entity
@Table(name = "account")
public class Account {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    private long version;

    protected Account() { }

    public Account(UUID id, String reference, String currency, BigDecimal opening) {
        this.id = id;
        this.reference = reference;
        this.currency = currency;
        this.balance = opening;
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(id, balance, amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public long getVersion() { return version; }
}
