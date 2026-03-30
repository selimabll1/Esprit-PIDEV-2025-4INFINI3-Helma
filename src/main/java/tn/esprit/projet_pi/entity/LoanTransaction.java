package tn.esprit.projet_pi.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.projet_pi.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long loanId;

    @Enumerated(EnumType.STRING)
    private TransactionType type; // DISBURSEMENT, REPAYMENT

    private BigDecimal amount;

    private LocalDate transactionDate;

    private String reference;
}
