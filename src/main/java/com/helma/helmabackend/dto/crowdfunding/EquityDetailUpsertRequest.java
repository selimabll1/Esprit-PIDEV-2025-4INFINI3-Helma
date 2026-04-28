package com.helma.helmabackend.dto.crowdfunding;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class EquityDetailUpsertRequest {

    @NotBlank(message = "companyLegalName is required")
    @Size(min = 2, max = 160, message = "companyLegalName must be between 2 and 160 characters")
    public String companyLegalName;

    @NotBlank(message = "companyRegistrationNumber is required")
    @Size(min = 2, max = 120, message = "companyRegistrationNumber must be between 2 and 120 characters")
    public String companyRegistrationNumber;

    @NotBlank(message = "cnreProfileUrl is required")
    @Size(max = 800, message = "cnreProfileUrl must be at most 800 characters")
    @Pattern(regexp = "^(https?://).+", message = "cnreProfileUrl must be a valid URL starting with http:// or https://")
    public String cnreProfileUrl;

    // Calculated by backend from fundingGoal and preMoneyValuation.
    // Kept optional only for backward compatibility with old frontend payloads.
    public BigDecimal equityOfferedPercent;

    @NotNull(message = "preMoneyValuation is required")
    @DecimalMin(value = "1.000", message = "preMoneyValuation must be greater than 0")
    @Digits(integer = 12, fraction = 3, message = "preMoneyValuation must have up to 12 integer digits and 3 decimals")
    public BigDecimal preMoneyValuation;

    @NotNull(message = "minInvestment is required")
    @DecimalMin(value = "1.000", message = "minInvestment must be greater than 0")
    @Digits(integer = 9, fraction = 3, message = "minInvestment must have up to 9 integer digits and 3 decimals")
    public BigDecimal minInvestment;
}
