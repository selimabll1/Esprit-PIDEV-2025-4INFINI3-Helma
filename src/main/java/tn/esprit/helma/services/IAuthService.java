package tn.esprit.helma.services;

import tn.esprit.helma.dtos.auth.AuthResponse;
import tn.esprit.helma.dtos.auth.LoginRequest;
import tn.esprit.helma.dtos.auth.RegisterRequest;

public interface IAuthService {
    AuthResponse register(RegisterRequest req);
    AuthResponse login(LoginRequest req);
}
