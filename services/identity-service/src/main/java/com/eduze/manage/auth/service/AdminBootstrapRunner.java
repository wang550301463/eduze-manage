package com.eduze.manage.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.domain.UserBranch;
import com.eduze.manage.auth.domain.UserRole;
import com.eduze.manage.auth.mapper.UserBranchMapper;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.auth.mapper.UserRoleMapper;
import com.eduze.manage.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    /** Known BCrypt hash for admin@123 from V9.0.0 seed. */
    static final String DEFAULT_ADMIN_HASH =
            "$2a$10$Vc9KSaM9T4nAJVhrG3kacu.930c1dN/FnftTYp7twKW2oaq.PF0iW";

    private final Environment environment;
    private final AppProperties appProperties;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserBranchMapper userBranchMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long tenantId =
                appProperties.getTenant().getDefaultId() == null
                        ? 1L
                        : appProperties.getTenant().getDefaultId();
        User admin =
                userMapper.selectOne(
                        Wrappers.<User>lambdaQuery()
                                .eq(User::getTenantId, tenantId)
                                .eq(User::getId, 1001L));

        String bootstrapUser = appProperties.getBootstrap().getAdminUsername();
        String bootstrapPassword = appProperties.getBootstrap().getAdminPassword();
        boolean hasBootstrap =
                bootstrapUser != null
                        && !bootstrapUser.isBlank()
                        && bootstrapPassword != null
                        && bootstrapPassword.length() >= 12;

        if (admin != null && DEFAULT_ADMIN_HASH.equals(admin.getPasswordHash())) {
            if (!hasBootstrap) {
                throw new IllegalStateException(
                        "Default admin password still active. Set BOOTSTRAP_ADMIN_USERNAME and "
                                + "BOOTSTRAP_ADMIN_PASSWORD (>=12 chars) for first production start.");
            }
            applyBootstrap(admin, bootstrapUser, bootstrapPassword, tenantId);
            log.warn("Production admin credentials rotated via bootstrap env vars");
            return;
        }

        if (hasBootstrap && admin == null) {
            createAdmin(bootstrapUser.trim(), bootstrapPassword, tenantId);
            log.warn("Production admin created via bootstrap env vars");
        }
    }

    private void applyBootstrap(User admin, String username, String password, Long tenantId) {
        admin.setUsername(username.trim());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setTokenVersion(admin.getTokenVersion() == null ? 2 : admin.getTokenVersion() + 1);
        admin.setStatus(1);
        userMapper.updateById(admin);
        ensureRoleAndBranch(admin.getId(), tenantId);
    }

    private void createAdmin(String username, String password, Long tenantId) {
        User admin = new User();
        admin.setId(1001L);
        admin.setTenantId(tenantId);
        admin.setBranchId(1L);
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setName("系统管理员");
        admin.setStatus(1);
        admin.setTokenVersion(1);
        userMapper.insert(admin);
        ensureRoleAndBranch(admin.getId(), tenantId);
    }

    private void ensureRoleAndBranch(Long userId, Long tenantId) {
        Long roleCount =
                userRoleMapper.selectCount(
                        Wrappers.<UserRole>lambdaQuery()
                                .eq(UserRole::getUserId, userId)
                                .eq(UserRole::getRoleId, 1L));
        if (roleCount == null || roleCount == 0) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(1L);
            userRoleMapper.insert(ur);
        }
        Long branchCount =
                userBranchMapper.selectCount(
                        Wrappers.<UserBranch>lambdaQuery()
                                .eq(UserBranch::getUserId, userId)
                                .eq(UserBranch::getBranchId, 1L));
        if (branchCount == null || branchCount == 0) {
            UserBranch ub = new UserBranch();
            ub.setUserId(userId);
            ub.setBranchId(1L);
            userBranchMapper.insert(ub);
        }
    }
}
