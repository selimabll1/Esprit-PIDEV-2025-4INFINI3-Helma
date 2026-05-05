package tn.esprit.helma.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rate", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"from_currency", "to_currency"})
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    public void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
