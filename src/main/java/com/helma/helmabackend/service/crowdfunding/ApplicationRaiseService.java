package com.helma.helmabackend.service.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.ApplicationRaiseCreateRequest;
import com.helma.helmabackend.dto.crowdfunding.ApplicationRaiseResponse;
import com.helma.helmabackend.dto.crowdfunding.ApplicationRaiseSearchCriteria;
import com.helma.helmabackend.dto.crowdfunding.ApplicationRaiseStatusPatchRequest;
import com.helma.helmabackend.dto.crowdfunding.EquityApplicationCreateRequest;
import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
        String companyNumber = normalizeRequiredField(
                req.application.companyNumber,
                "application.companyNumber",
                "companyNumber is required for EQUITY"
        );
        String website = normalizeOptional(req.application.website);

        validateTaxonomy(req.application.sector, req.application.subSector, "application.sector", "application.subSector");
        Set<AppTag> tags = normalizeRequiredTags(req.application.tags, "application.tags");
        assertUniqueBusinessName(me.getId(), businessName, "application.businessName", null);
        assertUniqueCompanyNumber(companyNumber, "application.companyNumber", null);
        assertUniqueEquityRegistrationNumber(
                req.equityDetail.companyRegistrationNumber,
                "equityDetail.companyRegistrationNumber",
                null
        );

        ApplicationRaise a = new ApplicationRaise();
        a.setOwnerUserId(me.getId());
        a.setType(CrowdfundingType.EQUITY);

        a.setBusinessName(businessName);
        a.setCompanyNumber(companyNumber);
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

        a.setAcceptedTerms(req.application.acceptedTerms);
        a.setStatus(ApplicationRaiseStatus.DRAFT);

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
        String companyNumber = normalizeRequiredField(
                req.application.companyNumber,
                "application.companyNumber",
                "companyNumber is required for EQUITY"
        );
        String website = normalizeOptional(req.application.website);

        validateTaxonomy(req.application.sector, req.application.subSector, "application.sector", "application.subSector");
        Set<AppTag> tags = normalizeRequiredTags(req.application.tags, "application.tags");
        assertUniqueBusinessName(me.getId(), businessName, "application.businessName", id);
        assertUniqueCompanyNumber(companyNumber, "application.companyNumber", id);
        assertUniqueEquityRegistrationNumber(
                req.equityDetail.companyRegistrationNumber,
                "equityDetail.companyRegistrationNumber",
                id
        );

        a.setBusinessName(businessName);
        a.setCompanyNumber(companyNumber);
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

        a.setAcceptedTerms(req.application.acceptedTerms);

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
        String companyNumber = normalizeOptional(req.companyNumber);
        String website = normalizeOptional(req.website);

        validateTaxonomy(req.sector, req.subSector, "sector", "subSector");
        Set<AppTag> tags = normalizeRequiredTags(req.tags, "tags");

        if (req.type == CrowdfundingType.EQUITY && companyNumber == null) {
            throw fieldError("companyNumber", "companyNumber is required for EQUITY crowdfunding.");
        }

        assertUniqueBusinessName(me.getId(), businessName, "businessName", null);
        assertUniqueCompanyNumber(companyNumber, "companyNumber", null);

        ApplicationRaise a = new ApplicationRaise();
        a.setOwnerUserId(me.getId());
        a.setType(req.type);

        a.setBusinessName(businessName);
        a.setCompanyNumber(companyNumber);
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

        a.setAcceptedTerms(req.acceptedTerms);
        a.setStatus(ApplicationRaiseStatus.DRAFT);

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
        String companyNumber = normalizeOptional(req.companyNumber);
        String website = normalizeOptional(req.website);

        validateTaxonomy(req.sector, req.subSector, "sector", "subSector");
        Set<AppTag> tags = normalizeRequiredTags(req.tags, "tags");

        if (req.type == CrowdfundingType.EQUITY && companyNumber == null) {
            throw fieldError("companyNumber", "companyNumber is required for EQUITY crowdfunding.");
        }

        assertUniqueBusinessName(a.getOwnerUserId(), businessName, "businessName", a.getId());
        assertUniqueCompanyNumber(companyNumber, "companyNumber", a.getId());

        a.setType(req.type);
        a.setBusinessName(businessName);
        a.setCompanyNumber(companyNumber);
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

        a.setAcceptedTerms(req.acceptedTerms);

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
    public List<ApplicationRaiseResponse> adminListAll(ApplicationRaiseSearchCriteria criteria) {
        User me = currentUser();
        requireAdminOrCompliance(me);

        ApplicationRaiseSearchCriteria effectiveCriteria = criteria != null ? criteria : new ApplicationRaiseSearchCriteria();
        Specification<ApplicationRaise> specification = ApplicationRaiseSpecifications.byCriteria(effectiveCriteria)
                .and(ApplicationRaiseSpecifications.statusNot(ApplicationRaiseStatus.DRAFT));
        Sort sort = buildSort(effectiveCriteria, "createdAt", Sort.Direction.DESC);

        return repo.findAll(specification, sort).stream()
                .map(this::toResponse)
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

        documentService.assertEquityDocsCompleteOrThrow(a.getId());

        a.setStatus(ApplicationRaiseStatus.SUBMITTED);
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
            case "companyNumber" -> "companyNumber";
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
            case "companyLegalName" -> "equityDetail.companyLegalName";
            case "companyRegistrationNumber" -> "equityDetail.companyRegistrationNumber";
            case "equityOfferedPercent" -> "equityDetail.equityOfferedPercent";
            case "preMoneyValuation" -> "equityDetail.preMoneyValuation";
            case "minInvestment" -> "equityDetail.minInvestment";
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
        r.companyNumber = a.getCompanyNumber();
        r.website = a.getWebsite();

        r.country = a.getCountry();
        r.currency = a.getCurrency();

        r.sector = a.getSector();
        r.subSector = a.getSubSector();
        r.tags = new LinkedHashSet<>(a.getTags());
        r.summary = a.getSummary();

        r.fundingGoal = a.getFundingGoal();
        r.investorsPledgedAmount = a.getInvestorsPledgedAmount();
        r.customerCount = a.getCustomerCount();

        r.contactFirstName = a.getContactFirstName();
        r.contactLastName = a.getContactLastName();
        r.contactTitle = a.getContactTitle();
        r.contactEmail = a.getContactEmail();
        r.contactPhone = a.getContactPhone();

        r.acceptedTerms = a.isAcceptedTerms();
        r.status = a.getStatus();

        if (a.getType() == CrowdfundingType.EQUITY) {
            equityRepo.findByApplicationRaiseId(a.getId())
                    .ifPresent(ed -> r.equityDetail = equityDetailService.toDto(ed));
        }

        r.documents = documentService.listDocumentResponses(a.getId());
        r.createdAt = a.getCreatedAt();
        r.updatedAt = a.getUpdatedAt();
        return r;
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

    private void assertUniqueCompanyNumber(String companyNumber, String fieldName, Long currentApplicationId) {
        if (companyNumber == null) {
            return;
        }

        boolean duplicate = currentApplicationId == null
                ? repo.existsByCompanyNumberIgnoreCase(companyNumber)
                : repo.existsByCompanyNumberIgnoreCaseAndIdNot(companyNumber, currentApplicationId);

        if (duplicate) {
            throw fieldError(fieldName, "companyNumber already exists.");
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