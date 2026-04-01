package tn.esprit.helma.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tn.esprit.helma.entities.BankAccount;
import tn.esprit.helma.entities.Transaction;
import tn.esprit.helma.enums.AccountStatus;
import tn.esprit.helma.enums.AccountType;
import tn.esprit.helma.enums.TransactionStatus;
import tn.esprit.helma.enums.TransactionType;
import tn.esprit.helma.services.ITransactionService;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ITransactionService transactionService;

    @Test
    void createTransaction_ignoresExtraFields() throws Exception {
        BankAccount account = BankAccount.builder()
                .id(10L)
                .userId(1L)
                .rib("TN123456789000")
                .balance(BigDecimal.valueOf(1000))
                .currency("TND")
                .accountType(AccountType.COURANT)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Transaction saved = Transaction.builder()
                .id(5L)
                .bankAccount(account)
                .beneficiaryName("Alice")
                .beneficiaryRib("TN999999999000")
                .amount(BigDecimal.valueOf(120))
                .type(TransactionType.EXTERNAL)
                .category("Shopping")
                .description("Test")
                .status(TransactionStatus.CONFIRMED)
                .riskScore(0)
                .createdAt(LocalDateTime.now())
                .confirmedAt(LocalDateTime.now())
                .build();

        when(transactionService.createTransaction(eq(10L), any(Transaction.class))).thenReturn(saved);

        String payload = "{" +
                "\"id\":1," +
                "\"bankAccount\":{\"id\":10}," +
                "\"beneficiaryName\":\"Alice\"," +
                "\"beneficiaryRib\":\"TN999999999000\"," +
                "\"amount\":120," +
                "\"type\":\"EXTERNAL\"," +
                "\"category\":\"Shopping\"," +
                "\"description\":\"Test\"," +
                "\"status\":\"CONFIRMED\"," +
                "\"riskScore\":0," +
                "\"createdAt\":\"2026-02-19T20:31:07.410Z\"," +
                "\"confirmedAt\":\"2026-02-19T20:31:07.410Z\"" +
                "}";

        mockMvc.perform(post("/transactions/add/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.bankAccountId").value(10))
                .andExpect(jsonPath("$.amount").value(120))
                .andExpect(jsonPath("$.type").value("EXTERNAL"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
