package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.ProjectStage;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

public class ApplicationRaiseDetailsStepRequest {

    @NotBlank(message = "businessName is required")
    @Size(min = 2, max = 160, message = "businessName must be between 2 and 160 characters")
    public String businessName;

    @Size(max = 255, message = "website must be at most 255 characters")
    @Pattern(
            regexp = "^(https?://)?([\\w-]+\\.)+[\\w-]{2,}(/.*)?$",
            message = "website must be a valid URL"
    )
    public String website;

    @NotNull(message = "sector is required")
    public Sector sector;

    @NotNull(message = "subSector is required")
    public SubSector subSector;

    @NotEmpty(message = "At least one tag is required")
    @Size(max = 3, message = "You can choose at most 3 tags")
    public Set<@NotNull AppTag> tags = new LinkedHashSet<>();

    @NotNull(message = "stage is required")
    public ProjectStage stage;

    @NotBlank(message = "summary is required")
    @Size(min = 10, max = 255, message = "summary must be between 10 and 255 characters")
    public String summary;

    @NotBlank(message = "problemStatement is required")
    @Size(min = 20, max = 3000, message = "problemStatement must be between 20 and 3000 characters")
    public String problemStatement;

    @NotBlank(message = "solution is required")
    @Size(min = 20, max = 3000, message = "solution must be between 20 and 3000 characters")
    public String solution;

    @NotBlank(message = "targetCustomers is required")
    @Size(min = 10, max = 2000, message = "targetCustomers must be between 10 and 2000 characters")
    public String targetCustomers;

    @NotBlank(message = "useOfFunds is required")
    @Size(min = 20, max = 3000, message = "useOfFunds must be between 20 and 3000 characters")
    public String useOfFunds;

    @NotNull(message = "fundingGoal is required")
    @DecimalMin(value = "500.000", message = "fundingGoal must be at least 500")
    @Digits(integer = 12, fraction = 3, message = "fundingGoal must be a valid amount")
    public BigDecimal fundingGoal;

    @Min(value = 0, message = "customerCount cannot be negative")
    public Integer customerCount;

    @NotNull(message = "teamSize is required")
    @Min(value = 1, message = "teamSize must be at least 1")
    @Max(value = 500, message = "teamSize must be at most 500")
    public Integer teamSize;

    @NotBlank(message = "governorate is required")
    @Size(max = 80, message = "governorate must be at most 80 characters")
    public String governorate;

    @NotBlank(message = "city is required")
    @Size(max = 80, message = "city must be at most 80 characters")
    public String city;

    @Valid
    public EquityDetailUpsertRequest equityDetail;

    public boolean acceptedTerms;
}
