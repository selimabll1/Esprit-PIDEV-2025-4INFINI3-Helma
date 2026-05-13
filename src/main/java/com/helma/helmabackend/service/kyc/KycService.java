package com.helma.helmabackend.service.kyc;

import com.helma.helmabackend.dto.kyc.KycReviewResponse;
import com.helma.helmabackend.dto.kyc.KycStatusResponse;
import com.helma.helmabackend.entity.kyc.KycDocument;
import com.helma.helmabackend.entity.kyc.KycDocumentType;
import com.helma.helmabackend.entity.kyc.KycStatus;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.entity.user.UserProfile;
import com.helma.helmabackend.exception.BadRequestException;
import com.helma.helmabackend.exception.NotFoundException;
import com.helma.helmabackend.repository.kyc.KycDocumentRepository;
import com.helma.helmabackend.service.user.CurrentUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KycService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final CurrentUserService currentUserService;
    private final KycDocumentRepository kycDocumentRepository;

    @Transactional
    public KycStatusResponse submitCin(MultipartFile file) {
        validateImage(file);

        User currentUser = currentUserService.getCurrentUser();
        KycDocument document = kycDocumentRepository.findByUserId(currentUser.getId())
                .orElseGet(KycDocument::new);

        document.setUser(currentUser);
        document.setDocumentType(KycDocumentType.CIN_TUNISIA);
        document.setStatus(KycStatus.PENDING_REVIEW);
        document.setFileName(safeFileName(file.getOriginalFilename()));
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setSubmittedAt(Instant.now());
        document.setReviewedAt(null);
        document.setReviewedBy(null);
        document.setRejectionReason(null);

        try {
            document.setImageData(file.getBytes());
        } catch (IOException ex) {
            throw new BadRequestException("Could not read uploaded image.");
        }

        return toStatusResponse(kycDocumentRepository.save(document));
    }

    @Transactional
    public KycStatusResponse getMyStatus() {
        User currentUser = currentUserService.getCurrentUser();
        return kycDocumentRepository.findByUserId(currentUser.getId())
                .map(this::toStatusResponse)
                .orElseGet(() -> new KycStatusResponse(
                        null,
                        KycStatus.NOT_STARTED,
                        null,
                        null,
                        null,
                        0,
                        null,
                        null,
                        null
                ));
    }

    @Transactional
    public List<KycReviewResponse> getPendingReviews() {
        return kycDocumentRepository.findByStatusOrderBySubmittedAtAsc(KycStatus.PENDING_REVIEW)
                .stream()
                .map(this::toReviewResponse)
                .toList();
    }

    @Transactional
    public KycReviewResponse approve(Long documentId) {
        KycDocument document = getDocument(documentId);
        User reviewer = currentUserService.getCurrentUser();

        document.setStatus(KycStatus.APPROVED);
        document.setReviewedAt(Instant.now());
        document.setReviewedBy(reviewer);
        document.setRejectionReason(null);

        return toReviewResponse(kycDocumentRepository.save(document));
    }

    @Transactional
    public KycReviewResponse reject(Long documentId, String reason) {
        KycDocument document = getDocument(documentId);
        User reviewer = currentUserService.getCurrentUser();

        document.setStatus(KycStatus.REJECTED);
        document.setReviewedAt(Instant.now());
        document.setReviewedBy(reviewer);
        document.setRejectionReason(reason.trim());

        return toReviewResponse(kycDocumentRepository.save(document));
    }

    @Transactional
    public KycDocument getReviewImage(Long documentId) {
        return getDocument(documentId);
    }

    private KycDocument getDocument(Long documentId) {
        return kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("KYC document not found"));
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CIN image is required.");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BadRequestException("CIN image must not exceed 5 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new BadRequestException("Only JPG or PNG images are allowed.");
        }

        try {
            if (ImageIO.read(new ByteArrayInputStream(file.getBytes())) == null) {
                throw new BadRequestException("Uploaded file must be a valid image.");
            }
        } catch (IOException ex) {
            throw new BadRequestException("Uploaded file must be a valid image.");
        }
    }

    private boolean isAllowedContentType(String contentType) {
        return contentType.equalsIgnoreCase("image/jpeg")
                || contentType.equalsIgnoreCase("image/png");
    }

    private String safeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "cin-image";
        }
        return originalFileName.replaceAll("[\\\\/\"]", "_").trim();
    }

    private KycStatusResponse toStatusResponse(KycDocument document) {
        return new KycStatusResponse(
                document.getId(),
                document.getStatus(),
                document.getDocumentType(),
                document.getFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getSubmittedAt(),
                document.getReviewedAt(),
                document.getRejectionReason()
        );
    }

    private KycReviewResponse toReviewResponse(KycDocument document) {
        User user = document.getUser();
        UserProfile profile = user.getProfile();
        User reviewer = document.getReviewedBy();

        return new KycReviewResponse(
                document.getId(),
                user.getId(),
                user.getEmail(),
                user.getRole(),
                profile == null ? null : profile.getFirstName(),
                profile == null ? null : profile.getLastName(),
                document.getStatus(),
                document.getDocumentType(),
                document.getFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getSubmittedAt(),
                document.getReviewedAt(),
                reviewer == null ? null : reviewer.getId(),
                document.getRejectionReason()
        );
    }
}
