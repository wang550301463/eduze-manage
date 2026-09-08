package com.eduze.manage.wechat;

import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.dto.LoginResponse;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.auth.security.CustomUserDetailsService;
import com.eduze.manage.auth.service.AuthService;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.manage.tenant.TenantContext;
import com.eduze.platform.runtime.PlatformException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WechatIdentityService {
    private final WechatClient wechat;
    private final JdbcTemplate jdbc;
    private final UserMapper users;
    private final CustomUserDetailsService details;
    private final AuthService auth;
    private final PasswordEncoder passwords;
    private final BranchAccessGuard guard;

    @Transactional
    public LoginResponse login(String code) {
        String openId = wechat.exchange(code);
        List<Long> ids =
                jdbc.queryForList(
                        "SELECT user_id FROM wechat_identity WHERE tenant_id=? AND app_id=? AND open_id=?",
                        Long.class,
                        TenantContext.getTenantId(),
                        wechat.appId(),
                        openId);
        Long userId;
        if (ids.isEmpty()) {
            User user = new User();
            user.setTenantId(TenantContext.getTenantId());
            user.setUsername("wx_" + UUID.randomUUID().toString().replace("-", ""));
            user.setName("微信用户");
            user.setPasswordHash(passwords.encode(UUID.randomUUID().toString()));
            user.setStatus(1);
            user.setTokenVersion(1);
            users.insert(user);
            userId = user.getId();
            jdbc.update(
                    "INSERT INTO wechat_identity(tenant_id,app_id,open_id,user_id) VALUES(?,?,?,?)",
                    TenantContext.getTenantId(),
                    wechat.appId(),
                    openId,
                    userId);
        } else {
            userId = ids.get(0);
        }
        var user = details.loadByUserId(userId);
        if (!user.isEnabled()) {
            throw new PlatformException(403, "账号已停用");
        }
        return auth.issueTokens(user);
    }

    @Transactional
    public Map<String, Boolean> link(String code) {
        var user = guard.requireUser();
        if (user.getRoleCodes().stream()
                .noneMatch(
                        java.util.Set.of(
                                        "SUPER_ADMIN",
                                        "PRINCIPAL",
                                        "TEACHER",
                                        "ADMIN",
                                        "ADVISOR",
                                        "FRONT_DESK")
                                ::contains)) {
            throw new PlatformException(403, "请先使用员工账号登录");
        }
        String openId = wechat.exchange(code);
        var ids =
                jdbc.queryForList(
                        "SELECT user_id FROM wechat_identity WHERE tenant_id=? AND app_id=? AND open_id=? FOR UPDATE",
                        Long.class,
                        user.getTenantId(),
                        wechat.appId(),
                        openId);
        if (!ids.isEmpty() && !ids.get(0).equals(user.getUserId())) {
            var parent = details.loadByUserId(ids.get(0));
            if (!parent.getRoleCodes().isEmpty()) {
                throw new PlatformException(409, "该微信已属于另一员工身份");
            }
            var linked =
                    jdbc.queryForList(
                            "SELECT staff_user_id FROM wechat_identity WHERE tenant_id=? AND app_id=? AND open_id=? AND staff_user_id IS NOT NULL",
                            Long.class,
                            user.getTenantId(),
                            wechat.appId(),
                            openId);
            if (!linked.isEmpty() && !linked.get(0).equals(user.getUserId())) {
                throw new PlatformException(409, "该微信已关联另一员工");
            }
            jdbc.update(
                    "UPDATE wechat_identity SET staff_user_id=? WHERE tenant_id=? AND app_id=? AND open_id=?",
                    user.getUserId(),
                    user.getTenantId(),
                    wechat.appId(),
                    openId);
        }
        if (ids.isEmpty()) {
            jdbc.update(
                    "INSERT INTO wechat_identity(tenant_id,app_id,open_id,user_id,staff_user_id) VALUES(?,?,?,?,?)",
                    user.getTenantId(),
                    wechat.appId(),
                    openId,
                    user.getUserId(),
                    user.getUserId());
        }
        return Map.of("linked", true);
    }
}
