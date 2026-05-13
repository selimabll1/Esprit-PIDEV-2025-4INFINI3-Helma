package com.helma.helmabackend.dto.kyc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KycRejectRequest(
        @NotBlank @Size(max = 1000) String reason
) {}
