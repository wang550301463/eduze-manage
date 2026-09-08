package com.eduze.manage.identity;

import com.eduze.manage.auth.security.*;
import com.eduze.manage.auth.service.*;
import com.eduze.manage.common.config.AppProperties;
import com.eduze.manage.tenant.TenantContext;
import com.eduze.platform.runtime.Actor;
import com.eduze.platform.runtime.PlatformException;
import io.jsonwebtoken.Claims;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolve current roles and branches on every request so revocations take immediate effect. */
@Service
@RequiredArgsConstructor
public class SessionIntrospection {
    private final JwtService jwt;
    private final JwtBlacklistService blacklist;
    private final CustomUserDetailsService users;
    private final AppProperties properties;

    public Actor introspect(String token) {
        try {
            Claims c = jwt.parse(token);
            if (!JwtService.TYPE_ACCESS.equals(c.get(JwtService.CLAIM_TYPE, String.class))
                    || blacklist.isBlacklisted(c.getId()))
                throw new PlatformException(401, "会话已失效");
            Long tenant = ((Number) c.get(JwtService.CLAIM_TENANT)).longValue();
            if (!tenant.equals(properties.getTenant().getDefaultId()))
                throw new PlatformException(401, "机构不匹配");
            TenantContext.setTenantId(tenant);
            CustomUserDetails u = users.loadByUserId(Long.valueOf(c.getSubject()));
            Integer version = c.get(JwtService.CLAIM_TOKEN_VERSION, Integer.class);
            if (!u.isEnabled() || version == null || version != u.getTokenVersion())
                throw new PlatformException(401, "会话已失效");
            if ("PARENT".equals(c.get(JwtService.CLAIM_ACTIVE_ROLE, String.class))) {
                return new Actor(
                        u.getUserId().toString(),
                        tenant.toString(),
                        java.util.Set.of(),
                        java.util.Set.of("PARENT"),
                        java.util.Set.of());
            }
            return new Actor(
                    u.getUserId().toString(),
                    tenant.toString(),
                    u.getBranchIds().stream().map(String::valueOf).collect(Collectors.toSet()),
                    u.getRoleCodes(),
                    u.getAuthorities().stream()
                            .map(a -> a.getAuthority())
                            .collect(Collectors.toSet()));
        } catch (PlatformException e) {
            throw e;
        } catch (com.eduze.manage.common.exception.BizException e) {
            throw new PlatformException(401, "无效会话");
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw new PlatformException(401, "无效会话");
        }
    }
}
