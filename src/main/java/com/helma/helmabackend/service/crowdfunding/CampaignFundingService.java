package com.helma.helmabackend.service.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.enums.PledgeStatus;
import com.helma.helmabackend.repository.crowdfunding.ApplicationRaiseRepository;
import com.helma.helmabackend.repository.crowdfunding.PledgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CampaignFundingService {

    private final ApplicationRaiseRepository applicationRaiseRepo;
    private final PledgeRepository pledgeRepo;

    @Transactional
    public void syncCampaignRaisedAmount(Long applicationRaiseId) {
        ApplicationRaise campaign = applicationRaiseRepo.findById(applicationRaiseId)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + applicationRaiseId));

        BigDecimal paidTotal = pledgeRepo.sumAmountByApplicationRaiseIdAndStatus(applicationRaiseId, PledgeStatus.PAID);
        campaign.setInvestorsPledgedAmount(paidTotal == null ? BigDecimal.ZERO : paidTotal);
        applicationRaiseRepo.save(campaign);
    }
}
