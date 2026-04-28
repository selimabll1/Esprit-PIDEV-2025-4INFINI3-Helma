package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import jakarta.validation.constraints.NotNull;

public class ApplicationRaiseTypeStepRequest {

    @NotNull(message = "type is required")
    public CrowdfundingType type;
}