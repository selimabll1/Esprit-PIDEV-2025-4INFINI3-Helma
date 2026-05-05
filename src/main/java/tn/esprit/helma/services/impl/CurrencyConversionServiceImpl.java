package tn.esprit.helma.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.helma.entities.ExchangeRate;
import tn.esprit.helma.repositories.ExchangeRateRepository;
import tn.esprit.helma.services.ICurrencyConversionService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CurrencyConversionServiceImpl implements ICurrencyConversionService {

    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public ExchangeRate saveOrUpdateRate(String fromCurrency, String toCurrency, BigDecimal rate) {
        ExchangeRate entity = exchangeRateRepository
                .findByFromCurrencyAndToCurrency(fromCurrency.toUpperCase(), toCurrency.toUpperCase())
                .orElseGet(() -> ExchangeRate.builder()
                        .fromCurrency(fromCurrency.toUpperCase())
                        .toCurrency(toCurrency.toUpperCase())
                        .build());
        entity.setRate(rate);
        ExchangeRate saved = exchangeRateRepository.save(entity);
        log.info("Taux de change enregistré: {} → {} = {}", fromCurrency, toCurrency, rate);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRate> getAllRates() {
        return exchangeRateRepository.findAll();
    }

    @Override
    public void deleteRate(Long id) {
        exchangeRateRepository.deleteById(id);
        log.info("Taux de change supprimé: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null || fromCurrency.equalsIgnoreCase(toCurrency)) {
            return Optional.empty();
        }
        return exchangeRateRepository
                .findByFromCurrencyAndToCurrency(fromCurrency.toUpperCase(), toCurrency.toUpperCase())
                .map(r -> amount.multiply(r.getRate()).setScale(2, RoundingMode.HALF_UP));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> getRate(String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null) return Optional.empty();
        return exchangeRateRepository
                .findByFromCurrencyAndToCurrency(fromCurrency.toUpperCase(), toCurrency.toUpperCase())
                .map(ExchangeRate::getRate);
    }
}
