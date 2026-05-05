package tn.esprit.helma.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tn.esprit.helma.entities.Gender;
import tn.esprit.helma.entities.Role;

import java.time.LocalDate;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotNull Role role,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phoneNumber,
        LocalDate dateOfBirth,
        Gender gender,
        String country,
        String city,
        String addressLine,
        String occupation,
        String nationalId,
        String bio
) {}
