package com.deepak.ledger.settlement;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlement")
public class Settlement {

    @Id
    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    protected Settlement() { }

    public Settlement(UUID transferId, String amount) {
        this.transferId = transferId;
        this.amount = new BigDecimal(amount);
        this.settledAt = Instant.now();
    }

    public UUID getTransferId() { return transferId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getSettledAt() { return settledAt; }
}
