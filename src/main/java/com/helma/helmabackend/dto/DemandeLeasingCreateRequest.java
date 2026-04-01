package com.helma.helmabackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DemandeLeasingCreateRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long equipementId;

    @NotNull
    @Min(6)
    @Max(48)
    private Integer dureeMois;

    @NotNull
    @Min(18)
    @Max(24)
    private Integer ageDemandeur;
}

