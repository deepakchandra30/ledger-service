package com.deepak.ledger.web;

import com.deepak.ledger.domain.Transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(UUID id, UUID from, UUID to, BigDecimal amount,
                               String status, Instant createdAt) {

    public static TransferResponse of(Transfer t) {
        return new TransferResponse(t.getId(), t.getFromAccount(), t.getToAccount(),
                t.getAmount(), t.getStatus().name(), t.getCreatedAt());
    }
}
