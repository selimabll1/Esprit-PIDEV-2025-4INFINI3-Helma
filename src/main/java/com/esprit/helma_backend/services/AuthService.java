package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.AuthDto;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.UserRepository;
import com.esprit.helma_backend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UserRepository repo, PasswordEncoder encoder, JwtService jwtService) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        if (repo.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User u = User.builder()
                .role(User.Role.USER)
                .fullName(req.fullName())
                .email(req.email())
                .password(encoder.encode(req.password()))
                .isEntrepreneur(req.isEntrepreneur())
                .build();

        repo.save(u);
        return toAuthResponse(u);
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        User u = repo.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String stored = u.getPassword();
        boolean ok;

        if (isBcryptHash(stored)) {
            ok = encoder.matches(req.password(), stored);
        } else {
            ok = stored != null && stored.equals(req.password());

            if (ok) {
                u.setPassword(encoder.encode(req.password()));
                repo.save(u);
            }
        }

        if (!ok) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return toAuthResponse(u);
    }

    private AuthDto.AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthDto.AuthResponse(
                token,
                new AuthDto.AuthUser(
                        user.getId(),
                        user.getRole(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getIsEntrepreneur()
                )
        );
    }

    private boolean isBcryptHash(String value) {
        return value != null && value.matches("^\\$2[aby]?\\$\\d{2}\\$.{53}$");
    }
}