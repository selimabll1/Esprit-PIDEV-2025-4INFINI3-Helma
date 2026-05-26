package tn.esprit.helma.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.helma.enums.AccountType;

/**
 * Requete de creation d'un compte bancaire.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankAccountCreateRequest {
    @NotBlank
    @Size(max = 27)
    private String rib;

    private AccountType accountType;

    @Size(max = 3)
    private String currency;
}
