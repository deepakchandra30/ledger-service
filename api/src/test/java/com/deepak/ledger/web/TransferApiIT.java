package com.deepak.ledger.web;

import com.deepak.ledger.domain.Account;
import com.deepak.ledger.repository.AccountRepository;
import com.deepak.ledger.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Import(IntegrationTest.Containers.class)
@AutoConfigureMockMvc
class TransferApiIT {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;

    @Test
    void posts_a_transfer_and_rejects_the_replay_as_the_same_resource() throws Exception {
        var from = accounts.save(new Account(UUID.randomUUID(), "API-" + UUID.randomUUID(), "EUR",
                new BigDecimal("500.0000")));
        var to = accounts.save(new Account(UUID.randomUUID(), "API-" + UUID.randomUUID(), "EUR",
                BigDecimal.ZERO));

        var body = """
                {"from":"%s","to":"%s","amount":"42.0000"}""".formatted(from.getId(), to.getId());

        mvc.perform(post("/api/transfers")
                        .with(jwt().jwt(j -> j.claim("scope", "ledger.write"))
                                .authorities(() -> "SCOPE_ledger.write"))
                        .header("Idempotency-Key", "api-test-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("POSTED"));

        mvc.perform(post("/api/transfers")
                        .with(jwt().authorities(() -> "SCOPE_ledger.write"))
                        .header("Idempotency-Key", "api-test-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void rejects_an_unauthenticated_request() throws Exception {
        mvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "nope")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
