package com.esprit.helma_backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "cash_flow_summaries",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cashflow_user_month",
                columnNames = {"user_id", "month_start"}
        )
)
public class CashFlowSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    @Column(name = "total_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "total_expense", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalExpense;

    @Column(name = "net_flow", nullable = false, precision = 12, scale = 2)
    private BigDecimal netFlow;  // totalIncome - totalExpense

    @Column(name = "cumulative_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal cumulativeBalance;  // netFlow + previous month balance

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}