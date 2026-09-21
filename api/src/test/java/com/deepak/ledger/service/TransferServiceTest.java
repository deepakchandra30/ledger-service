package com.deepak.ledger.service;

import com.deepak.ledger.domain.*;
import com.deepak.ledger.repository.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock AccountRepository accounts;
    @Mock TransferRepository transfers;
    @Mock LedgerEntryRepository entries;
    @Mock OutboxRepository outbox;

    TransferService service;

    UUID fromId = UUID.randomUUID();
    UUID toId = UUID.randomUUID();
    Account from;
    Account to;

    @BeforeEach
    void setUp() {
        service = new TransferService(accounts, transfers, entries, outbox, new SimpleMeterRegistry());
        from = new Account(fromId, "ACC-1", "EUR", new BigDecimal("100.0000"));
        to = new Account(toId, "ACC-2", "EUR", BigDecimal.ZERO);
    }

    @Test
    void posts_balanced_debit_and_credit() {
        when(transfers.findByIdempotencyKey("k1")).thenReturn(Optional.empty());
        when(accounts.findById(fromId)).thenReturn(Optional.of(from));
        when(accounts.findById(toId)).thenReturn(Optional.of(to));
        when(transfers.save(any())).thenAnswer(i -> i.getArgument(0));

        service.post("k1", fromId, toId, new BigDecimal("25.0000"));

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(entries, times(2)).save(captor.capture());

        var saved = captor.getAllValues();
        assertThat(saved).extracting(LedgerEntry::getDirection)
                .containsExactly(EntryDirection.DEBIT, EntryDirection.CREDIT);
        assertThat(saved.get(0).getAmount()).isEqualByComparingTo(saved.get(1).getAmount());
        assertThat(from.getBalance()).isEqualByComparingTo("75.0000");
        assertThat(to.getBalance()).isEqualByComparingTo("25.0000");
    }

    @Test
    void writes_one_outbox_event_per_posting() {
        when(transfers.findByIdempotencyKey("k2")).thenReturn(Optional.empty());
        when(accounts.findById(fromId)).thenReturn(Optional.of(from));
        when(accounts.findById(toId)).thenReturn(Optional.of(to));
        when(transfers.save(any())).thenAnswer(i -> i.getArgument(0));

        service.post("k2", fromId, toId, new BigDecimal("10"));

        verify(outbox, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void replayed_idempotency_key_posts_nothing() {
        var original = new Transfer("k3", fromId, toId, new BigDecimal("10"));
        when(transfers.findByIdempotencyKey("k3")).thenReturn(Optional.of(original));

        var result = service.post("k3", fromId, toId, new BigDecimal("10"));

        assertThat(result.getId()).isEqualTo(original.getId());
        verifyNoInteractions(entries);
        verifyNoInteractions(outbox);
    }

    @Test
    void rejects_overdraft() {
        when(transfers.findByIdempotencyKey("k4")).thenReturn(Optional.empty());
        when(accounts.findById(fromId)).thenReturn(Optional.of(from));
        when(accounts.findById(toId)).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> service.post("k4", fromId, toId, new BigDecimal("500")))
                .isInstanceOf(InsufficientFundsException.class);
        verifyNoInteractions(entries);
    }

    @Test
    void rejects_non_positive_amount_and_self_transfer() {
        assertThatThrownBy(() -> service.post("k5", fromId, toId, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.post("k6", fromId, fromId, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
