package com.helma.helmabackend.service.crowdfunding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helma.helmabackend.dto.crowdfunding.ApplicationDocumentResponse;
import com.helma.helmabackend.dto.crowdfunding.campaign.CampaignPageDocumentResponse;
import com.helma.helmabackend.dto.crowdfunding.campaign.CampaignPageResponse;
import com.helma.helmabackend.dto.crowdfunding.campaign.CampaignPageStatusPatchRequest;
import com.helma.helmabackend.dto.crowdfunding.campaign.CampaignPageUpsertRequest;
import com.helma.helmabackend.entity.crowdfunding.ApplicationDocument;
import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.CampaignPage;
import com.helma.helmabackend.entity.crowdfunding.CampaignPageDocument;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CampaignPageStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.DocumentType;
import com.helma.helmabackend.entity.user.Role;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.exception.UnauthorizedException;
import com.helma.helmabackend.repository.crowdfunding.ApplicationDocumentRepository;
import com.helma.helmabackend.repository.crowdfunding.ApplicationRaiseRepository;
import com.helma.helmabackend.repository.crowdfunding.CampaignPageDocumentRepository;
import com.helma.helmabackend.repository.crowdfunding.CampaignPageRepository;
import com.helma.helmabackend.repository.crowdfunding.EquityDetailRepository;
import com.helma.helmabackend.service.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CampaignPageService {

    private static final Set<DocumentType> PUBLIC_SAFE_DOCUMENT_TYPES = EnumSet.of(
            DocumentType.PROJECT_PITCH_DECK,
            DocumentType.CNRE_EXTRACT,
            DocumentType.FINANCIAL_STATEMENTS
    );

    private final CampaignPageRepository campaignPageRepo;
    private final CampaignPageDocumentRepository campaignPageDocumentRepo;
    private final ApplicationRaiseRepository applicationRaiseRepo;
    private final ApplicationDocumentRepository applicationDocumentRepo;
    private final EquityDetailRepository equityDetailRepo;
    private final CurrentUserService currentUserService;
    private final EquityDetailService equityDetailService;
    private final ApplicationDocumentService applicationDocumentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public CampaignPageResponse createForApprovedApplication(Long applicationRaiseId) {
        User me = currentUser();
        requireYouth(me);

        ApplicationRaise app = requireApplication(applicationRaiseId);
        assertOwner(app, me);
        assertApplicationApproved(app);

        CampaignPage existing = campaignPageRepo.findByApplicationRaiseId(applicationRaiseId).orElse(null);
        if (existing != null) {
            return toResponse(existing, true);
        }

        CampaignPage page = new CampaignPage();
        page.setApplicationRaiseId(app.getId());
        page.setOwnerUserId(me.getId());
        page.setTitle(nonBlankOr(app.getBusinessName(), "Untitled campaign"));
        page.setSubtitle(nonBlankOr(app.getSummary(), "Tell investors and donors what makes your project worth supporting."));
        page.setSlug(generateUniqueSlug(app.getBusinessName(), null));
        page.setContentJson(defaultContentJson(app));
        page.setStyleJson(defaultStyleJson());
        page.setStatus(CampaignPageStatus.DRAFT);

        CampaignPage saved = campaignPageRepo.save(page);
        return toResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public List<CampaignPageResponse> listMine() {
        User me = currentUser();
        requireYouth(me);

        return campaignPageRepo.findByOwnerUserIdOrderByUpdatedAtDesc(me.getId())
                .stream()
                .map(page -> toResponse(page, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public CampaignPageResponse getBuilder(Long campaignPageId) {
        CampaignPage page = requireReadableCampaignPage(campaignPageId);
        return toResponse(page, true);
    }

    @Transactional
    public CampaignPageResponse updateDraft(Long campaignPageId, CampaignPageUpsertRequest req) {
        User me = currentUser();
        requireYouth(me);

        CampaignPage page = campaignPageRepo.findByIdAndOwnerUserId(campaignPageId, me.getId())
                .orElseThrow(() -> new IllegalArgumentException("Campaign page not found: " + campaignPageId));

        if (page.getStatus() != CampaignPageStatus.DRAFT && page.getStatus() != CampaignPageStatus.CHANGES_REQUESTED) {
            throw new IllegalStateException("Only DRAFT or CHANGES_REQUESTED campaign pages can be edited.");
        }

        ApplicationRaise app = requireApplication(page.getApplicationRaiseId());
        assertApplicationApproved(app);

        applyUpsert(page, app, req);
        CampaignPage saved = campaignPageRepo.save(page);
        replacePublicDocuments(saved, req != null ? req.publicDocumentIds : null);

        return toResponse(saved, true);
    }

    @Transactional
    public CampaignPageResponse submitForReview(Long campaignPageId) {
        User me = currentUser();
        requireYouth(me);

        CampaignPage page = campaignPageRepo.findByIdAndOwnerUserId(campaignPageId, me.getId())
                .orElseThrow(() -> new IllegalArgumentException("Campaign page not found: " + campaignPageId));

        if (page.getStatus() != CampaignPageStatus.DRAFT && page.getStatus() != CampaignPageStatus.CHANGES_REQUESTED) {
            throw new IllegalStateException("Only DRAFT or CHANGES_REQUESTED campaign pages can be submitted.");
        }

        ApplicationRaise app = requireApplication(page.getApplicationRaiseId());
        assertApplicationApproved(app);
        assertCampaignReady(page);

        page.setStatus(CampaignPageStatus.PENDING_REVIEW);
        page.setReviewNote(null);

        return toResponse(campaignPageRepo.save(page), true);
    }

    @Transactional(readOnly = true)
    public List<CampaignPageResponse> adminList(CampaignPageStatus status) {
        User me = currentUser();
        requireAdminOrCompliance(me);

        List<CampaignPage> pages = status == null
                ? campaignPageRepo.findAllByOrderByUpdatedAtDesc()
                : campaignPageRepo.findByStatusOrderByUpdatedAtDesc(status);

        return pages.stream()
                .map(page -> toResponse(page, true))
                .toList();
    }

    @Transactional
    public CampaignPageResponse adminPatchStatus(Long campaignPageId, CampaignPageStatusPatchRequest req) {
        User me = currentUser();
        requireAdminOrCompliance(me);

        if (req == null || req.status == null) {
            throw new IllegalArgumentException("status is required");
        }

        CampaignPage page = campaignPageRepo.findById(campaignPageId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign page not found: " + campaignPageId));

        CampaignPageStatus oldStatus = page.getStatus();
        CampaignPageStatus target = req.status;

        boolean allowed = switch (oldStatus) {
            case PENDING_REVIEW -> target == CampaignPageStatus.PUBLISHED || target == CampaignPageStatus.CHANGES_REQUESTED;
            case PUBLISHED -> target == CampaignPageStatus.ARCHIVED || target == CampaignPageStatus.CHANGES_REQUESTED;
            case ARCHIVED -> target == CampaignPageStatus.PUBLISHED;
            case CHANGES_REQUESTED -> target == CampaignPageStatus.PENDING_REVIEW || target == CampaignPageStatus.ARCHIVED;
            case DRAFT -> target == CampaignPageStatus.ARCHIVED;
        };

        if (!allowed) {
            throw new IllegalStateException("Invalid campaign page status transition: " + oldStatus + " -> " + target);
        }

        ApplicationRaise app = requireApplication(page.getApplicationRaiseId());
        assertApplicationApproved(app);

        page.setStatus(target);
        page.setReviewNote(normalizeOptional(req.reviewNote));
        if (target == CampaignPageStatus.PUBLISHED && page.getPublishedAt() == null) {
            page.setPublishedAt(Instant.now());
        }
        if (target != CampaignPageStatus.PUBLISHED && target != CampaignPageStatus.ARCHIVED) {
            page.setPublishedAt(null);
        }

        return toResponse(campaignPageRepo.save(page), true);
    }

    @Transactional(readOnly = true)
    public List<CampaignPageResponse> listPublishedPublic() {
        return campaignPageRepo.findByStatusOrderByUpdatedAtDesc(CampaignPageStatus.PUBLISHED)
                .stream()
                .filter(page -> applicationRaiseRepo.findById(page.getApplicationRaiseId())
                        .map(app -> app.getStatus() == ApplicationRaiseStatus.APPROVED)
                        .orElse(false))
                .map(page -> toResponse(page, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public CampaignPageResponse getPublishedBySlug(String slug) {
        CampaignPage page = campaignPageRepo.findBySlugIgnoreCase(normalizeSlug(slug))
                .orElseThrow(() -> new IllegalArgumentException("Campaign page not found: " + slug));

        if (page.getStatus() != CampaignPageStatus.PUBLISHED) {
            throw new IllegalStateException("This campaign page is not published.");
        }

        ApplicationRaise app = requireApplication(page.getApplicationRaiseId());
        if (app.getStatus() != ApplicationRaiseStatus.APPROVED) {
            throw new IllegalStateException("This campaign is not available.");
        }

        return toResponse(page, false);
    }

    @Transactional(readOnly = true)
    public ApplicationDocument getPublishedPublicDocument(String slug, Long applicationDocumentId) {
        CampaignPage page = campaignPageRepo.findBySlugIgnoreCase(normalizeSlug(slug))
                .orElseThrow(() -> new IllegalArgumentException("Campaign page not found: " + slug));

        if (page.getStatus() != CampaignPageStatus.PUBLISHED) {
            throw new IllegalStateException("This campaign page is not published.");
        }

        requireApplication(page.getApplicationRaiseId());

        boolean selected = campaignPageDocumentRepo.findByCampaignPageId(page.getId())
                .stream()
                .anyMatch(d -> d.getApplicationDocumentId().equals(applicationDocumentId));

        if (!selected) {
            throw new UnauthorizedException("This document is not public for this campaign.");
        }

        ApplicationDocument doc = applicationDocumentRepo.findById(applicationDocumentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + applicationDocumentId));

        if (!doc.getApplicationRaiseId().equals(page.getApplicationRaiseId())) {
            throw new UnauthorizedException("This document does not belong to this campaign.");
        }

        assertPublicSafeDocument(doc);
        return doc;
    }

    private void applyUpsert(CampaignPage page, ApplicationRaise app, CampaignPageUpsertRequest req) {
        if (req == null) {
            return;
        }

        String title = normalizeOptional(req.title);
        if (title != null) {
            page.setTitle(title);
        }

        page.setSubtitle(normalizeOptional(req.subtitle));
        page.setCoverMediaUrl(normalizeOptional(req.coverMediaUrl));

        String requestedSlug = normalizeOptional(req.slug);
        if (requestedSlug != null) {
            String normalizedSlug = normalizeSlug(requestedSlug);
            if (normalizedSlug.length() < 3) {
                throw new IllegalArgumentException("slug must contain at least 3 characters");
            }
            if (!normalizedSlug.equalsIgnoreCase(page.getSlug()) && campaignPageRepo.existsBySlugIgnoreCase(normalizedSlug)) {
                throw new IllegalStateException("This public link is already used: " + normalizedSlug);
            }
            page.setSlug(normalizedSlug);
        } else if (page.getSlug() == null || page.getSlug().isBlank()) {
            page.setSlug(generateUniqueSlug(app.getBusinessName(), page.getId()));
        }

        String contentJson = normalizeOptional(req.contentJson);
        if (contentJson != null) {
            validateJsonObject(contentJson, "contentJson");
            page.setContentJson(contentJson);
        }

        String styleJson = normalizeOptional(req.styleJson);
        if (styleJson != null) {
            validateJsonObject(styleJson, "styleJson");
            page.setStyleJson(styleJson);
        }
    }

    private void replacePublicDocuments(CampaignPage page, Set<Long> requestedDocumentIds) {
        /*
         * Important:
         * We delete old selected documents first, then flush before inserting the new list.
         *
         * Without flush, Hibernate/MySQL can try to insert the same
         * (campaign_page_id, application_document_id) before the old row is actually deleted,
         * which triggers uk_campaign_page_document.
         */
        campaignPageDocumentRepo.deleteByCampaignPageId(page.getId());
        campaignPageDocumentRepo.flush();

        if (requestedDocumentIds == null || requestedDocumentIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueIds = new LinkedHashSet<>();

        for (Long documentId : requestedDocumentIds) {
            if (documentId != null) {
                uniqueIds.add(documentId);
            }
        }

        if (uniqueIds.isEmpty()) {
            return;
        }

        List<CampaignPageDocument> toSave = new ArrayList<>();
        Set<Long> preparedDocumentIds = new LinkedHashSet<>();

        for (Long documentId : uniqueIds) {
            ApplicationDocument doc = applicationDocumentRepo.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

            if (!doc.getApplicationRaiseId().equals(page.getApplicationRaiseId())) {
                throw new UnauthorizedException("Document " + documentId + " does not belong to this application.");
            }

            assertPublicSafeDocument(doc);

            /*
             * Defensive dedupe:
             * If the frontend has two Document components pointing to the same PDF,
             * save it only once in campaign_page_document.
             */
            if (!preparedDocumentIds.add(doc.getId())) {
                continue;
            }

            CampaignPageDocument item = new CampaignPageDocument();
            item.setCampaignPageId(page.getId());
            item.setApplicationDocumentId(doc.getId());
            item.setLabel(defaultDocumentLabel(doc.getDocType()));

            toSave.add(item);
        }

        if (!toSave.isEmpty()) {
            campaignPageDocumentRepo.saveAllAndFlush(toSave);
        }
    }

    private CampaignPage requireReadableCampaignPage(Long campaignPageId) {
        User me = currentUser();
        CampaignPage page = campaignPageRepo.findById(campaignPageId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign page not found: " + campaignPageId));

        if (page.getOwnerUserId().equals(me.getId()) || isAdminOrCompliance(me)) {
            return page;
        }

        throw new UnauthorizedException("You can only view your own campaign page.");
    }

    private ApplicationRaise requireApplication(Long applicationRaiseId) {
        return applicationRaiseRepo.findById(applicationRaiseId)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + applicationRaiseId));
    }

    private void assertOwner(ApplicationRaise app, User user) {
        if (!app.getOwnerUserId().equals(user.getId())) {
            throw new UnauthorizedException("You can only create a campaign page for your own application.");
        }
    }

    private void assertApplicationApproved(ApplicationRaise app) {
        if (app.getStatus() != ApplicationRaiseStatus.APPROVED) {
            throw new IllegalStateException("Campaign builder unlocks only after the application is APPROVED.");
        }
    }

    private void assertCampaignReady(CampaignPage page) {
        if (page.getTitle() == null || page.getTitle().isBlank()) {
            throw new IllegalStateException("Campaign title is required before submission.");
        }
        if (page.getContentJson() == null || page.getContentJson().isBlank()) {
            throw new IllegalStateException("Campaign content is required before submission.");
        }
        validateJsonObject(page.getContentJson(), "contentJson");
        validateJsonObject(page.getStyleJson(), "styleJson");
    }

    private void assertPublicSafeDocument(ApplicationDocument doc) {
        if (!PUBLIC_SAFE_DOCUMENT_TYPES.contains(doc.getDocType())) {
            throw new IllegalStateException(
                    "Document type " + doc.getDocType() + " cannot be exposed publicly on a campaign page."
            );
        }
    }

    private CampaignPageResponse toResponse(CampaignPage page, boolean includeBuilderPrivateData) {
        ApplicationRaise app = requireApplication(page.getApplicationRaiseId());

        CampaignPageResponse r = new CampaignPageResponse();
        r.id = page.getId();
        r.applicationRaiseId = page.getApplicationRaiseId();
        r.ownerUserId = page.getOwnerUserId();
        r.slug = page.getSlug();
        r.publicUrl = "/campaigns/" + page.getSlug();
        r.title = page.getTitle();
        r.subtitle = page.getSubtitle();
        r.coverMediaUrl = page.getCoverMediaUrl();
        r.contentJson = page.getContentJson();
        r.styleJson = page.getStyleJson();
        r.status = page.getStatus();
        r.reviewNote = page.getReviewNote();
        r.publishedAt = page.getPublishedAt();
        r.createdAt = page.getCreatedAt();
        r.updatedAt = page.getUpdatedAt();

        r.applicationType = app.getType();
        r.businessName = app.getBusinessName();
        r.website = app.getWebsite();
        r.sector = app.getSector();
        r.subSector = app.getSubSector();
        if (app.getTags() != null) {
            r.tags = new LinkedHashSet<>(app.getTags());
        }
        r.summary = app.getSummary();
        r.problemStatement = app.getProblemStatement();
        r.solution = app.getSolution();
        r.targetCustomers = app.getTargetCustomers();
        r.useOfFunds = app.getUseOfFunds();
        r.fundingGoal = moneyOrZero(app.getFundingGoal());
        r.investorsPledgedAmount = moneyOrZero(app.getInvestorsPledgedAmount());
        r.currency = app.getCurrency() != null ? app.getCurrency().name() : null;
        r.governorate = app.getGovernorate();
        r.city = app.getCity();

        if (app.getType() == CrowdfundingType.EQUITY) {
            equityDetailRepo.findByApplicationRaiseId(app.getId())
                    .ifPresent(equity -> r.equityDetail = equityDetailService.toDto(equity));
        }

        r.publicDocuments = campaignPageDocumentRepo.findByCampaignPageId(page.getId())
                .stream()
                .map(this::toDocumentResponse)
                .toList();

        if (includeBuilderPrivateData) {
            List<ApplicationDocumentResponse> safeDocs = applicationDocumentRepo.findByApplicationRaiseId(app.getId())
                    .stream()
                    .filter(doc -> PUBLIC_SAFE_DOCUMENT_TYPES.contains(doc.getDocType()))
                    .map(applicationDocumentService::toDto)
                    .toList();
            r.availableDocuments = safeDocs;
        }

        return r;
    }

    private CampaignPageDocumentResponse toDocumentResponse(CampaignPageDocument item) {
        ApplicationDocument doc = applicationDocumentRepo.findById(item.getApplicationDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + item.getApplicationDocumentId()));

        CampaignPageDocumentResponse r = new CampaignPageDocumentResponse();
        r.id = item.getId();
        r.campaignPageId = item.getCampaignPageId();
        r.applicationDocumentId = item.getApplicationDocumentId();
        r.docType = doc.getDocType();
        r.fileName = doc.getFileName();
        r.label = item.getLabel();
        r.sizeBytes = doc.getSizeBytes();
        r.createdAt = item.getCreatedAt();
        return r;
    }

    private BigDecimal moneyOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String defaultContentJson(ApplicationRaise app) {
        String title = escapeJson(nonBlankOr(app.getBusinessName(), "Untitled campaign"));
        String subtitle = escapeJson(nonBlankOr(app.getSummary(), "Tell your story and explain why people should support you."));
        return "{\"blocks\":["
                + "{\"id\":\"hero\",\"type\":\"hero\",\"title\":\"" + title + "\",\"subtitle\":\"" + subtitle + "\"},"
                + "{\"id\":\"about\",\"type\":\"paragraph\",\"content\":\"Introduce your project, your mission, and the impact you want to create.\"},"
                + "{\"id\":\"funding\",\"type\":\"funding_cta\"}"
                + "]}";
    }

    private String defaultStyleJson() {
        return "{\"fontFamily\":\"Inter\",\"primaryColor\":\"#111827\",\"accentColor\":\"#C9A227\",\"radius\":\"large\",\"heroLayout\":\"centered\",\"buttonStyle\":\"rounded\"}";
    }

    private void validateJsonObject(String json, String fieldName) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException(fieldName + " must be a JSON object");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " must be valid JSON object", e);
        }
    }

    private String generateUniqueSlug(String seed, Long currentPageId) {
        String base = normalizeSlug(nonBlankOr(seed, "campaign"));
        String candidate = base;
        int suffix = 2;
        while (slugTakenByAnotherPage(candidate, currentPageId)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean slugTakenByAnotherPage(String slug, Long currentPageId) {
        return campaignPageRepo.findBySlugIgnoreCase(slug)
                .map(existing -> currentPageId == null || !existing.getId().equals(currentPageId))
                .orElse(false);
    }

    private String normalizeSlug(String value) {
        String raw = normalizeOptional(value);
        if (raw == null) {
            return "campaign";
        }

        String noAccents = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String slug = noAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");

        if (slug.isBlank()) {
            return "campaign";
        }
        return slug.length() > 180 ? slug.substring(0, 180).replaceAll("-+$", "") : slug;
    }

    private String defaultDocumentLabel(DocumentType type) {
        if (type == null) {
            return "Document";
        }
        return switch (type) {
            case PROJECT_PITCH_DECK -> "Pitch deck";
            case CNRE_EXTRACT -> "Company registration extract";
            case FINANCIAL_STATEMENTS -> "Financial statements";
            default -> type.name().replace('_', ' ');
        };
    }

    private String nonBlankOr(String value, String fallback) {
        String normalized = normalizeOptional(value);
        return normalized == null ? fallback : normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String escapeJson(String value) {
        return value == null
                ? ""
                : value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private User currentUser() {
        return currentUserService.getCurrentUser();
    }

    private boolean isAdminOrCompliance(User user) {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.COMPLIANCE;
    }

    private void requireYouth(User user) {
        if (user.getRole() != Role.YOUTH_BENEFICIARY) {
            throw new UnauthorizedException("Only YOUTH_BENEFICIARY can manage campaign pages.");
        }
    }

    private void requireAdminOrCompliance(User user) {
        if (!isAdminOrCompliance(user)) {
            throw new UnauthorizedException("Only ADMIN/COMPLIANCE can review campaign pages.");
        }
    }
}
