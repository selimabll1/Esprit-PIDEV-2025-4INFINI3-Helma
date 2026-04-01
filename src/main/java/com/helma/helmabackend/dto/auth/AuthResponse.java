package com.helma.helmabackend.dto.auth;

import com.helma.helmabackend.entity.user.Role;

public record AuthResponse(
        String accessToken,
        Long userId,
        String email,
        Role role
) {}
