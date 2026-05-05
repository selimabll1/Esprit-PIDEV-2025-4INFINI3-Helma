package tn.esprit.helma.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetPinRequest {
    @NotBlank(message = "PIN cannot be blank")
    @Pattern(regexp = "^\\d{4}$", message = "PIN must be exactly 4 digits")
    private String pin;

    @NotBlank(message = "Confirmation PIN cannot be blank")
    @Pattern(regexp = "^\\d{4}$", message = "Confirmation PIN must be exactly 4 digits")
    private String confirmPin;
}
