package com.eduze.manage.identity;

import com.eduze.manage.auth.dto.LoginResponse;
import com.eduze.manage.auth.security.CustomUserDetailsService;
import com.eduze.manage.auth.service.AuthService;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.platform.runtime.PlatformException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleSwitchService {
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final BranchAccessGuard guard;
    private final CustomUserDetailsService users;
    private final AuthService auth;

    public LoginResponse switchRole(String role) {
        if (!java.util.Set.of("PARENT", "STAFF").contains(role))
            throw new PlatformException(400, "请选择家长或员工身份");
        var current = guard.requireUser();
        Long targetId = current.getUserId();
        if ("STAFF".equals(role)) {
            var linked =
                    jdbc.queryForList(
                            "SELECT staff_user_id FROM wechat_identity WHERE tenant_id=? AND user_id=? AND staff_user_id IS NOT NULL",
                            Long.class,
                            current.getTenantId(),
                            current.getUserId());
            if (!linked.isEmpty()) targetId = linked.get(0);
        } else {
            var linked =
                    jdbc.queryForList(
                            "SELECT user_id FROM wechat_identity WHERE tenant_id=? AND staff_user_id=?",
                            Long.class,
                            current.getTenantId(),
                            current.getUserId());
            if (!linked.isEmpty()) targetId = linked.get(0);
        }
        var user = users.loadByUserId(targetId);
        if (!user.isEnabled()) throw new PlatformException(403, "关联账号已停用");
        var verified =
                new com.eduze.platform.runtime.Actor(
                        user.getUserId().toString(),
                        user.getTenantId().toString(),
                        user.getBranchIds().stream()
                                .map(String::valueOf)
                                .collect(java.util.stream.Collectors.toSet()),
                        user.getRoleCodes(),
                        user.getAuthorities().stream()
                                .map(
                                        org.springframework.security.core.GrantedAuthority
                                                ::getAuthority)
                                .collect(java.util.stream.Collectors.toSet()));
        if ("STAFF".equals(role) && !verified.isStaff()) {
            throw new PlatformException(403, "没有员工身份");
        }
        return auth.issueTokens(user, role);
    }
}
