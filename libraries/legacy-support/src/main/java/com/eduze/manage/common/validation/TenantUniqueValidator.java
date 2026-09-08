package com.eduze.manage.common.validation;

import com.eduze.manage.tenant.TenantContext;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TenantUniqueValidator implements ConstraintValidator<TenantUnique, Object> {

    private final JdbcTemplate jdbcTemplate;

    private String table;
    private String column;
    private String excludeIdProperty;

    public TenantUniqueValidator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void initialize(TenantUnique annotation) {
        this.table = annotation.table();
        this.column = annotation.column();
        this.excludeIdProperty = annotation.excludeIdProperty();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String str = value.toString().trim();
        if (str.isEmpty()) {
            return true;
        }

        Long excludeId = null;
        // excludeIdProperty resolution requires class-level @TenantUnique; field-level defers to
        // service checks

        Long tenantId = TenantContext.getTenantId();
        String sql =
                "SELECT COUNT(1) FROM "
                        + table
                        + " WHERE tenant_id = ? AND "
                        + column
                        + " = ? AND deleted_at = 0"
                        + (excludeId != null ? " AND id <> ?" : "");
        Integer count;
        if (excludeId != null) {
            count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, str, excludeId);
        } else {
            count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, str);
        }
        return count == null || count == 0;
    }
}
