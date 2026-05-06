package com.esprit.helma_backend.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esprit.helma_backend.dto.TransactionDto;
import com.esprit.helma_backend.entities.Transaction;
import com.esprit.helma_backend.entities.Transaction.TransactionType;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.TransactionRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import com.esprit.helma_backend.services.risk.RiskDecision;
import com.esprit.helma_backend.services.risk.RiskEngineService;

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
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository txRepo,
                              UserRepository userRepo,
                              RiskEngineService riskEngine,
                              RiskCaseService riskCaseService,
                              CashFlowService cashFlowService,
                              BurnRateService burnRateService,
                              TrustBadgeService trustBadgeService,
                              AuditLogService auditLogService,
                            CategoryService categoryService ) {
        this.txRepo = txRepo;
        this.userRepo = userRepo;
        this.riskEngine = riskEngine;
        this.riskCaseService = riskCaseService;
        this.cashFlowService = cashFlowService;
        this.burnRateService = burnRateService;
        this.trustBadgeService = trustBadgeService;
        this.auditLogService = auditLogService;
         this.categoryService = categoryService; 
    }

    private static TransactionDto.Response toResponse(Transaction t) {
        return new TransactionDto.Response(
                t.getId(),
                t.getUser().getId(),
                t.getAmount(),
                t.getCategory(),
                t.getType(),
                t.getTxnDate(),
                t.getReceiptUrl()
        );
    }

    public TransactionDto.Response create(TransactionDto.Create req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Transaction t = Transaction.builder()
                .user(user)
                .amount(req.amount())
                .category(req.category())
                .type(req.type())
                .txnDate(Instant.now())
                .receiptUrl(req.receiptUrl())
                .build();

        Transaction saved = txRepo.save(t);
        categoryService.track(user.getId(), saved.getCategory());

        // Step 1 — Risk Engine (EXPENSE only)
        if (saved.getType() == TransactionType.EXPENSE) {
            RiskDecision decision = riskEngine.evaluate(saved);
            System.out.println("=== RISK DEBUG === Transaction created: " + saved.getAmount()
                    + " | Type: " + saved.getType()
                    + " | Risk triggered: " + decision.triggered()
                    + " | Risk level: " + decision.riskLevel()
                    + " | Reasons: " + decision.reasons());
            if (decision.triggered()) {
                riskCaseService.upsertOpenCase(user.getId(), decision.riskLevel(), decision.reasons());
            }
        }

        // Step 2 — Recalculate CashFlow for current month
        LocalDate month = LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1);
        cashFlowService.recalculateForUser(user.getId(), month);

        // Step 3 — Recompute Burn Rate + Runway
        burnRateService.compute(user.getId());

        // Step 4 — Recompute Trust Badge
        trustBadgeService.compute(user.getId());

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
        t.setAmount(req.amount());
        t.setCategory(req.category());
        t.setType(req.type());
        t.setReceiptUrl(req.receiptUrl());
        return toResponse(txRepo.save(t));
    }

    public void delete(Long id) {
        if (!txRepo.existsById(id)) {
            throw new IllegalArgumentException("Transaction not found");
        }
        txRepo.deleteById(id);
    }
}