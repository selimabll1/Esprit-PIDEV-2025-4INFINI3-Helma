package com.helma.helmabackend.service.user;

import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.service.core.IGenericService;

import java.util.Optional;

public interface IUserService extends IGenericService<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
