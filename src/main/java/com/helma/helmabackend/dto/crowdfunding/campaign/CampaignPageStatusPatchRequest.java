package com.helma.helmabackend.dto.crowdfunding.campaign;

import com.helma.helmabackend.entity.crowdfunding.enums.CampaignPageStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CampaignPageStatusPatchRequest {

    @NotNull(message = "status is required")
    public CampaignPageStatus status;

    @Size(max = 3000, message = "reviewNote too long")
    public String reviewNote;
}
