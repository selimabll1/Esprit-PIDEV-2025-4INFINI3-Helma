package com.helma.helmabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenuMensuelDTO {
    private String mois;
    private BigDecimal revenuAttendu;
    private BigDecimal revenuPaye;
    private Long nombrePaiements;
}
