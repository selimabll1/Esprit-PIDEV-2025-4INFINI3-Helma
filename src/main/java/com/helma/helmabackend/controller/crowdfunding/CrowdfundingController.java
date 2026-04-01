package com.helma.helmabackend.controller.crowdfunding;

import com.helma.helmabackend.controller.common.ApiMessage;
import com.helma.helmabackend.dto.crowdfunding.*;
import com.helma.helmabackend.entity.crowdfunding.ApplicationDocument;
import com.helma.helmabackend.entity.crowdfunding.enums.DocumentType;
import com.helma.helmabackend.service.crowdfunding.ApplicationDocumentService;
import com.helma.helmabackend.service.crowdfunding.ApplicationRaiseService;
import com.helma.helmabackend.service.crowdfunding.PaymentService;
import com.helma.helmabackend.service.crowdfunding.PledgeService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/crowdfunding")
public class CrowdfundingController {

    private final ApplicationRaiseService service;
    private final ApplicationDocumentService documentService;
    private final PledgeService pledgeService;
    private final PaymentService paymentService;

    public CrowdfundingController(
            ApplicationRaiseService service,
            ApplicationDocumentService documentService,
            PledgeService pledgeService,
            PaymentService paymentService
    ) {
        this.service = service;
        this.documentService = documentService;
        this.pledgeService = pledgeService;
        this.paymentService = paymentService;
    }

    // =========================================================
    // PUBLIC / INVESTOR CAMPAIGNS
    // =========================================================

    @GetMapping("/campaigns")
    public List<CampaignResponse> listApprovedCampaigns(
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort)
    {
        return pledgeService.listApprovedCampaigns(sort);
    }

    @GetMapping("/campaigns/{id}")
    public CampaignResponse getApprovedCampaign(@PathVariable Long id) {
        return pledgeService.getApprovedCampaign(id);
    }

    @PostMapping("/campaigns/{id}/pledges")
    public PledgeResponse createOrUpdateMyPledge(
            @PathVariable Long id,
            @Valid @RequestBody PledgeCreateRequest req
    ) {
        return pledgeService.createOrUpdateMyPledge(id, req);
    }

    @GetMapping("/my-pledges")
    public List<PledgeResponse> myPledges() {
        return pledgeService.listMyPledges();
    }

    @GetMapping("/my-pledges/{pledgeId}")
    public PledgeResponse myPledgeById(@PathVariable Long pledgeId) {
        return pledgeService.getMyPledgeById(pledgeId);
    }

    @PostMapping("/my-pledges/{pledgeId}/payments/initiate")
    public PaymentResponse initiateMyPayment(@PathVariable Long pledgeId) {
        return paymentService.initiateMyPayment(pledgeId);
    }

    @GetMapping("/my-payments")
    public List<PaymentResponse> myPayments() {
        return paymentService.listMyPayments();
    }

    @GetMapping("/my-payments/{paymentId}")
    public PaymentResponse myPaymentById(@PathVariable Long paymentId) {
        return paymentService.getMyPaymentById(paymentId);
    }

    @GetMapping("/my-payments/{paymentId}/mock-checkout")
    public ApiMessage myPaymentMockCheckout(@PathVariable Long paymentId) {
        paymentService.getMyPaymentById(paymentId);
        return new ApiMessage("Mock checkout ready. POST /api/crowdfunding/my-payments/" + paymentId + "/mock-result with status SUCCEEDED, FAILED or CANCELED.");
    }

    @PostMapping("/my-payments/{paymentId}/mock-result")
    public PaymentResponse mockPatchMyPaymentStatus(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentStatusPatchRequest req
    ) {
        return paymentService.mockPatchMyPaymentStatus(paymentId, req);
    }

    @GetMapping("/admin/pledges")
    public List<PledgeResponse> adminListPledges() {
        return pledgeService.adminListAllPledges();
    }

    @PatchMapping("/admin/pledges/{pledgeId}/status")
    public PledgeResponse adminPatchPledgeStatus(
            @PathVariable Long pledgeId,
            @Valid @RequestBody PledgeStatusPatchRequest req
    ) {
        return pledgeService.adminPatchPledgeStatus(pledgeId, req);
    }

