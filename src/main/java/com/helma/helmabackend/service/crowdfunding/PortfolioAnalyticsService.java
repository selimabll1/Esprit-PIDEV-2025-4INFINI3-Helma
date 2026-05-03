package com.helma.helmabackend.service.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.PortfolioAllocationItemResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioDiversificationResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioInsightResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioOptimizationMetricsResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioOverviewResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioPositionResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioSummaryResponse;
import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.EquityDetail;
import com.helma.helmabackend.entity.crowdfunding.Pledge;
import com.helma.helmabackend.entity.crowdfunding.PortfolioImportedPosition;
import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.PledgeStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;
import com.helma.helmabackend.entity.user.Role;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.exception.UnauthorizedException;
import com.helma.helmabackend.repository.crowdfunding.ApplicationRaiseRepository;
import com.helma.helmabackend.repository.crowdfunding.EquityDetailRepository;
import com.helma.helmabackend.repository.crowdfunding.PledgeRepository;
import com.helma.helmabackend.repository.crowdfunding.PortfolioImportedPositionRepository;
import com.helma.helmabackend.service.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioAnalyticsService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");
    private static final BigDecimal SUGGESTED_MAX_POSITION_WEIGHT_PCT = new BigDecimal("35.000");

    private final PledgeRepository pledgeRepository;
    private final PortfolioImportedPositionRepository importedPositionRepository;
    private final ApplicationRaiseRepository applicationRaiseRepository;
    private final EquityDetailRepository equityDetailRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public PortfolioOverviewResponse getMyOverview() {
        User me = currentUserService.getCurrentUser();
        requireInvestor(me);
        return buildOverview(me.getId());
    }

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getMySummary() {
        return getMyOverview().summary;
    }

    @Transactional(readOnly = true)
    public List<PortfolioPositionResponse> getMyPositions() {
        return getMyOverview().positions;
    }

    @Transactional(readOnly = true)
    public PortfolioDiversificationResponse getMyDiversification() {
        return getMyOverview().diversification;
    }

    private PortfolioOverviewResponse buildOverview(Long investorUserId) {
        List<Pledge> allPledges = pledgeRepository.findByBackerUserIdOrderByCreatedAtDesc(investorUserId);
        List<PortfolioImportedPosition> importedPositions = importedPositionRepository.findByInvestorUserIdOrderByCreatedAtDesc(investorUserId);

        PortfolioOverviewResponse response = new PortfolioOverviewResponse();
        response.summary.investorUserId = investorUserId;
        response.summary.currency = "TND";

        if (allPledges.isEmpty() && importedPositions.isEmpty()) {
            response.diversification = buildDiversification(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
            response.optimization = buildOptimizationMetrics(response, BigDecimal.ZERO, BigDecimal.ZERO);
            response.insights.add(new PortfolioInsightResponse(
                    "INFO",
                    "No tracked equity positions yet",
                    "Your portfolio will start showing analytics after an equity pledge is paid or after you import tracked equity positions."
            ));
            return response;
        }

        List<Long> applicationIds = allPledges.stream()
                .map(Pledge::getApplicationRaiseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, ApplicationRaise> campaignsById = applicationIds.isEmpty()
                ? Collections.emptyMap()
                : applicationRaiseRepository.findAllById(applicationIds)
                .stream()
                .collect(Collectors.toMap(ApplicationRaise::getId, Function.identity()));

        List<Pledge> equityPledges = allPledges.stream()
                .filter(pledge -> isEquityPledge(pledge, campaignsById))
                .toList();

        List<Pledge> paidPledges = equityPledges.stream()
                .filter(pledge -> pledge.getStatus() == PledgeStatus.PAID)
                .toList();

        List<Pledge> pendingPledges = equityPledges.stream()
                .filter(pledge -> pledge.getStatus() == PledgeStatus.PENDING)
                .toList();

        List<Pledge> failedOrCanceledPledges = equityPledges.stream()
                .filter(pledge -> pledge.getStatus() == PledgeStatus.FAILED
                        || pledge.getStatus() == PledgeStatus.CANCELED
                        || pledge.getStatus() == PledgeStatus.REFUNDED)
                .toList();

        BigDecimal pendingCommitments = sumAmounts(pendingPledges);
        BigDecimal failedOrCanceledAmount = sumAmounts(failedOrCanceledPledges);

        response.summary.pendingCommitments = money(pendingCommitments);
        response.summary.pendingCommitmentsCount = pendingPledges.size();
        response.summary.failedOrCanceledAmount = money(failedOrCanceledAmount);
        response.summary.failedOrCanceledCount = failedOrCanceledPledges.size();

        Map<Long, EquityDetail> equityByApplicationId = applicationIds.isEmpty()
                ? Collections.emptyMap()
                : equityDetailRepository.findByApplicationRaiseIdIn(applicationIds)
                .stream()
                .collect(Collectors.toMap(EquityDetail::getApplicationRaiseId, Function.identity()));

        List<PortfolioPositionResponse> positions = new ArrayList<>();

        for (Pledge pledge : paidPledges) {
            ApplicationRaise campaign = campaignsById.get(pledge.getApplicationRaiseId());
            if (campaign == null || campaign.getType() != CrowdfundingType.EQUITY) {
                continue;
            }

            BigDecimal investedAmount = zero(pledge.getAmount());

            PortfolioPositionResponse position = new PortfolioPositionResponse();
            position.campaignId = campaign.getId();
            position.campaignBusinessName = campaign.getBusinessName();
            position.campaignType = campaign.getType();
            position.positionSource = "HELMA";
            position.imported = false;
            position.sector = campaign.getSector();
            position.subSector = campaign.getSubSector();
            position.governorate = campaign.getGovernorate();
            position.city = campaign.getCity();
            if (campaign.getTags() != null) {
                position.tags = new LinkedHashSet<>(campaign.getTags());
            }

            position.investedAmount = money(investedAmount);
            position.currency = pledge.getCurrency();

            position.campaignFundingGoal = campaign.getFundingGoal();
            position.campaignRaisedAmount = campaign.getInvestorsPledgedAmount();
            position.campaignFundingProgressPct = percent(
                    zero(campaign.getInvestorsPledgedAmount()),
                    zero(campaign.getFundingGoal())
            );
            position.campaignFundingGap = zero(campaign.getFundingGoal())
                    .subtract(zero(campaign.getInvestorsPledgedAmount()))
                    .max(BigDecimal.ZERO)
                    .setScale(3, RoundingMode.HALF_UP);
            position.campaignFundingGapPct = ONE_HUNDRED
                    .subtract(position.campaignFundingProgressPct)
                    .max(BigDecimal.ZERO)
                    .setScale(3, RoundingMode.HALF_UP);

            position.pledgedAt = pledge.getCreatedAt();

            EquityDetail equity = equityByApplicationId.get(campaign.getId());
            if (equity != null) {
                position.minInvestment = equity.getMinInvestment();
                position.equityOfferedPercent = equity.getEquityOfferedPercent();
                position.preMoneyValuation = equity.getPreMoneyValuation();
                position.postMoneyValuation = calculatePostMoneyValuation(
                        campaign.getFundingGoal(),
                        equity.getEquityOfferedPercent()
                );
                position.ownershipPercent = calculateOwnershipPercent(
                        investedAmount,
                        campaign.getFundingGoal(),
                        equity.getEquityOfferedPercent()
                );
            }

            positions.add(position);
        }

        importedPositions.stream()
                .map(this::toImportedPosition)
                .forEach(positions::add);

        positions.sort(Comparator
                .comparing((PortfolioPositionResponse p) -> zero(p.investedAmount))
                .reversed());

        response.positions = positions;
        rebuildPortfolioFromPositions(response, pendingCommitments, failedOrCanceledAmount);

        return response;
    }

    private PortfolioPositionResponse toImportedPosition(PortfolioImportedPosition imported) {
        PortfolioPositionResponse position = new PortfolioPositionResponse();
        position.campaignId = null;
        position.campaignBusinessName = imported.getCampaignBusinessName();
        position.campaignType = CrowdfundingType.EQUITY;
        position.positionSource = "IMPORTED_XLSX";
        position.imported = true;
        position.sourceReference = imported.getSourceReference();
        position.notes = imported.getNotes();
        position.importedAt = imported.getCreatedAt();

        position.sector = imported.getSector();
        position.subSector = imported.getSubSector();
        position.governorate = imported.getGovernorate();
        position.city = imported.getCity();
        if (imported.getTags() != null) {
            position.tags = new LinkedHashSet<>(imported.getTags());
        }

        BigDecimal investedAmount = zero(imported.getInvestedAmount());
        position.investedAmount = money(investedAmount);
        position.currency = imported.getCurrency();

        position.campaignFundingGoal = imported.getCampaignFundingGoal();
        position.campaignRaisedAmount = imported.getCampaignRaisedAmount();
        position.campaignFundingProgressPct = percent(
                zero(imported.getCampaignRaisedAmount()),
                zero(imported.getCampaignFundingGoal())
        );
        position.campaignFundingGap = zero(imported.getCampaignFundingGoal())
                .subtract(zero(imported.getCampaignRaisedAmount()))
                .max(BigDecimal.ZERO)
                .setScale(3, RoundingMode.HALF_UP);
        position.campaignFundingGapPct = ONE_HUNDRED
                .subtract(position.campaignFundingProgressPct)
                .max(BigDecimal.ZERO)
                .setScale(3, RoundingMode.HALF_UP);

        position.equityOfferedPercent = imported.getEquityOfferedPercent();
        position.ownershipPercent = imported.getOwnershipPercent();
        position.pledgedAt = imported.getInvestedAt() == null ? imported.getCreatedAt() : imported.getInvestedAt();
        return position;
    }

    private void rebuildPortfolioFromPositions(
            PortfolioOverviewResponse response,
            BigDecimal pendingCommitments,
            BigDecimal failedOrCanceledAmount
    ) {
        BigDecimal totalInvested = sumPositionAmounts(response.positions);
        BigDecimal committedCapital = totalInvested.add(zero(pendingCommitments));

        if (response.summary.currency == null || response.summary.currency.isBlank()) {
            response.summary.currency = resolvePortfolioCurrency(response.positions);
        }

        Map<Sector, BigDecimal> sectorAmounts = new EnumMap<>(Sector.class);
        Map<SubSector, BigDecimal> subSectorAmounts = new EnumMap<>(SubSector.class);
        Map<AppTag, BigDecimal> tagAmounts = new EnumMap<>(AppTag.class);
        Map<String, BigDecimal> regionAmounts = new HashMap<>();

        Set<Sector> distinctSectors = new LinkedHashSet<>();
        Set<SubSector> distinctSubSectors = new LinkedHashSet<>();
        Set<String> distinctRegions = new LinkedHashSet<>();
        Set<AppTag> distinctTags = new LinkedHashSet<>();

        for (PortfolioPositionResponse position : response.positions) {
            BigDecimal investedAmount = zero(position.investedAmount);
            position.positionWeightPct = percent(investedAmount, totalInvested);
            position.concentrationRisk = concentrationRiskFromWeight(position.positionWeightPct);

            if (position.campaignFundingProgressPct == null) {
                position.campaignFundingProgressPct = percent(
                        zero(position.campaignRaisedAmount),
                        zero(position.campaignFundingGoal)
                );
            }

            if (position.campaignFundingGap == null) {
                position.campaignFundingGap = zero(position.campaignFundingGoal)
                        .subtract(zero(position.campaignRaisedAmount))
                        .max(BigDecimal.ZERO)
                        .setScale(3, RoundingMode.HALF_UP);
            }

            if (position.campaignFundingGapPct == null) {
                position.campaignFundingGapPct = ONE_HUNDRED
                        .subtract(zero(position.campaignFundingProgressPct))
                        .max(BigDecimal.ZERO)
                        .setScale(3, RoundingMode.HALF_UP);
            }

            if (position.sector != null) {
                distinctSectors.add(position.sector);
                sectorAmounts.merge(position.sector, investedAmount, BigDecimal::add);
            }

            if (position.subSector != null) {
                distinctSubSectors.add(position.subSector);
                subSectorAmounts.merge(position.subSector, investedAmount, BigDecimal::add);
            }

            String region = normalizeRegion(position.governorate);
            if (region != null) {
                distinctRegions.add(region);
                regionAmounts.merge(region, investedAmount, BigDecimal::add);
            }

            if (position.tags != null) {
                for (AppTag tag : position.tags) {
                    if (tag != null) {
                        distinctTags.add(tag);
                        tagAmounts.merge(tag, investedAmount, BigDecimal::add);
                    }
                }
            }
        }

        response.sectorAllocation = toAllocationItems(sectorAmounts, totalInvested);
        response.subSectorAllocation = toAllocationItems(subSectorAmounts, totalInvested);
        response.regionAllocation = toStringAllocationItems(regionAmounts, totalInvested);
        response.tagExposure = toAllocationItems(tagAmounts, totalInvested);

        PortfolioDiversificationResponse diversification = buildDiversification(
                response.positions,
                response.sectorAllocation,
                response.subSectorAllocation,
                response.regionAllocation
        );
        response.diversification = diversification;

        response.summary.totalInvested = money(totalInvested);
        response.summary.activePositions = response.positions.size();
        response.summary.averageTicket = response.positions.isEmpty()
                ? BigDecimal.ZERO
                : totalInvested.divide(BigDecimal.valueOf(response.positions.size()), 3, RoundingMode.HALF_UP);
        response.summary.committedCapital = money(committedCapital);
        response.summary.deploymentRatePct = percent(totalInvested, committedCapital);
        response.summary.pendingCommitmentWeightPct = percent(pendingCommitments, committedCapital);

        response.summary.largestPositionWeightPct = diversification.largestPositionWeightPct;
        response.summary.top3PositionsWeightPct = diversification.top3PositionsWeightPct;
        response.summary.largestSectorWeightPct = diversification.largestSectorWeightPct;
        response.summary.largestRegionWeightPct = diversification.largestRegionWeightPct;

        response.summary.equityInvested = money(totalInvested);
        response.summary.equityAllocationPct = response.positions.isEmpty() ? BigDecimal.ZERO : ONE_HUNDRED.setScale(3, RoundingMode.HALF_UP);

        response.summary.distinctSectors = distinctSectors.size();
        response.summary.distinctSubSectors = distinctSubSectors.size();
        response.summary.distinctRegions = distinctRegions.size();
        response.summary.distinctTags = distinctTags.size();
        response.summary.concentrationIndexHhi = diversification.concentrationIndexHhi;
        response.summary.sectorConcentrationHhi = diversification.sectorConcentrationHhi;
        response.summary.regionConcentrationHhi = diversification.regionConcentrationHhi;
        response.summary.effectiveNumberOfPositions = diversification.effectiveNumberOfPositions;
        response.summary.effectiveNumberOfSectors = diversification.effectiveNumberOfSectors;
        response.summary.effectiveNumberOfRegions = diversification.effectiveNumberOfRegions;
        response.summary.diversificationScore = diversification.diversificationScore;
        response.summary.averageOwnershipPercent = averageOwnershipPercent(response.positions);
        response.summary.maxOwnershipPercent = maxOwnershipPercent(response.positions);
        response.summary.weightedAverageFundingProgressPct = weightedAverageFundingProgress(response.positions);
        response.summary.concentrationRisk = portfolioConcentrationRisk(diversification);
        response.summary.portfolioHealth = portfolioHealth(diversification.diversificationScore, response.summary.concentrationRisk);

        response.optimization = buildOptimizationMetrics(response, pendingCommitments, failedOrCanceledAmount);
        response.insights = buildInsights(response, pendingCommitments, failedOrCanceledAmount);
    }

    private boolean isEquityPledge(Pledge pledge, Map<Long, ApplicationRaise> campaignsById) {
        ApplicationRaise campaign = campaignsById.get(pledge.getApplicationRaiseId());
        return campaign != null && campaign.getType() == CrowdfundingType.EQUITY;
    }

    private PortfolioOptimizationMetricsResponse buildOptimizationMetrics(
            PortfolioOverviewResponse response,
            BigDecimal pendingCommitments,
            BigDecimal failedOrCanceledAmount
    ) {
        PortfolioOptimizationMetricsResponse metrics = new PortfolioOptimizationMetricsResponse();
        BigDecimal totalInvested = zero(response.summary.totalInvested);
        BigDecimal committedCapital = totalInvested.add(zero(pendingCommitments));

        metrics.committedCapital = money(committedCapital);
        metrics.deploymentRatePct = percent(totalInvested, committedCapital);
        metrics.pendingCommitmentWeightPct = percent(pendingCommitments, committedCapital);
        metrics.weightedAverageFundingProgressPct = weightedAverageFundingProgress(response.positions);
        metrics.averagePositionWeightPct = response.positions.isEmpty()
                ? BigDecimal.ZERO
                : ONE_HUNDRED.divide(BigDecimal.valueOf(response.positions.size()), 3, RoundingMode.HALF_UP);
        metrics.effectiveNumberOfPositions = response.diversification.effectiveNumberOfPositions;
        metrics.effectiveNumberOfSectors = response.diversification.effectiveNumberOfSectors;
        metrics.effectiveNumberOfRegions = response.diversification.effectiveNumberOfRegions;
        metrics.sectorConcentrationHhi = response.diversification.sectorConcentrationHhi;
        metrics.regionConcentrationHhi = response.diversification.regionConcentrationHhi;
        metrics.largestRegionWeightPct = response.diversification.largestRegionWeightPct;
        metrics.suggestedMaxPositionWeightPct = SUGGESTED_MAX_POSITION_WEIGHT_PCT;
        metrics.overweightPositions = (int) response.positions.stream()
                .filter(p -> zero(p.positionWeightPct).compareTo(SUGGESTED_MAX_POSITION_WEIGHT_PCT) > 0)
                .count();
        metrics.concentrationRisk = portfolioConcentrationRisk(response.diversification);
        metrics.portfolioHealth = portfolioHealth(response.diversification.diversificationScore, metrics.concentrationRisk);

        response.summary.committedCapital = metrics.committedCapital;
        response.summary.deploymentRatePct = metrics.deploymentRatePct;
        response.summary.pendingCommitmentWeightPct = metrics.pendingCommitmentWeightPct;
        response.summary.weightedAverageFundingProgressPct = metrics.weightedAverageFundingProgressPct;
        response.summary.concentrationRisk = metrics.concentrationRisk;
        response.summary.portfolioHealth = metrics.portfolioHealth;

        if (failedOrCanceledAmount.signum() > 0 && response.summary.failedOrCanceledAmount.signum() == 0) {
            response.summary.failedOrCanceledAmount = money(failedOrCanceledAmount);
        }

        return metrics;
    }

    private List<PortfolioInsightResponse> buildInsights(
            PortfolioOverviewResponse response,
            BigDecimal pendingCommitments,
            BigDecimal failedOrCanceledAmount
    ) {
        List<PortfolioInsightResponse> insights = new ArrayList<>();

        if (response.summary.activePositions == 0) {
            insights.add(new PortfolioInsightResponse(
                    "INFO",
                    "No confirmed equity positions yet",
                    pendingCommitments.signum() > 0
                            ? "You have pending commitments, but they are not counted until payment succeeds."
                            : "Your portfolio will start showing analytics after an equity pledge is paid."
            ));
            return insights;
        }

        if (zero(response.summary.largestPositionWeightPct).compareTo(new BigDecimal("60")) >= 0) {
            insights.add(new PortfolioInsightResponse(
                    "WARNING",
                    "High single-position concentration",
                    "Your largest position represents " + formatPct(response.summary.largestPositionWeightPct) + " of confirmed invested capital."
            ));
        } else if (zero(response.summary.largestPositionWeightPct).compareTo(SUGGESTED_MAX_POSITION_WEIGHT_PCT) > 0) {
            insights.add(new PortfolioInsightResponse(
                    "ACTION",
                    "One position is above the suggested weight",
                    "The suggested early-stage cap is " + formatPct(SUGGESTED_MAX_POSITION_WEIGHT_PCT) + "; your largest position is " + formatPct(response.summary.largestPositionWeightPct) + "."
            ));
        } else {
            insights.add(new PortfolioInsightResponse(
                    "POSITIVE",
                    "Single-position exposure looks controlled",
                    "No confirmed campaign is above the suggested " + formatPct(SUGGESTED_MAX_POSITION_WEIGHT_PCT) + " position weight."
            ));
        }

        if (zero(response.summary.largestSectorWeightPct).compareTo(new BigDecimal("50")) >= 0) {
            PortfolioAllocationItemResponse topSector = response.sectorAllocation.isEmpty() ? null : response.sectorAllocation.get(0);
            insights.add(new PortfolioInsightResponse(
                    "WARNING",
                    "Sector concentration detected",
                    topSector == null
                            ? "Your portfolio is concentrated in one sector."
                            : "Your biggest sector exposure is " + topSector.key + " at " + formatPct(topSector.weightPct) + "."
            ));
        }

        if (zero(response.summary.largestRegionWeightPct).compareTo(new BigDecimal("50")) >= 0) {
            PortfolioAllocationItemResponse topRegion = response.regionAllocation.isEmpty() ? null : response.regionAllocation.get(0);
            insights.add(new PortfolioInsightResponse(
                    "INFO",
                    "Geographic concentration",
                    topRegion == null
                            ? "Region exposure can be improved as more confirmed investments are added."
                            : "Your largest region exposure is " + topRegion.key + " at " + formatPct(topRegion.weightPct) + "."
            ));
        }

        if (response.optimization.effectiveNumberOfPositions.compareTo(new BigDecimal("3")) < 0) {
            insights.add(new PortfolioInsightResponse(
                    "ACTION",
                    "Effective diversification is still low",
                    "Your capital behaves like it is spread across " + formatNumber(response.optimization.effectiveNumberOfPositions) + " equally weighted positions."
            ));
        }

        if (zero(response.summary.weightedAverageFundingProgressPct).compareTo(new BigDecimal("70")) >= 0) {
            insights.add(new PortfolioInsightResponse(
                    "POSITIVE",
                    "Campaign progress is strong",
                    "Your weighted average campaign funding progress is " + formatPct(response.summary.weightedAverageFundingProgressPct) + "."
            ));
        }

        if (response.summary.distinctRegions != null && response.summary.distinctRegions >= 3) {
            insights.add(new PortfolioInsightResponse(
                    "POSITIVE",
                    "Good regional spread",
                    "Your confirmed investments cover " + response.summary.distinctRegions + " regions."
            ));
        } else {
            insights.add(new PortfolioInsightResponse(
                    "INFO",
                    "Regional spread can improve",
                    "Explore equity campaigns from more governorates to reduce geographic concentration."
            ));
        }

        if (pendingCommitments.signum() > 0) {
            insights.add(new PortfolioInsightResponse(
                    "ACTION",
                    "Pending checkout",
                    "You have " + formatMoney(pendingCommitments) + " in pending equity commitments that are not counted as confirmed investments yet."
            ));
        }

        if (failedOrCanceledAmount.signum() > 0) {
            insights.add(new PortfolioInsightResponse(
                    "WARNING",
                    "Unsuccessful commitments",
                    formatMoney(failedOrCanceledAmount) + " is linked to failed, cancelled, or refunded equity commitments."
            ));
        }

        return insights;
    }

    private PortfolioDiversificationResponse buildDiversification(
            List<PortfolioPositionResponse> positions,
            List<PortfolioAllocationItemResponse> sectorAllocation,
            List<PortfolioAllocationItemResponse> subSectorAllocation,
            List<PortfolioAllocationItemResponse> regionAllocation
    ) {
        PortfolioDiversificationResponse d = new PortfolioDiversificationResponse();

        Set<Sector> distinctSectors = new LinkedHashSet<>();
        Set<SubSector> distinctSubSectors = new LinkedHashSet<>();
        Set<String> distinctRegions = new LinkedHashSet<>();
        Set<AppTag> distinctTags = new LinkedHashSet<>();

        for (PortfolioPositionResponse p : positions) {
            if (p.sector != null) distinctSectors.add(p.sector);
            if (p.subSector != null) distinctSubSectors.add(p.subSector);
            if (p.governorate != null && !p.governorate.isBlank()) distinctRegions.add(p.governorate.trim());
            if (p.tags != null) distinctTags.addAll(p.tags);
        }

        d.distinctSectors = distinctSectors.size();
        d.distinctSubSectors = distinctSubSectors.size();
        d.distinctRegions = distinctRegions.size();
        d.distinctTags = distinctTags.size();

        d.top3PositionsWeightPct = positions.stream()
                .limit(3)
                .map(p -> zero(p.positionWeightPct))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);

        d.largestPositionWeightPct = largestWeight(positions.stream()
                .map(p -> zero(p.positionWeightPct))
                .toList());
        d.largestSectorWeightPct = largestWeight(sectorAllocation.stream()
                .map(a -> zero(a.weightPct))
                .toList());
        d.largestRegionWeightPct = largestWeight(regionAllocation.stream()
                .map(a -> zero(a.weightPct))
                .toList());

        d.concentrationIndexHhi = hhiFromWeights(positions.stream()
                .map(p -> zero(p.positionWeightPct))
                .toList());
        d.sectorConcentrationHhi = hhiFromWeights(sectorAllocation.stream()
                .map(a -> zero(a.weightPct))
                .toList());
        d.regionConcentrationHhi = hhiFromWeights(regionAllocation.stream()
                .map(a -> zero(a.weightPct))
                .toList());

        d.effectiveNumberOfPositions = effectiveNumberFromHhi(d.concentrationIndexHhi);
        d.effectiveNumberOfSectors = effectiveNumberFromHhi(d.sectorConcentrationHhi);
        d.effectiveNumberOfRegions = effectiveNumberFromHhi(d.regionConcentrationHhi);

        int breadthPenalty = 0;
        breadthPenalty += Math.max(0, positions.size() < 5 ? (5 - positions.size()) * 6 : 0);
        breadthPenalty += Math.max(0, distinctSectors.size() < 3 ? (3 - distinctSectors.size()) * 8 : 0);
        breadthPenalty += Math.max(0, distinctSubSectors.size() < 4 ? (4 - distinctSubSectors.size()) * 4 : 0);
        breadthPenalty += Math.max(0, distinctRegions.size() < 3 ? (3 - distinctRegions.size()) * 5 : 0);

        int positionPenalty = 0;
        positionPenalty += d.concentrationIndexHhi.compareTo(new BigDecimal("0.25")) > 0 ? 20 : 0;
        positionPenalty += d.top3PositionsWeightPct.compareTo(new BigDecimal("70")) > 0 ? 10 : 0;
        positionPenalty += d.largestPositionWeightPct.compareTo(new BigDecimal("60")) > 0 ? 10 : 0;

        int sectorPenalty = d.largestSectorWeightPct.compareTo(new BigDecimal("50")) > 0 ? 10 : 0;
        int regionPenalty = d.largestRegionWeightPct.compareTo(new BigDecimal("50")) > 0 ? 6 : 0;

        int score = 100 - breadthPenalty - positionPenalty - sectorPenalty - regionPenalty;

        d.breadthPenalty = breadthPenalty;
        d.positionConcentrationPenalty = positionPenalty;
        d.sectorConcentrationPenalty = sectorPenalty;
        d.regionConcentrationPenalty = regionPenalty;
        d.diversificationScore = Math.max(0, Math.min(100, score));

        return d;
    }

    private <T extends Enum<T>> List<PortfolioAllocationItemResponse> toAllocationItems(
            Map<T, BigDecimal> amounts,
            BigDecimal total
    ) {
        return amounts.entrySet().stream()
                .sorted(Map.Entry.<T, BigDecimal>comparingByValue().reversed())
                .map(entry -> {
                    PortfolioAllocationItemResponse item = new PortfolioAllocationItemResponse();
                    item.key = entry.getKey().name();
                    item.amount = money(entry.getValue());
                    item.weightPct = percent(entry.getValue(), total);
                    return item;
                })
                .toList();
    }

    private List<PortfolioAllocationItemResponse> toStringAllocationItems(
            Map<String, BigDecimal> amounts,
            BigDecimal total
    ) {
        return amounts.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(entry -> {
                    PortfolioAllocationItemResponse item = new PortfolioAllocationItemResponse();
                    item.key = entry.getKey();
                    item.amount = money(entry.getValue());
                    item.weightPct = percent(entry.getValue(), total);
                    return item;
                })
                .toList();
    }

    private BigDecimal calculatePostMoneyValuation(BigDecimal fundingGoal, BigDecimal equityOfferedPercent) {
        BigDecimal goal = zero(fundingGoal);
        BigDecimal offered = zero(equityOfferedPercent);
        if (goal.signum() <= 0 || offered.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return goal.divide(offered.divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP), 3, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOwnershipPercent(BigDecimal investedAmount, BigDecimal fundingGoal, BigDecimal equityOfferedPercent) {
        BigDecimal invested = zero(investedAmount);
        BigDecimal goal = zero(fundingGoal);
        BigDecimal offered = zero(equityOfferedPercent);

        if (invested.signum() <= 0 || goal.signum() <= 0 || offered.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        return invested.divide(goal, 8, RoundingMode.HALF_UP)
                .multiply(offered)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal averageOwnershipPercent(List<PortfolioPositionResponse> positions) {
        List<BigDecimal> ownershipValues = positions.stream()
                .map(p -> zero(p.ownershipPercent))
                .filter(value -> value.signum() > 0)
                .toList();

        if (ownershipValues.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = ownershipValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(ownershipValues.size()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal maxOwnershipPercent(List<PortfolioPositionResponse> positions) {
        return positions.stream()
                .map(p -> zero(p.ownershipPercent))
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal weightedAverageFundingProgress(List<PortfolioPositionResponse> positions) {
        return positions.stream()
                .map(p -> zero(p.campaignFundingProgressPct)
                        .multiply(zero(p.positionWeightPct))
                        .divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal sumPositionAmounts(Collection<PortfolioPositionResponse> positions) {
        if (positions == null) {
            return BigDecimal.ZERO;
        }
        return positions.stream()
                .map(p -> p == null ? BigDecimal.ZERO : zero(p.investedAmount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String resolvePortfolioCurrency(List<PortfolioPositionResponse> positions) {
        if (positions == null || positions.isEmpty()) {
            return "TND";
        }
        return positions.stream()
                .map(p -> p.currency)
                .filter(Objects::nonNull)
                .filter(currency -> !currency.isBlank())
                .findFirst()
                .orElse("TND");
    }

    private BigDecimal sumAmounts(Collection<Pledge> pledges) {
        return pledges.stream()
                .map(Pledge::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal hhiFromWeights(List<BigDecimal> weightsPct) {
        if (weightsPct == null || weightsPct.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return weightsPct.stream()
                .map(weight -> zero(weight).multiply(zero(weight)))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(TEN_THOUSAND, 6, RoundingMode.HALF_UP)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal effectiveNumberFromHhi(BigDecimal hhi) {
        BigDecimal safeHhi = zero(hhi);
        if (safeHhi.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ONE.divide(safeHhi, 3, RoundingMode.HALF_UP);
    }

    private BigDecimal largestWeight(List<BigDecimal> weightsPct) {
        return weightsPct.stream()
                .map(this::zero)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO)
                .setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal num = zero(numerator);
        BigDecimal den = zero(denominator);
        if (den.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return num.multiply(ONE_HUNDRED)
                .divide(den, 3, RoundingMode.HALF_UP);
    }

    private String normalizeRegion(String governorate) {
        if (governorate == null || governorate.isBlank()) {
            return null;
        }
        return governorate.trim();
    }

    private String concentrationRiskFromWeight(BigDecimal weightPct) {
        BigDecimal weight = zero(weightPct);
        if (weight.compareTo(new BigDecimal("60")) >= 0) return "VERY_HIGH";
        if (weight.compareTo(SUGGESTED_MAX_POSITION_WEIGHT_PCT) > 0) return "HIGH";
        if (weight.compareTo(new BigDecimal("20")) >= 0) return "MEDIUM";
        return "LOW";
    }

    private String portfolioConcentrationRisk(PortfolioDiversificationResponse d) {
        if (d == null || zero(d.concentrationIndexHhi).signum() <= 0) return "NO_POSITIONS";
        if (zero(d.largestPositionWeightPct).compareTo(new BigDecimal("60")) >= 0
                || zero(d.concentrationIndexHhi).compareTo(new BigDecimal("0.40")) >= 0) {
            return "VERY_HIGH";
        }
        if (zero(d.largestPositionWeightPct).compareTo(SUGGESTED_MAX_POSITION_WEIGHT_PCT) > 0
                || zero(d.concentrationIndexHhi).compareTo(new BigDecimal("0.25")) >= 0) {
            return "HIGH";
        }
        if (zero(d.concentrationIndexHhi).compareTo(new BigDecimal("0.15")) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String portfolioHealth(Integer diversificationScore, String concentrationRisk) {
        int score = diversificationScore == null ? 0 : diversificationScore;
        if ("NO_POSITIONS".equals(concentrationRisk)) return "NO_POSITIONS";
        if (score >= 75 && ("LOW".equals(concentrationRisk) || "MEDIUM".equals(concentrationRisk))) return "STRONG";
        if (score >= 55 && !"VERY_HIGH".equals(concentrationRisk)) return "WATCH";
        return "CONCENTRATED";
    }

    private String formatPct(BigDecimal value) {
        return zero(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private String formatNumber(BigDecimal value) {
        return zero(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatMoney(BigDecimal value) {
        return money(value).stripTrailingZeros().toPlainString() + " TND";
    }

    private BigDecimal money(BigDecimal value) {
        return zero(value).setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal zero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private void requireInvestor(User u) {
        if (u.getRole() != Role.INVESTOR) {
            throw new UnauthorizedException("Only INVESTOR can perform this action.");
        }
    }
}
