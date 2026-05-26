package tn.esprit.helma.services;

import tn.esprit.helma.entities.User;

import java.util.Optional;

public interface IUserService {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    User add(User user);
}
