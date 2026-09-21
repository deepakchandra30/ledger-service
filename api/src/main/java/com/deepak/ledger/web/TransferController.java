package com.deepak.ledger.web;

import com.deepak.ledger.domain.Transfer;
import com.deepak.ledger.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_ledger.write')")
    public ResponseEntity<TransferResponse> post(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        Transfer t = transfers.post(idempotencyKey, request.from(), request.to(), request.amount());
        return ResponseEntity.created(URI.create("/api/transfers/" + t.getId()))
                .body(TransferResponse.of(t));
    }
}
