package com.helma.helmabackend.dto.user;

import com.helma.helmabackend.entity.user.Gender;
import com.helma.helmabackend.entity.user.Role;

import java.time.Instant;
import java.time.LocalDate;

public record UserProfileResponse(
        Long profileId,
        Long userId,
        String email,
        Role role,
        boolean enabled,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate dateOfBirth,
        Gender gender,
        String country,
        String city,
        String addressLine,
        String occupation,
        String nationalId,
        String bio,
        Instant createdAt,
        Instant updatedAt
) {}