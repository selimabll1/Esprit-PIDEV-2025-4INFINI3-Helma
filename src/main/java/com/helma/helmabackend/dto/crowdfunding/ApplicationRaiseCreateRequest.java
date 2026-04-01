package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

public class ApplicationRaiseCreateRequest {

    @NotNull(message = "type is required")
    public CrowdfundingType type;

    @NotBlank(message = "businessName is required")
    @Size(min = 2, max = 150, message = "businessName must be between 2 and 150 characters")
    public String businessName;

    @Size(max = 80, message = "companyNumber must be at most 80 characters")
    public String companyNumber;

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
    public Set<@NotNull(message = "tags cannot contain null values") AppTag> tags = new LinkedHashSet<>();

    @NotBlank(message = "summary is required")
    @Size(min = 10, max = 255, message = "summary must be between 10 and 255 characters")
    public String summary;

    @NotNull(message = "fundingGoal is required")
    @DecimalMin(value = "500.000", message = "fundingGoal must be at least 500")
    @Digits(integer = 11, fraction = 3, message = "fundingGoal must be a valid amount")
    public BigDecimal fundingGoal;

    @Min(value = 0, message = "customerCount cannot be negative")
    public Integer customerCount;

    @NotBlank(message = "contactFirstName is required")
    @Size(min = 2, max = 80, message = "contactFirstName must be between 2 and 80 characters")
    public String contactFirstName;

    @NotBlank(message = "contactLastName is required")
    @Size(min = 2, max = 80, message = "contactLastName must be between 2 and 80 characters")
    public String contactLastName;

    @Size(max = 80, message = "contactTitle must be at most 80 characters")
    public String contactTitle;

    @NotBlank(message = "contactEmail is required")
    @Email(message = "contactEmail must be a valid email")
    @Size(max = 150, message = "contactEmail must be at most 150 characters")
    public String contactEmail;

    @Pattern(
            regexp = "^(\\+216|216)?\\d{8}$",
            message = "contactPhone must be a valid Tunisian phone number"
    )
    public String contactPhone;

    @AssertTrue(message = "acceptedTerms must be accepted")
    public boolean acceptedTerms;
}
