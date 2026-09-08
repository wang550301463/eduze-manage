package com.eduze.manage.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import java.util.Set;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

@Component
public class EduzeTenantLineHandler implements TenantLineHandler {

    private static final Set<String> IGNORED_TABLES =
            Set.of(
                    "t_tenant",
                    "t_permission",
                    "t_user_role",
                    "t_role_permission",
                    "t_user_branch",
                    "t_outbox_message",
                    "t_audit_log",
                    "flyway_schema_history");

    @Override
    public Expression getTenantId() {
        return new LongValue(TenantContext.getTenantId());
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return IGNORED_TABLES.contains(tableName);
    }
}
