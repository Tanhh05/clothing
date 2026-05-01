package com.clothing.service;

import com.clothing.dto.request.LoginRequest;
import com.clothing.dto.request.LogoutRequest;
import com.clothing.dto.request.RefreshTokenRequest;
import com.clothing.dto.request.RegisterRequest;
import com.clothing.dto.request.UpdateProfileRequest;
import com.clothing.dto.request.ChangePasswordRequest;
import com.clothing.dto.response.AuthResponse;
import com.clothing.dto.response.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithGoogle(String idToken);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    UserResponse getCurrentUser(String username);

    UserResponse updateCurrentUser(String username, UpdateProfileRequest request);

    void changeCurrentUserPassword(String username, ChangePasswordRequest request);
}
