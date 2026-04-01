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
import java.util.*;
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

        for (PortfolioPositionResponse position : positions) {
            if (position.sector != null) distinctSectors.add(position.sector);
            if (position.subSector != null) distinctSubSectors.add(position.subSector);
            if (position.tags != null) {
                for (AppTag tag : position.tags) {
                    if (tag != null) distinctTags.add(tag);
                }
            }
        }

        d.distinctSectors = distinctSectors.size();
        d.distinctSubSectors = distinctSubSectors.size();
        d.distinctTags = distinctTags.size();

        d.largestPositionWeightPct = positions.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : zero(positions.get(0).positionWeightPct).setScale(2, RoundingMode.HALF_UP);

        d.top3PositionsWeightPct = positions.stream()
                .limit(3)
                .map(p -> zero(p.positionWeightPct))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        d.largestSectorWeightPct = sectorAllocation.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : zero(sectorAllocation.get(0).weightPct).setScale(2, RoundingMode.HALF_UP);

        d.concentrationIndexHhi = calculateHhi(positions);

        int score = 100;
        int breadthPenalty = 0;
        int positionPenalty = 0;
        int sectorPenalty = 0;

        if (positions.size() < 3) {
            breadthPenalty += 20;
        } else if (positions.size() < 5) {
            breadthPenalty += 10;
        }

        if (d.distinctSectors < 2) {
            breadthPenalty += 10;
        }
        if (d.distinctSubSectors < 3) {
            breadthPenalty += 5;
        }

        if (d.largestPositionWeightPct.compareTo(new BigDecimal("35.00")) > 0) {
            positionPenalty += 20;
        } else if (d.largestPositionWeightPct.compareTo(new BigDecimal("25.00")) > 0) {
            positionPenalty += 10;
        }

        if (d.top3PositionsWeightPct.compareTo(new BigDecimal("70.00")) > 0) {
            positionPenalty += 15;
        } else if (d.top3PositionsWeightPct.compareTo(new BigDecimal("55.00")) > 0) {
            positionPenalty += 8;
        }

        if (d.largestSectorWeightPct.compareTo(new BigDecimal("50.00")) > 0) {
            sectorPenalty += 20;
        } else if (d.largestSectorWeightPct.compareTo(new BigDecimal("35.00")) > 0) {
            sectorPenalty += 10;
        }

        score = score - breadthPenalty - positionPenalty - sectorPenalty;
        if (score < 0) score = 0;
        if (score > 100) score = 100;

        d.breadthPenalty = breadthPenalty;
        d.positionConcentrationPenalty = positionPenalty;
        d.sectorConcentrationPenalty = sectorPenalty;
        d.diversificationScore = score;

        return d;
    }

    private BigDecimal calculateHhi(List<PortfolioPositionResponse> positions) {
        if (positions == null || positions.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal hhi = BigDecimal.ZERO;

        for (PortfolioPositionResponse p : positions) {
            BigDecimal weightPct = zero(p.positionWeightPct);
            hhi = hhi.add(weightPct.multiply(weightPct));
        }

        return hhi.setScale(2, RoundingMode.HALF_UP);
    }

    private <T extends Enum<T>> List<PortfolioAllocationItemResponse> toAllocationItems(
            Map<T, BigDecimal> amounts,
            BigDecimal totalInvested
    ) {
        return amounts.entrySet().stream()
                .map(entry -> {
                    PortfolioAllocationItemResponse item = new PortfolioAllocationItemResponse();
                    item.key = entry.getKey().name();
                    item.amount = entry.getValue().setScale(3, RoundingMode.HALF_UP);
                    item.weightPct = percent(entry.getValue(), totalInvested);
                    return item;
                })
                .sorted(Comparator
                        .comparing((PortfolioAllocationItemResponse item) -> zero(item.amount))
                        .reversed())
                .toList();
    }

    private BigDecimal calculatePostMoneyValuation(BigDecimal fundingGoal, BigDecimal equityOfferedPercent) {
        if (fundingGoal == null || equityOfferedPercent == null || equityOfferedPercent.signum() <= 0) {
            return null;
        }

        return fundingGoal.multiply(ONE_HUNDRED)
                .divide(equityOfferedPercent, 3, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOwnershipPercent(
            BigDecimal investedAmount,
            BigDecimal fundingGoal,
            BigDecimal equityOfferedPercent
    ) {
        if (investedAmount == null || fundingGoal == null || equityOfferedPercent == null || fundingGoal.signum() <= 0) {
            return null;
        }

        return investedAmount.multiply(equityOfferedPercent)
                .divide(fundingGoal, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return part.multiply(ONE_HUNDRED)
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void requireInvestor(User user) {
        if (user.getRole() != Role.INVESTOR) {
            throw new UnauthorizedException("Only INVESTOR can access portfolio analytics.");
        }
    }
}