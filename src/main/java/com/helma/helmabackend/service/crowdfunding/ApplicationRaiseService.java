package com.helma.helmabackend.service.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.*;
import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseDraftStep;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;
import com.helma.helmabackend.entity.user.Role;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.exception.FieldValidationException;
import com.helma.helmabackend.exception.UnauthorizedException;
import com.helma.helmabackend.repository.crowdfunding.ApplicationRaiseRepository;
import com.helma.helmabackend.repository.crowdfunding.ApplicationRaiseSpecifications;
import com.helma.helmabackend.repository.crowdfunding.EquityDetailRepository;
import com.helma.helmabackend.service.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApplicationRaiseService {

    private final ApplicationRaiseRepository repo;
    private final EquityDetailRepository equityRepo;

    private final CurrentUserService currentUserService;
    private final EquityDetailService equityDetailService;
    private final ApplicationDocumentService documentService;
    private final ApplicationRaiseNotificationService notificationService;

    @Transactional
    public ApplicationRaiseResponse createDonationDraft(ApplicationRaiseCreateRequest application) {
        application.type = CrowdfundingType.DONATION;
        return createDraft(application);
    }
    private AdminApplicationRaiseSummaryResponse toAdminSummaryResponse(ApplicationRaise a) {
        AdminApplicationRaiseSummaryResponse r = new AdminApplicationRaiseSummaryResponse();

        r.id = a.getId();
        r.ownerUserId = a.getOwnerUserId();
        r.type = a.getType();

        r.businessName = a.getBusinessName();
        r.website = a.getWebsite();

        r.country = a.getCountry();
        r.currency = a.getCurrency();

        r.sector = a.getSector();
        r.subSector = a.getSubSector();

        if (a.getTags() != null) {
            r.tags = new LinkedHashSet<>(a.getTags());
        }

        r.stage = a.getStage();

        r.summary = a.getSummary();
        r.problemStatement = a.getProblemStatement();
        r.solution = a.getSolution();
        r.targetCustomers = a.getTargetCustomers();
        r.useOfFunds = a.getUseOfFunds();

        r.fundingGoal = moneyOrZero(a.getFundingGoal());
        r.investorsPledgedAmount = moneyOrZero(a.getInvestorsPledgedAmount());
        r.raisedAmount = r.investorsPledgedAmount;
        r.remainingAmount = r.fundingGoal.subtract(r.raisedAmount).max(BigDecimal.ZERO);
        r.fundingProgressPercent = percent(r.raisedAmount, r.fundingGoal);

        r.customerCount = a.getCustomerCount();
        r.teamSize = a.getTeamSize();
        r.governorate = a.getGovernorate();
        r.city = a.getCity();

        r.contactFirstName = a.getContactFirstName();
        r.contactLastName = a.getContactLastName();
        r.contactTitle = a.getContactTitle();
        r.contactEmail = a.getContactEmail();
        r.contactPhone = a.getContactPhone();

        r.useProfileContact = a.isUseProfileContact();
        r.acceptedTerms = a.isAcceptedTerms();

        r.status = a.getStatus();
        r.draftStep = a.getDraftStep();

        if (a.getType() == CrowdfundingType.EQUITY) {
            equityRepo.findByApplicationRaiseId(a.getId())
                    .ifPresent(ed -> r.equityDetail = equityDetailService.toDto(ed));
        }

        r.documents = documentService.listDocumentResponses(a.getId());
        r.documentCompletionPercent = documentService.documentCompletionPercent(a.getId());
        r.applicationCompletionPercent = calculateApplicationCompletionPercent(a, r.documentCompletionPercent);

        r.createdAt = a.getCreatedAt();
        r.updatedAt = a.getUpdatedAt();
        r.submittedAt = a.getSubmittedAt();

        return r;
    }

    @Transactional
    public ApplicationRaiseResponse updateDonationDraft(Long id, ApplicationRaiseCreateRequest application) {
        application.type = CrowdfundingType.DONATION;

        ApplicationRaise a = requireDraftOwner(id);
        if (a.getType() != CrowdfundingType.DONATION) {
            throw new IllegalStateException("This application is not DONATION type.");
        }

        return updateDraft(id, application);
    }

    @Transactional
    public ApplicationRaiseResponse createEquityDraftMerged(EquityApplicationCreateRequest req) {
        User me = currentUser();
        requireYouthBeneficiary(me);

        req.application.type = CrowdfundingType.EQUITY;

        String businessName = normalizeRequiredField(
                req.application.businessName,
                "application.businessName",
                "businessName is required"
        );

        String website = normalizeOptional(req.application.website);

        validateTaxonomy(req.application.sector, req.application.subSector, "application.sector", "application.subSector");
        Set<AppTag> tags = normalizeRequiredTags(req.application.tags, "application.tags");
        assertUniqueBusinessName(me.getId(), businessName, "application.businessName", null);
        assertUniqueEquityRegistrationNumber(
                req.equityDetail.companyRegistrationNumber,
                "equityDetail.companyRegistrationNumber",
                null
        );

        ApplicationRaise a = new ApplicationRaise();
        a.setOwnerUserId(me.getId());
        a.setType(CrowdfundingType.EQUITY);

        a.setBusinessName(businessName);
        a.setWebsite(website);

        a.setSector(req.application.sector);
        a.setSubSector(req.application.subSector);
        a.setTags(tags);
        a.setSummary(req.application.summary);

        a.setFundingGoal(req.application.fundingGoal);
        a.setInvestorsPledgedAmount(BigDecimal.ZERO);
        a.setCustomerCount(req.application.customerCount);

        a.setContactFirstName(req.application.contactFirstName);
        a.setContactLastName(req.application.contactLastName);
        a.setContactTitle(req.application.contactTitle);
        a.setContactEmail(req.application.contactEmail);
        a.setContactPhone(req.application.contactPhone);

        a.setUseProfileContact(false);
        a.setAcceptedTerms(req.application.acceptedTerms);
        a.setStatus(ApplicationRaiseStatus.DRAFT);
        a.setDraftStep(ApplicationRaiseDraftStep.DOCUMENTS);

        ApplicationRaise saved = repo.save(a);

        equityDetailService.upsert(saved.getId(), req.equityDetail);

        return getById(saved.getId());
    }

    @Transactional
    public ApplicationRaiseResponse updateEquityDraftMerged(Long id, EquityApplicationCreateRequest req) {
        User me = currentUser();
        requireYouthBeneficiary(me);

        ApplicationRaise a = requireDraftOwner(id);
        if (a.getType() != CrowdfundingType.EQUITY) {
            throw new IllegalStateException("This application is not EQUITY type.");
        }

        req.application.type = CrowdfundingType.EQUITY;

        String businessName = normalizeRequiredField(
                req.application.businessName,
                "application.businessName",
                "businessName is required"
        );
        String website = normalizeOptional(req.application.website);

        validateTaxonomy(req.application.sector, req.application.subSector, "application.sector", "application.subSector");
        Set<AppTag> tags = normalizeRequiredTags(req.application.tags, "application.tags");
        assertUniqueBusinessName(me.getId(), businessName, "application.businessName", id);

        assertUniqueEquityRegistrationNumber(
                req.equityDetail.companyRegistrationNumber,
                "equityDetail.companyRegistrationNumber",
                id
        );

        a.setBusinessName(businessName);
        a.setWebsite(website);

        a.setSector(req.application.sector);
        a.setSubSector(req.application.subSector);
        a.setTags(tags);
        a.setSummary(req.application.summary);

        a.setFundingGoal(req.application.fundingGoal);
        a.setCustomerCount(req.application.customerCount);

        a.setContactFirstName(req.application.contactFirstName);
        a.setContactLastName(req.application.contactLastName);
        a.setContactTitle(req.application.contactTitle);
        a.setContactEmail(req.application.contactEmail);
        a.setContactPhone(req.application.contactPhone);

        a.setUseProfileContact(false);
        a.setAcceptedTerms(req.application.acceptedTerms);
        a.setDraftStep(ApplicationRaiseDraftStep.DOCUMENTS);

        repo.save(a);
        equityDetailService.upsert(id, req.equityDetail);

        return getById(id);
    }

    @Transactional
    public ApplicationRaiseResponse createDraft(ApplicationRaiseCreateRequest req) {
        User me = currentUser();
        requireYouthBeneficiary(me);

        if (req.type == null) {
            throw fieldError("type", "type is required");
        }

        String businessName = normalizeRequiredField(req.businessName, "businessName", "businessName is required");

        String website = normalizeOptional(req.website);

        validateTaxonomy(req.sector, req.subSector, "sector", "subSector");
        Set<AppTag> tags = normalizeRequiredTags(req.tags, "tags");


        assertUniqueBusinessName(me.getId(), businessName, "businessName", null);


        ApplicationRaise a = new ApplicationRaise();
        a.setOwnerUserId(me.getId());
        a.setType(req.type);

        a.setBusinessName(businessName);
        a.setWebsite(website);

        a.setSector(req.sector);
        a.setSubSector(req.subSector);
        a.setTags(tags);
        a.setSummary(req.summary);

        a.setFundingGoal(req.fundingGoal);
        a.setInvestorsPledgedAmount(BigDecimal.ZERO);
        a.setCustomerCount(req.customerCount);

        a.setContactFirstName(req.contactFirstName);
        a.setContactLastName(req.contactLastName);
        a.setContactTitle(req.contactTitle);
        a.setContactEmail(req.contactEmail);
        a.setContactPhone(req.contactPhone);

        a.setUseProfileContact(false);
        a.setAcceptedTerms(req.acceptedTerms);
        a.setStatus(ApplicationRaiseStatus.DRAFT);
        a.setDraftStep(ApplicationRaiseDraftStep.DOCUMENTS);

        ApplicationRaise saved = repo.save(a);
        return getById(saved.getId());
    }

    @Transactional
    public ApplicationRaiseResponse updateDraft(Long id, ApplicationRaiseCreateRequest req) {
        ApplicationRaise a = requireDraftOwner(id);

        if (req.type == null) {
            throw fieldError("type", "type is required");
        }

        String businessName = normalizeRequiredField(req.businessName, "businessName", "businessName is required");
        String website = normalizeOptional(req.website);

        validateTaxonomy(req.sector, req.subSector, "sector", "subSector");
        Set<AppTag> tags = normalizeRequiredTags(req.tags, "tags");


        assertUniqueBusinessName(a.getOwnerUserId(), businessName, "businessName", a.getId());


        a.setType(req.type);
        a.setBusinessName(businessName);

        a.setWebsite(website);

        a.setSector(req.sector);
        a.setSubSector(req.subSector);
        a.setTags(tags);
        a.setSummary(req.summary);

        a.setFundingGoal(req.fundingGoal);
        a.setCustomerCount(req.customerCount);

        a.setContactFirstName(req.contactFirstName);
        a.setContactLastName(req.contactLastName);
        a.setContactTitle(req.contactTitle);
        a.setContactEmail(req.contactEmail);
        a.setContactPhone(req.contactPhone);

        a.setUseProfileContact(false);
        a.setAcceptedTerms(req.acceptedTerms);
        a.setDraftStep(ApplicationRaiseDraftStep.DOCUMENTS);

        repo.save(a);
        return getById(id);
    }

    @Transactional
    public void deleteDonationDraft(Long id) {
        ApplicationRaise a = requireDraftOwner(id);
        if (a.getType() != CrowdfundingType.DONATION) {
            throw new IllegalStateException("Not a DONATION draft.");
        }

        documentService.deleteAllDocumentsForApplication(id);
        repo.delete(a);
    }
    @Transactional
    public void deleteDraft(Long id) {
        ApplicationRaise a = requireDraftOwner(id);

        documentService.deleteAllDocumentsForApplication(id);

        equityRepo.findByApplicationRaiseId(id)
                .ifPresent(equityRepo::delete);

        repo.delete(a);
    }

    @Transactional
    public void deleteEquityDraft(Long id) {
        ApplicationRaise a = requireDraftOwner(id);
        if (a.getType() != CrowdfundingType.EQUITY) {
            throw new IllegalStateException("Not an EQUITY draft.");
        }

        documentService.deleteAllDocumentsForApplication(id);
        equityRepo.findByApplicationRaiseId(id).ifPresent(equityRepo::delete);
        repo.delete(a);
    }

    @Transactional(readOnly = true)
    public ApplicationRaiseResponse getById(Long id) {
        User me = currentUser();

        ApplicationRaise a = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + id));

        if (!a.getOwnerUserId().equals(me.getId()) && !isAdminOrCompliance(me)) {
            throw new UnauthorizedException("You can only view your own application.");
        }

        return toResponse(a);
    }

    @Transactional(readOnly = true)
    public List<ApplicationRaiseResponse> listMyApplications(ApplicationRaiseSearchCriteria criteria) {
        User me = currentUser();
        requireYouthBeneficiary(me);

        ApplicationRaiseSearchCriteria effectiveCriteria = criteria != null ? criteria : new ApplicationRaiseSearchCriteria();
        effectiveCriteria.setOwnerUserId(me.getId());

        Specification<ApplicationRaise> specification = ApplicationRaiseSpecifications.byCriteria(effectiveCriteria);
        Sort sort = buildSort(effectiveCriteria, "createdAt", Sort.Direction.DESC);

        return repo.findAll(specification, sort).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminApplicationRaiseSummaryResponse> adminListAll(ApplicationRaiseSearchCriteria criteria) {
        User me = currentUser();
        requireAdminOrCompliance(me);

        ApplicationRaiseSearchCriteria effectiveCriteria =
                criteria != null ? criteria : new ApplicationRaiseSearchCriteria();

        Specification<ApplicationRaise> specification =
                ApplicationRaiseSpecifications.byCriteria(effectiveCriteria)
                        .and(ApplicationRaiseSpecifications.statusNot(ApplicationRaiseStatus.DRAFT));

        Sort sort = buildSort(effectiveCriteria, "createdAt", Sort.Direction.DESC);

        return repo.findAll(specification, sort)
                .stream()
                .map(this::toAdminSummaryResponse)
                .toList();
    }

    @Transactional
    public ApplicationRaiseResponse youthPatchStatus(Long id, ApplicationRaiseStatusPatchRequest req) {
        User me = currentUser();
        requireYouthBeneficiary(me);

        if (req == null || req.status == null) {
            throw new IllegalArgumentException("status is required");
        }

        ApplicationRaise a = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + id));

        if (!a.getOwnerUserId().equals(me.getId())) {
            throw new UnauthorizedException("You can only change status of your own application.");
        }
        if (req.status != ApplicationRaiseStatus.SUBMITTED) {
            throw new IllegalStateException("YOUTH_BENEFICIARY can only set status to SUBMITTED.");
        }
        if (a.getStatus() != ApplicationRaiseStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT can be submitted.");
        }
        if (!a.isAcceptedTerms()) {
            throw new IllegalStateException("Terms must be accepted before submission.");
        }

        if (a.getType() == CrowdfundingType.EQUITY) {
            documentService.assertRequiredDocsCompleteOrThrow(a.getId());
        }

        documentService.assertRequiredDocsCompleteOrThrow(a.getId());

        a.setStatus(ApplicationRaiseStatus.SUBMITTED);
        a.setSubmittedAt(Instant.now());
        return toResponse(repo.save(a));
    }

    @Transactional
    public ApplicationRaiseResponse adminPatchStatus(Long id, ApplicationRaiseStatusPatchRequest req) {
        User me = currentUser();
        requireAdminOrCompliance(me);

        if (req == null || req.status == null) {
            throw new IllegalArgumentException("status is required");
        }

        ApplicationRaise a = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + id));

        ApplicationRaiseStatus oldStatus = a.getStatus();
        ApplicationRaiseStatus target = req.status;

        if (oldStatus == ApplicationRaiseStatus.DRAFT) {
            throw new IllegalStateException("Admins cannot change status of DRAFT applications.");
        }

        boolean allowed =
                (oldStatus == ApplicationRaiseStatus.SUBMITTED
                        && (target == ApplicationRaiseStatus.UNDER_REVIEW
                        || target == ApplicationRaiseStatus.APPROVED
                        || target == ApplicationRaiseStatus.REJECTED))
                        ||
                        (oldStatus == ApplicationRaiseStatus.UNDER_REVIEW
                                && (target == ApplicationRaiseStatus.APPROVED
                                || target == ApplicationRaiseStatus.REJECTED));

        if (!allowed) {
            throw new IllegalStateException("Invalid status transition: " + oldStatus + " -> " + target);
        }

        if (oldStatus == target) {
            return toResponse(a);
        }

        a.setStatus(target);
        ApplicationRaise saved = repo.save(a);
        notificationService.notifyStatusChanged(saved, oldStatus);
        return toResponse(saved);
    }

    @Transactional
    public ApplicationRaiseResponse createEmptyDraft() {
        User me = currentUser();
        requireYouthBeneficiary(me);

        boolean alreadyHasUntypedDraft =
                repo.existsByOwnerUserIdAndTypeIsNullAndStatus(me.getId(), ApplicationRaiseStatus.DRAFT);

        if (alreadyHasUntypedDraft) {
            throw new IllegalStateException("You already have an unfinished draft. Complete it before creating another one.");
        }

        ApplicationRaise a = new ApplicationRaise();
        a.setOwnerUserId(me.getId());
        a.setStatus(ApplicationRaiseStatus.DRAFT);
        a.setDraftStep(ApplicationRaiseDraftStep.CONTACT);
        a.setUseProfileContact(true);
        a.setFundingGoal(BigDecimal.ZERO);
        a.setInvestorsPledgedAmount(BigDecimal.ZERO);

        return toResponse(repo.save(a));
    }

    @Transactional
    public ApplicationRaiseResponse saveContactStep(Long id, ApplicationRaiseContactStepRequest req) {
        ApplicationRaise a = requireDraftOwner(id);

        a.setUseProfileContact(req.useProfileContact);
        a.setContactFirstName(normalizeRequiredField(req.contactFirstName, "contactFirstName", "contactFirstName is required"));
        a.setContactLastName(normalizeRequiredField(req.contactLastName, "contactLastName", "contactLastName is required"));
        a.setContactTitle(normalizeOptional(req.contactTitle));
        a.setContactEmail(normalizeRequiredField(req.contactEmail, "contactEmail", "contactEmail is required"));
        a.setContactPhone(normalizeOptional(req.contactPhone));

        a.setDraftStep(ApplicationRaiseDraftStep.TYPE);

        repo.save(a);
        return toResponse(a);
    }

    @Transactional
    public ApplicationRaiseResponse saveTypeStep(Long id, ApplicationRaiseTypeStepRequest req) {
        if (req == null || req.type == null) {
            throw fieldError("type", "type is required");
        }

        ApplicationRaise a = requireDraftOwner(id);

        if (hasAnotherActiveApplicationOfType(a.getOwnerUserId(), req.type, a.getId())) {
            throw new IllegalStateException(
                    "You already have an active " + req.type + " application. Only one active application per type is allowed."
            );
        }

        a.setType(req.type);
        a.setDraftStep(ApplicationRaiseDraftStep.DETAILS);

        repo.save(a);
        return toResponse(a);
    }

    @Transactional
    public ApplicationRaiseResponse saveDetailsStep(Long id, ApplicationRaiseDetailsStepRequest req) {
        ApplicationRaise a = requireDraftOwner(id);

        if (a.getType() == null) {
            throw new IllegalStateException("Choose application type before saving details.");
        }

        String businessName = normalizeRequiredField(req.businessName, "businessName", "businessName is required");
        String website = normalizeOptional(req.website);

        validateTaxonomy(req.sector, req.subSector, "sector", "subSector");
        Set<AppTag> tags = normalizeRequiredTags(req.tags, "tags");


        assertUniqueBusinessName(a.getOwnerUserId(), businessName, "businessName", a.getId());


        a.setBusinessName(businessName);

        a.setWebsite(website);

        a.setSector(req.sector);
        a.setSubSector(req.subSector);
        a.setTags(tags);
        a.setSummary(req.summary);
        a.setProblemStatement(req.problemStatement);
        a.setSolution(req.solution);
        a.setTargetCustomers(req.targetCustomers);
        a.setUseOfFunds(req.useOfFunds);

        a.setFundingGoal(req.fundingGoal);
        a.setCustomerCount(req.customerCount);
        a.setStage(req.stage);
        a.setTeamSize(req.teamSize);
        a.setGovernorate(normalizeOptional(req.governorate));
        a.setCity(normalizeOptional(req.city));
        a.setAcceptedTerms(req.acceptedTerms);

        if (a.getType() == CrowdfundingType.EQUITY) {
            if (req.equityDetail == null) {
                throw fieldError("equityDetail", "equityDetail is required for EQUITY crowdfunding.");
            }

            assertUniqueEquityRegistrationNumber(
                    req.equityDetail.companyRegistrationNumber,
                    "equityDetail.companyRegistrationNumber",
                    a.getId()
            );

            equityDetailService.upsert(a.getId(), req.equityDetail);
        } else {
            equityRepo.findByApplicationRaiseId(a.getId()).ifPresent(equityRepo::delete);
        }

        a.setDraftStep(ApplicationRaiseDraftStep.DOCUMENTS);

        repo.save(a);
        return getById(a.getId());
    }

    @Transactional
    public ApplicationRaiseResponse submitDraft(Long id) {
        ApplicationRaise a = requireDraftOwner(id);

        if (a.getType() == null) {
            throw new IllegalStateException("Application type must be selected before submission.");
        }

        String businessName = normalizeRequiredField(a.getBusinessName(), "businessName", "businessName is required");

        validateTaxonomy(a.getSector(), a.getSubSector(), "sector", "subSector");
        normalizeRequiredTags(a.getTags(), "tags");

        if (!a.isAcceptedTerms()) {
            throw new IllegalStateException("Terms must be accepted before submission.");
        }

        if (a.getFundingGoal() == null || a.getFundingGoal().compareTo(new BigDecimal("500.000")) < 0) {
            throw fieldError("fundingGoal", "fundingGoal must be at least 500");
        }

        normalizeRequiredField(a.getContactFirstName(), "contactFirstName", "contactFirstName is required");
        normalizeRequiredField(a.getContactLastName(), "contactLastName", "contactLastName is required");
        normalizeRequiredField(a.getContactEmail(), "contactEmail", "contactEmail is required");
        normalizeRequiredField(a.getSummary(), "summary", "summary is required");
        normalizeRequiredField(a.getProblemStatement(), "problemStatement", "problemStatement is required");
        normalizeRequiredField(a.getSolution(), "solution", "solution is required");
        normalizeRequiredField(a.getTargetCustomers(), "targetCustomers", "targetCustomers is required");
        normalizeRequiredField(a.getUseOfFunds(), "useOfFunds", "useOfFunds is required");

        if (a.getStage() == null) {
            throw fieldError("stage", "stage is required");
        }

        if (a.getTeamSize() == null || a.getTeamSize() < 1) {
            throw fieldError("teamSize", "teamSize is required and must be at least 1");
        }

        normalizeRequiredField(a.getGovernorate(), "governorate", "governorate is required");
        normalizeRequiredField(a.getCity(), "city", "city is required");

        assertUniqueBusinessName(a.getOwnerUserId(), businessName, "businessName", a.getId());


        if (a.getType() == CrowdfundingType.EQUITY) {


            equityRepo.findByApplicationRaiseId(a.getId())
                    .orElseThrow(() -> new IllegalStateException("Equity details are required before submission."));

            documentService.assertRequiredDocsCompleteOrThrow(a.getId());
        }

        documentService.assertRequiredDocsCompleteOrThrow(a.getId());

        a.setStatus(ApplicationRaiseStatus.SUBMITTED);
        a.setSubmittedAt(Instant.now());
        repo.save(a);

        return toResponse(a);
    }

    private Sort buildSort(ApplicationRaiseSearchCriteria criteria, String defaultField, Sort.Direction defaultDirection) {
        String requestedSortBy = criteria != null ? normalizeOptional(criteria.getSortBy()) : null;
        String requestedSortDir = criteria != null ? normalizeOptional(criteria.getSortDir()) : null;

        String sortBy = requestedSortBy != null ? requestedSortBy : defaultField;
        Sort.Direction direction = defaultDirection;
        if (requestedSortDir != null) {
            direction = "asc".equalsIgnoreCase(requestedSortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        }

        String property = switch (sortBy) {
            case "id" -> "id";
            case "ownerUserId" -> "ownerUserId";
            case "businessName" -> "businessName";
            case "website" -> "website";
            case "country" -> "country";
            case "currency" -> "currency";
            case "sector" -> "sector";
            case "subSector" -> "subSector";
            case "summary" -> "summary";
            case "fundingGoal" -> "fundingGoal";
            case "investorsPledgedAmount", "raisedAmount" -> "investorsPledgedAmount";
            case "customerCount" -> "customerCount";
            case "contactFirstName" -> "contactFirstName";
            case "contactLastName" -> "contactLastName";
            case "contactEmail" -> "contactEmail";
            case "status" -> "status";
            case "type" -> "type";
            case "createdAt", "newest", "oldest" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            case "companyLegalName" -> "businessName";
            case "companyRegistrationNumber" -> "businessName";
            case "equityOfferedPercent" -> "createdAt";
            case "preMoneyValuation" -> "createdAt";
            case "stage" -> "stage";
            case "teamSize" -> "teamSize";
            case "governorate" -> "governorate";
            case "city" -> "city";
            case "problemStatement" -> "problemStatement";
            case "solution" -> "solution";
            case "targetCustomers" -> "targetCustomers";
            case "useOfFunds" -> "useOfFunds";
            case "minInvestment" -> "createdAt";
            default -> defaultField;
        };

        if ("newest".equalsIgnoreCase(sortBy)) {
            direction = Sort.Direction.DESC;
        } else if ("oldest".equalsIgnoreCase(sortBy)) {
            direction = Sort.Direction.ASC;
        }

        return Sort.by(direction, property);
    }

    private ApplicationRaiseResponse toResponse(ApplicationRaise a) {
        ApplicationRaiseResponse r = new ApplicationRaiseResponse();
        r.id = a.getId();
        r.ownerUserId = a.getOwnerUserId();
        r.type = a.getType();

        r.businessName = a.getBusinessName();
        r.website = a.getWebsite();

        r.country = a.getCountry();
        r.currency = a.getCurrency();

        r.sector = a.getSector();
        r.subSector = a.getSubSector();
        r.tags = new LinkedHashSet<>(a.getTags());
        r.stage = a.getStage();
        r.summary = a.getSummary();
        r.problemStatement = a.getProblemStatement();
        r.solution = a.getSolution();
        r.targetCustomers = a.getTargetCustomers();
        r.useOfFunds = a.getUseOfFunds();

        r.fundingGoal = moneyOrZero(a.getFundingGoal());
        r.investorsPledgedAmount = moneyOrZero(a.getInvestorsPledgedAmount());
        r.raisedAmount = r.investorsPledgedAmount;
        r.remainingAmount = r.fundingGoal.subtract(r.raisedAmount).max(BigDecimal.ZERO);
        r.fundingProgressPercent = percent(r.raisedAmount, r.fundingGoal);

        r.customerCount = a.getCustomerCount();
        r.teamSize = a.getTeamSize();
        r.governorate = a.getGovernorate();
        r.city = a.getCity();

        r.contactFirstName = a.getContactFirstName();
        r.contactLastName = a.getContactLastName();
        r.contactTitle = a.getContactTitle();
        r.contactEmail = a.getContactEmail();
        r.contactPhone = a.getContactPhone();

        r.useProfileContact = a.isUseProfileContact();
        r.acceptedTerms = a.isAcceptedTerms();
        r.status = a.getStatus();
        r.draftStep = a.getDraftStep();

        if (a.getType() == CrowdfundingType.EQUITY) {
            equityRepo.findByApplicationRaiseId(a.getId())
                    .ifPresent(ed -> r.equityDetail = equityDetailService.toDto(ed));
        }

        r.documents = documentService.listDocumentResponses(a.getId());
        r.documentCompletionPercent = documentService.documentCompletionPercent(a.getId());
        r.applicationCompletionPercent = calculateApplicationCompletionPercent(a, r.documentCompletionPercent);

        r.createdAt = a.getCreatedAt();
        r.updatedAt = a.getUpdatedAt();
        r.submittedAt = a.getSubmittedAt();
        return r;
    }

    private BigDecimal moneyOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return numerator
                .multiply(new BigDecimal("100"))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private int calculateApplicationCompletionPercent(ApplicationRaise a, int documentCompletionPercent) {
        int score = 0;

        if (a.getContactFirstName() != null && a.getContactLastName() != null && a.getContactEmail() != null) {
            score += 20;
        }
        if (a.getType() != null) {
            score += 10;
        }
        if (a.getBusinessName() != null
                && a.getSector() != null
                && a.getSubSector() != null
                && a.getSummary() != null
                && a.getProblemStatement() != null
                && a.getSolution() != null
                && a.getTargetCustomers() != null
                && a.getUseOfFunds() != null
                && a.getFundingGoal() != null
                && a.getFundingGoal().compareTo(BigDecimal.ZERO) > 0
                && a.getStage() != null) {
            score += 30;
        }
        score += Math.round(documentCompletionPercent * 0.30f);

        if (a.isAcceptedTerms()) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    private void validateTaxonomy(Sector sector, SubSector subSector, String sectorField, String subSectorField) {
        if (sector == null) {
            throw fieldError(sectorField, "sector is required");
        }
        if (subSector == null) {
            throw fieldError(subSectorField, "subSector is required");
        }

        boolean valid = switch (sector) {
            case TECHNOLOGY -> EnumSet.of(
                    SubSector.AI,
                    SubSector.SAAS,
                    SubSector.CYBERSECURITY,
                    SubSector.DATA_ANALYTICS
            ).contains(subSector);

            case FINTECH_FINANCIAL_SERVICES -> EnumSet.of(
                    SubSector.PAYMENTS,
                    SubSector.EMBEDDED_FINANCE,
                    SubSector.ACCOUNTING_FINANCE_SOFTWARE
            ).contains(subSector);

            case HEALTH_BIO -> EnumSet.of(
                    SubSector.DIGITAL_HEALTH,
                    SubSector.WELLNESS,
                    SubSector.MENTAL_HEALTH
            ).contains(subSector);

            case CONSUMER_PRODUCTS -> EnumSet.of(
                    SubSector.FOOD_BEVERAGE,
                    SubSector.FASHION_APPAREL,
                    SubSector.BEAUTY_PERSONAL_CARE
            ).contains(subSector);

            case COMMERCE_MARKETPLACES -> EnumSet.of(
                    SubSector.D2C_BRANDS,
                    SubSector.ONLINE_MARKETPLACE,
                    SubSector.ECOMMERCE_ENABLEMENT
            ).contains(subSector);

            case CLIMATE_ENERGY -> EnumSet.of(
                    SubSector.CLEAN_ENERGY,
                    SubSector.CLIMATE_SOFTWARE,
                    SubSector.SUSTAINABLE_MATERIALS
            ).contains(subSector);

            case REAL_ESTATE_BUILT_ENVIRONMENT -> EnumSet.of(
                    SubSector.PROPTECH
            ).contains(subSector);

            case MOBILITY_INDUSTRY_LOGISTICS -> EnumSet.of(
                    SubSector.LOGISTICSTECH,
                    SubSector.INDUSTRIAL_AUTOMATION
            ).contains(subSector);

            case EDUCATION_WORK -> EnumSet.of(
                    SubSector.FUTURE_OF_WORK,
                    SubSector.PRODUCTIVITY_TOOLS
            ).contains(subSector);

            case MEDIA_COMMUNITY_LEISURE -> EnumSet.of(
                    SubSector.GAMING,
                    SubSector.CREATOR_ECONOMY
            ).contains(subSector);
        };

        if (!valid) {
            throw fieldError(
                    subSectorField,
                    "subSector " + subSector + " is not valid for sector " + sector
            );
        }
    }

    private Set<AppTag> normalizeRequiredTags(Set<AppTag> tags, String fieldName) {
        if (tags == null || tags.isEmpty()) {
            throw fieldError(fieldName, "At least one tag is required.");
        }

        LinkedHashSet<AppTag> normalized = new LinkedHashSet<>();
        for (AppTag tag : tags) {
            if (tag == null) {
                throw fieldError(fieldName, "tags cannot contain null values.");
            }
            normalized.add(tag);
        }

        if (normalized.isEmpty()) {
            throw fieldError(fieldName, "At least one tag is required.");
        }

        if (normalized.size() > 3) {
            throw fieldError(fieldName, "You can choose at most 3 tags.");
        }

        return normalized;
    }

    private User currentUser() {
        return currentUserService.getCurrentUser();
    }

    private void requireYouthBeneficiary(User u) {
        if (u.getRole() != Role.YOUTH_BENEFICIARY) {
            throw new UnauthorizedException("Only YOUTH_BENEFICIARY can perform this action.");
        }
    }

    private void requireAdminOrCompliance(User u) {
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.COMPLIANCE) {
            throw new UnauthorizedException("Only ADMIN or COMPLIANCE can perform this action.");
        }
    }

    private boolean isAdminOrCompliance(User u) {
        return u.getRole() == Role.ADMIN || u.getRole() == Role.COMPLIANCE;
    }

    private ApplicationRaise requireDraftOwner(Long id) {
        User me = currentUser();
        requireYouthBeneficiary(me);

        ApplicationRaise a = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + id));

        if (!a.getOwnerUserId().equals(me.getId())) {
            throw new UnauthorizedException("You can only modify your own application.");
        }
        if (a.getStatus() != ApplicationRaiseStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT applications can be changed.");
        }
        return a;
    }

    private String normalizeRequiredField(String value, String fieldName, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw fieldError(fieldName, message);
        }
        return value.trim();
    }

    private void assertUniqueBusinessName(Long ownerUserId, String businessName, String fieldName, Long currentApplicationId) {
        boolean duplicate = currentApplicationId == null
                ? repo.existsByOwnerUserIdAndBusinessNameIgnoreCase(ownerUserId, businessName)
                : repo.existsByOwnerUserIdAndBusinessNameIgnoreCase(ownerUserId, businessName)
                  && repo.findById(currentApplicationId)
                     .map(existing -> existing.getBusinessName() == null || !existing.getBusinessName().equalsIgnoreCase(businessName))
                     .orElse(true);

        if (duplicate) {
            throw fieldError(fieldName, "You already have an application with the same businessName.");
        }
    }


    private void assertUniqueEquityRegistrationNumber(String registrationNumber, String fieldName, Long currentApplicationId) {
        String normalized = normalizeOptional(registrationNumber);
        if (normalized == null) {
            return;
        }

        boolean duplicate = currentApplicationId == null
                ? equityRepo.existsByCompanyRegistrationNumberIgnoreCase(normalized)
                : equityRepo.existsByCompanyRegistrationNumberIgnoreCaseAndApplicationRaiseIdNot(normalized, currentApplicationId);

        if (duplicate) {
            throw fieldError(fieldName, "companyRegistrationNumber already exists in another equity application.");
        }
    }

    private boolean hasAnotherActiveApplicationOfType(Long ownerUserId, CrowdfundingType type, Long currentId) {
        return repo.findAll().stream()
                .anyMatch(a ->
                        Objects.equals(a.getOwnerUserId(), ownerUserId)
                                && a.getType() == type
                                && !Objects.equals(a.getId(), currentId)
                                && isActiveApplicationStatus(a.getStatus())
                );
    }

    private boolean isActiveApplicationStatus(ApplicationRaiseStatus status) {
        return status == ApplicationRaiseStatus.DRAFT
                || status == ApplicationRaiseStatus.SUBMITTED
                || status == ApplicationRaiseStatus.UNDER_REVIEW
                || status == ApplicationRaiseStatus.APPROVED;
    }

    private FieldValidationException fieldError(String fieldName, String message) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(fieldName, message);
        return new FieldValidationException("Validation failed", fieldErrors);
    }

    private String normalizeOptional(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}