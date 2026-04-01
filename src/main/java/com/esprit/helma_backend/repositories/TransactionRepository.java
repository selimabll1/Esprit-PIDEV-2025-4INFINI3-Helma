package com.esprit.helma_backend.repositories;

import com.esprit.helma_backend.entities.Transaction;
import com.esprit.helma_backend.entities.Transaction.TransactionType;
import com.esprit.helma_backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUser(User user);

    @Query("SELECT MIN(t.txnDate) FROM Transaction t WHERE t.user.id = :userId")
    Instant findMinTxnDateByUserId(@Param("userId") Long userId);

    @Query("SELECT MAX(t.txnDate) FROM Transaction t WHERE t.user.id = :userId")
    Instant findMaxTxnDateByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.txnDate >= :from AND t.txnDate < :to")
    BigDecimal sumAmountForUserBetween(@Param("userId") Long userId,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);

    @Query(value = """
        SELECT COUNT(DISTINCT
            YEAR(CONVERT_TZ(FROM_UNIXTIME(UNIX_TIMESTAMP(t.txn_date)), '+00:00', @@session.time_zone)) * 100 +
            MONTH(CONVERT_TZ(FROM_UNIXTIME(UNIX_TIMESTAMP(t.txn_date)), '+00:00', @@session.time_zone))
        )
        FROM transactions t
        WHERE t.user_id = :userId
          AND t.txn_date >= :from
          AND t.txn_date < :to
          AND t.type = 'EXPENSE'
        """, nativeQuery = true)
    long countDistinctYearMonthForUserBetween(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("SELECT MAX(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.txnDate >= :from AND t.txnDate < :to")
    BigDecimal maxAmountForUserBetween(@Param("userId") Long userId,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = 'EXPENSE' AND t.txnDate >= :from AND t.txnDate < :to")
    BigDecimal sumExpenseForUserBetween(@Param("userId") Long userId,
                                        @Param("from") Instant from,
                                        @Param("to") Instant to);

    @Query("SELECT MAX(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = 'EXPENSE' AND t.txnDate >= :from AND t.txnDate < :to")
    BigDecimal maxExpenseForUserBetween(@Param("userId") Long userId,
                                        @Param("from") Instant from,
                                        @Param("to") Instant to);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = 'INCOME' AND t.txnDate >= :from AND t.txnDate < :to")
    BigDecimal sumIncomeForUserBetween(@Param("userId") Long userId,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);

    List<Transaction> findByUserAndType(User user, TransactionType type);
}