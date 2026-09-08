package com.eduze.manage.auth.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final UserInfo user;

    @Getter
    @Builder
    public static class UserInfo {
        private final Long id;
        private final String name;
        private final String username;
        private final List<String> roles;
        private final List<String> permissions;
        private final List<BranchInfo> branches;
    }

    @Getter
    @Builder
    public static class BranchInfo {
        private final Long id;
        private final String name;
        private final String code;
    }
}
