package com.helma.helmabackend.controller.auth;

import com.helma.helmabackend.dto.auth.AuthResponse;
import com.helma.helmabackend.dto.auth.LoginRequest;
import com.helma.helmabackend.dto.auth.RegisterRequest;
import com.helma.helmabackend.service.auth.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
