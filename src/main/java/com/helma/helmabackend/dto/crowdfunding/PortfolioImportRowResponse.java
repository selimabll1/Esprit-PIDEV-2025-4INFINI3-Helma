package com.helma.helmabackend.dto.crowdfunding;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PortfolioImportRowResponse {
    public Integer rowNumber;
    public Boolean valid = false;
    public List<String> errors = new ArrayList<>();

    public String campaignBusinessName;
    public String sector;
    public String subSector;
    public String governorate;
    public String city;
    public List<String> tags = new ArrayList<>();

    public BigDecimal investedAmount;
    public String currency;
    public BigDecimal campaignFundingGoal;
    public BigDecimal campaignRaisedAmount;
    public BigDecimal equityOfferedPercent;
    public BigDecimal ownershipPercent;
    public Instant investedAt;
    public String sourceReference;
    public String notes;
}
