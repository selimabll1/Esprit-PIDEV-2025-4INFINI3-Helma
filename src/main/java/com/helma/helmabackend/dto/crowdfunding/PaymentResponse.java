package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.PaymentProvider;
import com.helma.helmabackend.entity.crowdfunding.enums.PaymentStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.PledgeStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentResponse {
    public Long id;
    public Long pledgeId;
    public Long applicationRaiseId;
    public Long backerUserId;

    public BigDecimal amount;
    public String currency;

    public PaymentProvider provider;
    public String providerReference;
    public String checkoutSessionId;
    public String checkoutUrl;

    public PaymentStatus status;
    public String failureReason;

    public PledgeStatus pledgeStatus;
    public String campaignBusinessName;

    public Instant paidAt;
    public Instant refundedAt;
    public Instant createdAt;
    public Instant updatedAt;
}
