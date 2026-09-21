package com.deepak.ledger.settlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementConsumerTest {

    @Mock SettlementRepository settlements;
    SettlementConsumer consumer;

    UUID transferId = UUID.randomUUID();
    String payload;

    @BeforeEach
    void setUp() {
        consumer = new SettlementConsumer(settlements, new ObjectMapper(), new SimpleMeterRegistry());
        payload = """
                {"transferId":"%s","from":"a","to":"b","amount":"25.0000"}""".formatted(transferId);
    }

    @Test
    void settles_a_new_transfer() throws Exception {
        when(settlements.existsById(transferId)).thenReturn(false);
        consumer.onTransferPosted(payload);
        verify(settlements).save(any(Settlement.class));
    }

    @Test
    void drops_a_redelivered_transfer() throws Exception {
        when(settlements.existsById(transferId)).thenReturn(true);
        consumer.onTransferPosted(payload);
        verify(settlements, never()).save(any());
    }
}
