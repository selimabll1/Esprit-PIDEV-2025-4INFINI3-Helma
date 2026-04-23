package com.helma.helmabackend.service.auth;

import com.helma.helmabackend.dto.auth.AuthResponse;
import com.helma.helmabackend.dto.auth.LoginRequest;
import com.helma.helmabackend.dto.auth.RegisterRequest;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.entity.user.UserProfile;
import com.helma.helmabackend.exception.UnauthorizedException;
import com.helma.helmabackend.security.jwt.JwtService;
import com.helma.helmabackend.service.user.IUserService;
import jakarta.transaction.Transactional;
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
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userService.existsByEmail(req.email())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(req.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        user.setEnabled(true);

        UserProfile profile = new UserProfile();
        profile.setFirstName(req.firstName().trim());
        profile.setLastName(req.lastName().trim());
        profile.setPhoneNumber(trimToNull(req.phoneNumber()));
        profile.setDateOfBirth(req.dateOfBirth());
        profile.setGender(req.gender());
        profile.setCountry(trimToNull(req.country()));
        profile.setCity(trimToNull(req.city()));
        profile.setAddressLine(trimToNull(req.addressLine()));
        profile.setOccupation(trimToNull(req.occupation()));
        profile.setNationalId(trimToNull(req.nationalId()));
        profile.setBio(trimToNull(req.bio()));
        user.setProfile(profile);

        User saved = userService.add(user);
        return buildAuthResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        User user = userService.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("User is disabled");
        }

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), Map.of(
                "role", user.getRole().name(),
                "userId", user.getId()
        ));

        UserProfile profile = user.getProfile();
        Long profileId = profile != null ? profile.getId() : null;
        String firstName = profile != null ? profile.getFirstName() : null;
        String lastName = profile != null ? profile.getLastName() : null;

        return new AuthResponse(
                token,
                user.getId(),
                profileId,
                user.getEmail(),
                user.getRole(),
                firstName,
                lastName
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}