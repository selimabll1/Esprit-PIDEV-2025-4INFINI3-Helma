package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.TransactionDto;
import com.esprit.helma_backend.entities.Transaction;
import com.esprit.helma_backend.entities.Transaction.TransactionType;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.TransactionRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import com.esprit.helma_backend.services.risk.RiskDecision;
import com.esprit.helma_backend.services.risk.RiskEngineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository txRepo;
    private final UserRepository userRepo;
    private final RiskEngineService riskEngine;
    private final RiskCaseService riskCaseService;
    private final CashFlowService cashFlowService;
    private final BurnRateService burnRateService;
    private final TrustBadgeService trustBadgeService;
    private final AuditLogService auditLogService;

    public TransactionService(TransactionRepository txRepo,
                              UserRepository userRepo,
                              RiskEngineService riskEngine,
                              RiskCaseService riskCaseService,
                              CashFlowService cashFlowService,
                              BurnRateService burnRateService,
                              TrustBadgeService trustBadgeService,
                              AuditLogService auditLogService) {
        this.txRepo = txRepo;
        this.userRepo = userRepo;
        this.riskEngine = riskEngine;
        this.riskCaseService = riskCaseService;
        this.cashFlowService = cashFlowService;
        this.burnRateService = burnRateService;
        this.trustBadgeService = trustBadgeService;
        this.auditLogService = auditLogService;
    }

    private static TransactionDto.Response toResponse(Transaction t) {
        return new TransactionDto.Response(
                t.getId(),
                t.getUser().getId(),
                t.getAmount(),
                t.getCategory(),
                t.getType(),
                t.getTxnDate()
        );
    }

    private LocalDate monthStart(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1);
    }

    private void recomputeAllForUser(Long userId, Instant... instants) {
        Set<LocalDate> months = new LinkedHashSet<>();
        for (Instant instant : instants) {
            if (instant != null) {
                months.add(monthStart(instant));
            }
        }

        if (months.isEmpty()) {
            months.add(LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1));
        }

        LocalDate firstAffectedMonth = months.stream()
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1));

        LocalDate latestAffectedMonth = months.stream()
                .max(LocalDate::compareTo)
                .orElse(firstAffectedMonth);

        Instant latestTxnInstant = txRepo.findMaxTxnDateByUserId(userId);
        LocalDate latestExistingTxnMonth = latestTxnInstant != null
                ? monthStart(latestTxnInstant)
                : latestAffectedMonth;

        LocalDate finalMonth = latestExistingTxnMonth.isAfter(latestAffectedMonth)
                ? latestExistingTxnMonth
                : latestAffectedMonth;

        cashFlowService.recalculateRangeForUser(userId, firstAffectedMonth, finalMonth);
        burnRateService.compute(userId);
        trustBadgeService.compute(userId);
    }

    public TransactionDto.Response create(TransactionDto.Create req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Instant txnInstant = req.txnDate() != null ? req.txnDate() : Instant.now();

        Transaction t = Transaction.builder()
                .user(user)
                .amount(req.amount())
                .category(req.category())
                .type(req.type())
                .txnDate(txnInstant)
                .build();

        Transaction saved = txRepo.save(t);

        if (saved.getType() == TransactionType.EXPENSE) {
            RiskDecision decision = riskEngine.evaluate(saved);
            if (decision.triggered()) {
                riskCaseService.upsertOpenCase(user.getId(), decision.riskLevel(), decision.reasons());
            }
        }

        recomputeAllForUser(user.getId(), saved.getTxnDate());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionDto.Response getById(Long id) {
        Transaction t = txRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        return toResponse(t);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto.Response> getAll() {
        return txRepo.findAll().stream().map(TransactionService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDto.Response> getByUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return txRepo.findByUser(user).stream().map(TransactionService::toResponse).toList();
    }

    public TransactionDto.Response update(Long id, TransactionDto.Update req) {
        Transaction t = txRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        Instant oldTxnDate = t.getTxnDate();
        Instant newTxnDate = req.txnDate() != null ? req.txnDate() : oldTxnDate;

        t.setAmount(req.amount());
        t.setCategory(req.category());
        t.setType(req.type());
        t.setTxnDate(newTxnDate);

        Transaction saved = txRepo.save(t);

        recomputeAllForUser(saved.getUser().getId(), oldTxnDate, newTxnDate);

        return toResponse(saved);
    }

    public void delete(Long id) {
        Transaction t = txRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        Long userId = t.getUser().getId();
        Instant txnDate = t.getTxnDate();

        txRepo.delete(t);

        recomputeAllForUser(userId, txnDate);
    }
}