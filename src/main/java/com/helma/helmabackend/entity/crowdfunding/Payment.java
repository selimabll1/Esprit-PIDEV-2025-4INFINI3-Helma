package com.helma.helmabackend.entity.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.PaymentProvider;
import com.helma.helmabackend.entity.crowdfunding.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "payment",
        indexes = {
                @Index(name = "idx_payment_pledge", columnList = "pledge_id"),
                @Index(name = "idx_payment_application", columnList = "application_raise_id"),
                @Index(name = "idx_payment_backer", columnList = "backer_user_id"),
                @Index(name = "idx_payment_status", columnList = "status"),
                @Index(name = "idx_payment_provider_ref", columnList = "provider_reference")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "pledgeId is required")
    @Column(name = "pledge_id", nullable = false, updatable = false)
    private Long pledgeId;

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

    @NotNull(message = "provider is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private PaymentProvider provider = PaymentProvider.MOCK;

    @Size(max = 120, message = "Provider reference must be at most 120 characters")
    @Column(name = "provider_reference", length = 120, unique = true)
    private String providerReference;

    @Size(max = 120, message = "Checkout session id must be at most 120 characters")
    @Column(name = "checkout_session_id", length = 120, unique = true)
    private String checkoutSessionId;

    @NotNull(message = "status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.CREATED;

    @Size(max = 255, message = "Failure reason must be at most 255 characters")
    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

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

        if (providerReference != null) {
            providerReference = providerReference.trim();
        }

        if (checkoutSessionId != null) {
            checkoutSessionId = checkoutSessionId.trim();
        }

        if (failureReason != null) {
            String trimmed = failureReason.trim();
            failureReason = trimmed.isEmpty() ? null : trimmed;
        }
    }

    public Long getId() { return id; }

    public Long getPledgeId() { return pledgeId; }
    public void setPledgeId(Long pledgeId) { this.pledgeId = pledgeId; }

    public Long getApplicationRaiseId() { return applicationRaiseId; }
    public void setApplicationRaiseId(Long applicationRaiseId) { this.applicationRaiseId = applicationRaiseId; }

    public Long getBackerUserId() { return backerUserId; }
    public void setBackerUserId(Long backerUserId) { this.backerUserId = backerUserId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentProvider getProvider() { return provider; }
    public void setProvider(PaymentProvider provider) { this.provider = provider; }

    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }

    public String getCheckoutSessionId() { return checkoutSessionId; }
    public void setCheckoutSessionId(String checkoutSessionId) { this.checkoutSessionId = checkoutSessionId; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }

    public Instant getRefundedAt() { return refundedAt; }
    public void setRefundedAt(Instant refundedAt) { this.refundedAt = refundedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
