package com.helma.helmabackend.dto.crowdfunding;

import java.math.BigDecimal;

public class PortfolioAllocationItemResponse {
    public String key;
    public BigDecimal amount = BigDecimal.ZERO;
    public BigDecimal weightPct = BigDecimal.ZERO;
}