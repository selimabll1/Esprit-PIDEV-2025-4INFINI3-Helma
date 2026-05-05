package tn.esprit.helma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.helma.entities.Transaction;
import tn.esprit.helma.enums.TransactionPeriodicity;
import tn.esprit.helma.enums.TransactionStatus;
import tn.esprit.helma.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository des transactions bancaires.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByBankAccountId(Long bankAccountId);

    List<Transaction> findByBankAccountIdOrderByCreatedAtDesc(Long bankAccountId);

    List<Transaction> findTop20ByBankAccountIdOrderByCreatedAtDesc(Long bankAccountId);

    Optional<Transaction> findTopByBankAccountIdOrderByCreatedAtDesc(Long bankAccountId);

    List<Transaction> findByBankAccountIdAndType(Long bankAccountId, TransactionType type);

    List<Transaction> findByBankAccountIdAndStatus(Long bankAccountId, TransactionStatus status);

        List<Transaction> findByPeriodicityAndStatusAndScheduledDateLessThanEqual(TransactionPeriodicity periodicity,
                                                                                                                                                         TransactionStatus status,
                                                                                                                                                         LocalDateTime scheduledBefore);

        List<Transaction> findByPeriodicityAndNextExecutionDateLessThanEqual(TransactionPeriodicity periodicity,
                                                                                                                                                 LocalDateTime nextExecutionBefore);

    @Query("SELECT t FROM Transaction t WHERE t.bankAccount.id = :bankAccountId " +
            "AND t.createdAt BETWEEN :start AND :end ORDER BY t.createdAt DESC")
    List<Transaction> findBetweenDates(@Param("bankAccountId") Long bankAccountId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.bankAccount WHERE t.id = :id")
    Optional<Transaction> findByIdWithAccount(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.bankAccount.id = :bankAccountId " +
            "AND t.status = :status AND t.createdAt >= :start")
    BigDecimal sumSince(@Param("bankAccountId") Long bankAccountId,
                        @Param("status") TransactionStatus status,
                        @Param("start") LocalDateTime start);

    long countByBankAccountIdAndCreatedAtAfter(Long bankAccountId, LocalDateTime createdAfter);
}
