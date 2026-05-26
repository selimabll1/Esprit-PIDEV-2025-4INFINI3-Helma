package tn.esprit.helma.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.helma.dtos.BankAccountDTO;
import tn.esprit.helma.dtos.VirtualCardDTO;
import tn.esprit.helma.entities.BankAccount;
import tn.esprit.helma.entities.VirtualCard;
import tn.esprit.helma.repositories.BankAccountRepository;
import tn.esprit.helma.repositories.UserRepository;
import tn.esprit.helma.services.IBankAccountService;
import tn.esprit.helma.services.IVirtualCardService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/admin/banking")
@RequiredArgsConstructor
public class AdminBankingController {

    private final IBankAccountService bankAccountService;
    private final BankAccountRepository bankAccountRepository;
    private final IVirtualCardService virtualCardService;
    private final UserRepository userRepository;

    @GetMapping("/accounts/search")
    public BankAccountDTO searchByRib(@RequestParam String rib) {
        BankAccount account = bankAccountRepository.findByRib(rib)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
        return mapToDTO(account);
    }

    @PostMapping("/accounts/{accountId}/deposit")
    public BankAccountDTO deposit(
            @PathVariable Long accountId,
            @RequestParam BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le montant doit être positif.");
        }
        BankAccount updated = bankAccountService.creditAccount(accountId, amount);
        return mapToDTO(updated);
    }

    @PostMapping("/accounts/{accountId}/generate-card")
    public VirtualCardDTO generateCard(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "5000") BigDecimal paymentLimit) {

        LocalDate expiry = LocalDate.now().plusYears(3);
        String expiryDate = String.format("%02d/%02d", expiry.getMonthValue(), expiry.getYear() % 100);

        String rawCvv = String.format("%03d", 100 + new Random().nextInt(900));
        String cvvHash = new BCryptPasswordEncoder().encode(rawCvv);

        VirtualCard card = virtualCardService.createCard(accountId, expiryDate, cvvHash, paymentLimit);
        return mapCardToDTO(card);
    }

    @GetMapping("/accounts/{accountId}/cards")
    public List<VirtualCardDTO> getCards(@PathVariable Long accountId) {
        return virtualCardService.getCardsByBankAccount(accountId)
                .stream()
                .map(this::mapCardToDTO)
                .toList();
    }

    private BankAccountDTO mapToDTO(BankAccount account) {
        String prenom = null;
        String nom = null;
        if (account.getUserId() != null) {
            try {
                var owner = userRepository.findById(account.getUserId()).orElse(null);
                if (owner != null && owner.getProfile() != null) {
                    prenom = owner.getProfile().getFirstName();
                    nom = owner.getProfile().getLastName();
                }
            } catch (Exception ignored) {}
        }
        return BankAccountDTO.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .userPrenom(prenom)
                .userNom(nom)
                .rib(account.getRib())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .accountType(account.getAccountType() != null ? account.getAccountType().toString() : null)
                .status(account.getStatus() != null ? account.getStatus().toString() : null)
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private VirtualCardDTO mapCardToDTO(VirtualCard card) {
        String raw = card.getCardNumber();
        String masked = (raw != null && raw.length() >= 4)
                ? "**** **** **** " + raw.substring(raw.length() - 4)
                : raw;
        return VirtualCardDTO.builder()
                .id(card.getId())
                .bankAccountId(card.getBankAccount().getId())
                .cardNumber(masked)
                .expiryDate(card.getExpiryDate())
                .paymentLimit(card.getPaymentLimit())
                .monthlySpent(card.getMonthlySpent())
                .status(card.getStatus().toString())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }

    @GetMapping("/accounts/{accountId}/pin-counter")
    public Map<String, Integer> getPinCounter(@PathVariable Long accountId) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "NOT_FOUND"));
        return java.util.Map.of("pinCounterUnderThreshold", account.getPinCounterUnderThreshold() == null ? 0 : account.getPinCounterUnderThreshold());
    }
}
