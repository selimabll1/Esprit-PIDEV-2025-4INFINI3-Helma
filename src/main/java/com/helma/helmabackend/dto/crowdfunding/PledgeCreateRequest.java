package com.helma.helmabackend.dto.crowdfunding;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class PledgeCreateRequest {

    @DecimalMin(value = "1.000", inclusive = true, message = "Amount must be at least 1 TND")
    @Digits(integer = 16, fraction = 3, message = "Amount must have up to 16 digits and 3 decimals")
    public BigDecimal amount;

    @Size(max = 500, message = "Message must be at most 500 characters")
    public String message;
}
