package com.helma.helmabackend.service.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.PortfolioAllocationItemResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioDiversificationResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioOverviewResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioPositionResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioSummaryResponse;
import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.EquityDetail;
import com.helma.helmabackend.entity.crowdfunding.Pledge;
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

    private final PledgeRepository pledgeRepository;
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
        List<Pledge> paidPledges =
                pledgeRepository.findByBackerUserIdAndStatusOrderByCreatedAtDesc(investorUserId, PledgeStatus.PAID);

        PortfolioOverviewResponse response = new PortfolioOverviewResponse();
        response.summary.investorUserId = investorUserId;
        response.summary.currency = "TND";

        if (paidPledges.isEmpty()) {
            response.summary.totalInvested = BigDecimal.ZERO;
            response.summary.activePositions = 0;
            response.summary.averageTicket = BigDecimal.ZERO;
            response.summary.largestPositionWeightPct = BigDecimal.ZERO;
            response.summary.top3PositionsWeightPct = BigDecimal.ZERO;
            response.summary.largestSectorWeightPct = BigDecimal.ZERO;
            response.summary.equityInvested = BigDecimal.ZERO;
            response.summary.donationInvested = BigDecimal.ZERO;
            response.summary.equityAllocationPct = BigDecimal.ZERO;
            response.summary.donationAllocationPct = BigDecimal.ZERO;
            response.summary.distinctSectors = 0;
            response.summary.distinctSubSectors = 0;
            response.summary.distinctTags = 0;
            response.summary.concentrationIndexHhi = BigDecimal.ZERO;
            response.summary.diversificationScore = 0;
            response.diversification = buildDiversification(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
            return response;
        }

        List<Long> applicationIds = paidPledges.stream()
                .map(Pledge::getApplicationRaiseId)
                .distinct()
                .toList();

        Map<Long, ApplicationRaise> campaignsById = applicationRaiseRepository.findAllById(applicationIds)
                .stream()
                .collect(Collectors.toMap(ApplicationRaise::getId, Function.identity()));

        Map<Long, EquityDetail> equityByApplicationId = equityDetailRepository.findByApplicationRaiseIdIn(applicationIds)
                .stream()
                .collect(Collectors.toMap(EquityDetail::getApplicationRaiseId, Function.identity()));

        BigDecimal totalInvested = paidPledges.stream()
                .map(Pledge::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal equityInvested = BigDecimal.ZERO;
        BigDecimal donationInvested = BigDecimal.ZERO;

        Map<Sector, BigDecimal> sectorAmounts = new EnumMap<>(Sector.class);
        Map<SubSector, BigDecimal> subSectorAmounts = new EnumMap<>(SubSector.class);
        Map<AppTag, BigDecimal> tagAmounts = new EnumMap<>(AppTag.class);

        Set<Sector> distinctSectors = new LinkedHashSet<>();
        Set<SubSector> distinctSubSectors = new LinkedHashSet<>();
        Set<AppTag> distinctTags = new LinkedHashSet<>();

        List<PortfolioPositionResponse> positions = new ArrayList<>();

        for (Pledge pledge : paidPledges) {
            ApplicationRaise campaign = campaignsById.get(pledge.getApplicationRaiseId());
            if (campaign == null) {
                continue;
            }

            BigDecimal investedAmount = zero(pledge.getAmount());

            if (campaign.getType() == CrowdfundingType.EQUITY) {
                equityInvested = equityInvested.add(investedAmount);
            } else {
                donationInvested = donationInvested.add(investedAmount);
            }

            if (campaign.getSector() != null) {
                distinctSectors.add(campaign.getSector());
                sectorAmounts.merge(campaign.getSector(), investedAmount, BigDecimal::add);
            }

            if (campaign.getSubSector() != null) {
                distinctSubSectors.add(campaign.getSubSector());
                subSectorAmounts.merge(campaign.getSubSector(), investedAmount, BigDecimal::add);
            }

            if (campaign.getTags() != null) {
                for (AppTag tag : campaign.getTags()) {
                    if (tag != null) {
                        distinctTags.add(tag);
                        tagAmounts.merge(tag, investedAmount, BigDecimal::add);
                    }
                }
            }

            PortfolioPositionResponse position = new PortfolioPositionResponse();
            position.campaignId = campaign.getId();
            position.campaignBusinessName = campaign.getBusinessName();
            position.campaignType = campaign.getType();
            position.sector = campaign.getSector();
            position.subSector = campaign.getSubSector();
            if (campaign.getTags() != null) {
                position.tags = new LinkedHashSet<>(campaign.getTags());
            }

            position.investedAmount = investedAmount;
            position.currency = pledge.getCurrency();
            position.positionWeightPct = percent(investedAmount, totalInvested);

            position.campaignFundingGoal = campaign.getFundingGoal();
            position.campaignRaisedAmount = campaign.getInvestorsPledgedAmount();
            position.campaignFundingProgressPct = percent(
                    zero(campaign.getInvestorsPledgedAmount()),
                    zero(campaign.getFundingGoal())
            );

            position.pledgedAt = pledge.getCreatedAt();

            if (campaign.getType() == CrowdfundingType.EQUITY) {
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
            }

            positions.add(position);
        }

        positions.sort(Comparator
                .comparing((PortfolioPositionResponse p) -> zero(p.investedAmount))
                .reversed());

        response.positions = positions;
        response.sectorAllocation = toAllocationItems(sectorAmounts, totalInvested);
        response.subSectorAllocation = toAllocationItems(subSectorAmounts, totalInvested);
        response.tagExposure = toAllocationItems(tagAmounts, totalInvested);

        PortfolioDiversificationResponse diversification = buildDiversification(
                positions,
                response.sectorAllocation,
                response.subSectorAllocation
        );
        response.diversification = diversification;

        response.summary.totalInvested = totalInvested.setScale(3, RoundingMode.HALF_UP);
        response.summary.activePositions = positions.size();
        response.summary.averageTicket = positions.isEmpty()
                ? BigDecimal.ZERO
                : totalInvested.divide(BigDecimal.valueOf(positions.size()), 3, RoundingMode.HALF_UP);

        response.summary.largestPositionWeightPct = positions.isEmpty()
                ? BigDecimal.ZERO
                : positions.get(0).positionWeightPct;

        response.summary.top3PositionsWeightPct = diversification.top3PositionsWeightPct;
        response.summary.largestSectorWeightPct = diversification.largestSectorWeightPct;

        response.summary.equityInvested = equityInvested.setScale(3, RoundingMode.HALF_UP);
        response.summary.donationInvested = donationInvested.setScale(3, RoundingMode.HALF_UP);
        response.summary.equityAllocationPct = percent(equityInvested, totalInvested);
        response.summary.donationAllocationPct = percent(donationInvested, totalInvested);

        response.summary.distinctSectors = distinctSectors.size();
        response.summary.distinctSubSectors = distinctSubSectors.size();
        response.summary.distinctTags = distinctTags.size();
        response.summary.concentrationIndexHhi = diversification.concentrationIndexHhi;
        response.summary.diversificationScore = diversification.diversificationScore;

        return response;
    }

    private PortfolioDiversificationResponse buildDiversification(
            List<PortfolioPositionResponse> positions,
            List<PortfolioAllocationItemResponse> sectorAllocation,
            List<PortfolioAllocationItemResponse> subSectorAllocation
    ) {
        PortfolioDiversificationResponse d = new PortfolioDiversificationResponse();

        Set<Sector> distinctSectors = new LinkedHashSet<>();
        Set<SubSector> distinctSubSectors = new LinkedHashSet<>();
        Set<AppTag> distinctTags = new LinkedHashSet<>();

        for (PortfolioPositionResponse p : positions) {
            if (p.sector != null) distinctSectors.add(p.sector);
            if (p.subSector != null) distinctSubSectors.add(p.subSector);
            if (p.tags != null) distinctTags.addAll(p.tags);
        }

        d.distinctSectors = distinctSectors.size();
        d.distinctSubSectors = distinctSubSectors.size();
        d.distinctTags = distinctTags.size();

        d.top3PositionsWeightPct = positions.stream()
                .limit(3)
                .map(p -> zero(p.positionWeightPct))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);

        d.largestSectorWeightPct = sectorAllocation.stream()
                .map(a -> zero(a.weightPct))
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO)
                .setScale(3, RoundingMode.HALF_UP);

        BigDecimal hhi = positions.stream()
                .map(p -> {
                    BigDecimal weight = zero(p.positionWeightPct);
                    return weight.multiply(weight);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(TEN_THOUSAND, 6, RoundingMode.HALF_UP);

        d.concentrationIndexHhi = hhi.setScale(6, RoundingMode.HALF_UP);

        int score = 100;
        score -= Math.max(0, positions.size() < 5 ? (5 - positions.size()) * 6 : 0);
        score -= Math.max(0, distinctSectors.size() < 3 ? (3 - distinctSectors.size()) * 8 : 0);
        score -= Math.max(0, distinctSubSectors.size() < 4 ? (4 - distinctSubSectors.size()) * 4 : 0);
        score -= hhi.compareTo(new BigDecimal("0.25")) > 0 ? 20 : 0;
        score -= d.top3PositionsWeightPct.compareTo(new BigDecimal("70")) > 0 ? 10 : 0;
        score -= d.largestSectorWeightPct.compareTo(new BigDecimal("50")) > 0 ? 10 : 0;

        d.breadthPenalty = Math.max(0, 100 - score);
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
                    item.amount = zero(entry.getValue()).setScale(3, RoundingMode.HALF_UP);
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

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal num = zero(numerator);
        BigDecimal den = zero(denominator);
        if (den.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return num.multiply(ONE_HUNDRED)
                .divide(den, 3, RoundingMode.HALF_UP);
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