package com.helma.helmabackend.controller.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.campaign.CampaignPageResponse;
import com.helma.helmabackend.dto.crowdfunding.campaign.CampaignPageStatusPatchRequest;
import com.helma.helmabackend.dto.crowdfunding.campaign.CampaignPageUpsertRequest;
import com.helma.helmabackend.entity.crowdfunding.ApplicationDocument;
import com.helma.helmabackend.entity.crowdfunding.enums.CampaignPageStatus;
import com.helma.helmabackend.service.crowdfunding.ApplicationDocumentService;
import com.helma.helmabackend.service.crowdfunding.CampaignPageService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/crowdfunding")
public class CampaignPageController {

    private final CampaignPageService campaignPageService;
    private final ApplicationDocumentService applicationDocumentService;

    public CampaignPageController(
            CampaignPageService campaignPageService,
            ApplicationDocumentService applicationDocumentService
    ) {
        this.campaignPageService = campaignPageService;
        this.applicationDocumentService = applicationDocumentService;
    }

    @PostMapping("/application-raises/{applicationRaiseId}/campaign-page")
    public CampaignPageResponse createForApprovedApplication(@PathVariable Long applicationRaiseId) {
        return campaignPageService.createForApprovedApplication(applicationRaiseId);
    }

    @GetMapping("/campaign-pages/mine")
    public List<CampaignPageResponse> myCampaignPages() {
        return campaignPageService.listMine();
    }

    @GetMapping("/campaign-pages/{campaignPageId}/builder")
    public CampaignPageResponse getBuilder(@PathVariable Long campaignPageId) {
        return campaignPageService.getBuilder(campaignPageId);
    }

    @PutMapping("/campaign-pages/{campaignPageId}")
    public CampaignPageResponse updateDraft(
            @PathVariable Long campaignPageId,
            @Valid @RequestBody CampaignPageUpsertRequest req
    ) {
        return campaignPageService.updateDraft(campaignPageId, req);
    }

    @PostMapping("/campaign-pages/{campaignPageId}/submit")
    public CampaignPageResponse submitForReview(@PathVariable Long campaignPageId) {
        return campaignPageService.submitForReview(campaignPageId);
    }

    @GetMapping("/admin/campaign-pages")
    public List<CampaignPageResponse> adminList(
            @RequestParam(name = "status", required = false) CampaignPageStatus status
    ) {
        return campaignPageService.adminList(status);
    }

    @PatchMapping("/admin/campaign-pages/{campaignPageId}/status")
    public CampaignPageResponse adminPatchStatus(
            @PathVariable Long campaignPageId,
            @Valid @RequestBody CampaignPageStatusPatchRequest req
    ) {
        return campaignPageService.adminPatchStatus(campaignPageId, req);
    }

    @GetMapping("/public-campaigns")
    public List<CampaignPageResponse> listPublishedPublic() {
        return campaignPageService.listPublishedPublic();
    }

    @GetMapping("/public-campaigns/{slug}")
    public CampaignPageResponse getPublishedBySlug(@PathVariable String slug) {
        return campaignPageService.getPublishedBySlug(slug);
    }

    @GetMapping(
            value = "/public-campaigns/{slug}/documents/{applicationDocumentId}/content",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<Resource> viewPublishedDocument(
            @PathVariable String slug,
            @PathVariable Long applicationDocumentId
    ) throws IOException {
        ApplicationDocument doc = campaignPageService.getPublishedPublicDocument(slug, applicationDocumentId);
        Path path = applicationDocumentService.resolveReadableDocumentPath(doc);

        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(Files.size(path))
                .body(resource);
    }
}
