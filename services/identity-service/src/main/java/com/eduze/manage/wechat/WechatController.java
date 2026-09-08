package com.eduze.manage.wechat;

import com.eduze.manage.auth.dto.LoginResponse;
import com.eduze.platform.runtime.ApiResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identity/wechat")
@RequiredArgsConstructor
public class WechatController {
    private final WechatIdentityService identities;

    public record CodeRequest(String code) {}

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody CodeRequest body) {
        return ApiResponse.ok(identities.login(body.code()));
    }

    @PostMapping("/link")
    public ApiResponse<Map<String, Boolean>> link(@RequestBody CodeRequest body) {
        return ApiResponse.ok(identities.link(body.code()));
    }
}
