package com.helma.helmabackend.dto.crowdfunding.campaign;

import com.helma.helmabackend.entity.crowdfunding.enums.DocumentType;

import java.time.Instant;

public class CampaignPageDocumentResponse {
    public Long id;
    public Long campaignPageId;
    public Long applicationDocumentId;
    public DocumentType docType;
    public String fileName;
    public String label;
    public Long sizeBytes;
    public Instant createdAt;
}
