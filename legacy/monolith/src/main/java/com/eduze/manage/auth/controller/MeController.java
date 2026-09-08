package com.eduze.manage.auth.controller;

import com.eduze.manage.auth.dto.ChangePasswordRequest;
import com.eduze.manage.auth.dto.LoginResponse;
import com.eduze.manage.auth.dto.UpdateProfileRequest;
import com.eduze.manage.auth.service.MeService;
import com.eduze.manage.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final MeService meService;

    @GetMapping
    public ApiResponse<LoginResponse.UserInfo> me() {
        return ApiResponse.ok(meService.current());
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        meService.changePassword(request);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/profile")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        meService.updateProfile(request);
        return ApiResponse.ok(null);
    }
}
