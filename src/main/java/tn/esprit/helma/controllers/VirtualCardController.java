package tn.esprit.helma.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tn.esprit.helma.dtos.VirtualCardDTO;
import tn.esprit.helma.entities.VirtualCard;
import tn.esprit.helma.enums.CardStatus;
import tn.esprit.helma.services.IVirtualCardService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/cards")
public class VirtualCardController {

    IVirtualCardService cardService;

    @PostMapping("/add")
    public VirtualCardDTO createCard(
            @RequestParam Long bankAccountId,
            @RequestParam String expiryDate,
            @RequestParam String cvvHash,
            @RequestParam BigDecimal paymentLimit) {
        VirtualCard created = cardService.createCard(bankAccountId, expiryDate, cvvHash, paymentLimit);
        return mapToDTO(created);
    }

    @GetMapping("/get/{id}")
    public VirtualCardDTO getCard(@PathVariable Long id) {
        return cardService.getCardById(id)
                .map(this::mapToDTO)
                .orElse(null);
    }

    @GetMapping("/get-by-number/{cardNumber}")
    public VirtualCardDTO getCardByNumber(@PathVariable String cardNumber) {
        return cardService.getCardByNumber(cardNumber)
                .map(this::mapToDTO)
                .orElse(null);
    }

    @GetMapping("/account/{bankAccountId}")
    public List<VirtualCardDTO> getCardsByBankAccount(@PathVariable Long bankAccountId) {
        return cardService.getCardsByBankAccount(bankAccountId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/account/{bankAccountId}/active")
    public List<VirtualCardDTO> getActiveCardsByBankAccount(@PathVariable Long bankAccountId) {
        return cardService.getActiveCardsByBankAccount(bankAccountId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @PutMapping("/block/{id}")
    public VirtualCardDTO blockCard(@PathVariable Long id) {
        VirtualCard blocked = cardService.blockCard(id);
        return mapToDTO(blocked);
    }

    @PutMapping("/unblock/{id}")
    public VirtualCardDTO unblockCard(@PathVariable Long id) {
        VirtualCard unblocked = cardService.unblockCard(id);
        return mapToDTO(unblocked);
    }

    @PutMapping("/update-status/{id}")
    public VirtualCardDTO updateCardStatus(
            @PathVariable Long id,
            @RequestParam CardStatus newStatus) {
        VirtualCard updated = cardService.updateCardStatus(id, newStatus);
        return mapToDTO(updated);
    }

    @PutMapping("/update-limit/{id}")
    public VirtualCardDTO updatePaymentLimit(
            @PathVariable Long id,
            @RequestParam BigDecimal newLimit) {
        VirtualCard updated = cardService.updatePaymentLimit(id, newLimit);
        return mapToDTO(updated);
    }

    @PutMapping("/reset-monthly-spent/{id}")
    public VirtualCardDTO resetMonthlySpent(@PathVariable Long id) {
        VirtualCard reset = cardService.resetMonthlySpent(id);
        return mapToDTO(reset);
    }

    @PutMapping("/add-monthly-spending/{id}")
    public VirtualCardDTO addMonthlySpending(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        VirtualCard updated = cardService.addMonthlySpending(id, amount);
        return mapToDTO(updated);
    }

    @GetMapping("/is-limit-reached/{id}")
    public boolean isMonthlyLimitReached(@PathVariable Long id) {
        return cardService.isMonthlyLimitReached(id);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
    }

    @GetMapping("/exists/{cardNumber}")
    public boolean cardNumberExists(@PathVariable String cardNumber) {
        return cardService.cardNumberExists(cardNumber);
    }

    @GetMapping("/account/{bankAccountId}/count")
    public long countCardsByBankAccount(@PathVariable Long bankAccountId) {
        return cardService.countCardsByBankAccount(bankAccountId);
    }

    @PostMapping("/{cardId}/reveal-number")
    public ResponseEntity<Map<String, String>> revealCardNumber(
            @PathVariable Long cardId,
            @RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        String fullNumber = cardService.revealCardNumber(cardId, pin);
        String formatted = fullNumber.replaceAll("(.{4})", "$1 ").trim();
        return ResponseEntity.ok(Map.of("cardNumber", formatted));
    }

    @PostMapping("/admin-generate/{bankAccountId}")
    public VirtualCardDTO adminGenerateCard(
            @PathVariable Long bankAccountId,
            @RequestParam(defaultValue = "5000") BigDecimal paymentLimit) {
        LocalDate expiry = LocalDate.now().plusYears(3);
        String expiryDate = String.format("%02d/%02d", expiry.getMonthValue(), expiry.getYear() % 100);
        String cvvHash = new BCryptPasswordEncoder().encode(
                String.format("%03d", 100 + new Random().nextInt(900)));
        VirtualCard card = cardService.createCard(bankAccountId, expiryDate, cvvHash, paymentLimit);
        return mapToDTO(card);
    }

    private VirtualCardDTO mapToDTO(VirtualCard card) {
        return VirtualCardDTO.builder()
                .id(card.getId())
                .bankAccountId(card.getBankAccount().getId())
                .cardNumber(maskCardNumber(card.getCardNumber()))
                .expiryDate(card.getExpiryDate())
                .paymentLimit(card.getPaymentLimit())
                .monthlySpent(card.getMonthlySpent())
                .status(card.getStatus().toString())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
