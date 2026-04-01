package com.helma.helmabackend.dto.crowdfunding;

import java.math.BigDecimal;
import java.time.Instant;

public class EquityDetailResponse {
    public Long applicationRaiseId;

    public String companyLegalName;
    public String companyRegistrationNumber;
    public String cnreProfileUrl;

    public BigDecimal equityOfferedPercent;
    public BigDecimal preMoneyValuation;
    public BigDecimal minInvestment;

    public Instant createdAt;
    public Instant updatedAt;
}