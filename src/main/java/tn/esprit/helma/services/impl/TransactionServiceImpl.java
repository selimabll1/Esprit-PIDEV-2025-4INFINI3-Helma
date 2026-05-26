package tn.esprit.helma.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.helma.entities.BankAccount;
import tn.esprit.helma.entities.Transaction;
import tn.esprit.helma.enums.AccountStatus;
import tn.esprit.helma.enums.TransactionPeriodicity;
import tn.esprit.helma.enums.TransactionStatus;
import tn.esprit.helma.enums.TransactionType;
import tn.esprit.helma.repositories.BankAccountRepository;
import tn.esprit.helma.repositories.TransactionRepository;
import tn.esprit.helma.dtos.BeneficiaryStatisticsDTO;
import tn.esprit.helma.dtos.RecommendationDTO;
import tn.esprit.helma.dtos.TransactionStatisticsDTO;
import tn.esprit.helma.services.FraudAlertMailService;
import tn.esprit.helma.services.ICurrencyConversionService;
import tn.esprit.helma.services.ITransactionService;
import tn.esprit.helma.services.ISavedBeneficiaryService;
import tn.esprit.helma.services.auth.CurrentUserProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implémentation de la logique métier autour des transactions.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements ITransactionService {

    private static final int REVIEW_THRESHOLD = 40;
    private static final int ALERT_THRESHOLD = 70;

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final ISavedBeneficiaryService savedBeneficiaryService;
    private final FraudAlertMailService fraudAlertMailService;
    private final CurrentUserProvider currentUserProvider;
    private final ICurrencyConversionService currencyConversionService;

    @Override
    public Transaction createTransaction(Long accountId, Transaction transaction) {
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));
        assertCurrentUserOwnsAccount(bankAccount);

        validateAccount(bankAccount);
        validateIncomingTransaction(transaction);

        transaction.setBankAccount(bankAccount);
        applyPeriodicityRules(transaction);

        RiskEvaluation evaluation = evaluateRisk(bankAccount, transaction);
        transaction.setRiskScore(evaluation.score());
        boolean blocked = evaluation.blocked();

        // Score >= 70 → SUSPICIOUS + email, toujours, PIN ou pas
        if (blocked) {
            transaction.setStatus(TransactionStatus.SUSPICIOUS);
            log.warn("Transaction {} bloquée (score={})", transaction.getBeneficiaryRib(), evaluation.score());
            Transaction saved = transactionRepository.save(transaction);
            fraudAlertMailService.sendSuspiciousTransactionAlert(saved);
            return saved;
        }

        // Score < 70 : PIN requis si configuré
        if (requiresPinVerification(transaction)) {
            transaction.setStatus(TransactionStatus.PENDING);
            Transaction savedPending = transactionRepository.save(transaction);
            log.info("Transaction {} marquée PENDING car PIN requis", savedPending.getId());
            return savedPending;
        }

        if (transaction.getPeriodicity() == TransactionPeriodicity.NOW && evaluation.score() < REVIEW_THRESHOLD) {
            debitAccount(bankAccount, transaction.getAmount());
            creditBeneficiaryAccountIfInternal(transaction);
            recordExternalConversionIfApplicable(bankAccount, transaction);
            transaction.setStatus(TransactionStatus.CONFIRMED);
            transaction.setConfirmedAt(LocalDateTime.now());
            transaction.setLastExecutionDate(LocalDateTime.now());
        } else {
            transaction.setStatus(TransactionStatus.PENDING);
        }

        Transaction saved = transactionRepository.save(transaction);
        persistBeneficiaryIfNeeded(bankAccount, saved);
        incrementTransferCount(bankAccount, saved);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByType(Long accountId, TransactionType type) {
        assertCurrentUserOwnsAccount(accountId);
        return transactionRepository.findByBankAccountIdAndType(accountId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByStatus(Long accountId, TransactionStatus status) {
        assertCurrentUserOwnsAccount(accountId);
        return transactionRepository.findByBankAccountIdAndStatus(accountId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByAmountRange(Long accountId, BigDecimal minAmount, BigDecimal maxAmount) {
        assertCurrentUserOwnsAccount(accountId);
        return transactionRepository.findByBankAccountId(accountId).stream()
                .filter(tx -> (minAmount == null || tx.getAmount().compareTo(minAmount) >= 0)
                        && (maxAmount == null || tx.getAmount().compareTo(maxAmount) <= 0))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsBetweenDates(Long accountId, LocalDateTime start, LocalDateTime end) {
        assertCurrentUserOwnsAccount(accountId);
        return transactionRepository.findBetweenDates(accountId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalSpentThisMonth(Long accountId) {
        assertCurrentUserOwnsAccount(accountId);
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
        return transactionRepository.sumSince(accountId, TransactionStatus.CONFIRMED, start);
    }

        @Override
        @Transactional(readOnly = true)
        public TransactionStatisticsDTO getMonthlyStatistics(Long accountId) {
        assertCurrentUserOwnsAccount(accountId);
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime end = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Transaction> monthlyTransactions = transactionRepository.findBetweenDates(accountId, start, end);
        List<Transaction> confirmedTransactions = monthlyTransactions.stream()
            .filter(tx -> tx.getStatus() == TransactionStatus.CONFIRMED)
            .collect(Collectors.toList());

        BigDecimal totalSpentThisMonth = confirmedTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> spendingByCategory = confirmedTransactions.stream()
            .collect(Collectors.groupingBy(
                tx -> tx.getCategory() == null || tx.getCategory().isBlank() ? "Autres" : tx.getCategory(),
                Collectors.mapping(Transaction::getAmount,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        List<BeneficiaryStatisticsDTO> topBeneficiaries = confirmedTransactions.stream()
            .collect(Collectors.groupingBy(this::buildBeneficiaryKey, Collectors.toList()))
            .entrySet().stream()
            .map(entry -> {
                List<Transaction> beneficiaryTransactions = entry.getValue();
                Transaction first = beneficiaryTransactions.get(0);
                BigDecimal totalAmount = beneficiaryTransactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return BeneficiaryStatisticsDTO.builder()
                    .beneficiaryName(first.getBeneficiaryName())
                    .beneficiaryRib(first.getBeneficiaryRib())
                    .transactionCount((long) beneficiaryTransactions.size())
                    .totalAmount(totalAmount)
                    .build();
            })
            .sorted((left, right) -> right.getTotalAmount().compareTo(left.getTotalAmount()))
            .limit(5)
            .collect(Collectors.toList());

        Map<String, Long> transactionCountByStatus = monthlyTransactions.stream()
            .collect(Collectors.groupingBy(
                tx -> tx.getStatus() == null ? "UNKNOWN" : tx.getStatus().name(),
                Collectors.counting()));

        return TransactionStatisticsDTO.builder()
            .totalSpentThisMonth(totalSpentThisMonth)
            .spendingByCategory(spendingByCategory)
            .topBeneficiaries(topBeneficiaries)
            .transactionCountByStatus(transactionCountByStatus)
            .build();
        }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationDTO> getRecommendations(Long accountId) {
        assertCurrentUserOwnsAccount(accountId);
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime start = currentMonth.minusMonths(3).atDay(1).atStartOfDay();
        LocalDateTime end = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Transaction> history = transactionRepository.findBetweenDates(accountId, start, end);
        List<Transaction> confirmedCurrentMonth = history.stream()
                .filter(tx -> tx.getStatus() == TransactionStatus.CONFIRMED)
                .filter(tx -> isInMonth(tx, currentMonth))
                .collect(Collectors.toList());

        List<RecommendationDTO> recommendations = new ArrayList<>();

        addCategoryIncreaseRecommendation(recommendations, history, currentMonth);
        addDayPatternRecommendation(recommendations, confirmedCurrentMonth);
        addCategoryDecreaseRecommendation(recommendations, history, currentMonth);
        addFrequentBeneficiaryRecommendation(recommendations, confirmedCurrentMonth);
        addSecurityRecommendation(recommendations, accountId);
        addSavingsRecommendation(recommendations, history, confirmedCurrentMonth, currentMonth);

        if (recommendations.isEmpty()) {
            recommendations.add(RecommendationDTO.builder()
                    .type("INFO")
                    .priority("LOW")
                    .title("Aucune recommandation critique")
                    .message("Ton activité récente est stable. Continue à suivre tes transactions régulièrement.")
                    .action("Consulter les statistiques mensuelles")
                    .value("STABLE")
                    .build());
        }

        return recommendations;
    }

    @Override
    @Transactional(readOnly = true)
    public String generateMonthlySummary(Long accountId) {
        assertCurrentUserOwnsAccount(accountId);
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDay = currentMonth.atDay(1);
        LocalDate endDay = currentMonth.atEndOfMonth();
        List<Transaction> monthlyTransactions = transactionRepository.findBetweenDates(
                accountId,
                startDay.atStartOfDay(),
                endDay.atTime(23, 59, 59));

        BigDecimal total = monthlyTransactions.stream()
                .filter(tx -> tx.getStatus() == TransactionStatus.CONFIRMED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String topCategory = monthlyTransactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCategory, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return String.format(
                "Ce mois vos dépenses sont de %s TND. Catégorie la plus active: %s.",
                total,
                topCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> searchTransactions(Long accountId, Map<String, Object> filters) {
        assertCurrentUserOwnsAccount(accountId);
        List<Transaction> base = new ArrayList<>(transactionRepository.findByBankAccountId(accountId));
        if (filters == null || filters.isEmpty()) {
            return base;
        }

        return base.stream()
                .filter(tx -> filterByType(filters, tx))
                .filter(tx -> filterByStatus(filters, tx))
                .filter(tx -> filterByAmount(filters, tx))
                .collect(Collectors.toList());
    }

    @Override
    public void executeScheduledTransactions() {
        LocalDateTime now = LocalDateTime.now();
        List<Transaction> dueTransactions = transactionRepository
                .findByPeriodicityAndStatusAndScheduledDateLessThanEqual(TransactionPeriodicity.SCHEDULED,
                        TransactionStatus.PENDING,
                        now);

        dueTransactions.forEach(this::executeDeferredTransaction);
    }

    @Override
    public void executePermanentTransactions() {
        LocalDateTime now = LocalDateTime.now();
        List<Transaction> dueTransactions = transactionRepository
                .findByPeriodicityAndNextExecutionDateLessThanEqual(TransactionPeriodicity.PERMANENT, now);

        dueTransactions.stream()
                .filter(transaction -> transaction.getStatus() != TransactionStatus.SUSPICIOUS)
                .forEach(transaction -> executeDeferredTransaction(transaction, true));
    }

    @Override
    public Transaction confirmSuspiciousTransaction(Long transactionId) {
        Transaction transaction = getTransactionWithAccount(transactionId);

        if (transaction.getStatus() != TransactionStatus.SUSPICIOUS) {
            throw new IllegalStateException("La transaction n'est pas en attente de confirmation.");
        }

        BankAccount bankAccount = transaction.getBankAccount();
        boolean hasPinConfigured = bankAccount.getPinHash() != null && !bankAccount.getPinHash().isBlank();
        int riskScore = transaction.getRiskScore() != null ? transaction.getRiskScore() : 0;

        // For high-risk transactions, require that a PIN is configured; if not, ask the user to set one first
        if (riskScore >= ALERT_THRESHOLD && !hasPinConfigured) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PIN_NOT_CONFIGURED: configurez un PIN sur votre compte avant de confirmer les transactions à haut risque.");
        }

        if (requiresPinVerification(transaction)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PIN_REQUIRED");
        }

        boolean recurring = transaction.getPeriodicity() == TransactionPeriodicity.PERMANENT;
        finalizeExecution(bankAccount, transaction, recurring);
        log.info("Transaction {} confirmée manuellement", transactionId);
        return transaction;
    }

    @Override
    public Transaction rejectSuspiciousTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction introuvable"));

        if (transaction.getStatus() != TransactionStatus.SUSPICIOUS) {
            throw new IllegalStateException("La transaction n'est pas en attente de confirmation.");
        }

        transaction.setStatus(TransactionStatus.CANCELED);
        transaction.setConfirmedAt(null);
        transactionRepository.save(transaction);
        log.info("Transaction {} rejetée par l'utilisateur", transactionId);
        return transaction;
    }
    @Override
    public Transaction approvePendingTransaction(Long transactionId) {
        Transaction transaction = getTransactionWithAccount(transactionId);

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("La transaction n'est pas en attente de révision (statut: " + transaction.getStatus() + ")");
        }

        BankAccount bankAccount = transaction.getBankAccount();
        boolean hasPinConfigured = bankAccount.getPinHash() != null && !bankAccount.getPinHash().isBlank();
        int riskScore = transaction.getRiskScore() != null ? transaction.getRiskScore() : 0;

        if (riskScore >= ALERT_THRESHOLD && !hasPinConfigured) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PIN_NOT_CONFIGURED: configurez un PIN sur votre compte avant de confirmer les transactions à haut risque.");
        }

        if (requiresPinVerification(transaction)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PIN_REQUIRED");
        }

        boolean recurring = transaction.getPeriodicity() == TransactionPeriodicity.PERMANENT;
        finalizeExecution(bankAccount, transaction, recurring);
        log.info("Transaction {} approuvée manuellement", transactionId);
        return transaction;
    }
    private void validateAccount(BankAccount bankAccount) {
        if (bankAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Le compte n'est pas actif");
        }
    }

    private void validateIncomingTransaction(Transaction transaction) {
        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        if (transaction.getType() == null) {
            throw new IllegalArgumentException("Le type de transaction est requis");
        }
    }

    private void applyPeriodicityRules(Transaction transaction) {
        TransactionPeriodicity periodicity = transaction.getPeriodicity();
        if (periodicity == null) {
            periodicity = TransactionPeriodicity.NOW;
            transaction.setPeriodicity(periodicity);
        }

        if (periodicity == TransactionPeriodicity.SCHEDULED) {
            if (transaction.getScheduledDate() == null) {
                throw new IllegalArgumentException("scheduledDate est requis pour une transaction programmée");
            }
        } else {
            transaction.setScheduledDate(null);
        }

        if (periodicity == TransactionPeriodicity.PERMANENT) {
            if (transaction.getNextExecutionDate() == null) {
                transaction.setNextExecutionDate(LocalDateTime.now().plusDays(30));
            }
        } else {
            transaction.setNextExecutionDate(null);
        }
    }

    private RiskEvaluation evaluateRisk(BankAccount account, Transaction transaction) {
        int score = 0;

        boolean newBeneficiary = isNewBeneficiary(account, transaction);
        if (newBeneficiary) score += 40;

        List<Transaction> recentTransactions = transactionRepository
                .findTop20ByBankAccountIdOrderByCreatedAtDesc(account.getId());
        int amountScore = evaluateAmountRisk(transaction.getAmount(), recentTransactions);
        score += amountScore;

        boolean oddHour = isOddHour(determineReferenceTime(transaction));
        if (oddHour) score += 20;

        int freqScore = evaluateFrequencyRisk(account.getId());
        score += freqScore;

        boolean blocked = score >= ALERT_THRESHOLD;
        log.info("[RISK] tx={} montant={} score={} (newBenef={}/+40, amount=+{}, oddHour={}/+20, freq=+{}) → {}",
                transaction.getBeneficiaryRib(), transaction.getAmount(), score,
                newBeneficiary, amountScore, oddHour, freqScore,
                blocked ? "SUSPICIOUS" : "ok");

        return new RiskEvaluation(score, blocked);
    }

    private boolean isNewBeneficiary(BankAccount account, Transaction transaction) {
        if (transaction.getBeneficiaryRib() == null) {
            return false;
        }
        return savedBeneficiaryService.getBeneficiaryByRib(account.getUserId(), transaction.getBeneficiaryRib()).isEmpty();
    }

    private int evaluateAmountRisk(BigDecimal amount, List<Transaction> recentTransactions) {
        if (recentTransactions.isEmpty()) {
            return amount.compareTo(BigDecimal.valueOf(5000)) > 0 ? 35 : 0;
        }

        List<BigDecimal> values = recentTransactions.stream()
                .map(Transaction::getAmount)
                .collect(Collectors.toList());
        double zScore = computeZScore(amount, values);

        if (zScore >= 3) {
            return 50;
        } else if (zScore >= 2) {
            return 30;
        }
        return 0;
    }

    private double computeZScore(BigDecimal amount, List<BigDecimal> baseline) {
        if (baseline.isEmpty()) {
            return 0;
        }

        double mean = baseline.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0);

        double variance = baseline.stream()
                .mapToDouble(value -> Math.pow(value.doubleValue() - mean, 2))
                .average()
                .orElse(0);

        double stdDev = Math.sqrt(variance);
        if (stdDev == 0) {
            return 0;
        }

        return (amount.doubleValue() - mean) / stdDev;
    }

    private boolean isOddHour(LocalDateTime referenceTime) {
        int hour = referenceTime.getHour();
        return hour < 6 || hour >= 22;
    }

    private LocalDateTime determineReferenceTime(Transaction transaction) {
        if (transaction.getPeriodicity() == TransactionPeriodicity.SCHEDULED && transaction.getScheduledDate() != null) {
            return transaction.getScheduledDate();
        }
        if (transaction.getPeriodicity() == TransactionPeriodicity.PERMANENT && transaction.getNextExecutionDate() != null) {
            return transaction.getNextExecutionDate();
        }
        return LocalDateTime.now();
    }

    private int evaluateFrequencyRisk(Long accountId) {
        int score = 0;
        Optional<Transaction> lastTransaction = transactionRepository
                .findTopByBankAccountIdOrderByCreatedAtDesc(accountId);
        if (lastTransaction.isPresent()) {
            Duration sinceLast = Duration.between(lastTransaction.get().getCreatedAt(), LocalDateTime.now());
            if (!sinceLast.isNegative() && sinceLast.toMinutes() <= 2) {
                score += 10;
            }
        }

        long burstCount = transactionRepository.countByBankAccountIdAndCreatedAtAfter(accountId, LocalDateTime.now().minusHours(1));
        if (burstCount > 10) {
            score += 10;
        }
        return score;
    }

    private void debitAccount(BankAccount account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Solde insuffisant");
        }
        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(LocalDateTime.now());
        bankAccountRepository.save(account);
    }

    private void creditBeneficiaryAccountIfInternal(Transaction transaction) {
        String beneficiaryRib = transaction.getBeneficiaryRib();
        if (beneficiaryRib == null) return;

        bankAccountRepository.findByRib(beneficiaryRib)
                .filter(destination -> !destination.getId().equals(transaction.getBankAccount().getId()))
                .ifPresent(destination -> {
                    String senderCurrency = transaction.getBankAccount().getCurrency();
                    String destCurrency = destination.getCurrency();
                    BigDecimal creditAmount = transaction.getAmount();

                    if (!senderCurrency.equalsIgnoreCase(destCurrency)) {
                        java.util.Optional<BigDecimal> converted = currencyConversionService.convert(
                                transaction.getAmount(), senderCurrency, destCurrency);
                        if (converted.isPresent()) {
                            creditAmount = converted.get();
                            transaction.setConvertedAmount(creditAmount);
                            transaction.setTargetCurrency(destCurrency);
                            currencyConversionService.getRate(senderCurrency, destCurrency)
                                    .ifPresent(transaction::setExchangeRate);
                            log.info("Conversion interne: {} {} → {} {} (taux {})",
                                    transaction.getAmount(), senderCurrency,
                                    creditAmount, destCurrency, transaction.getExchangeRate());
                        }
                    }

                    destination.setBalance(destination.getBalance().add(creditAmount));
                    destination.setUpdatedAt(LocalDateTime.now());
                    bankAccountRepository.save(destination);
                });
    }

    private void recordExternalConversionIfApplicable(BankAccount bankAccount, Transaction transaction) {
        String targetCurrency = transaction.getTargetCurrency();
        if (targetCurrency == null || targetCurrency.isBlank()) return;
        if (transaction.getConvertedAmount() != null) return; // déjà traité par le transfert interne

        String senderCurrency = bankAccount.getCurrency();
        if (senderCurrency.equalsIgnoreCase(targetCurrency)) return;

        currencyConversionService.getRate(senderCurrency, targetCurrency).ifPresent(rate -> {
            BigDecimal converted = transaction.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
            transaction.setConvertedAmount(converted);
            transaction.setExchangeRate(rate);
            log.info("Conversion externe enregistrée: {} {} ≈ {} {} (taux {})",
                    transaction.getAmount(), senderCurrency, converted, targetCurrency, rate);
        });
    }

    private void persistBeneficiaryIfNeeded(BankAccount account, Transaction transaction) {
        if (transaction.getBeneficiaryRib() == null) {
            return;
        }
        if (!savedBeneficiaryService.beneficiaryExists(account.getUserId(), transaction.getBeneficiaryRib())) {
            savedBeneficiaryService.saveBeneficiary(
                    account.getUserId(),
                    transaction.getBeneficiaryName(),
                    transaction.getBeneficiaryRib(),
                    transaction.getBeneficiaryName());
        }
    }

    private void incrementTransferCount(BankAccount account, Transaction transaction) {
        if (transaction.getBeneficiaryRib() == null) {
            return;
        }
        savedBeneficiaryService.getBeneficiaryByRib(account.getUserId(), transaction.getBeneficiaryRib())
                .ifPresent(beneficiary -> savedBeneficiaryService.incrementTransferCount(beneficiary.getId()));
    }

    private void assertCurrentUserOwnsAccount(Long accountId) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));
        assertCurrentUserOwnsAccount(account);
    }

    private void assertCurrentUserOwnsAccount(BankAccount account) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (!currentUserId.equals(account.getUserId())) {
            throw new IllegalArgumentException("Accès refusé: ce compte n'appartient pas à l'utilisateur courant");
        }
    }

    private String buildBeneficiaryKey(Transaction transaction) {
        String name = transaction.getBeneficiaryName() == null ? "Inconnu" : transaction.getBeneficiaryName();
        String rib = transaction.getBeneficiaryRib() == null ? "" : transaction.getBeneficiaryRib();
        return name + "|" + rib;
    }

    private void addCategoryIncreaseRecommendation(List<RecommendationDTO> recommendations,
                                                   List<Transaction> history,
                                                   YearMonth currentMonth) {
        Map<String, BigDecimal> currentByCategory = history.stream()
                .filter(tx -> tx.getStatus() == TransactionStatus.CONFIRMED)
                .filter(tx -> isInMonth(tx, currentMonth))
                .collect(Collectors.groupingBy(
                        tx -> tx.getCategory() == null || tx.getCategory().isBlank() ? "Autres" : tx.getCategory(),
                        Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        String selectedCategory = null;
        double selectedIncrease = 0;
        BigDecimal selectedCurrentAmount = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : currentByCategory.entrySet()) {
            String category = entry.getKey();
            BigDecimal currentAmount = entry.getValue();
            BigDecimal previousTotal = BigDecimal.ZERO;

            for (int i = 1; i <= 3; i++) {
                YearMonth previousMonth = currentMonth.minusMonths(i);
                BigDecimal monthTotal = history.stream()
                        .filter(tx -> tx.getStatus() == TransactionStatus.CONFIRMED)
                        .filter(tx -> isInMonth(tx, previousMonth))
                        .filter(tx -> category.equals(normalizeCategory(tx.getCategory())))
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                previousTotal = previousTotal.add(monthTotal);
            }

            BigDecimal previousAverage = previousTotal.divide(BigDecimal.valueOf(3), 2, BigDecimal.ROUND_HALF_UP);
            if (previousAverage.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            double increasePercent = currentAmount.subtract(previousAverage)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousAverage, 2, BigDecimal.ROUND_HALF_UP)
                    .doubleValue();

            if (increasePercent >= 20 && increasePercent > selectedIncrease) {
                selectedIncrease = increasePercent;
                selectedCategory = category;
                selectedCurrentAmount = currentAmount;
            }
        }

        if (selectedCategory != null) {
            recommendations.add(RecommendationDTO.builder()
                    .type("BUDGET")
                    .priority("MEDIUM")
                    .title("Hausse de dépenses détectée")
                    .message(String.format("Tes dépenses %s ont augmenté de %.0f%% ce mois.", selectedCategory, selectedIncrease))
                    .action("Fixer un plafond mensuel")
                    .value(selectedCurrentAmount + " TND")
                    .build());
        }
    }

    private void addFrequentBeneficiaryRecommendation(List<RecommendationDTO> recommendations,
                                                      List<Transaction> confirmedCurrentMonth) {
        Map<String, List<Transaction>> byBeneficiary = confirmedCurrentMonth.stream()
                .filter(tx -> tx.getBeneficiaryName() != null && !tx.getBeneficiaryName().isBlank())
                .collect(Collectors.groupingBy(this::buildBeneficiaryKey));

        byBeneficiary.entrySet().stream()
                .max((left, right) -> Integer.compare(left.getValue().size(), right.getValue().size()))
                .ifPresent(entry -> {
                    int count = entry.getValue().size();
                    if (count >= 3) {
                        Transaction first = entry.getValue().get(0);
                        recommendations.add(RecommendationDTO.builder()
                                .type("BENEFICIARY")
                                .priority("LOW")
                                .title("Bénéficiaire fréquent")
                                .message(first.getBeneficiaryName() + " est utilisé " + count + " fois ce mois.")
                                .action("Ajouter aux favoris")
                                .value(first.getBeneficiaryRib())
                                .build());
                    }
                });
    }

    private void addSecurityRecommendation(List<RecommendationDTO> recommendations, Long accountId) {
        long pendingCount = transactionRepository.findByBankAccountIdAndStatus(accountId, TransactionStatus.PENDING).size();
        long suspiciousCount = transactionRepository.findByBankAccountIdAndStatus(accountId, TransactionStatus.SUSPICIOUS).size();

        if (pendingCount > 0 || suspiciousCount > 0) {
            recommendations.add(RecommendationDTO.builder()
                    .type("SECURITY")
                    .priority("HIGH")
                    .title("Transactions à valider")
                    .message(String.format("%d pending et %d suspicious nécessitent une validation.", pendingCount, suspiciousCount))
                    .action("Ouvrir la liste des transactions à valider")
                    .value("PENDING=" + pendingCount + ", SUSPICIOUS=" + suspiciousCount)
                    .build());
        }
    }

    private void addSavingsRecommendation(List<RecommendationDTO> recommendations,
                                          List<Transaction> history,
                                          List<Transaction> confirmedCurrentMonth,
                                          YearMonth currentMonth) {
        BigDecimal currentTotal = confirmedCurrentMonth.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousThreeMonthsTotal = BigDecimal.ZERO;
        for (int i = 1; i <= 3; i++) {
            YearMonth previousMonth = currentMonth.minusMonths(i);
            BigDecimal monthTotal = history.stream()
                    .filter(tx -> tx.getStatus() == TransactionStatus.CONFIRMED)
                    .filter(tx -> isInMonth(tx, previousMonth))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            previousThreeMonthsTotal = previousThreeMonthsTotal.add(monthTotal);
        }

        BigDecimal averagePreviousMonths = previousThreeMonthsTotal.divide(BigDecimal.valueOf(3), 2, BigDecimal.ROUND_HALF_UP);
        if (averagePreviousMonths.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (currentTotal.compareTo(averagePreviousMonths.multiply(BigDecimal.valueOf(1.10))) <= 0) {
            return;
        }

        Map<String, BigDecimal> currentByCategory = confirmedCurrentMonth.stream()
                .collect(Collectors.groupingBy(
                        tx -> normalizeCategory(tx.getCategory()),
                        Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        Optional<Map.Entry<String, BigDecimal>> topCategory = currentByCategory.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (topCategory.isEmpty()) {
            return;
        }

        String category = topCategory.get().getKey();
        BigDecimal monthlySaving = topCategory.get().getValue().multiply(BigDecimal.valueOf(0.10))
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        recommendations.add(RecommendationDTO.builder()
                .type("SAVINGS")
                .priority("MEDIUM")
                .title("Suggestion d'épargne")
                .message(String.format("Si tu réduis %s de 10%%, tu peux économiser environ %s TND/mois.", category, monthlySaving))
                .action("Créer un objectif d'épargne")
                .value(monthlySaving + " TND")
                .build());
    }

    private void addDayPatternRecommendation(List<RecommendationDTO> recommendations,
                                             List<Transaction> confirmedCurrentMonth) {
        if (confirmedCurrentMonth.isEmpty()) {
            return;
        }

        Map<DayOfWeek, BigDecimal> amountByDay = confirmedCurrentMonth.stream()
                .filter(tx -> tx.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        tx -> tx.getCreatedAt().getDayOfWeek(),
                        Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        Map<DayOfWeek, Long> countByDay = confirmedCurrentMonth.stream()
                .filter(tx -> tx.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        tx -> tx.getCreatedAt().getDayOfWeek(),
                        Collectors.counting()
                ));

        BigDecimal weekdayTotal = BigDecimal.ZERO;
        long weekdayCount = 0;
        BigDecimal weekendTotal = BigDecimal.ZERO;
        long weekendCount = 0;

        for (Map.Entry<DayOfWeek, BigDecimal> entry : amountByDay.entrySet()) {
            DayOfWeek day = entry.getKey();
            BigDecimal amount = entry.getValue();
            long count = countByDay.getOrDefault(day, 0L);
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                weekendTotal = weekendTotal.add(amount);
                weekendCount += count;
            } else {
                weekdayTotal = weekdayTotal.add(amount);
                weekdayCount += count;
            }
        }

        if (weekdayCount > 0 && weekendCount > 0) {
            BigDecimal weekdayAverage = weekdayTotal.divide(BigDecimal.valueOf(weekdayCount), 2, RoundingMode.HALF_UP);
            BigDecimal weekendAverage = weekendTotal.divide(BigDecimal.valueOf(weekendCount), 2, RoundingMode.HALF_UP);

            if (weekendAverage.compareTo(weekdayAverage.multiply(BigDecimal.valueOf(1.20))) > 0) {
                recommendations.add(RecommendationDTO.builder()
                        .type("WEEKLY")
                        .priority("MEDIUM")
                        .title("Dépenses du week-end plus élevées")
                        .message("Vos dépenses moyennes du week-end dépassent celles des jours ouvrés. Essayez de fixer un budget spécifique pour le samedi et le dimanche.")
                        .action("Planifier un budget week-end")
                        .value(weekendAverage + " TND")
                        .build());
                return;
            }
        }

        DayOfWeek dominantDay = null;
        BigDecimal dominantDayAmount = BigDecimal.ZERO;
        long dominantDayCount = 0;
        BigDecimal monthTotal = confirmedCurrentMonth.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Map.Entry<DayOfWeek, BigDecimal> entry : amountByDay.entrySet()) {
            if (entry.getValue().compareTo(dominantDayAmount) > 0) {
                dominantDay = entry.getKey();
                dominantDayAmount = entry.getValue();
                dominantDayCount = countByDay.getOrDefault(entry.getKey(), 0L);
            }
        }

        if (dominantDay != null && monthTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal share = dominantDayAmount.multiply(BigDecimal.valueOf(100))
                    .divide(monthTotal, 2, RoundingMode.HALF_UP);
            if (share.compareTo(BigDecimal.valueOf(30)) >= 0 && dominantDayCount >= 3) {
                recommendations.add(RecommendationDTO.builder()
                        .type("WEEKLY")
                        .priority("LOW")
                        .title("Jour de dépense récurrent")
                        .message(String.format("Le %s concentre %.0f%% de vos dépenses ce mois. Vous pouvez lisser vos achats sur la semaine.",
                                dayLabel(dominantDay), share))
                        .action("Répartir les dépenses")
                        .value(dominantDayAmount + " TND")
                        .build());
            }
        }
    }

    private void addCategoryDecreaseRecommendation(List<RecommendationDTO> recommendations,
                                                   List<Transaction> history,
                                                   YearMonth currentMonth) {
        Map<String, BigDecimal> currentByCategory = history.stream()
                .filter(tx -> tx.getStatus() == TransactionStatus.CONFIRMED)
                .filter(tx -> isInMonth(tx, currentMonth))
                .collect(Collectors.groupingBy(
                        tx -> normalizeCategory(tx.getCategory()),
                        Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        String selectedCategory = null;
        double selectedDropPercent = 0.0;
        BigDecimal selectedCurrentAmount = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : currentByCategory.entrySet()) {
            String category = entry.getKey();
            BigDecimal currentAmount = entry.getValue();
            BigDecimal previousTotal = BigDecimal.ZERO;

            for (int i = 1; i <= 3; i++) {
                YearMonth previousMonth = currentMonth.minusMonths(i);
                BigDecimal monthTotal = history.stream()
                        .filter(tx -> tx.getStatus() == TransactionStatus.CONFIRMED)
                        .filter(tx -> isInMonth(tx, previousMonth))
                        .filter(tx -> category.equals(normalizeCategory(tx.getCategory())))
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                previousTotal = previousTotal.add(monthTotal);
            }

            BigDecimal previousAverage = previousTotal.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            if (previousAverage.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            if (currentAmount.compareTo(previousAverage) >= 0) {
                continue;
            }

            double dropPercent = previousAverage.subtract(currentAmount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousAverage, 2, RoundingMode.HALF_UP)
                    .doubleValue();

            if (dropPercent >= 15 && dropPercent > selectedDropPercent) {
                selectedDropPercent = dropPercent;
                selectedCategory = category;
                selectedCurrentAmount = currentAmount;
            }
        }

        if (selectedCategory != null) {
            recommendations.add(RecommendationDTO.builder()
                    .type("PROGRESS")
                    .priority("LOW")
                    .title("Baisse de dépenses sur une catégorie")
                    .message(String.format("Bonne tendance: vos dépenses %s ont baissé de %.0f%% par rapport à la moyenne des 3 derniers mois.",
                            selectedCategory, selectedDropPercent))
                    .action("Maintenir cette tendance")
                    .value(selectedCurrentAmount + " TND")
                    .build());
        }
    }

    private String dayLabel(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "lundi";
            case TUESDAY -> "mardi";
            case WEDNESDAY -> "mercredi";
            case THURSDAY -> "jeudi";
            case FRIDAY -> "vendredi";
            case SATURDAY -> "samedi";
            case SUNDAY -> "dimanche";
        };
    }

    private boolean isInMonth(Transaction transaction, YearMonth targetMonth) {
        if (transaction.getCreatedAt() == null) {
            return false;
        }
        return YearMonth.from(transaction.getCreatedAt()).equals(targetMonth);
    }

    private String normalizeCategory(String category) {
        return category == null || category.isBlank() ? "Autres" : category;
    }

    private void executeDeferredTransaction(Transaction transaction) {
        executeDeferredTransaction(transaction, false);
    }

    private void executeDeferredTransaction(Transaction transaction, boolean recurring) {
        Long bankAccountId = Optional.ofNullable(transaction.getBankAccount())
                .map(BankAccount::getId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction sans compte source"));

        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable pour la transaction " + transaction.getId()));

        transaction.setBankAccount(bankAccount);

        RiskEvaluation evaluation = evaluateRisk(bankAccount, transaction);
        transaction.setRiskScore(evaluation.score());

        if (evaluation.blocked()) {
            transaction.setStatus(TransactionStatus.SUSPICIOUS);
            transactionRepository.save(transaction);
            fraudAlertMailService.sendSuspiciousTransactionAlert(transaction);
            log.warn("Transaction différée {} bloquée (score={})", transaction.getId(), evaluation.score());
            return;
        }

        finalizeExecution(bankAccount, transaction, recurring);
    }

    private void finalizeExecution(BankAccount bankAccount, Transaction transaction, boolean recurring) {
        debitAccount(bankAccount, transaction.getAmount());
        creditBeneficiaryAccountIfInternal(transaction);
        recordExternalConversionIfApplicable(bankAccount, transaction);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime previousNext = transaction.getNextExecutionDate();

        transaction.setStatus(TransactionStatus.CONFIRMED);
        transaction.setConfirmedAt(now);
        transaction.setLastExecutionDate(now);

        if (recurring) {
            transaction.setNextExecutionDate(resolveNextExecutionDate(previousNext, now));
        } else {
            transaction.setNextExecutionDate(null);
        }

        transactionRepository.save(transaction);
        persistBeneficiaryIfNeeded(bankAccount, transaction);
        incrementTransferCount(bankAccount, transaction);

        int riskScore = transaction.getRiskScore() != null ? transaction.getRiskScore() : 0;
        if (riskScore < 30) {
            incrementPinCounterUnderThreshold(bankAccount);
        }
    }

    private LocalDateTime resolveNextExecutionDate(LocalDateTime previousNext, LocalDateTime reference) {
        if (previousNext != null) {
            return previousNext.plusDays(30);
        }
        return reference.plusDays(30);
    }

    private boolean filterByType(Map<String, Object> filters, Transaction transaction) {
        if (!filters.containsKey("type")) {
            return true;
        }
        Object value = filters.get("type");
        if (value instanceof TransactionType type) {
            return transaction.getType() == type;
        }
        return true;
    }

    private boolean filterByStatus(Map<String, Object> filters, Transaction transaction) {
        if (!filters.containsKey("status")) {
            return true;
        }
        Object value = filters.get("status");
        if (value instanceof TransactionStatus status) {
            return transaction.getStatus() == status;
        }
        return true;
    }

    private boolean filterByAmount(Map<String, Object> filters, Transaction transaction) {
        BigDecimal min = getBigDecimal(filters.get("minAmount"));
        BigDecimal max = getBigDecimal(filters.get("maxAmount"));
        boolean matchesMin = min == null || transaction.getAmount().compareTo(min) >= 0;
        boolean matchesMax = max == null || transaction.getAmount().compareTo(max) <= 0;
        return matchesMin && matchesMax;
    }

    private BigDecimal getBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }

    private record RiskEvaluation(int score, boolean blocked) {}

    @Override
    public void setPinForAccount(Long accountId, String pin) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
        assertCurrentUserOwnsAccount(account);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        account.setPinHash(encoder.encode(pin));
        account.setPinAttemptCount(0);
        account.setLastPinAttempt(null);
        account.setPinCounterUnderThreshold(0);
        bankAccountRepository.save(account);
        log.info("PIN défini pour le compte {}", accountId);
    }

    /**
     * Confirme une transaction SUSPICIOUS après vérification du PIN.
     * Renvoie 401 PIN_INCORRECT si le PIN est faux, 423 PIN_LOCKED après 5 échecs.
     */
    @Override
    public Transaction confirmSuspiciousTransactionWithPin(Long transactionId, String pin) {
        Transaction tx = getTransactionWithAccount(transactionId);
        BankAccount account = tx.getBankAccount();
        assertCurrentUserOwnsAccount(account);

        if (account.getPinHash() == null || account.getPinHash().isBlank()) {
            throw new IllegalArgumentException("Aucun PIN configuré pour ce compte. Définissez-en un d'abord.");
        }

        // Lockout: 5 échecs dans les 15 dernières minutes
        if (account.getPinAttemptCount() >= 5
                && account.getLastPinAttempt() != null
                && account.getLastPinAttempt().isAfter(LocalDateTime.now().minusMinutes(15))) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "PIN_LOCKED");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(pin, account.getPinHash())) {
            account.setPinAttemptCount(account.getPinAttemptCount() + 1);
            account.setLastPinAttempt(LocalDateTime.now());
            bankAccountRepository.save(account);
            log.warn("PIN échoué ({}/5) pour le compte {}", account.getPinAttemptCount(), account.getId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PIN_INCORRECT");
        }

        // PIN correct — réinitialiser les compteurs
        account.setPinAttemptCount(0);
        account.setLastPinAttempt(null);
        account.setPinCounterUnderThreshold(0);
        bankAccountRepository.save(account);

        tx.setStatus(TransactionStatus.CONFIRMED);
        tx.setConfirmedAt(LocalDateTime.now());
        tx.setLastExecutionDate(LocalDateTime.now());
        debitAccount(account, tx.getAmount());
        creditBeneficiaryAccountIfInternal(tx);
        recordExternalConversionIfApplicable(account, tx);
        persistBeneficiaryIfNeeded(account, tx);
        incrementTransferCount(account, tx);

        Transaction saved = transactionRepository.save(tx);
        log.info("Transaction {} confirmée avec PIN (utilisateur {})", transactionId, account.getUserId());
        return saved;
    }

    @Override
    public boolean requiresPinVerification(Transaction transaction) {
        BankAccount account = transaction.getBankAccount();
        boolean hasPinConfigured = account.getPinHash() != null && !account.getPinHash().isBlank();
        int riskScore = transaction.getRiskScore() != null ? transaction.getRiskScore() : 0;

        // riskScore >= 70 : transactions à haut risque — exiger le PIN si le compte en a configuré un
        if (riskScore >= ALERT_THRESHOLD) {
            return hasPinConfigured;
        }

        // riskScore 30-69 (PENDING ou SUSPICIOUS < 70) : PIN obligatoire si configuré
        if (riskScore >= 30 && hasPinConfigured) {
            return true;
        }

        // Transactions à faible risque (<30) : PIN demandé à la 4ème consécutive (compteur >= 3)
        if (riskScore < 30 && hasPinConfigured) {
            int counter = account.getPinCounterUnderThreshold() != null ? account.getPinCounterUnderThreshold() : 0;
            return counter >= 3;
        }

        return false;
    }

    @Override
    public Transaction getTransactionById(Long transactionId) {
        return getTransactionWithAccount(transactionId);
    }

    public void incrementPinCounterUnderThreshold(BankAccount account) {
        int current = account.getPinCounterUnderThreshold() != null ? account.getPinCounterUnderThreshold() : 0;
        account.setPinCounterUnderThreshold(current + 1);
        bankAccountRepository.save(account);
    }

    private Transaction getTransactionWithAccount(Long transactionId) {
        return transactionRepository.findByIdWithAccount(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
    }
}
