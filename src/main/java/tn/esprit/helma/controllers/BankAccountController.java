package tn.esprit.helma.controllers;

import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import tn.esprit.helma.dtos.BankAccountCreateRequest;
import tn.esprit.helma.dtos.BankAccountDTO;
import tn.esprit.helma.entities.BankAccount;
import tn.esprit.helma.services.IBankAccountService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/accounts")
public class BankAccountController {

    IBankAccountService accountService;

    @PostMapping("/add")
    public BankAccountDTO createAccount(@Valid @RequestBody BankAccountCreateRequest request) {
        String currency = normalizeCurrency(request.getCurrency());
        BankAccount created = accountService.createAccount(
                request.getUserId(),
                request.getRib(),
                request.getAccountType(),
                currency
        );
        return mapToDTO(created);
    }

    @GetMapping("/get/{id}")
    public BankAccountDTO getAccount(@PathVariable Long id) {
        return accountService.getAccountById(id)
                .map(this::mapToDTO)
                .orElse(null);
    }

    @GetMapping("/balance/{id}")
    public BigDecimal getBalance(@PathVariable Long id) {
        return accountService.getBalance(id);
    }

    @GetMapping("/user/{userId}")
    public List<BankAccountDTO> getUserAccounts(@PathVariable Long userId) {
        return accountService.getUserAccounts(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/credit/{id}")
    public BankAccountDTO creditAccount(@PathVariable Long id, @RequestParam BigDecimal amount) {
        BankAccount updated = accountService.creditAccount(id, amount);
        return mapToDTO(updated);
    }

    @PostMapping("/debit/{id}")
    public BankAccountDTO debitAccount(@PathVariable Long id, @RequestParam BigDecimal amount) {
        BankAccount updated = accountService.debitAccount(id, amount);
        return mapToDTO(updated);
    }

    @PutMapping("/update-status")
    public BankAccountDTO updateStatus(@RequestBody BankAccount account) {
        BankAccount updated = accountService.updateAccountStatus(account.getId(), account.getStatus());
        return mapToDTO(updated);
    }

    @PutMapping("/freeze/{id}")
    public BankAccountDTO freezeAccount(@PathVariable Long id) {
        BankAccount frozen = accountService.freezeAccount(id);
        return mapToDTO(frozen);
    }

    @PutMapping("/unfreeze/{id}")
    public BankAccountDTO unfreezeAccount(@PathVariable Long id) {
        BankAccount unfrozen = accountService.unfreezeAccount(id);
        return mapToDTO(unfrozen);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
    }

    private BankAccountDTO mapToDTO(BankAccount account) {
        return BankAccountDTO.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .rib(account.getRib())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .accountType(account.getAccountType().toString())
                .status(account.getStatus().toString())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        return currency.trim();
    }
}

