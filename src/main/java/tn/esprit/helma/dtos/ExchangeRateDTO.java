package tn.esprit.helma.dtos;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateDTO {
    private Long id;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private LocalDateTime updatedAt;
}
