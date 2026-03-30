package tn.esprit.projet_pi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.projet_pi.entity.LoanPayment;

import java.math.BigDecimal;
import java.util.List;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, Long> {

    @Query("SELECT lp FROM LoanPayment lp WHERE lp.loanId = :loanId ORDER BY lp.paidAt DESC")
    List<LoanPayment> findByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT SUM(lp.amount) FROM LoanPayment lp WHERE lp.loanId = :loanId")
    BigDecimal getTotalPaidAmountByLoanId(@Param("loanId") Long loanId);
}
