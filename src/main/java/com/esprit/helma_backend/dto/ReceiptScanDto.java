package com.esprit.helma_backend.dto;

import java.math.BigDecimal;

public record ReceiptScanDto(
        String category,
        String type,
        BigDecimal amount,
        String date,
        String description
) {}
