package tn.esprit.helma.services;

import tn.esprit.helma.entities.Transaction;
import tn.esprit.helma.enums.TransactionStatus;
import tn.esprit.helma.enums.TransactionType;
import tn.esprit.helma.dtos.RecommendationDTO;
import tn.esprit.helma.dtos.TransactionStatisticsDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Interface principale pour la gestion des transactions.
 */
public interface ITransactionService {

    Transaction createTransaction(Long accountId, Transaction transaction);

    List<Transaction> getTransactionsByType(Long accountId, TransactionType type);

    List<Transaction> getTransactionsByStatus(Long accountId, TransactionStatus status);

    List<Transaction> getTransactionsByAmountRange(Long accountId, BigDecimal minAmount, BigDecimal maxAmount);

    List<Transaction> getTransactionsBetweenDates(Long accountId, LocalDateTime start, LocalDateTime end);

    BigDecimal getTotalSpentThisMonth(Long accountId);

    TransactionStatisticsDTO getMonthlyStatistics(Long accountId);

    List<RecommendationDTO> getRecommendations(Long accountId);

    String generateMonthlySummary(Long accountId);

    List<Transaction> searchTransactions(Long accountId, Map<String, Object> filters);

    void executeScheduledTransactions();

    void executePermanentTransactions();

    Transaction confirmSuspiciousTransaction(Long transactionId);

    Transaction rejectSuspiciousTransaction(Long transactionId);

    Transaction approvePendingTransaction(Long transactionId);

    /**
     * Définir un PIN (4 chiffres) pour un compte
     */
    void setPinForAccount(Long accountId, String pin);

    /**
     * Valider et confirmer une transaction avec vérification du PIN
     */
    Transaction confirmSuspiciousTransactionWithPin(Long transactionId, String pin);

    /**
     * Vérifier si une transaction nécessite un PIN avant confirmation
     */
    boolean requiresPinVerification(Transaction transaction);

    /**
     * Charger une transaction par son identifiant
     */
    Transaction getTransactionById(Long transactionId);
}
