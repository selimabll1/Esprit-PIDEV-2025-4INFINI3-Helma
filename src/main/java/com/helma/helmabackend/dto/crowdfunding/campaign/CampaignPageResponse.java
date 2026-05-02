package com.helma.helmabackend.dto.crowdfunding.campaign;

import com.helma.helmabackend.dto.crowdfunding.ApplicationDocumentResponse;
import com.helma.helmabackend.dto.crowdfunding.EquityDetailResponse;
import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.CampaignPageStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CampaignPageResponse {
    public Long id;
    public Long applicationRaiseId;
    public Long ownerUserId;

    public String slug;
    public String publicUrl;
    public String title;
    public String subtitle;
    public String coverMediaUrl;
    public String contentJson;
    public String styleJson;
    public CampaignPageStatus status;
    public String reviewNote;
    public Instant publishedAt;
    public Instant createdAt;
    public Instant updatedAt;

    public CrowdfundingType applicationType;
    public String businessName;
    public String website;
    public Sector sector;
    public SubSector subSector;
    public Set<AppTag> tags = new LinkedHashSet<>();
    public String summary;
    public String problemStatement;
    public String solution;
    public String targetCustomers;
    public String useOfFunds;
    public BigDecimal fundingGoal;
    public BigDecimal investorsPledgedAmount;
    public String currency;
    public String governorate;
    public String city;

    /** Present for EQUITY application campaigns. Used in the right RNE/company info panel. */
    public EquityDetailResponse equityDetail;

    /** Only returned to owner/admin/compliance builder screens. Not returned from public endpoints. */
    public List<ApplicationDocumentResponse> availableDocuments;

    public List<CampaignPageDocumentResponse> publicDocuments;
}
