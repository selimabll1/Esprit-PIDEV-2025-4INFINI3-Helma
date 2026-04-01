package com.esprit.helma_backend.bootstrap;

import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureUser(
                "user@helma.tn",
                "Test User",
                User.Role.USER,
                true,
                "test123"
        );

        ensureUser(
                "admin@helma.tn",
                "Helma Admin",
                User.Role.ADMIN,
                false,
                "admin123"
        );
    }

    private void ensureUser(String email,
                            String fullName,
                            User.Role role,
                            boolean isEntrepreneur,
                            String rawPassword) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setIsEntrepreneur(isEntrepreneur);
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }
}