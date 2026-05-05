package tn.esprit.helma.controllers;

import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import tn.esprit.helma.dtos.BankAccountCreateRequest;
import tn.esprit.helma.dtos.BankAccountDTO;
import tn.esprit.helma.entities.BankAccount;
import tn.esprit.helma.entities.User;
import tn.esprit.helma.services.IBankAccountService;
import tn.esprit.helma.repositories.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/accounts")
public class BankAccountController {

    IBankAccountService accountService;
    UserRepository userRepository;

    @PostMapping("/add")
    public BankAccountDTO createAccount(@Valid @RequestBody BankAccountCreateRequest request) {
        String currency = normalizeCurrency(request.getCurrency());
        BankAccount created = accountService.createAccount(
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

    @GetMapping("/rib/{rib}")
    public BankAccountDTO getAccountByRib(@PathVariable String rib) {
        return accountService.getAccountByRib(rib)
                .map(this::mapToDTO)
                .orElse(null);
    }

    @GetMapping("/balance/{id}")
    public BigDecimal getBalance(@PathVariable Long id) {
        return accountService.getBalance(id);
    }

    @GetMapping("/my")
    public List<BankAccountDTO> getCurrentUserAccounts() {
        return accountService.getCurrentUserAccounts().stream()
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
        String ownerPrenom = null;
        String ownerNom = null;

        if (account.getUserId() != null) {
            try {
                var owner = userRepository.findById(account.getUserId()).orElse(null);
                if (owner != null && owner.getProfile() != null) {
                    ownerPrenom = owner.getProfile().getFirstName();
                    ownerNom = owner.getProfile().getLastName();
                }
            } catch (Exception ignored) {
                // Keep account response available even if owner lookup fails.
            }
        }

        return BankAccountDTO.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .userPrenom(ownerPrenom)
                .userNom(ownerNom)
                .rib(account.getRib())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .accountType(account.getAccountType() != null ? account.getAccountType().toString() : null)
                .status(account.getStatus() != null ? account.getStatus().toString() : null)
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

