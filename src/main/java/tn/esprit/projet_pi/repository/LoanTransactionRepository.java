package tn.esprit.projet_pi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.projet_pi.entity.LoanTransaction;

import java.util.List;

public interface LoanTransactionRepository extends JpaRepository<LoanTransaction, Long> {

    @Query("SELECT lt FROM LoanTransaction lt WHERE lt.loanId = :loanId ORDER BY lt.transactionDate DESC")
    List<LoanTransaction> findByLoanId(@Param("loanId") Long loanId);
}
