package com.helma.helmabackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "voucher")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double value;

    private LocalDate creationDate;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private VoucherStatus status;

    // 1 voucher lié à 1 goal
    @OneToOne
    @JoinColumn(name = "goal_id")
    @JsonIgnore
    private SavingsGoal savingsGoal;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    private String code;
    private boolean claimed;
    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public LocalDate getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDate creationDate) { this.creationDate = creationDate; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public VoucherStatus getStatus() { return status; }
    public void setStatus(VoucherStatus status) { this.status = status; }

    public SavingsGoal getSavingsGoal() { return savingsGoal; }
    public void setSavingsGoal(SavingsGoal savingsGoal) { this.savingsGoal = savingsGoal; }
}
