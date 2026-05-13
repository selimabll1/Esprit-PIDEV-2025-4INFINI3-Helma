package com.helma.helmabackend.dto.kyc;

import com.helma.helmabackend.entity.kyc.KycDocumentType;
import com.helma.helmabackend.entity.kyc.KycStatus;

import java.time.Instant;

public record KycStatusResponse(
        Long documentId,
        KycStatus status,
        KycDocumentType documentType,
        String fileName,
        String contentType,
        long fileSize,
        Instant submittedAt,
        Instant reviewedAt,
        String rejectionReason
) {}
