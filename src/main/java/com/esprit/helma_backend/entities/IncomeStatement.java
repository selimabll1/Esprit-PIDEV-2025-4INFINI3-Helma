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
        name = "income_statements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "month_start"})
)
public class IncomeStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "total_expenses", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalExpenses;

    @Column(name = "net_result", nullable = false, precision = 12, scale = 2)
    private BigDecimal netResult;

    @Column(name = "margin_rate", precision = 8, scale = 2)
    private BigDecimal marginRate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;
        if (netResult == null) netResult = BigDecimal.ZERO;
    }
}