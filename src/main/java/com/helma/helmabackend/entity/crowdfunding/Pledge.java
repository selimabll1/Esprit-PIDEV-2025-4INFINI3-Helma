package com.helma.helmabackend.entity.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.PledgeStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "pledge",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_pledge_application_backer",
                        columnNames = {"application_raise_id", "backer_user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_pledge_application", columnList = "application_raise_id"),
                @Index(name = "idx_pledge_backer", columnList = "backer_user_id"),
                @Index(name = "idx_pledge_status", columnList = "status")
        }
)
public class Pledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "applicationRaiseId is required")
    @Column(name = "application_raise_id", nullable = false, updatable = false)
    private Long applicationRaiseId;

    @NotNull(message = "backerUserId is required")
    @Column(name = "backer_user_id", nullable = false, updatable = false)
    private Long backerUserId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "1.000", inclusive = true, message = "Amount must be at least 1 TND")
    @Digits(integer = 16, fraction = 3, message = "Amount must have up to 16 digits and 3 decimals")
    @Column(name = "amount", nullable = false, precision = 19, scale = 3)
    private BigDecimal amount;

    @NotNull(message = "currency is required")
    @Size(max = 10, message = "Currency must be at most 10 characters")
    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "TND";

    @Size(max = 500, message = "Message must be at most 500 characters")
    @Column(name = "message", length = 500)
    private String message;

    @NotNull(message = "status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PledgeStatus status = PledgeStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        normalize();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        normalize();
    }

    private void normalize() {
        if (currency == null || currency.isBlank()) {
            currency = "TND";
        } else {
            currency = currency.trim();
        }

        if (message != null) {
            String trimmed = message.trim();
            message = trimmed.isEmpty() ? null : trimmed;
        }
    }

    public Long getId() { return id; }

    public Long getApplicationRaiseId() { return applicationRaiseId; }
    public void setApplicationRaiseId(Long applicationRaiseId) { this.applicationRaiseId = applicationRaiseId; }

    public Long getBackerUserId() { return backerUserId; }
    public void setBackerUserId(Long backerUserId) { this.backerUserId = backerUserId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public PledgeStatus getStatus() { return status; }
    public void setStatus(PledgeStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
