package com.eduze.manage.auth.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final Long tenantId;
    private final String username;
    private final String displayName;
    private final String passwordHash;
    private final List<Long> branchIds;
    private final Set<String> roleCodes;
    private final Collection<? extends GrantedAuthority> authorities;
    private final int tokenVersion;
    private final boolean enabled;

    public CustomUserDetails(
            Long userId,
            Long tenantId,
            String username,
            String displayName,
            String passwordHash,
            List<Long> branchIds,
            Set<String> roleCodes,
            Collection<String> permissionCodes,
            int tokenVersion,
            boolean enabled) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.branchIds = branchIds != null ? List.copyOf(branchIds) : List.of();
        this.roleCodes = roleCodes != null ? Set.copyOf(roleCodes) : Set.of();
        this.authorities = permissionCodes.stream().map(SimpleGrantedAuthority::new).toList();
        this.tokenVersion = tokenVersion <= 0 ? 1 : tokenVersion;
        this.enabled = enabled;
    }

    public boolean isSuperAdmin() {
        return roleCodes.contains("SUPER_ADMIN");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
