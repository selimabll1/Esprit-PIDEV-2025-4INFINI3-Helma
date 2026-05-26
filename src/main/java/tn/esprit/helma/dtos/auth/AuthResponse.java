package tn.esprit.helma.dtos.auth;

import tn.esprit.helma.entities.Role;

public record AuthResponse(
        String accessToken,
        Long userId,
        Long profileId,
        String email,
        Role role,
        String firstName,
        String lastName
) {}
