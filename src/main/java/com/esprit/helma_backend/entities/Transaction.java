package com.esprit.helma_backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transactions")
public class Transaction {

    public enum TransactionType {
        INCOME, EXPENSE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private TransactionType type = TransactionType.EXPENSE;

    @Column(name = "txn_date", nullable = false)
    private Instant txnDate;

    @PrePersist
    public void prePersist() {
        if (txnDate == null) txnDate = Instant.now();
        if (type == null) type = TransactionType.EXPENSE;
    }
}