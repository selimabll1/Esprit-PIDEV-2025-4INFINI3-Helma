package tn.esprit.helma.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tn.esprit.helma.dtos.TransactionCreateRequest;
import tn.esprit.helma.dtos.TransactionDTO;
import tn.esprit.helma.entities.Transaction;
import tn.esprit.helma.enums.TransactionPeriodicity;
import tn.esprit.helma.enums.TransactionStatus;
import tn.esprit.helma.enums.TransactionType;
import tn.esprit.helma.services.ITransactionService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final ITransactionService transactionService;

    @PostMapping("/add/{accountId}")
    public ResponseEntity<TransactionDTO> createTransaction(
            @PathVariable Long accountId,
            @Valid @RequestBody TransactionCreateRequest request) {

        Transaction transaction = mapToEntity(request);
        Transaction saved = transactionService.createTransaction(accountId, transaction);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @GetMapping("/{accountId}/monthly-summary")
    public ResponseEntity<String> getMonthlySummary(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.generateMonthlySummary(accountId));
    }

    @GetMapping("/{accountId}/search")
    public ResponseEntity<List<TransactionDTO>> searchTransactions(
            @PathVariable Long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) TransactionStatus status) {

        Map<String, Object> filters = new HashMap<>();
        if (type != null) {
            filters.put("type", type);
        }
        if (minAmount != null) {
            filters.put("minAmount", minAmount);
        }
        if (maxAmount != null) {
            filters.put("maxAmount", maxAmount);
        }
        if (status != null) {
            filters.put("status", status);
        }

        List<TransactionDTO> response = transactionService.searchTransactions(accountId, filters)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/pending")
    public ResponseEntity<List<TransactionDTO>> getPendingTransactions(@PathVariable Long accountId) {
        List<TransactionDTO> response = transactionService.getTransactionsByStatus(accountId, TransactionStatus.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/suspicious")
    public ResponseEntity<List<TransactionDTO>> getSuspiciousTransactions(@PathVariable Long accountId) {
        List<TransactionDTO> response = transactionService.getTransactionsByStatus(accountId, TransactionStatus.SUSPICIOUS)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/confirmed")
    public ResponseEntity<List<TransactionDTO>> getConfirmedTransactions(@PathVariable Long accountId) {
        List<TransactionDTO> response = transactionService.getTransactionsByStatus(accountId, TransactionStatus.CONFIRMED)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/canceled")
    public ResponseEntity<List<TransactionDTO>> getCanceledTransactions(@PathVariable Long accountId) {
        List<TransactionDTO> response = transactionService.getTransactionsByStatus(accountId, TransactionStatus.CANCELED)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{transactionId}/confirm")
    public ResponseEntity<TransactionDTO> confirmSuspicious(@PathVariable Long transactionId) {
        Transaction transaction = transactionService.confirmSuspiciousTransaction(transactionId);
        return ResponseEntity.ok(mapToDto(transaction));
    }

    @GetMapping(value = "/{transactionId}/confirm", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmSuspiciousFromEmail(@PathVariable Long transactionId) {
        try {
            transactionService.confirmSuspiciousTransaction(transactionId);
            return ResponseEntity.ok(buildHtmlResponse("Transaction confirmee", "La transaction a ete confirmee et executee avec succes."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildHtmlResponse("Confirmation impossible", ex.getMessage()));
        }
    }

    @PutMapping("/{transactionId}/reject")
    public ResponseEntity<TransactionDTO> rejectSuspicious(@PathVariable Long transactionId) {
        Transaction transaction = transactionService.rejectSuspiciousTransaction(transactionId);
        return ResponseEntity.ok(mapToDto(transaction));
    }

    @GetMapping(value = "/{transactionId}/reject", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> rejectSuspiciousFromEmail(@PathVariable Long transactionId) {
        try {
            transactionService.rejectSuspiciousTransaction(transactionId);
            return ResponseEntity.ok(buildHtmlResponse("Transaction rejetee", "La transaction a ete rejetee et annulee."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildHtmlResponse("Rejet impossible", ex.getMessage()));
        }
    }

    @PutMapping("/{transactionId}/approve")
    public ResponseEntity<TransactionDTO> approvePending(@PathVariable Long transactionId) {
        Transaction transaction = transactionService.approvePendingTransaction(transactionId);
        return ResponseEntity.ok(mapToDto(transaction));
    }

    @GetMapping(value = "/{transactionId}/approve", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> approvePendingFromLink(@PathVariable Long transactionId) {
        try {
            transactionService.approvePendingTransaction(transactionId);
            return ResponseEntity.ok(buildHtmlResponse("Transaction approuvee", "La transaction a ete approuvee et executee avec succes."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildHtmlResponse("Approbation impossible", ex.getMessage()));
        }
    }

    private Transaction mapToEntity(TransactionCreateRequest request) {
        TransactionPeriodicity periodicity = request.getPeriodicity() == null
                ? TransactionPeriodicity.NOW
                : request.getPeriodicity();

        return Transaction.builder()
                .beneficiaryName(request.getBeneficiaryName())
                .beneficiaryRib(request.getBeneficiaryRib())
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .description(request.getDescription())
                .periodicity(periodicity)
                .scheduledDate(request.getScheduledDate())
                .nextExecutionDate(request.getNextExecutionDate())
                .build();
    }

    private TransactionDTO mapToDto(Transaction transaction) {
        Long bankAccountId = transaction.getBankAccount() != null ? transaction.getBankAccount().getId() : null;
        return TransactionDTO.builder()
                .id(transaction.getId())
                .bankAccountId(bankAccountId)
                .beneficiaryName(transaction.getBeneficiaryName())
                .beneficiaryRib(transaction.getBeneficiaryRib())
                .amount(transaction.getAmount())
                .type(transaction.getType() != null ? transaction.getType().name() : null)
                .category(transaction.getCategory())
                .description(transaction.getDescription())
                .status(transaction.getStatus() != null ? transaction.getStatus().name() : null)
                .periodicity(transaction.getPeriodicity() != null ? transaction.getPeriodicity().name() : null)
                .scheduledDate(transaction.getScheduledDate())
                .nextExecutionDate(transaction.getNextExecutionDate())
                .lastExecutionDate(transaction.getLastExecutionDate())
                .riskScore(transaction.getRiskScore())
                .createdAt(transaction.getCreatedAt())
                .confirmedAt(transaction.getConfirmedAt())
                .build();
    }

    private String buildHtmlResponse(String title, String message) {
        return "<html><body style='font-family:Arial,sans-serif;padding:24px'>"
                + "<h2>" + title + "</h2>"
                + "<p>" + message + "</p>"
                + "</body></html>";
    }
}
