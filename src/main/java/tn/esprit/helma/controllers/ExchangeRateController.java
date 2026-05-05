package tn.esprit.helma.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.helma.dtos.ExchangeRateDTO;
import tn.esprit.helma.entities.ExchangeRate;
import tn.esprit.helma.services.ICurrencyConversionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ICurrencyConversionService currencyService;

    @GetMapping
    public List<ExchangeRateDTO> getAll() {
        return currencyService.getAllRates().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @PutMapping
    public ExchangeRateDTO saveOrUpdate(
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency,
            @RequestParam BigDecimal rate) {
        return toDto(currencyService.saveOrUpdateRate(fromCurrency, toCurrency, rate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        currencyService.deleteRate(id);
        return ResponseEntity.noContent().build();
    }

    private ExchangeRateDTO toDto(ExchangeRate r) {
        return ExchangeRateDTO.builder()
                .id(r.getId())
                .fromCurrency(r.getFromCurrency())
                .toCurrency(r.getToCurrency())
                .rate(r.getRate())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
