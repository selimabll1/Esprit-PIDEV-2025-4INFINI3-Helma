package com.helma.helmabackend.service.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.PaymentResponse;
import com.helma.helmabackend.dto.crowdfunding.PaymentStatusPatchRequest;
import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.Payment;
import com.helma.helmabackend.entity.crowdfunding.Pledge;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.PaymentProvider;
import com.helma.helmabackend.entity.crowdfunding.enums.PaymentStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.PledgeStatus;
import com.helma.helmabackend.entity.user.Role;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.exception.UnauthorizedException;
import com.helma.helmabackend.repository.crowdfunding.ApplicationRaiseRepository;
import com.helma.helmabackend.repository.crowdfunding.PaymentRepository;
import com.helma.helmabackend.repository.crowdfunding.PledgeRepository;
import com.helma.helmabackend.service.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final EnumSet<PaymentStatus> ACTIVE_STATUSES = EnumSet.of(
            PaymentStatus.CREATED,
            PaymentStatus.PENDING_PROVIDER
    );

    private final PaymentRepository paymentRepo;
    private final PledgeRepository pledgeRepo;
    private final ApplicationRaiseRepository applicationRaiseRepo;
    private final CurrentUserService currentUserService;
    private final CampaignFundingService campaignFundingService;

    @Transactional
    public PaymentResponse initiateMyPayment(Long pledgeId) {
        User me = currentUser();
        requireInvestor(me);

        Pledge pledge = pledgeRepo.findById(pledgeId)
                .orElseThrow(() -> new IllegalArgumentException("Pledge not found: " + pledgeId));

        if (!pledge.getBackerUserId().equals(me.getId())) {
            throw new UnauthorizedException("You can only pay your own pledge.");
        }

        if (pledge.getStatus() == PledgeStatus.PAID) {
            throw new IllegalStateException("This pledge is already paid.");
        }

        if (pledge.getStatus() == PledgeStatus.REFUNDED) {
            throw new IllegalStateException("Refunded pledges cannot be paid again.");
        }

        ApplicationRaise campaign = requireApprovedCampaign(pledge.getApplicationRaiseId());

        Payment existingActive = paymentRepo
                .findFirstByPledgeIdAndStatusInOrderByCreatedAtDesc(pledgeId, ACTIVE_STATUSES)
                .orElse(null);

        if (existingActive != null) {
            return toPaymentResponse(existingActive, pledge, campaign);
        }

        Payment payment = new Payment();
        payment.setPledgeId(pledge.getId());
        payment.setApplicationRaiseId(pledge.getApplicationRaiseId());
        payment.setBackerUserId(pledge.getBackerUserId());
        payment.setAmount(pledge.getAmount());
        payment.setCurrency(pledge.getCurrency());
        payment.setProvider(PaymentProvider.MOCK);
        payment.setProviderReference("mock_pay_" + UUID.randomUUID().toString().replace("-", ""));
        payment.setCheckoutSessionId("mock_session_" + UUID.randomUUID().toString().replace("-", ""));
        payment.setStatus(PaymentStatus.PENDING_PROVIDER);

        if (pledge.getStatus() != PledgeStatus.PENDING) {
            pledge.setStatus(PledgeStatus.PENDING);
            pledgeRepo.save(pledge);
        }

        Payment saved = paymentRepo.save(payment);
        return toPaymentResponse(saved, pledge, campaign);
    }


    @Transactional
    public void cancelActivePaymentsForPledge(Long pledgeId, String reason) {
        List<Payment> activePayments = paymentRepo.findByPledgeIdAndStatusInOrderByCreatedAtDesc(pledgeId, ACTIVE_STATUSES);
        if (activePayments.isEmpty()) {
            return;
        }

        for (Payment payment : activePayments) {
            payment.setStatus(PaymentStatus.CANCELED);
            payment.setFailureReason(cleanFailureReason(reason, "Pledge updated before checkout completion."));
            paymentRepo.save(payment);
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listMyPayments() {
        User me = currentUser();
        requireInvestor(me);

        return paymentRepo.findByBackerUserIdOrderByCreatedAtDesc(me.getId())
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getMyPaymentById(Long paymentId) {
        User me = currentUser();
        requireInvestor(me);

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (!payment.getBackerUserId().equals(me.getId())) {
            throw new UnauthorizedException("You can only view your own payments.");
        }

        return toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> adminListAllPayments() {
        User me = currentUser();
        requireAdminOrCompliance(me);

        return paymentRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .limit(100)
                .map(this::toPaymentResponseSafe)
                .toList();
    }

    @Transactional
    public PaymentResponse mockPatchMyPaymentStatus(Long paymentId, PaymentStatusPatchRequest req) {
        User me = currentUser();
        requireInvestor(me);

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (!payment.getBackerUserId().equals(me.getId())) {
            throw new UnauthorizedException("You can only update your own payments.");
        }

        return applyMockPaymentStatus(payment, req);
    }

    @Transactional
    public PaymentResponse adminMockPatchPaymentStatus(Long paymentId, PaymentStatusPatchRequest req) {
        User me = currentUser();
        requireAdminOrCompliance(me);

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        return applyMockPaymentStatus(payment, req);
    }

    private PaymentResponse applyMockPaymentStatus(Payment payment, PaymentStatusPatchRequest req) {
        if (req == null || req.status == null) {
            throw new IllegalArgumentException("status is required");
        }

        PaymentStatus target = req.status;
        PaymentStatus current = payment.getStatus();

        if (current == target) {
            return toPaymentResponse(payment);
        }

        Pledge pledge = pledgeRepo.findById(payment.getPledgeId())
                .orElseThrow(() -> new IllegalArgumentException("Pledge not found: " + payment.getPledgeId()));

        boolean allowed =
                ((current == PaymentStatus.CREATED || current == PaymentStatus.PENDING_PROVIDER)
                        && (target == PaymentStatus.SUCCEEDED || target == PaymentStatus.FAILED || target == PaymentStatus.CANCELED))
                || (current == PaymentStatus.SUCCEEDED && target == PaymentStatus.REFUNDED);

        if (!allowed) {
            throw new IllegalStateException("Invalid payment status transition: " + current + " -> " + target);
        }

        switch (target) {
            case SUCCEEDED -> {
                payment.setStatus(PaymentStatus.SUCCEEDED);
                payment.setFailureReason(null);
                if (payment.getPaidAt() == null) {
                    payment.setPaidAt(Instant.now());
                }
                pledge.setStatus(PledgeStatus.PAID);
            }
            case FAILED -> {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(cleanFailureReason(req.failureReason, "Mock payment failed."));
                pledge.setStatus(PledgeStatus.FAILED);
            }
            case CANCELED -> {
                payment.setStatus(PaymentStatus.CANCELED);
                payment.setFailureReason(cleanFailureReason(req.failureReason, "Mock payment canceled."));
                pledge.setStatus(PledgeStatus.CANCELED);
            }
            case REFUNDED -> {
                payment.setStatus(PaymentStatus.REFUNDED);
                if (payment.getRefundedAt() == null) {
                    payment.setRefundedAt(Instant.now());
                }
                payment.setFailureReason(cleanFailureReason(req.failureReason, "Payment refunded."));
                pledge.setStatus(PledgeStatus.REFUNDED);
            }
            default -> throw new IllegalArgumentException("Unsupported mock payment status: " + target);
        }

        pledgeRepo.save(pledge);
        Payment saved = paymentRepo.save(payment);
        campaignFundingService.syncCampaignRaisedAmount(saved.getApplicationRaiseId());

        return toPaymentResponse(saved);
    }

    private String cleanFailureReason(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        Pledge pledge = pledgeRepo.findById(payment.getPledgeId())
                .orElseThrow(() -> new IllegalArgumentException("Pledge not found: " + payment.getPledgeId()));
        ApplicationRaise campaign = applicationRaiseRepo.findById(payment.getApplicationRaiseId())
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + payment.getApplicationRaiseId()));
        return toPaymentResponse(payment, pledge, campaign);
    }

    private PaymentResponse toPaymentResponse(Payment payment, Pledge pledge, ApplicationRaise campaign) {
        PaymentResponse r = new PaymentResponse();
        r.id = payment.getId();
        r.pledgeId = payment.getPledgeId();
        r.applicationRaiseId = payment.getApplicationRaiseId();
        r.backerUserId = payment.getBackerUserId();
        r.amount = payment.getAmount();
        r.currency = payment.getCurrency();
        r.provider = payment.getProvider();
        r.providerReference = payment.getProviderReference();
        r.checkoutSessionId = payment.getCheckoutSessionId();
        r.checkoutUrl = buildMockCheckoutUrl(payment);
        r.status = payment.getStatus();
        r.failureReason = payment.getFailureReason();
        r.pledgeStatus = pledge.getStatus();
        r.campaignBusinessName = campaign.getBusinessName();
        r.paidAt = payment.getPaidAt();
        r.refundedAt = payment.getRefundedAt();
        r.createdAt = payment.getCreatedAt();
        r.updatedAt = payment.getUpdatedAt();
        return r;
    }

    private String buildMockCheckoutUrl(Payment payment) {
        if (payment.getCheckoutSessionId() == null) {
            return null;
        }
        return "/api/crowdfunding/my-payments/" + payment.getId() + "/mock-checkout?session=" + payment.getCheckoutSessionId();
    }

    private ApplicationRaise requireApprovedCampaign(Long id) {
        ApplicationRaise campaign = applicationRaiseRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));

        if (campaign.getStatus() != ApplicationRaiseStatus.APPROVED) {
            throw new IllegalStateException("This campaign is not available for investors.");
        }
        return campaign;
    }

    private PaymentResponse toPaymentResponseSafe(Payment payment) {
        Pledge pledge = pledgeRepo.findById(payment.getPledgeId()).orElse(null);
        ApplicationRaise campaign = applicationRaiseRepo.findById(payment.getApplicationRaiseId()).orElse(null);

        PaymentResponse r = new PaymentResponse();

        r.id = payment.getId();
        r.pledgeId = payment.getPledgeId();
        r.applicationRaiseId = payment.getApplicationRaiseId();
        r.backerUserId = payment.getBackerUserId();

        r.amount = payment.getAmount();
        r.currency = payment.getCurrency();

        r.provider = payment.getProvider();
        r.providerReference = payment.getProviderReference();
        r.checkoutSessionId = payment.getCheckoutSessionId();
        r.checkoutUrl = buildMockCheckoutUrl(payment);

        r.status = payment.getStatus();
        r.failureReason = payment.getFailureReason();

        r.pledgeStatus = pledge != null ? pledge.getStatus() : null;
        r.campaignBusinessName = campaign != null ? campaign.getBusinessName() : "Missing campaign";

        r.paidAt = payment.getPaidAt();
        r.refundedAt = payment.getRefundedAt();
        r.createdAt = payment.getCreatedAt();
        r.updatedAt = payment.getUpdatedAt();

        return r;
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
