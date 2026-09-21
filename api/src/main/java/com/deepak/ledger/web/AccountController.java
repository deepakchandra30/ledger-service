package com.deepak.ledger.web;

import com.deepak.ledger.domain.Account;
import com.deepak.ledger.repository.AccountRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accounts;

    public AccountController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_ledger.read')")
    public List<AccountView> all() {
        return accounts.findAll().stream().map(AccountView::of).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ledger.read')")
    public AccountView one(@PathVariable UUID id) {
        return AccountView.of(accounts.findById(id).orElseThrow());
    }

    public record AccountView(UUID id, String reference, String currency, BigDecimal balance) {
        static AccountView of(Account a) {
            return new AccountView(a.getId(), a.getReference(), a.getCurrency(), a.getBalance());
        }
    }
}
