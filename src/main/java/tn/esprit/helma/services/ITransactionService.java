package tn.esprit.helma.services;

import tn.esprit.helma.entities.Transaction;
import tn.esprit.helma.enums.TransactionStatus;
import tn.esprit.helma.enums.TransactionType;

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

    String generateMonthlySummary(Long accountId);

    List<Transaction> searchTransactions(Long accountId, Map<String, Object> filters);

    void executeScheduledTransactions();

    void executePermanentTransactions();

    Transaction confirmSuspiciousTransaction(Long transactionId);

    Transaction rejectSuspiciousTransaction(Long transactionId);

    Transaction approvePendingTransaction(Long transactionId);
}
