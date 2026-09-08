package com.eduze.manage.tenant;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class BranchScopeInterceptor implements InnerInterceptor {

    private static final Set<String> BRANCH_SCOPED_TABLES =
            Set.of(
                    "t_student",
                    "t_class_group",
                    "t_class_room",
                    "t_lesson",
                    "t_attendance",
                    "t_pickup_record",
                    "t_leave_request",
                    "t_teacher_availability",
                    "t_lesson_subscription",
                    "t_lesson_student",
                    "t_course_package",
                    "t_student_mentor_history");

    @Override
    public void beforeQuery(
            Executor executor,
            MappedStatement ms,
            Object parameter,
            RowBounds rowBounds,
            ResultHandler resultHandler,
            BoundSql boundSql)
            throws SQLException {
        CustomUserDetails user = currentUser();
        if (user == null || user.isSuperAdmin()) {
            return;
        }

        String sql = boundSql.getSql();
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select select)
                    || !(select.getSelectBody() instanceof PlainSelect plainSelect)) {
                return;
            }
            String tableName = resolveMainTableName(plainSelect.getFromItem());
            if (tableName == null || !BRANCH_SCOPED_TABLES.contains(tableName.toLowerCase())) {
                return;
            }

            List<Long> branchIds = user.getBranchIds();
            // Fail-closed: empty branch assignment sees nothing.
            if (branchIds == null || branchIds.isEmpty()) {
                branchIds = List.of(-1L);
            }

            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(new Column("branch_id"));
            ExpressionList<Expression> values = new ExpressionList<>();
            for (Long branchId : branchIds) {
                values.add(new LongValue(branchId));
            }
            inExpression.setRightExpression(new ParenthesedExpressionList<>(values));

            if (plainSelect.getWhere() == null) {
                plainSelect.setWhere(inExpression);
            } else {
                plainSelect.setWhere(new AndExpression(plainSelect.getWhere(), inExpression));
            }

            String newSql = select.toString();
            org.apache.ibatis.reflection.MetaObject metaObject =
                    org.apache.ibatis.reflection.SystemMetaObject.forObject(boundSql);
            metaObject.setValue("sql", newSql);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.FORBIDDEN, "校区范围过滤失败，已拒绝查询");
        }
    }

    private CustomUserDetails currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details;
        }
        return null;
    }

    private String resolveMainTableName(FromItem fromItem) {
        if (fromItem instanceof Table table) {
            return table.getName();
        }
        return null;
    }
}
