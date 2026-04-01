package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.AuthDto;
import com.esprit.helma_backend.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public AuthDto.AuthResponse register(@Valid @RequestBody AuthDto.RegisterRequest req) {
        return service.register(req);
    }

    @PostMapping("/login")
    public AuthDto.AuthResponse login(@Valid @RequestBody AuthDto.LoginRequest req) {
        return service.login(req);
    }
}
