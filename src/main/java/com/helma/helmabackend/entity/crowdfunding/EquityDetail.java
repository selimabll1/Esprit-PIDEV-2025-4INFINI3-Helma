package com.helma.helmabackend.entity.crowdfunding;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "equity_detail")
@Getter
@Setter
public class EquityDetail {

    @Id
    @Column(name = "application_raise_id")
    private Long applicationRaiseId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_raise_id")
    private ApplicationRaise applicationRaise;

    @Column(nullable = false, length = 160)
    private String companyLegalName;

    @Column(nullable = false, unique = true, length = 120)
    private String companyRegistrationNumber;

    @Column(nullable = false, length = 500)
    private String cnreProfileUrl;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal equityOfferedPercent;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal preMoneyValuation;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minInvestment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}