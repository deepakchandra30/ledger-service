package com.deepak.ledger.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer")
public class Transfer {

    @Id
    private UUID id;

    /** Client supplied key. Unique index makes a replayed request a no-op. */
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "from_account", nullable = false)
    private UUID fromAccount;

    @Column(name = "to_account", nullable = false)
    private UUID toAccount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransferStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Transfer() { }

    public Transfer(String idempotencyKey, UUID fromAccount, UUID toAccount, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.status = TransferStatus.POSTED;
        this.createdAt = Instant.now();
    }

    public void settled() { this.status = TransferStatus.SETTLED; }
    public void failed()  { this.status = TransferStatus.FAILED; }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getFromAccount() { return fromAccount; }
    public UUID getToAccount() { return toAccount; }
    public BigDecimal getAmount() { return amount; }
    public TransferStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
