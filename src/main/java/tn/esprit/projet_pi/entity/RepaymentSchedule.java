package tn.esprit.projet_pi.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.projet_pi.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repayment_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long loanId;

    private Integer installmentNumber;

    private LocalDate dueDate;

    private BigDecimal expectedAmount;

    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // PENDING, PAID, OVERDUE
}
