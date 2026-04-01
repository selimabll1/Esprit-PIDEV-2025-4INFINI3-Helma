package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PaymentStatusPatchRequest {

    @NotNull(message = "status is required")
    public PaymentStatus status;

    @Size(max = 255, message = "failureReason must be at most 255 characters")
    public String failureReason;
}
