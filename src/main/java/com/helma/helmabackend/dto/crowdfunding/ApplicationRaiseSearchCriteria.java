package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class ApplicationRaiseSearchCriteria {

    private String search;

    private Long id;
    private Long ownerUserId;
    private CrowdfundingType type;

    private String businessName;
    private String website;
    private String country;
    private String currency;

    private Sector sector;
    private SubSector subSector;
    private AppTag tag;

    private String summary;

    private BigDecimal fundingGoalMin;
    private BigDecimal fundingGoalMax;
    private BigDecimal investorsPledgedAmountMin;
    private BigDecimal investorsPledgedAmountMax;

    private Integer customerCountMin;
    private Integer customerCountMax;

    private String contactFirstName;
    private String contactLastName;
    private String contactTitle;
    private String contactEmail;
    private String contactPhone;

    private Boolean acceptedTerms;
    private ApplicationRaiseStatus status;

    private String companyLegalName;
    private String companyRegistrationNumber;
    private String cnreProfileUrl;

    private BigDecimal equityOfferedPercentMin;
    private BigDecimal equityOfferedPercentMax;
    private BigDecimal preMoneyValuationMin;
    private BigDecimal preMoneyValuationMax;
    private BigDecimal minInvestmentMin;
    private BigDecimal minInvestmentMax;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant updatedFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant updatedTo;

    private String sortBy;
    private String sortDir;
}