package com.eduze.manage.identity;

import com.eduze.manage.auth.dto.LoginResponse;
import com.eduze.platform.runtime.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity")
public class RoleSwitchController {
    private final RoleSwitchService roles;

    public record SwitchRequest(String role) {}

    @PostMapping("/switch-role")
    public ApiResponse<LoginResponse> switchRole(@RequestBody SwitchRequest body) {
        return ApiResponse.ok(roles.switchRole(body.role()));
    }
}
