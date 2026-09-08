package com.eduze.manage.integration;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.eduze.platform.runtime.*;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommerceEntitlements implements EventHandler {
    private final JdbcTemplate jdbc;
    private final IntegrationScope scope;
    private final Outbox outbox;

    public record RefundRequest(
            String refundId,
            String orderId,
            String tenantId,
            String branchId,
            String studentId,
            int lessonUnits) {}

    public record RefundState(String refundId, String orderId, String status, int lessonUnits) {}

    @Override
    public boolean supports(String type) {
        return "commerce.order-paid".equals(type);
    }

    @Override
    @Transactional
    public void handle(EventEnvelope event) {
        try {
            issue(event);
        } catch (PlatformException ex) {
            if (ex.getStatus() >= 500) throw ex;
            outbox.enqueue(
                    "commerce",
                    "academic.entitlement-rejected",
                    event.tenantId(),
                    event.branchId(),
                    event.aggregateId(),
                    Map.of(
                            "orderId",
                            event.payload().path("orderId").asText(event.aggregateId()),
                            "reason",
                            ex.getMessage()));
        }
    }

    private void issue(EventEnvelope event) {
        scope.requireTenant(event.tenantId());
        String order = event.payload().path("orderId").asText();
        scope.requireId(order);
        if (!order.equals(event.aggregateId())) throw new PlatformException(400, "订单事件标识不一致");
        String student = event.payload().path("studentId").asText(),
                course = event.payload().path("courseId").asText();
        int units = event.payload().path("lessonUnits").asInt();
        if (units < 1
                || units > 100000
                || event.payload().path("quantity").asInt() < 1
                || event.payload().path("totalMinor").asLong() < 0)
            throw new PlatformException(400, "课程权益数量无效");
        if (jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_student WHERE tenant_id=? AND id=? AND branch_id=? AND deleted_at=0",
                        Integer.class,
                        event.tenantId(),
                        student,
                        event.branchId())
                != 1) throw new PlatformException(403, "订单学员校区不匹配");
        if (jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_course WHERE tenant_id=? AND id=? AND deleted_at=0",
                        Integer.class,
                        event.tenantId(),
                        course)
                != 1) throw new PlatformException(404, "订单课程不存在");
        var existing =
                jdbc.queryForList(
                        "SELECT id,student_id,course_id,total_lessons,branch_id FROM t_course_package WHERE tenant_id=? AND source_order_id=? FOR UPDATE",
                        event.tenantId(),
                        order);
        String packageId;
        if (!existing.isEmpty()) {
            var row = existing.get(0);
            if (!student.equals(row.get("student_id").toString())
                    || !course.equals(row.get("course_id").toString())
                    || units != ((Number) row.get("total_lessons")).intValue()
                    || !event.branchId().equals(row.get("branch_id").toString()))
                throw new PlatformException(409, "订单权益已存在且内容不同");
            packageId = row.get("id").toString();
        } else {
            packageId = String.valueOf(IdWorker.getId());
            jdbc.update(
                    "INSERT INTO t_course_package(id,tenant_id,branch_id,student_id,course_id,total_lessons,remaining_lessons,source_order_id,note) VALUES(?,?,?,?,?,?,?,?,?)",
                    packageId,
                    event.tenantId(),
                    event.branchId(),
                    student,
                    course,
                    units,
                    units,
                    order,
                    "商城课程权益");
            ledger(
                    event.tenantId(),
                    event.branchId(),
                    student,
                    packageId,
                    units,
                    "ORDER_GRANT",
                    "ORDER:" + order);
        }
        outbox.enqueue(
                "commerce",
                "academic.entitlement-issued",
                event.tenantId(),
                event.branchId(),
                order,
                Map.of("orderId", order, "packageId", packageId));
    }

    @Transactional
    public RefundState freeze(RefundRequest request) {
        validate(request);
        var pkg = packageFor(request);
        var old = refund(request.refundId());
        if (!old.isEmpty()) {
            match(old.get(0), request);
            return state(old.get(0));
        }
        int remaining = ((Number) pkg.get("remaining_lessons")).intValue(),
                frozen = ((Number) pkg.get("frozen_lessons")).intValue();
        if (remaining - frozen < request.lessonUnits())
            throw new PlatformException(409, "订单剩余可退课时不足");
        jdbc.update(
                "UPDATE t_course_package SET frozen_lessons=frozen_lessons+?,version=version+1 WHERE tenant_id=? AND id=?",
                request.lessonUnits(),
                request.tenantId(),
                pkg.get("id"));
        jdbc.update(
                "INSERT INTO academic_refund(refund_id,tenant_id,branch_id,order_id,student_id,package_id,lesson_units,status) VALUES(?,?,?,?,?,?,?,'FROZEN')",
                request.refundId(),
                request.tenantId(),
                request.branchId(),
                request.orderId(),
                request.studentId(),
                pkg.get("id"),
                request.lessonUnits());
        return new RefundState(
                request.refundId(), request.orderId(), "FROZEN", request.lessonUnits());
    }

    @Transactional
    public RefundState complete(RefundRequest request) {
        return change(request, true);
    }

    @Transactional
    public RefundState release(RefundRequest request) {
        return change(request, false);
    }

    private RefundState change(RefundRequest request, boolean complete) {
        validate(request);
        var pkg = packageFor(request);
        var rows = refund(request.refundId());
        if (rows.isEmpty()) throw new PlatformException(404, "退款冻结不存在");
        var row = rows.get(0);
        match(row, request);
        String target = complete ? "COMPLETED" : "RELEASED";
        if (target.equals(row.get("status"))) return state(row);
        if (!"FROZEN".equals(row.get("status"))) throw new PlatformException(409, "退款状态不可逆转");
        int frozen = ((Number) pkg.get("frozen_lessons")).intValue();
        if (frozen < request.lessonUnits()) throw new PlatformException(409, "冻结权益不一致，需人工处理");
        jdbc.update(
                "UPDATE t_course_package SET frozen_lessons=frozen_lessons-?,remaining_lessons=remaining_lessons-?,version=version+1 WHERE tenant_id=? AND id=?",
                request.lessonUnits(),
                complete ? request.lessonUnits() : 0,
                request.tenantId(),
                pkg.get("id"));
        jdbc.update(
                "UPDATE academic_refund SET status=?,updated_at=NOW(3) WHERE tenant_id=? AND refund_id=?",
                target,
                request.tenantId(),
                request.refundId());
        if (complete)
            ledger(
                    request.tenantId(),
                    request.branchId(),
                    request.studentId(),
                    pkg.get("id").toString(),
                    -request.lessonUnits(),
                    "ORDER_REFUND",
                    "REFUND:" + request.refundId());
        return new RefundState(
                request.refundId(), request.orderId(), target, request.lessonUnits());
    }

    public RefundState get(String id) {
        scope.requireId(id);
        var rows = refund(id);
        if (rows.isEmpty()) throw new PlatformException(404, "退款记录不存在");
        return state(rows.get(0));
    }

    private List<Map<String, Object>> refund(String id) {
        return jdbc.queryForList(
                "SELECT refund_id,order_id,tenant_id,branch_id,student_id,lesson_units,status FROM academic_refund WHERE tenant_id=? AND refund_id=?",
                scope.tenant(),
                id);
    }

    private Map<String, Object> packageFor(RefundRequest r) {
        var rows =
                jdbc.queryForList(
                        "SELECT id,student_id,branch_id,remaining_lessons,frozen_lessons FROM t_course_package WHERE tenant_id=? AND source_order_id=? AND deleted_at=0 FOR UPDATE",
                        r.tenantId(),
                        r.orderId());
        if (rows.isEmpty()) throw new PlatformException(404, "原订单课时包不存在");
        var row = rows.get(0);
        if (!r.studentId().equals(row.get("student_id").toString())
                || !r.branchId().equals(row.get("branch_id").toString()))
            throw new PlatformException(403, "退款不属于该学员校区");
        return row;
    }

    private void validate(RefundRequest r) {
        scope.requireTenant(r.tenantId());
        scope.requireId(r.refundId());
        scope.requireId(r.orderId());
        scope.requireId(r.studentId());
        scope.requireId(r.branchId());
        if (r.lessonUnits() < 1 || r.lessonUnits() > 100000)
            throw new PlatformException(400, "退款课时无效");
    }

    private void match(Map<String, Object> row, RefundRequest r) {
        if (!r.orderId().equals(row.get("order_id"))
                || !r.studentId().equals(row.get("student_id").toString())
                || !r.branchId().equals(row.get("branch_id").toString())
                || r.lessonUnits() != ((Number) row.get("lesson_units")).intValue())
            throw new PlatformException(409, "退款标识对应不同内容");
    }

    private RefundState state(Map<String, Object> row) {
        return new RefundState(
                row.get("refund_id").toString(),
                row.get("order_id").toString(),
                row.get("status").toString(),
                ((Number) row.get("lesson_units")).intValue());
    }

    private void ledger(
            String tenant,
            String branch,
            String student,
            String pkg,
            int units,
            String type,
            String reference) {
        Integer available =
                jdbc.queryForObject(
                        "SELECT COALESCE(SUM(remaining_lessons-frozen_lessons),0) FROM t_course_package WHERE tenant_id=? AND student_id=? AND deleted_at=0 AND (expire_date IS NULL OR expire_date>=CURRENT_DATE)",
                        Integer.class,
                        tenant,
                        student);
        jdbc.update(
                "INSERT INTO t_student_lesson_hour_ledger(id,tenant_id,branch_id,student_id,package_id,event_type,minutes_delta,lesson_units_delta,remaining_lessons_after,occurred_at,note,external_reference) VALUES(?,?,?,?,?,?,0,?,?,NOW(3),?,?)",
                IdWorker.getId(),
                tenant,
                branch,
                student,
                pkg,
                type,
                units,
                available,
                "订单权益调整",
                reference);
    }
}
