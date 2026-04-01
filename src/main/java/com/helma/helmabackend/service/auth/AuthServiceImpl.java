package com.helma.helmabackend.service.auth;

import com.helma.helmabackend.dto.auth.AuthResponse;
import com.helma.helmabackend.dto.auth.LoginRequest;
import com.helma.helmabackend.dto.auth.RegisterRequest;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.exception.UnauthorizedException;
import com.helma.helmabackend.security.jwt.JwtService;
import com.helma.helmabackend.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(RegisterRequest req) {
        if (userService.existsByEmail(req.email())) {
            throw new RuntimeException("Email already exists");
        }

        User u = new User();
        u.setEmail(req.email().trim().toLowerCase());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setRole(req.role());
        u.setEnabled(true);

        User saved = userService.add(u);

        String token = jwtService.generateToken(saved.getEmail(), Map.of(
                "role", saved.getRole().name(),
                "userId", saved.getId()
        ));

        return new AuthResponse(token, saved.getId(), saved.getEmail(), saved.getRole());
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        User user = userService.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isEnabled()) throw new UnauthorizedException("User is disabled");

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail(), Map.of(
                "role", user.getRole().name(),
                "userId", user.getId()
        ));

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole());
    }
}
