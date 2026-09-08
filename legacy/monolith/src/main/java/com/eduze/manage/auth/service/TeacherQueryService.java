package com.eduze.manage.auth.service;

import com.eduze.manage.auth.dto.TeacherSummaryResponse;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.manage.tenant.TenantContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherQueryService {

    private static final String SQL =
            """
            SELECT u.id, u.username, u.name, u.branch_id
            FROM t_user u
            JOIN t_user_role ur ON ur.user_id = u.id
            JOIN t_role r ON r.id = ur.role_id AND r.code = 'TEACHER'
            WHERE u.tenant_id = ? AND u.deleted_at = 0 AND u.status = 1
              AND (? IS NULL OR u.branch_id = ?)
            ORDER BY u.id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final BranchAccessGuard branchAccessGuard;

    public List<TeacherSummaryResponse> list(Long branchId) {
        if (branchId != null) {
            branchAccessGuard.requireBranchAccess(branchId);
        }
        Long tenantId = TenantContext.getTenantId();
        return jdbcTemplate.query(
                SQL,
                (rs, i) ->
                        TeacherSummaryResponse.builder()
                                .id(rs.getLong("id"))
                                .username(rs.getString("username"))
                                .name(rs.getString("name"))
                                .branchId(rs.getObject("branch_id", Long.class))
                                .build(),
                tenantId,
                branchId,
                branchId);
    }
}
