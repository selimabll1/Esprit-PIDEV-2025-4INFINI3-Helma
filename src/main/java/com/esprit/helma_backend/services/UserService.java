package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.UserDto;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private static final String DEFAULT_PASSWORD = "test123";

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    private static UserDto.Response toResponse(User u) {
        return new UserDto.Response(
                u.getId(),
                u.getRole(),
                u.getFullName(),
                u.getEmail(),
                u.getIsEntrepreneur()
        );
    }

    public UserDto.Response create(UserDto.Create req) {
        if (repo.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User u = new User();
        u.setRole(req.role());
        u.setFullName(req.fullName());
        u.setEmail(req.email());
        u.setIsEntrepreneur(req.isEntrepreneur());
        u.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));

        return toResponse(repo.save(u));
    }

    @Transactional(readOnly = true)
    public UserDto.Response getById(Long id) {
        User u = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toResponse(u);
    }

    @Transactional(readOnly = true)
    public List<UserDto.Response> getAll() {
        return repo.findAll().stream().map(UserService::toResponse).toList();
    }

    public UserDto.Response update(Long id, UserDto.Update req) {
        User u = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (repo.existsByEmailAndIdNot(req.email(), id)) {
            throw new IllegalArgumentException("Email already exists");
        }

        u.setRole(req.role());
        u.setFullName(req.fullName());
        u.setEmail(req.email());
        u.setIsEntrepreneur(req.isEntrepreneur());

        if (u.getPassword() == null || u.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        }

        return toResponse(repo.save(u));
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("User not found");
        }
        repo.deleteById(id);
    }
}