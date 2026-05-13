package com.helma.helmabackend.dto.kyc;

import com.helma.helmabackend.entity.kyc.KycDocumentType;
import com.helma.helmabackend.entity.kyc.KycStatus;
import com.helma.helmabackend.entity.user.Role;

import java.time.Instant;

public record KycReviewResponse(
        Long documentId,
        Long userId,
        String email,
        Role role,
        String firstName,
        String lastName,
        KycStatus status,
        KycDocumentType documentType,
        String fileName,
        String contentType,
        long fileSize,
        Instant submittedAt,
        Instant reviewedAt,
        Long reviewedById,
        String rejectionReason
) {}
