package com.esprit.helma_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.esprit.helma_backend.entities.User;

public class UserDto {

    // used for POST /users
    public record Create(
            @NotNull User.Role role,
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotNull Boolean isEntrepreneur
    ) {}

    // used for PUT /users/{id}
    public record Update(
            @NotNull User.Role role,
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotNull Boolean isEntrepreneur
    ) {}

    // used for responses (GET/POST/PUT)
    public record Response(
            Long id,
            User.Role role,
            String fullName,
            String email,
            Boolean isEntrepreneur
    ) {}

    public record RegisterRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank String password,
            boolean isEntrepreneur
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(String accessToken) {}
}
