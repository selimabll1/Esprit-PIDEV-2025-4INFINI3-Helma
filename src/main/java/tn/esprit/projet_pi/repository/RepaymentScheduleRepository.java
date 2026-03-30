package tn.esprit.projet_pi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.projet_pi.entity.RepaymentSchedule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    @Query("SELECT r FROM RepaymentSchedule r WHERE r.loanId = :loanId ORDER BY r.installmentNumber ASC")
    List<RepaymentSchedule> findByLoanIdOrdered(@Param("loanId") Long loanId);

    @Query("SELECT r FROM RepaymentSchedule r WHERE r.status = 'PENDING' AND r.dueDate < :today")
    List<RepaymentSchedule> findOverdueSchedules(@Param("today") LocalDate today);

    @Query("SELECT r FROM RepaymentSchedule r WHERE r.loanId = :loanId AND r.status = 'PENDING' ORDER BY r.dueDate ASC")
    Optional<RepaymentSchedule> findNextPendingByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT COUNT(r) FROM RepaymentSchedule r WHERE r.loanId = :loanId AND r.status = 'OVERDUE'")
    int countOverdueByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT SUM(r.paidAmount) FROM RepaymentSchedule r WHERE r.loanId = :loanId AND r.status = 'PAID'")
    BigDecimal getTotalPaidByLoanId(@Param("loanId") Long loanId);
}
