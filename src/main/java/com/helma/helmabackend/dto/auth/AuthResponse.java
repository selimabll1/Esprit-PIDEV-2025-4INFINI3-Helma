package com.helma.helmabackend.dto.auth;

import com.helma.helmabackend.entity.user.Role;

public record AuthResponse(
        String accessToken,
        Long userId,
        Long profileId,
        String email,
        Role role,
        String firstName,
        String lastName
) {}