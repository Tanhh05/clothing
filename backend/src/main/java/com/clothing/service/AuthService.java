package com.clothing.service;

import com.clothing.dto.request.LoginRequest;
import com.clothing.dto.request.RegisterRequest;
import com.clothing.dto.request.UpdateProfileRequest;
import com.clothing.dto.response.AuthResponse;
import com.clothing.dto.response.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithGoogle(String idToken);

    UserResponse getCurrentUser(String username);

    UserResponse updateCurrentUser(String username, UpdateProfileRequest request);
}
