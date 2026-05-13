package com.helma.helmabackend.controller.kyc;

import com.helma.helmabackend.dto.kyc.KycRejectRequest;
import com.helma.helmabackend.dto.kyc.KycReviewResponse;
import com.helma.helmabackend.entity.kyc.KycDocument;
import com.helma.helmabackend.service.kyc.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/kyc")
@RequiredArgsConstructor
public class AdminKycController {

    private final KycService kycService;

    @GetMapping("/pending")
    public List<KycReviewResponse> getPendingReviews() {
        return kycService.getPendingReviews();
    }

    @GetMapping("/{documentId}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long documentId) {
        KycDocument document = kycService.getReviewImage(documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .contentLength(document.getFileSize())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFileName() + "\"")
                .body(document.getImageData());
    }

    @PostMapping("/{documentId}/approve")
    public KycReviewResponse approve(@PathVariable Long documentId) {
        return kycService.approve(documentId);
    }

    @PostMapping("/{documentId}/reject")
    public KycReviewResponse reject(
            @PathVariable Long documentId,
            @Valid @RequestBody KycRejectRequest request
    ) {
        return kycService.reject(documentId, request.reason());
    }
}
