package com.helma.helmabackend.dto.crowdfunding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class EquityApplicationCreateRequest {

    @NotNull(message = "application is required")
    @Valid
    public ApplicationRaiseCreateRequest application;

    @NotNull(message = "equityDetail is required")
    @Valid
    public EquityDetailUpsertRequest equityDetail;
}
