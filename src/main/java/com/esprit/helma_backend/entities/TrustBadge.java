package com.esprit.helma_backend.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "trust_badges",
        uniqueConstraints = @UniqueConstraint(name = "uq_trust_badge_user", columnNames = "user_id")
)
public class TrustBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BadgeLevel level;

    @Column(name = "credit_capacity", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditCapacity;

    @Column(nullable = false)
    private Instant computedAt;

    @Column(length = 1000)
    private String reasons;

    public TrustBadge() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BadgeLevel getLevel() {
        return level;
    }

    public void setLevel(BadgeLevel level) {
        this.level = level;
    }

    public BigDecimal getCreditCapacity() {
        return creditCapacity;
    }

    public void setCreditCapacity(BigDecimal creditCapacity) {
        this.creditCapacity = creditCapacity;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public String getReasons() {
        return reasons;
    }

    public void setReasons(String reasons) {
        this.reasons = reasons;
    }
}