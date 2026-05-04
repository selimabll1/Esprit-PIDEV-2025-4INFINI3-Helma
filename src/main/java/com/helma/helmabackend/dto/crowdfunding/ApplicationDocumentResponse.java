package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.DocumentReviewStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.DocumentType;

import java.time.Instant;

public class ApplicationDocumentResponse {
    public Long id;
    public Long applicationRaiseId;
    public DocumentType docType;
    public String fileName;
    public String mimeType;
    public Long sizeBytes;
    public Boolean required;
    public DocumentReviewStatus reviewStatus;
    public Long reviewedByUserId;
    public Instant reviewedAt;
    public String reviewNote;
    public Instant createdAt;
    public Instant updatedAt;
}
