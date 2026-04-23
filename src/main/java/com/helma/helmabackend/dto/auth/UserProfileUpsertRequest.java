package com.helma.helmabackend.dto.user;

import com.helma.helmabackend.entity.user.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserProfileUpsertRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @Size(max = 30) String phoneNumber,
        @Past LocalDate dateOfBirth,
        Gender gender,
        @Size(max = 80) String country,
        @Size(max = 80) String city,
        @Size(max = 180) String addressLine,
        @Size(max = 120) String occupation,
        @Size(max = 50) String nationalId,
        @Size(max = 1000) String bio
) {}