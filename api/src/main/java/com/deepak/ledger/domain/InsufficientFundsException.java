package com.deepak.ledger.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(UUID accountId, BigDecimal balance, BigDecimal requested) {
        super("Account %s has %s, cannot debit %s".formatted(accountId, balance, requested));
    }
}
