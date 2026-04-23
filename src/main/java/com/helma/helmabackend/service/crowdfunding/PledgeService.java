package com.helma.helmabackend.service.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.ApplicationRaiseSearchCriteria;
import com.helma.helmabackend.dto.crowdfunding.CampaignResponse;
import com.helma.helmabackend.dto.crowdfunding.PledgeCreateRequest;
import com.helma.helmabackend.dto.crowdfunding.PledgeResponse;
import com.helma.helmabackend.dto.crowdfunding.PledgeStatusPatchRequest;
import com.helma.helmabackend.dto.crowdfunding.ai.AiCampaignInsightBatchResponse;
import com.helma.helmabackend.dto.crowdfunding.ai.AiCampaignInsightRequest;
import com.helma.helmabackend.dto.crowdfunding.ai.AiCampaignInsightResponse;
import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.EquityDetail;
import com.helma.helmabackend.entity.crowdfunding.Pledge;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.PledgeStatus;
import com.helma.helmabackend.entity.user.Role;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.exception.UnauthorizedException;
import com.helma.helmabackend.repository.crowdfunding.ApplicationRaiseRepository;
import com.helma.helmabackend.repository.crowdfunding.ApplicationRaiseSpecifications;
import com.helma.helmabackend.repository.crowdfunding.EquityDetailRepository;
import com.helma.helmabackend.repository.crowdfunding.PledgeRepository;
import com.helma.helmabackend.service.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PledgeService {

    private final PledgeRepository pledgeRepo;
    private final ApplicationRaiseRepository applicationRaiseRepo;
    private final EquityDetailRepository equityDetailRepo;
    private final CurrentUserService currentUserService;
    private final CampaignFundingService campaignFundingService;
    private final PaymentService paymentService;
    private final AiModelClientService aiModelClientService;

    @Transactional(readOnly = true)
    public List<CampaignResponse> listApprovedCampaigns(String sort, String sortDir, ApplicationRaiseSearchCriteria criteria) {
        ApplicationRaiseSearchCriteria effectiveCriteria = criteria != null ? criteria : new ApplicationRaiseSearchCriteria();
        effectiveCriteria.setStatus(ApplicationRaiseStatus.APPROVED);

        Specification<ApplicationRaise> specification =
                ApplicationRaiseSpecifications.byCriteria(effectiveCriteria)
                        .and(ApplicationRaiseSpecifications.hasStatus(ApplicationRaiseStatus.APPROVED));

        List<ApplicationRaise> campaigns;
        if (!"trending".equalsIgnoreCase(sort)) {
            campaigns = applicationRaiseRepo.findAll(specification, buildCampaignSort(sort, sortDir));
            return campaigns.stream()
                    .map(a -> toCampaignResponse(a, false))
                    .toList();
        }

        campaigns = applicationRaiseRepo.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<AiCampaignInsightRequest> aiRequests = new ArrayList<>();
        Map<Long, CampaignResponse> responseById = new HashMap<>();

        for (ApplicationRaise a : campaigns) {
            CampaignResponse r = toCampaignResponse(a, false);
            responseById.put(a.getId(), r);

            AiCampaignInsightRequest aiReq = new AiCampaignInsightRequest();
            aiReq.campaignId = a.getId();
            aiReq.businessName = a.getBusinessName();
            aiReq.type = a.getType();
            aiReq.sector = a.getSector();
            aiReq.subSector = a.getSubSector();
            aiReq.tags = new LinkedHashSet<>(a.getTags());
            aiReq.summary = a.getSummary();
            aiReq.fundingGoal = a.getFundingGoal();
            aiReq.investorsPledgedAmount = a.getInvestorsPledgedAmount();
            aiReq.currency = a.getCurrency() != null ? a.getCurrency().name() : null;

            if (a.getType() == CrowdfundingType.EQUITY) {
                equityDetailRepo.findByApplicationRaiseId(a.getId()).ifPresent(ed -> {
                    aiReq.equityOfferedPercent = ed.getEquityOfferedPercent();
                    aiReq.preMoneyValuation = ed.getPreMoneyValuation();
                    aiReq.minInvestment = ed.getMinInvestment();
                });
            }

            aiRequests.add(aiReq);
        }

        Map<Long, AiCampaignInsightResponse> aiByCampaignId = new HashMap<>();
        try {
            AiCampaignInsightBatchResponse batchResponse = aiModelClientService.getCampaignInsightsBatch(aiRequests);
            if (batchResponse != null && batchResponse.items != null) {
                for (AiCampaignInsightResponse item : batchResponse.items) {
                    if (item != null && item.campaignId != null) {
                        aiByCampaignId.put(item.campaignId, item);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        for (CampaignResponse r : responseById.values()) {
            AiCampaignInsightResponse ai = aiByCampaignId.get(r.id);
            r.trendScore = ai != null ? ai.trendAlignmentScore : null;
            r.trendLabel = ai != null ? ai.overallLabel : null;
        }

        List<CampaignResponse> results = new ArrayList<>(responseById.values());

        results.sort((a, b) -> {
            double scoreA = extractTrendingScore(aiByCampaignId.get(a.id));
            double scoreB = extractTrendingScore(aiByCampaignId.get(b.id));

            int cmp = Double.compare(scoreB, scoreA);
            if (cmp != 0) {
                return cmp;
            }

            return b.createdAt.compareTo(a.createdAt);
        });

        return results;
    }

    private Sort buildCampaignSort(String sort, String sortDir) {
        String normalizedSort = normalizeOptional(sort);
        String normalizedSortDir = normalizeOptional(sortDir);

        if (normalizedSort == null || "newest".equalsIgnoreCase(normalizedSort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("oldest".equalsIgnoreCase(normalizedSort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(normalizedSortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        String property = switch (normalizedSort) {
            case "id" -> "id";
            case "businessName" -> "businessName";
            case "website" -> "website";
            case "sector" -> "sector";
            case "subSector" -> "subSector";
            case "type" -> "type";
            case "summary" -> "summary";
            case "fundingGoal" -> "fundingGoal";
            case "investorsPledgedAmount", "raisedAmount" -> "investorsPledgedAmount";
            case "currency" -> "currency";
            case "updatedAt" -> "updatedAt";
            case "equityOfferedPercent" -> "equityDetail.equityOfferedPercent";
            case "preMoneyValuation" -> "equityDetail.preMoneyValuation";
            case "minInvestment" -> "equityDetail.minInvestment";
            default -> "createdAt";
        };

        return Sort.by(direction, property);
    }

    private double extractTrendingScore(AiCampaignInsightResponse ai) {
        if (ai == null || ai.trendAlignmentScore == null) {
            return -1.0;
        }
        return ai.trendAlignmentScore.doubleValue();
    }

    @Transactional(readOnly = true)
    public CampaignResponse getApprovedCampaign(Long id) {
        ApplicationRaise campaign = requireApprovedCampaign(id);
        return toCampaignResponse(campaign, true);
    }

    @Transactional
    public PledgeResponse createOrUpdateMyPledge(Long campaignId, PledgeCreateRequest req) {
        User me = currentUser();
        requireInvestor(me);

        if (req == null || req.amount == null) {
            throw new IllegalArgumentException("amount is required");
        }

        ApplicationRaise campaign = requireApprovedCampaign(campaignId);

        if (campaign.getType() == CrowdfundingType.EQUITY) {
            EquityDetail equity = equityDetailRepo.findByApplicationRaiseId(campaignId)
                    .orElseThrow(() -> new IllegalStateException("Equity campaign is missing equity detail."));

            if (equity.getMinInvestment() != null && req.amount.compareTo(equity.getMinInvestment()) < 0) {
                throw new IllegalStateException(
                        "Amount must be at least the minimum investment: "
                                + equity.getMinInvestment() + " "
                                + (campaign.getCurrency() != null ? campaign.getCurrency().name() : "TND")
                );
            }
        }

        Optional<Pledge> existingOpt = pledgeRepo.findByApplicationRaiseIdAndBackerUserId(campaignId, me.getId());

        Pledge pledge = existingOpt.orElseGet(Pledge::new);
        boolean updatingExisting = pledge.getId() != null;
        if (pledge.getId() == null) {
            pledge.setApplicationRaiseId(campaignId);
            pledge.setBackerUserId(me.getId());
        } else if (pledge.getStatus() == PledgeStatus.PAID) {
            throw new IllegalStateException("You already have a paid pledge for this campaign.");
        }

        if (updatingExisting) {
            paymentService.cancelActivePaymentsForPledge(pledge.getId(), "Pledge updated before payment completion.");
        }

        pledge.setAmount(req.amount);
        pledge.setCurrency(campaign.getCurrency() != null ? campaign.getCurrency().name() : "TND");
        pledge.setMessage(req.message);
        pledge.setStatus(PledgeStatus.PENDING);

        Pledge saved = pledgeRepo.save(pledge);
        return toPledgeResponse(saved, campaign);
    }

    @Transactional(readOnly = true)
    public List<PledgeResponse> listMyPledges() {
        User me = currentUser();
        requireInvestor(me);

        return pledgeRepo.findByBackerUserIdOrderByCreatedAtDesc(me.getId())
                .stream()
                .map(this::toPledgeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PledgeResponse getMyPledgeById(Long pledgeId) {
        User me = currentUser();
        requireInvestor(me);

        Pledge pledge = pledgeRepo.findById(pledgeId)
                .orElseThrow(() -> new IllegalArgumentException("Pledge not found: " + pledgeId));

        if (!pledge.getBackerUserId().equals(me.getId())) {
            throw new UnauthorizedException("You can only view your own pledges.");
        }

        return toPledgeResponse(pledge);
    }

    @Transactional(readOnly = true)
    public List<PledgeResponse> adminListAllPledges() {
        User me = currentUser();
        requireAdminOrCompliance(me);

        return pledgeRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toPledgeResponse)
                .toList();
    }

    @Transactional
    public PledgeResponse adminPatchPledgeStatus(Long pledgeId, PledgeStatusPatchRequest req) {
        User me = currentUser();
        requireAdminOrCompliance(me);

        if (req == null || req.status == null) {
            throw new IllegalArgumentException("status is required");
        }

        Pledge pledge = pledgeRepo.findById(pledgeId)
                .orElseThrow(() -> new IllegalArgumentException("Pledge not found: " + pledgeId));

        PledgeStatus oldStatus = pledge.getStatus();
        PledgeStatus target = req.status;

        if (oldStatus == target) {
            return toPledgeResponse(pledge);
        }

        boolean allowed =
                (oldStatus == PledgeStatus.PENDING &&
                        (target == PledgeStatus.PAID || target == PledgeStatus.FAILED || target == PledgeStatus.CANCELED))
                        ||
                        ((oldStatus == PledgeStatus.FAILED || oldStatus == PledgeStatus.CANCELED || oldStatus == PledgeStatus.REFUNDED)
                                && target == PledgeStatus.PENDING)
                        ||
                        (oldStatus == PledgeStatus.PAID && target == PledgeStatus.REFUNDED);

        if (!allowed) {
            throw new IllegalStateException("Invalid pledge status transition: " + oldStatus + " -> " + target);
        }

        pledge.setStatus(target);
        Pledge saved = pledgeRepo.save(pledge);
        campaignFundingService.syncCampaignRaisedAmount(saved.getApplicationRaiseId());

        return toPledgeResponse(saved);
    }

    private CampaignResponse toCampaignResponse(ApplicationRaise a, boolean includeAi) {
        CampaignResponse r = new CampaignResponse();
        r.id = a.getId();
        r.type = a.getType();
        r.businessName = a.getBusinessName();
        r.website = a.getWebsite();
        r.sector = a.getSector();
        r.subSector = a.getSubSector();
        r.tags = new LinkedHashSet<>(a.getTags());
        r.summary = a.getSummary();
        r.fundingGoal = a.getFundingGoal();
        r.investorsPledgedAmount = a.getInvestorsPledgedAmount();
        r.currency = a.getCurrency() != null ? a.getCurrency().name() : null;
        r.createdAt = a.getCreatedAt();
        r.updatedAt = a.getUpdatedAt();

        if (a.getType() == CrowdfundingType.EQUITY) {
            equityDetailRepo.findByApplicationRaiseId(a.getId()).ifPresent(ed -> {
                r.equityOfferedPercent = ed.getEquityOfferedPercent();
                r.preMoneyValuation = ed.getPreMoneyValuation();
                r.minInvestment = ed.getMinInvestment();
            });
        }

        if (includeAi) {
            AiCampaignInsightRequest aiReq = new AiCampaignInsightRequest();
            aiReq.campaignId = a.getId();
            aiReq.businessName = a.getBusinessName();
            aiReq.type = a.getType();
            aiReq.sector = a.getSector();
            aiReq.subSector = a.getSubSector();
            aiReq.tags = new LinkedHashSet<>(a.getTags());
            aiReq.summary = a.getSummary();
            aiReq.fundingGoal = a.getFundingGoal();
            aiReq.investorsPledgedAmount = a.getInvestorsPledgedAmount();
            aiReq.currency = a.getCurrency() != null ? a.getCurrency().name() : null;
            aiReq.equityOfferedPercent = r.equityOfferedPercent;
            aiReq.preMoneyValuation = r.preMoneyValuation;
            aiReq.minInvestment = r.minInvestment;

            AiCampaignInsightResponse aiResponse = aiModelClientService.getCampaignInsightSafe(aiReq);
            r.aiInsights = aiResponse;
            r.aiUnavailable = (aiResponse == null);
        }

        return r;
    }

    private PledgeResponse toPledgeResponse(Pledge pledge) {
        ApplicationRaise campaign = applicationRaiseRepo.findById(pledge.getApplicationRaiseId())
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + pledge.getApplicationRaiseId()));
        return toPledgeResponse(pledge, campaign);
    }

    private PledgeResponse toPledgeResponse(Pledge pledge, ApplicationRaise campaign) {
        PledgeResponse r = new PledgeResponse();
        r.id = pledge.getId();
        r.applicationRaiseId = pledge.getApplicationRaiseId();
        r.backerUserId = pledge.getBackerUserId();
        r.amount = pledge.getAmount();
        r.currency = pledge.getCurrency();
        r.message = pledge.getMessage();
        r.status = pledge.getStatus();
        r.campaignBusinessName = campaign.getBusinessName();
        r.campaignType = campaign.getType();
        r.campaignFundingGoal = campaign.getFundingGoal();
        r.campaignInvestorsPledgedAmount = campaign.getInvestorsPledgedAmount();

        if (campaign.getType() == CrowdfundingType.EQUITY) {
            equityDetailRepo.findByApplicationRaiseId(campaign.getId())
                    .map(EquityDetail::getMinInvestment)
                    .ifPresent(v -> r.campaignMinInvestment = v);
        }

        r.createdAt = pledge.getCreatedAt();
        r.updatedAt = pledge.getUpdatedAt();
        return r;
    }

    private ApplicationRaise requireApprovedCampaign(Long id) {
        ApplicationRaise campaign = applicationRaiseRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));

        if (campaign.getStatus() != ApplicationRaiseStatus.APPROVED) {
            throw new IllegalStateException("This campaign is not available for investors.");
        }
        return campaign;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User currentUser() {
        return currentUserService.getCurrentUser();
    }

    private void requireInvestor(User u) {
        if (u.getRole() != Role.INVESTOR) {
            throw new UnauthorizedException("Only INVESTOR can perform this action.");
        }
    }

    private void requireAdminOrCompliance(User u) {
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.COMPLIANCE) {
            throw new UnauthorizedException("Only ADMIN/COMPLIANCE can perform this action.");
        }
    }
}