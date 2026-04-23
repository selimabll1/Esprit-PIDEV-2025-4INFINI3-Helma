package com.helma.helmabackend.service.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.EquityDetailResponse;
import com.helma.helmabackend.dto.crowdfunding.EquityDetailUpsertRequest;
import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.EquityDetail;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.user.Role;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.exception.UnauthorizedException;
import com.helma.helmabackend.repository.crowdfunding.ApplicationRaiseRepository;
import com.helma.helmabackend.repository.crowdfunding.EquityDetailRepository;
import com.helma.helmabackend.service.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EquityDetailService {

    private final EquityDetailRepository equityRepo;
    private final ApplicationRaiseRepository appRepo;
    private final CurrentUserService currentUserService;

    @Transactional
    public EquityDetailResponse upsert(Long applicationRaiseId, EquityDetailUpsertRequest req) {
        User me = currentUser();

        if (me.getRole() != Role.YOUTH_BENEFICIARY) {
            throw new UnauthorizedException("Only YOUTH_BENEFICIARY can fill equity details.");
        }

        ApplicationRaise app = appRepo.findById(applicationRaiseId)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + applicationRaiseId));

        if (!app.getOwnerUserId().equals(me.getId())) {
            throw new UnauthorizedException("You can only edit equity details for your own application.");
        }

        if (app.getType() != CrowdfundingType.EQUITY) {
            throw new IllegalStateException("Equity details are only allowed when crowdfunding type is EQUITY.");
        }

        if (app.getStatus() != ApplicationRaiseStatus.DRAFT) {
            throw new IllegalStateException("Equity details can only be edited while application is DRAFT.");
        }

        if (req == null) {
            throw new IllegalArgumentException("Equity details payload is required.");
        }

        String reg = normalizeRequired(req.companyRegistrationNumber, "companyRegistrationNumber is required");

        boolean duplicate = equityRepo.existsByCompanyRegistrationNumberIgnoreCaseAndApplicationRaiseIdNot(
                reg, applicationRaiseId
        );
        if (duplicate) {
            throw new IllegalStateException("companyRegistrationNumber already exists in another equity application.");
        }

        EquityDetail detail = equityRepo.findByApplicationRaiseId(applicationRaiseId)
                .orElseGet(() -> {
                    EquityDetail d = new EquityDetail();
                    d.setApplicationRaise(app);
                    return d;
                });

        detail.setCompanyLegalName(normalizeRequired(req.companyLegalName, "companyLegalName is required"));
        detail.setCompanyRegistrationNumber(reg);
        detail.setCnreProfileUrl(normalizeRequired(req.cnreProfileUrl, "cnreProfileUrl is required"));
        detail.setEquityOfferedPercent(req.equityOfferedPercent);
        detail.setPreMoneyValuation(req.preMoneyValuation);
        detail.setMinInvestment(req.minInvestment);

        EquityDetail saved = equityRepo.save(detail);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public EquityDetailResponse getByApplicationRaiseId(Long applicationRaiseId) {
        User me = currentUser();

        ApplicationRaise app = appRepo.findById(applicationRaiseId)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + applicationRaiseId));

        boolean isOwner = app.getOwnerUserId().equals(me.getId());
        boolean isAdminOrCompliance = me.getRole() == Role.ADMIN || me.getRole() == Role.COMPLIANCE;

        if (!isOwner && !isAdminOrCompliance) {
            throw new UnauthorizedException("You can only view equity details for your own application.");
        }

        EquityDetail detail = equityRepo.findByApplicationRaiseId(applicationRaiseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "EquityDetail not found for application: " + applicationRaiseId));

        return toDto(detail);
    }

    public EquityDetailResponse toDto(EquityDetail d) {
        EquityDetailResponse r = new EquityDetailResponse();
        r.applicationRaiseId = d.getApplicationRaiseId();
        r.companyLegalName = d.getCompanyLegalName();
        r.companyRegistrationNumber = d.getCompanyRegistrationNumber();
        r.cnreProfileUrl = d.getCnreProfileUrl();
        r.equityOfferedPercent = d.getEquityOfferedPercent();
        r.preMoneyValuation = d.getPreMoneyValuation();
        r.minInvestment = d.getMinInvestment();
        r.createdAt = d.getCreatedAt();
        r.updatedAt = d.getUpdatedAt();
        return r;
    }

    private User currentUser() {
        return currentUserService.getCurrentUser();
    }

    private String normalizeOptional(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String normalizeRequired(String s, String messageIfMissing) {
        String t = normalizeOptional(s);
        if (t == null) throw new IllegalArgumentException(messageIfMissing);
        return t;
    }
}