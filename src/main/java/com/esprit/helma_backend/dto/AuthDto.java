package com.esprit.helma_backend.dto;

import com.esprit.helma_backend.entities.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public record RegisterRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters")
            String password,
            boolean isEntrepreneur
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record AuthUser(
            Long id,
            User.Role role,
            String fullName,
            String email,
            Boolean isEntrepreneur
    ) {}

    public record AuthResponse(
            String accessToken,
            AuthUser user
    ) {}
}