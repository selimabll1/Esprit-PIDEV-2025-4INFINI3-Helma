package com.esprit.helma_backend.services.risk;

import java.util.List;

public record RiskDecision(
        boolean triggered,
        int riskLevel,           // 0..100
        List<String> reasons
) {}