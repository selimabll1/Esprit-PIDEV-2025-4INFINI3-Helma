package tn.esprit.helma.services;

import tn.esprit.helma.entities.ExchangeRate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ICurrencyConversionService {
    ExchangeRate saveOrUpdateRate(String fromCurrency, String toCurrency, BigDecimal rate);
    List<ExchangeRate> getAllRates();
    void deleteRate(Long id);
    Optional<BigDecimal> convert(BigDecimal amount, String fromCurrency, String toCurrency);
    Optional<BigDecimal> getRate(String fromCurrency, String toCurrency);
}
