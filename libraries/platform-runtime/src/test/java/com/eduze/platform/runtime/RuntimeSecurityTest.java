package com.eduze.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RuntimeSecurityTest {
    @Test
    void familyCannotAcquireStaffPermissionsFromBranchSelection() {
        Actor family = new Actor("parent", "1", Set.of("2"), Set.of("PARENT"), Set.of());
        assertThat(family.isStaff()).isFalse();
        assertThatThrownBy(() -> family.requirePermission("portfolio:write"))
                .isInstanceOf(PlatformException.class);
    }

    @Test
    void branchPermissionDoesNotCrossCampus() {
        Actor teacher =
                new Actor("3", "1", Set.of("2"), Set.of("TEACHER"), Set.of("portfolio:write"));
        teacher.requireBranch("2");
        assertThatThrownBy(() -> teacher.requireBranch("9")).isInstanceOf(PlatformException.class);
    }

    @Test
    void callerNameAloneCannotAuthorizeInternalRequests() {
        RuntimeProperties properties = new RuntimeProperties();
        properties.getCallers().put("portfolio", "a-long-random-per-caller-service-token");
        InternalCredentials credentials = new InternalCredentials(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Service-Name", "portfolio");
        request.addHeader("X-Service-Token", "wrong");
        assertThatThrownBy(() -> credentials.require(request))
                .isInstanceOf(PlatformException.class);
    }
}
