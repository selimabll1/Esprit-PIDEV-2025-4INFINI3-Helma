package tn.esprit.projet_pi.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.projet_pi.enums.LoanStatus;
import tn.esprit.projet_pi.enums.LoanType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence vers user-service (microservices = pas de relation JPA)
    private Long userId;

    private BigDecimal principalAmount;

    private BigDecimal interestRate;

    private Integer durationMonths;

    private BigDecimal monthlyPayment;

    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    private LoanType loanType;

    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    private LoanStatus status; // PENDING, ACTIVE, CLOSED, DEFAULTED
}
