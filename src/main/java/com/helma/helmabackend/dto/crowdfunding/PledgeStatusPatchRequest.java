package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.PledgeStatus;
import jakarta.validation.constraints.NotNull;

public class PledgeStatusPatchRequest {

    @NotNull(message = "status is required")
    public PledgeStatus status;
}
