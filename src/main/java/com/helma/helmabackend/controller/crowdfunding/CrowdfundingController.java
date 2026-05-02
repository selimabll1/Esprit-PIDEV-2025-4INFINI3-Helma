package com.helma.helmabackend.controller.crowdfunding;

import com.helma.helmabackend.controller.common.ApiMessage;
import com.helma.helmabackend.dto.crowdfunding.*;
import com.helma.helmabackend.entity.crowdfunding.ApplicationDocument;
import com.helma.helmabackend.entity.crowdfunding.enums.DocumentType;
import com.helma.helmabackend.service.crowdfunding.ApplicationDocumentService;
import com.helma.helmabackend.service.crowdfunding.ApplicationRaiseService;
import com.helma.helmabackend.service.crowdfunding.PaymentService;
import com.helma.helmabackend.service.crowdfunding.PledgeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

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

    @GetMapping("/campaigns")
    public List<CampaignResponse> listApprovedCampaigns(
            @RequestParam(name = "sort", required = false, defaultValue = "newest") String sort,
            @RequestParam(name = "sortDir", required = false) String sortDir,
            @ModelAttribute ApplicationRaiseSearchCriteria criteria
    ) {
        return pledgeService.listApprovedCampaigns(sort, sortDir, criteria);
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

    @PostMapping("/my-payments/{paymentId}/stripe-sync")
    public PaymentResponse syncMyStripePayment(@PathVariable Long paymentId) {
        return paymentService.syncMyStripePayment(paymentId);
    }

    @GetMapping("/my-payments/{paymentId}/mock-checkout")
    public ApiMessage myPaymentMockCheckout(@PathVariable Long paymentId) {
        paymentService.getMyPaymentById(paymentId);
        return new ApiMessage(
                "Mock checkout ready. POST /api/crowdfunding/my-payments/"
                        + paymentId
                        + "/mock-result with status SUCCEEDED, FAILED or CANCELED."
        );
    }

    @PostMapping("/my-payments/{paymentId}/mock-result")
    public PaymentResponse mockPatchMyPaymentStatus(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentStatusPatchRequest req
    ) {
        return paymentService.mockPatchMyPaymentStatus(paymentId, req);
    }



    @PostMapping(value = "/stripe/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessage stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signatureHeader
    ) {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe webhook secret is not configured.");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret.trim());
        } catch (SignatureVerificationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature.");
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook payload.");
        }

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);

        if (stripeObject instanceof Session session) {
            switch (event.getType()) {
                case "checkout.session.completed" -> {
                    if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
                        paymentService.markStripePaymentSucceeded(session.getId(), session.getPaymentIntent());
                    }
                }
                case "checkout.session.expired" ->
                        paymentService.markStripePaymentCanceled(session.getId(), "Stripe checkout session expired.");
                case "checkout.session.async_payment_failed" ->
                        paymentService.markStripePaymentFailed(session.getId(), "Stripe asynchronous payment failed.");
                default -> {
                    // Ignore unrelated Checkout Session events.
                }
            }
        }

        return new ApiMessage("Stripe event processed: " + event.getType());
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

    @PostMapping(
            value = "/application-raises/{id}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApplicationDocumentResponse uploadDocument(
            @PathVariable Long id,
            @RequestParam("type") DocumentType type,
            @RequestPart("file") MultipartFile file
    ) {
        return documentService.uploadOwnerDocument(id, type, file);
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
        documentService.deleteOwnerDocument(id, type);
    }

    @DeleteMapping("/donations/{id}")
    public void deleteDonationDraft(@PathVariable Long id) {
        service.deleteDonationDraft(id);
    }

    @DeleteMapping("/equities/{id}")
    public void deleteEquityDraft(@PathVariable Long id) {
        service.deleteEquityDraft(id);
    }

    @GetMapping("/application-raises/mine")
    public List<ApplicationRaiseResponse> myApplications(
            @ModelAttribute ApplicationRaiseSearchCriteria criteria
    ) {
        return service.listMyApplications(criteria);
    }

    @GetMapping("/application-raises/{id}")
    public ApplicationRaiseResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PatchMapping("/application-raises/{id}/status")
    public ApplicationRaiseResponse youthPatchStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRaiseStatusPatchRequest req
    ) {
        return service.youthPatchStatus(id, req);
    }

    @PatchMapping("/application-raises/{id}/admin/status")
    public ApplicationRaiseResponse adminPatchStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRaiseStatusPatchRequest req
    ) {
        return service.adminPatchStatus(id, req);
    }

    @GetMapping("/application-raises/admin")
    public List<AdminApplicationRaiseSummaryResponse> adminListAll(
            @ModelAttribute ApplicationRaiseSearchCriteria criteria
    ) {
        return service.adminListAll(criteria);
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

    @PostMapping("/application-raises/drafts")
    public ApplicationRaiseResponse createEmptyDraft() {
        return service.createEmptyDraft();
    }

    @PatchMapping("/application-raises/{id}/steps/contact")
    public ApplicationRaiseResponse saveContactStep(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRaiseContactStepRequest req
    ) {
        return service.saveContactStep(id, req);
    }

    @PatchMapping("/application-raises/{id}/steps/type")
    public ApplicationRaiseResponse saveTypeStep(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRaiseTypeStepRequest req
    ) {
        return service.saveTypeStep(id, req);
    }

    @PatchMapping("/application-raises/{id}/steps/details")
    public ApplicationRaiseResponse saveDetailsStep(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRaiseDetailsStepRequest req
    ) {
        return service.saveDetailsStep(id, req);
    }

    @PostMapping("/application-raises/{id}/submit")
    public ApplicationRaiseResponse submitDraft(@PathVariable Long id) {
        return service.submitDraft(id);
    }
    @DeleteMapping("/application-raises/{id}/draft")
    public void deleteDraft(@PathVariable Long id) {
        service.deleteDraft(id);
    }
}