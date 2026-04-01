package com.helma.helmabackend.dto.crowdfunding.ai;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AiForecastPointResponse {
    public LocalDate weekStart;
    public BigDecimal predictedInterest;
    public BigDecimal lowerBound;
    public BigDecimal upperBound;
}