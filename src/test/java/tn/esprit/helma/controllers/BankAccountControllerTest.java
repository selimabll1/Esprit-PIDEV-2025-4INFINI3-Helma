package tn.esprit.helma.controllers;

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
import tn.esprit.helma.entities.User;
import tn.esprit.helma.enums.AccountStatus;
import tn.esprit.helma.enums.AccountType;
import tn.esprit.helma.services.IBankAccountService;

@WebMvcTest(BankAccountController.class)
class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IBankAccountService accountService;

    @Test
    void createAccount_ignoresExtraFields() throws Exception {
        User user = User.builder()
                .id(1L)
                .prenom("yassine")
                .nom("kamoun")
                .createdAt(LocalDateTime.now())
                .build();

        BankAccount saved = BankAccount.builder()
                .id(1L)
                .user(user)
                .rib("TN123456789000")
                .balance(BigDecimal.ZERO)
                .currency("TND")
                .accountType(AccountType.COURANT)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(accountService.createAccount(eq("TN123456789000"), eq(AccountType.COURANT), eq("TND")))
                .thenReturn(saved);

        String payload = "{" +
                "\"id\":1," +
                "\"userId\":1," +
                "\"rib\":\"TN123456789000\"," +
                "\"balance\":0," +
                "\"currency\":\"TND\"," +
                "\"accountType\":\"COURANT\"," +
                "\"status\":\"ACTIVE\"," +
                "\"createdAt\":\"2026-02-19T20:31:07.410Z\"," +
                "\"updatedAt\":\"2026-02-19T20:31:07.410Z\"," +
                "\"virtualCards\":[{" +
                "\"id\":0," +
                "\"bankAccount\":\"string\"," +
                "\"cardNumber\":\"string\"," +
                "\"expiryDate\":\"string\"," +
                "\"cvvHash\":\"string\"," +
                "\"status\":\"ACTIVE\"," +
                "\"paymentLimit\":0," +
                "\"monthlySpent\":0," +
                "\"createdAt\":\"2026-02-19T20:31:07.410Z\"," +
                "\"updatedAt\":\"2026-02-19T20:31:07.410Z\"" +
                "}]" +
                "}";

        mockMvc.perform(post("/accounts/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.userPrenom").value("yassine"))
                .andExpect(jsonPath("$.userNom").value("kamoun"))
                .andExpect(jsonPath("$.rib").value("TN123456789000"))
                .andExpect(jsonPath("$.currency").value("TND"))
                .andExpect(jsonPath("$.accountType").value("COURANT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
