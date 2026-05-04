package com.helma.helmabackend.service.auth;

import com.helma.helmabackend.dto.auth.AuthResponse;
import com.helma.helmabackend.dto.auth.LoginRequest;
import com.helma.helmabackend.dto.auth.RegisterRequest;

public interface IAuthService {
    AuthResponse register(RegisterRequest req);
    AuthResponse login(LoginRequest req);
}
