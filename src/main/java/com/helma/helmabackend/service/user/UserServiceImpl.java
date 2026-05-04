package com.helma.helmabackend.service.user;

import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.repository.user.UserRepository;
import com.helma.helmabackend.service.core.GenericServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends GenericServiceImpl<User, Long> implements IUserService {

    private final UserRepository userRepository;

    @Override
    protected JpaRepository<User, Long> getRepository() {
        return userRepository;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }
}
