package com.helma.helmabackend.dto.crowdfunding;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class EquityDetailUpsertRequest {

    @NotBlank(message = "companyLegalName is required")
    @Size(min = 2, max = 255, message = "companyLegalName must be between 2 and 255 characters")
    public String companyLegalName;

    @NotBlank(message = "companyRegistrationNumber is required")
    @Size(min = 2, max = 120, message = "companyRegistrationNumber must be between 2 and 120 characters")
    public String companyRegistrationNumber;

    @NotBlank(message = "cnreProfileUrl is required")
    @Size(max = 800, message = "cnreProfileUrl must be at most 800 characters")
    @Pattern(regexp = "^(https?://).+", message = "cnreProfileUrl must be a valid URL starting with http:// or https://")
    public String cnreProfileUrl;

    @DecimalMin(value = "0.01", message = "equityOfferedPercent must be at least 0.01")
    @DecimalMax(value = "100.00", message = "equityOfferedPercent must be at most 100.00")
    @Digits(integer = 3, fraction = 2, message = "equityOfferedPercent must have up to 3 integer digits and 2 decimals")
    public BigDecimal equityOfferedPercent;

    @DecimalMin(value = "0.000", message = "preMoneyValuation cannot be negative")
    @Digits(integer = 11, fraction = 3, message = "preMoneyValuation must have up to 11 integer digits and 3 decimals")
    public BigDecimal preMoneyValuation;

    @DecimalMin(value = "0.000", message = "minInvestment cannot be negative")
    @Digits(integer = 9, fraction = 3, message = "minInvestment must have up to 9 integer digits and 3 decimals")
    public BigDecimal minInvestment;
}
