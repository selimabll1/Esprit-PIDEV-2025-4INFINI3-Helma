package com.esprit.helma_backend.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "budgets",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "month_start", "category"})
        }
)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    @Column(name = "category")
    private String category;

    public Budget() {}

    public Budget(Long userId, BigDecimal limitAmount, LocalDate monthStart, String category) {
        this.userId = userId;
        this.limitAmount = limitAmount;
        this.monthStart = monthStart;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public LocalDate getMonthStart() {
        return monthStart;
    }

    public void setMonthStart(LocalDate monthStart) {
        this.monthStart = monthStart;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}