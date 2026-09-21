package com.deepak.ledger.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID from,
        @NotNull UUID to,
        @NotNull @Positive BigDecimal amount) { }
