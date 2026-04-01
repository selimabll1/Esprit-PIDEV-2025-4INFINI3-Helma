package tn.esprit.helma.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.helma.entities.BankAccount;
import tn.esprit.helma.entities.Transaction;
import tn.esprit.helma.enums.AccountStatus;
import tn.esprit.helma.enums.TransactionPeriodicity;
import tn.esprit.helma.enums.TransactionStatus;
import tn.esprit.helma.enums.TransactionType;
import tn.esprit.helma.repositories.BankAccountRepository;
import tn.esprit.helma.repositories.TransactionRepository;
import tn.esprit.helma.services.FraudAlertMailService;
import tn.esprit.helma.services.ITransactionService;
import tn.esprit.helma.services.ISavedBeneficiaryService;

import java.math.BigDecimal;
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

    @Override
    public Transaction createTransaction(Long accountId, Transaction transaction) {
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));

        validateAccount(bankAccount);
        validateIncomingTransaction(transaction);

        transaction.setBankAccount(bankAccount);
        applyPeriodicityRules(transaction);

        RiskEvaluation evaluation = evaluateRisk(bankAccount, transaction);
        transaction.setRiskScore(evaluation.score());
        boolean blocked = evaluation.blocked();

        if (blocked) {
            transaction.setStatus(TransactionStatus.SUSPICIOUS);
            log.warn("Transaction {} bloquée (score={})", transaction.getBeneficiaryRib(), evaluation.score());
        } else if (transaction.getPeriodicity() == TransactionPeriodicity.NOW && evaluation.score() < REVIEW_THRESHOLD) {
            debitAccount(bankAccount, transaction.getAmount());
            creditBeneficiaryAccountIfInternal(transaction);
            transaction.setStatus(TransactionStatus.CONFIRMED);
            transaction.setConfirmedAt(LocalDateTime.now());
            transaction.setLastExecutionDate(LocalDateTime.now());
        } else {
            transaction.setStatus(TransactionStatus.PENDING);
        }

        Transaction saved = transactionRepository.save(transaction);

        if (!blocked) {
            persistBeneficiaryIfNeeded(bankAccount, saved);
            incrementTransferCount(bankAccount, saved);
        } else {
            fraudAlertMailService.sendSuspiciousTransactionAlert(saved);
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByType(Long accountId, TransactionType type) {
        return transactionRepository.findByBankAccountIdAndType(accountId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByStatus(Long accountId, TransactionStatus status) {
        return transactionRepository.findByBankAccountIdAndStatus(accountId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByAmountRange(Long accountId, BigDecimal minAmount, BigDecimal maxAmount) {
        return transactionRepository.findByBankAccountId(accountId).stream()
                .filter(tx -> (minAmount == null || tx.getAmount().compareTo(minAmount) >= 0)
                        && (maxAmount == null || tx.getAmount().compareTo(maxAmount) <= 0))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsBetweenDates(Long accountId, LocalDateTime start, LocalDateTime end) {
        return transactionRepository.findBetweenDates(accountId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalSpentThisMonth(Long accountId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
        return transactionRepository.sumSince(accountId, TransactionStatus.CONFIRMED, start);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateMonthlySummary(Long accountId) {
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
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction introuvable"));

        if (transaction.getStatus() != TransactionStatus.SUSPICIOUS) {
            throw new IllegalStateException("La transaction n'est pas en attente de confirmation.");
        }

        BankAccount bankAccount = bankAccountRepository.findById(transaction.getBankAccount().getId())
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));

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
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction introuvable"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("La transaction n'est pas en attente de révision (statut: " + transaction.getStatus() + ")");
        }

        BankAccount bankAccount = bankAccountRepository.findById(transaction.getBankAccount().getId())
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));

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

        if (isNewBeneficiary(account, transaction)) {
            score += 40;
        }

        List<Transaction> recentTransactions = transactionRepository
                .findTop20ByBankAccountIdOrderByCreatedAtDesc(account.getId());
        score += evaluateAmountRisk(transaction.getAmount(), recentTransactions);

        if (isOddHour(determineReferenceTime(transaction))) {
            score += 20;
        }

        score += evaluateFrequencyRisk(account.getId());

        boolean blocked = score >= ALERT_THRESHOLD;
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
        if (beneficiaryRib == null) {
            return;
        }

        bankAccountRepository.findByRib(beneficiaryRib)
                .filter(destination -> !destination.getId().equals(transaction.getBankAccount().getId()))
                .ifPresent(destination -> {
                    destination.setBalance(destination.getBalance().add(transaction.getAmount()));
                    destination.setUpdatedAt(LocalDateTime.now());
                    bankAccountRepository.save(destination);
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
}
