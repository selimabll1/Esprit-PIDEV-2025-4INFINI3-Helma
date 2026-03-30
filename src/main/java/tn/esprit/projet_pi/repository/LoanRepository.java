package tn.esprit.projet_pi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.projet_pi.entity.Loan;

import java.math.BigDecimal;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Query("SELECT l FROM Loan l WHERE l.userId = :userId")
    List<Loan> findByUserId(@Param("userId") Long userId);

    @Query("SELECT l FROM Loan l WHERE l.userId = :userId AND l.status = 'ACTIVE'")
    List<Loan> findActiveLoansByUserId(@Param("userId") Long userId);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE'")
    List<Loan> findAllActiveLoans();

    @Query("SELECT l FROM Loan l WHERE l.status = 'DEFAULTED'")
    List<Loan> findAllDefaultedLoans();

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.riskScore > :threshold")
    List<Loan> findHighRiskActiveLoans(@Param("threshold") int threshold);

    @Query("SELECT l.loanType, COUNT(l), AVG(l.principalAmount) FROM Loan l GROUP BY l.loanType")
    List<Object[]> getLoanStatsByType();

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.userId = :userId AND l.status = 'DEFAULTED'")
    int countDefaultedLoansByUser(@Param("userId") Long userId);

    @Query("SELECT SUM(l.principalAmount) FROM Loan l WHERE l.status = 'ACTIVE'")
    BigDecimal getTotalActiveLoanAmount();
}
