package com.eduze.manage.auth.controller;

import com.eduze.manage.auth.dto.LoginRequest;
import com.eduze.manage.auth.dto.LoginResponse;
import com.eduze.manage.auth.dto.LogoutRequest;
import com.eduze.manage.auth.dto.RefreshRequest;
import com.eduze.manage.auth.service.AuthService;
import com.eduze.manage.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) LogoutRequest request) {
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7).trim();
        }
        String refreshToken = request == null ? null : request.getRefreshToken();
        authService.logout(token, refreshToken);
        return ApiResponse.ok(null);
    }
}
