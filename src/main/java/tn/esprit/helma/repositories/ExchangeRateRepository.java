package tn.esprit.helma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.helma.entities.ExchangeRate;

import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}
