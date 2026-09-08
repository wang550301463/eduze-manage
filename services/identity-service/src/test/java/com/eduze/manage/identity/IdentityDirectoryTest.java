package com.eduze.manage.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.auth.security.CustomUserDetailsService;
import com.eduze.manage.branch.mapper.BranchMapper;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.platform.runtime.PlatformException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;

class IdentityDirectoryTest {
    @Test
    void paymentIdentityUsesLiveOwnAccountAndConfiguredApp() {
        JdbcTemplate jdbc =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                "jdbc:h2:mem:paymentIdentity;MODE=MySQL;DB_CLOSE_DELAY=-1",
                                "sa",
                                ""));
        jdbc.execute(
                "CREATE TABLE t_user(id BIGINT,tenant_id BIGINT,status INT,deleted_at BIGINT)");
        jdbc.execute(
                "CREATE TABLE wechat_identity(tenant_id BIGINT,app_id VARCHAR(64),open_id VARCHAR(64),user_id BIGINT,staff_user_id BIGINT)");
        jdbc.update("INSERT INTO t_user VALUES(7,1,1,0),(8,1,1,0),(9,2,1,0)");
        jdbc.update(
                "INSERT INTO wechat_identity VALUES(1,'wx-own','parent-open',7,8),(2,'wx-own','foreign-open',9,NULL),(1,'other-app','wrong-open',7,NULL)");
        BranchAccessGuard guard = mock(BranchAccessGuard.class);
        when(guard.requireUser())
                .thenReturn(
                        new CustomUserDetails(
                                7L,
                                1L,
                                "p",
                                "p",
                                null,
                                List.of(),
                                Set.of("PARENT"),
                                List.of(),
                                1,
                                true));
        IdentityDirectoryService service =
                new IdentityDirectoryService(
                        new MockEnvironment()
                                .withProperty("eduze.wechat.app-id", "wx-own")
                                .withProperty("eduze.tenant.default-id", "1"),
                        jdbc,
                        mock(UserMapper.class),
                        mock(BranchMapper.class),
                        mock(CustomUserDetailsService.class),
                        guard);
        assertEquals(
                Map.of("openid", "parent-open", "appId", "wx-own"), service.paymentIdentity(7L));
        assertEquals(
                403,
                assertThrows(PlatformException.class, () -> service.paymentIdentity(8L)).status());
        jdbc.update("UPDATE t_user SET status=0 WHERE id=7");
        assertEquals(
                403,
                assertThrows(PlatformException.class, () -> service.paymentIdentity(7L)).status());
    }
}
