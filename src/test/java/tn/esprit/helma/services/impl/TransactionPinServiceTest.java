package tn.esprit.helma.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.helma.entities.BankAccount;
import tn.esprit.helma.entities.Transaction;
import tn.esprit.helma.enums.TransactionStatus;
import tn.esprit.helma.repositories.BankAccountRepository;
import tn.esprit.helma.repositories.TransactionRepository;
import tn.esprit.helma.services.FraudAlertMailService;
import tn.esprit.helma.services.ISavedBeneficiaryService;
import tn.esprit.helma.services.auth.CurrentUserProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionServiceImpl — logique PIN")
class TransactionPinServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock BankAccountRepository bankAccountRepository;
    @Mock ISavedBeneficiaryService savedBeneficiaryService;
    @Mock FraudAlertMailService fraudAlertMailService;
    @Mock CurrentUserProvider currentUserProvider;

    @InjectMocks TransactionServiceImpl service;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private BankAccount account;
    private Transaction tx;

    @BeforeEach
    void setUp() {
        account = BankAccount.builder()
                .id(1L)
                .userId(10L)
                .rib("12345678901234567890")
                .balance(BigDecimal.valueOf(5000))
                .pinAttemptCount(0)
                .pinCounterUnderThreshold(0)
                .build();

        tx = Transaction.builder()
                .id(100L)
                .bankAccount(account)
                .amount(BigDecimal.valueOf(200))
                .status(TransactionStatus.SUSPICIOUS)
                .riskScore(45)
                .build();
    }

    // ─────────────────────── requiresPinVerification ───────────────────────

    @Test
    @DisplayName("Risque moyen [30,70) → PIN toujours requis")
    void requiresPin_mediumRisk_returnsTrue() {
        for (int score : new int[]{30, 45, 69}) {
            tx.setRiskScore(score);
            assertThat(service.requiresPinVerification(tx))
                    .as("riskScore=%d", score).isTrue();
        }
    }

    @Test
    @DisplayName("Risque élevé ≥70 → PIN non requis (tx déjà bloquée)")
    void requiresPin_highRisk_returnsFalse() {
        tx.setRiskScore(70);
        assertThat(service.requiresPinVerification(tx)).isFalse();

        tx.setRiskScore(100);
        assertThat(service.requiresPinVerification(tx)).isFalse();
    }

    @Test
    @DisplayName("Risque faible <30 avec compteur < 3 → pas de PIN")
    void requiresPin_lowRisk_counterBelow3_returnsFalse() {
        tx.setRiskScore(10);
        account.setPinCounterUnderThreshold(2);
        assertThat(service.requiresPinVerification(tx)).isFalse();
    }

    @Test
    @DisplayName("Risque faible <30 avec compteur = 3 → PIN requis (4ème tx)")
    void requiresPin_lowRisk_counterEquals3_returnsTrue() {
        tx.setRiskScore(10);
        account.setPinCounterUnderThreshold(3);
        assertThat(service.requiresPinVerification(tx)).isTrue();
    }

    @Test
    @DisplayName("incrementPinCounterUnderThreshold incrémente le compteur")
    void incrementCounter_incrementsByOne() {
        account.setPinCounterUnderThreshold(1);
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.incrementPinCounterUnderThreshold(account);

        assertThat(account.getPinCounterUnderThreshold()).isEqualTo(2);
    }

    // ─────────────────────── confirmSuspiciousTransactionWithPin ───────────

    @Test
    @DisplayName("PIN correct → transaction confirmée, compteurs réinitialisés")
    void confirmWithPin_correctPin_success() {
        String rawPin = "1234";
        account.setPinHash(encoder.encode(rawPin));
        account.setPinAttemptCount(2); // doit être réinitialisé
        account.setPinCounterUnderThreshold(3); // doit être réinitialisé

        when(currentUserProvider.getCurrentUserId()).thenReturn(10L);
        when(transactionRepository.findByIdWithAccount(100L)).thenReturn(Optional.of(tx));
        when(bankAccountRepository.findByRib(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(savedBeneficiaryService.beneficiaryExists(any(), any())).thenReturn(true);
        when(savedBeneficiaryService.getBeneficiaryByRib(any(), any())).thenReturn(Optional.empty());

        Transaction result = service.confirmSuspiciousTransactionWithPin(100L, rawPin);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.CONFIRMED);
        assertThat(account.getPinAttemptCount()).isZero();
        assertThat(account.getPinCounterUnderThreshold()).isZero();
        assertThat(account.getLastPinAttempt()).isNull();
    }

    @Test
    @DisplayName("PIN incorrect → 401 PIN_INCORRECT + compteur incrémenté")
    void confirmWithPin_wrongPin_returns401AndIncrements() {
        account.setPinHash(encoder.encode("1234"));
        when(currentUserProvider.getCurrentUserId()).thenReturn(10L);
        when(transactionRepository.findByIdWithAccount(100L)).thenReturn(Optional.of(tx));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.confirmSuspiciousTransactionWithPin(100L, "9999"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(rse.getReason()).isEqualTo("PIN_INCORRECT");
                });

        assertThat(account.getPinAttemptCount()).isEqualTo(1);
        assertThat(account.getLastPinAttempt()).isNotNull();
    }

    @Test
    @DisplayName("5 échecs récents → 423 PIN_LOCKED sans vérifier le PIN")
    void confirmWithPin_after5RecentFailures_returns423() {
        account.setPinHash(encoder.encode("1234"));
        account.setPinAttemptCount(5);
        account.setLastPinAttempt(LocalDateTime.now().minusMinutes(5)); // < 15 min
        when(currentUserProvider.getCurrentUserId()).thenReturn(10L);
        when(transactionRepository.findByIdWithAccount(100L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> service.confirmSuspiciousTransactionWithPin(100L, "9999"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
                    assertThat(rse.getReason()).isEqualTo("PIN_LOCKED");
                });

        // Le compteur ne doit pas être modifié pendant le lockout
        assertThat(account.getPinAttemptCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Lockout expiré (>15 min) → nouvelle tentative autorisée")
    void confirmWithPin_lockoutExpired_allowsAttempt() {
        account.setPinHash(encoder.encode("1234"));
        account.setPinAttemptCount(5);
        account.setLastPinAttempt(LocalDateTime.now().minusMinutes(20)); // > 15 min = expiré
        when(currentUserProvider.getCurrentUserId()).thenReturn(10L);
        when(transactionRepository.findByIdWithAccount(100L)).thenReturn(Optional.of(tx));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Mauvais PIN → doit lever PIN_INCORRECT (pas PIN_LOCKED)
        assertThatThrownBy(() -> service.confirmSuspiciousTransactionWithPin(100L, "9999"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("Compte sans PIN configuré → 400 BAD_REQUEST")
    void confirmWithPin_noPinSet_throwsIllegalArgument() {
        account.setPinHash(null);
        when(currentUserProvider.getCurrentUserId()).thenReturn(10L);
        when(transactionRepository.findByIdWithAccount(100L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> service.confirmSuspiciousTransactionWithPin(100L, "1234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aucun PIN configuré");
    }
}