    @GetMapping("/admin/payments")
    public List<PaymentResponse> adminListPayments() {
        return paymentService.adminListAllPayments();
    }

    @PostMapping("/admin/payments/{paymentId}/mock-result")
    public PaymentResponse adminMockPatchPaymentStatus(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentStatusPatchRequest req
    ) {
        return paymentService.adminMockPatchPaymentStatus(paymentId, req);
    }

    // =========================================================
    // CREATE (JSON)
    // =========================================================

    @PostMapping(value = "/donations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApplicationRaiseResponse createDonation(
            @Valid @RequestBody ApplicationRaiseCreateRequest application
    ) {
        return service.createDonationDraft(application);
    }

    @PostMapping(value = "/equities", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApplicationRaiseResponse createEquity(
            @Valid @RequestBody EquityApplicationCreateRequest payload
    ) {
        return service.createEquityDraftMerged(payload);
    }

    // =========================================================
    // UPDATE (JSON)
    // =========================================================

    @PutMapping(value = "/donations/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApplicationRaiseResponse updateDonationDraft(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRaiseCreateRequest application
    ) {
        return service.updateDonationDraft(id, application);
    }

    @PutMapping(value = "/equities/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApplicationRaiseResponse updateEquityDraft(
            @PathVariable Long id,
            @Valid @RequestBody EquityApplicationCreateRequest payload
    ) {
        return service.updateEquityDraftMerged(id, payload);
    }

    // =========================================================
    // DOCUMENTS (multipart, explicit type)
    // =========================================================

    @PostMapping(
            value = "/application-raises/{id}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApplicationDocumentResponse uploadDocument(
            @PathVariable Long id,
            @RequestParam("type") DocumentType type,
            @RequestPart("file") MultipartFile file
    ) {
        return documentService.uploadFounderDocument(id, type, file);
    }

    @GetMapping("/application-raises/{id}/documents")
    public List<ApplicationDocumentResponse> listDocuments(@PathVariable Long id) {
        return documentService.listDocumentResponses(id);
    }

    @DeleteMapping("/application-raises/{id}/documents/{type}")
    public void deleteDocument(
            @PathVariable Long id,
            @PathVariable DocumentType type
    ) {
        documentService.deleteFounderDocument(id, type);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/donations/{id}")
    public void deleteDonationDraft(@PathVariable Long id) {
        service.deleteDonationDraft(id);
    }

    @DeleteMapping("/equities/{id}")
    public void deleteEquityDraft(@PathVariable Long id) {
        service.deleteEquityDraft(id);
    }

    // =========================================================
    // READ
    // =========================================================

    @GetMapping("/application-raises/mine")
    public List<ApplicationRaiseResponse> myApplications() {
        return service.listMyApplications();
    }

    @GetMapping("/application-raises/{id}")
    public ApplicationRaiseResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // =========================================================
    // STATUS (PATCH one field)
    // =========================================================

    @PatchMapping("/application-raises/{id}/status")
    public ApplicationRaiseResponse founderPatchStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRaiseStatusPatchRequest req
    ) {
        return service.founderPatchStatus(id, req);
    }

    @PatchMapping("/application-raises/{id}/admin/status")
    public ApplicationRaiseResponse adminPatchStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRaiseStatusPatchRequest req
    ) {
        return service.adminPatchStatus(id, req);
    }

    // =========================================================
    // ADMIN LIST
    // =========================================================

    @GetMapping("/application-raises/admin")
    public List<ApplicationRaiseResponse> adminListAll() {
        return service.adminListAll();
    }

    @GetMapping(
            value = "/application-raises/{id}/documents/{type}/content",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<Resource> viewDocumentContent(
            @PathVariable Long id,
            @PathVariable DocumentType type
    ) throws IOException {
        ApplicationDocument doc = documentService.getReadableDocument(id, type);
        Path path = documentService.resolveReadableDocumentPath(doc);

        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(Files.size(path))
                .body(resource);
    }
}
