package com.eduze.commerce;

import static com.eduze.commerce.CommerceModels.*;

import com.eduze.platform.runtime.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    static final String COLUMNS =
            "id,branch_id,user_id,product_id,student_id,course_id,product_type,product_title,unit_minor,quantity,total_minor,lesson_units,payment_status,fulfillment_status,tracking_number,created_at,fulfillment_method,recipient_name,recipient_phone,delivery_address";
    private final JdbcTemplate jdbc;
    private final InternalClient client;
    private final Outbox outbox;

    public OrderService(JdbcTemplate jdbc, InternalClient client, Outbox outbox) {
        this.jdbc = jdbc;
        this.client = client;
        this.outbox = outbox;
    }

    static String id() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Transactional
    public Order create(Actor actor, OrderInput input) {
        CatalogService.text(input.idempotencyKey(), 100);
        if (input.quantity() < 1 || input.quantity() > 100) {
            throw new PlatformException(400, "数量应为1至100");
        }
        // Product row serializes reservation and repeated create commands for the same item.
        var products =
                jdbc.queryForList(
                        "SELECT tenant_id,branch_id,type,title,price_minor,course_id,lesson_units,published FROM product WHERE id=? FOR UPDATE",
                        input.productId());
        if (products.isEmpty()
                || !actor.tenantId().equals(products.get(0).get("tenant_id"))
                || !input.branchId().equals(products.get(0).get("branch_id"))) {
            throw new PlatformException(404, "商品不存在");
        }
        String method = input.fulfillmentMethod() == null ? "PICKUP" : input.fulfillmentMethod();
        if (!Set.of("PICKUP", "DELIVERY").contains(method)) {
            throw new PlatformException(400, "配送方式无效");
        }
        boolean delivery =
                "PHYSICAL".equals(products.get(0).get("type")) && "DELIVERY".equals(method);
        if ("DELIVERY".equals(method) && !delivery) {
            throw new PlatformException(400, "该商品不支持配送");
        }
        String name = delivery ? CatalogService.text(input.recipientName(), 80) : null,
                phone = delivery ? CatalogService.text(input.recipientPhone(), 32) : null,
                address = delivery ? CatalogService.text(input.address(), 500) : null;
        if (delivery && !phone.matches("[+0-9 -]{7,25}")) {
            throw new PlatformException(400, "收件电话无效");
        }
        var previous =
                jdbc.query(
                        "SELECT "
                                + COLUMNS
                                + " FROM commerce_order WHERE tenant_id=? AND user_id=? AND idempotency_key=?",
                        OrderService::map,
                        actor.tenantId(),
                        actor.userId(),
                        input.idempotencyKey());
        if (!previous.isEmpty()) {
            var old = previous.get(0);
            if (!old.productId().equals(input.productId())
                    || !Objects.equals(old.studentId(), input.studentId())
                    || old.quantity() != input.quantity()
                    || !old.fulfillmentMethod().equals(method)
                    || !Objects.equals(old.recipientName(), name)
                    || !Objects.equals(old.recipientPhone(), phone)
                    || !Objects.equals(old.address(), address)) {
                throw new PlatformException(409, "幂等键已用于不同订单");
            }
            return old;
        }
        var p = products.get(0);
        String type = (String) p.get("type");
        if ("COURSE".equals(type)) {
            CatalogService.text(input.studentId(), 64);
            Map<?, ?> access =
                    client.get(
                            "academic",
                            "/internal/academic/students/" + input.studentId() + "/access",
                            Map.class);
            if (access == null
                    || !Boolean.TRUE.equals(access.get("allowed"))
                    || !input.branchId().equals(access.get("branchId"))) {
                throw new PlatformException(403, "无法为该学生购买课程");
            }
        }
        if (jdbc.update(
                        "UPDATE product SET stock=stock-?,version=version+1 WHERE id=? AND published=true AND stock>=?",
                        input.quantity(),
                        input.productId(),
                        input.quantity())
                != 1) {
            throw new PlatformException(409, "商品未上架或库存不足");
        }
        long unit = ((Number) p.get("price_minor")).longValue();
        long total = Math.multiplyExact(unit, input.quantity());
        int lessons =
                "COURSE".equals(type)
                        ? Math.multiplyExact(
                                ((Number) p.get("lesson_units")).intValue(), input.quantity())
                        : 0;
        String orderId = id();
        jdbc.update(
                "INSERT INTO commerce_order(id,tenant_id,branch_id,user_id,product_id,student_id,course_id,product_type,product_title,unit_minor,quantity,total_minor,lesson_units,payment_status,fulfillment_status,idempotency_key,fulfillment_method,recipient_name,recipient_phone,delivery_address) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,'UNPAID','PENDING',?,?,?,?,?)",
                orderId,
                actor.tenantId(),
                input.branchId(),
                actor.userId(),
                input.productId(),
                input.studentId(),
                p.get("course_id"),
                type,
                p.get("title"),
                unit,
                input.quantity(),
                total,
                lessons,
                input.idempotencyKey(),
                method,
                name,
                phone,
                address);
        return get(actor, orderId);
    }

    public Order get(Actor actor, String id) {
        var order = owned(actor, id);
        if (!actor.userId().equals(order.userId())) {
            CatalogService.staff(actor, order.branchId());
        }
        return order;
    }

    Order owned(Actor actor, String id) {
        var rows =
                jdbc.query(
                        "SELECT " + COLUMNS + " FROM commerce_order WHERE id=? AND tenant_id=?",
                        OrderService::map,
                        id,
                        actor.tenantId());
        if (rows.isEmpty()) {
            throw new PlatformException(404, "订单不存在");
        }
        return rows.get(0);
    }

    public List<Order> list(Actor actor, String branch) {
        if (branch != null) {
            CatalogService.staff(actor, branch);
        }
        return jdbc.query(
                "SELECT "
                        + COLUMNS
                        + " FROM commerce_order WHERE tenant_id=? AND "
                        + (branch == null ? "user_id=?" : "branch_id=?")
                        + " ORDER BY created_at DESC LIMIT 200",
                OrderService::map,
                actor.tenantId(),
                branch == null ? actor.userId() : branch);
    }

    @Transactional
    public void paid(String id, String transactionId, long amount) {
        var rows =
                jdbc.queryForList(
                        "SELECT tenant_id,branch_id,student_id,course_id,quantity,lesson_units,total_minor,payment_status,product_type,transaction_id FROM commerce_order WHERE id=? FOR UPDATE",
                        id);
        if (rows.isEmpty()) {
            throw new PlatformException(404, "订单不存在");
        }
        var row = rows.get(0);
        if (((Number) row.get("total_minor")).longValue() != amount) {
            throw new PlatformException(409, "支付金额不符");
        }
        String status = (String) row.get("payment_status");
        if (Set.of("PAID", "REFUNDING", "REFUNDED").contains(status)) {
            if (!transactionId.equals(row.get("transaction_id"))) {
                throw new PlatformException(409, "支付交易不符");
            }
            return;
        }
        if (!"UNPAID".equals(status)) {
            throw new PlatformException(409, "订单不可支付，需要人工核对");
        }
        jdbc.update(
                "UPDATE commerce_order SET payment_status='PAID',transaction_id=?,paid_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                CatalogService.text(transactionId, 100),
                id);
        if ("COURSE".equals(row.get("product_type"))) {
            outbox.enqueue(
                    "academic",
                    "commerce.order-paid",
                    (String) row.get("tenant_id"),
                    (String) row.get("branch_id"),
                    id,
                    Map.of(
                            "orderId",
                            id,
                            "studentId",
                            row.get("student_id"),
                            "courseId",
                            row.get("course_id"),
                            "quantity",
                            row.get("quantity"),
                            "lessonUnits",
                            row.get("lesson_units"),
                            "totalMinor",
                            amount));
        } else {
            jdbc.update("UPDATE commerce_order SET fulfillment_status='READY' WHERE id=?", id);
        }
    }

    @Transactional
    public void fulfillment(Actor actor, String id, FulfillmentInput input) {
        var order = get(actor, id);
        CatalogService.staff(actor, order.branchId());
        if (!"PAID".equals(order.paymentStatus()) || "COURSE".equals(order.productType())) {
            throw new PlatformException(409, "订单不可手工履约");
        }
        if ("SHIPPED".equals(input.status()) && !"DELIVERY".equals(order.fulfillmentMethod())) {
            throw new PlatformException(409, "自提订单不能标记发货");
        }
        boolean allowed =
                ("PHYSICAL".equals(order.productType())
                                && (("READY".equals(order.fulfillmentStatus())
                                                && Set.of("SHIPPED", "COMPLETED")
                                                        .contains(input.status()))
                                        || ("SHIPPED".equals(order.fulfillmentStatus())
                                                && "COMPLETED".equals(input.status()))))
                        || ("ACTIVITY".equals(order.productType())
                                && "READY".equals(order.fulfillmentStatus())
                                && "REDEEMED".equals(input.status()));
        if (input.status().equals(order.fulfillmentStatus())) {
            return;
        }
        if (!allowed) {
            throw new PlatformException(409, "履约状态不可跳转");
        }
        String tracking =
                "SHIPPED".equals(input.status())
                        ? CatalogService.text(input.trackingNumber(), 160)
                        : order.trackingNumber();
        if (jdbc.update(
                        "UPDATE commerce_order SET fulfillment_status=?,tracking_number=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND payment_status='PAID' AND fulfillment_status=?",
                        input.status(),
                        tracking,
                        id,
                        order.fulfillmentStatus())
                != 1) {
            throw new PlatformException(409, "订单已变化");
        }
    }

    public Map<String, Object> dashboard(Actor actor, String branch) {
        CatalogService.staff(actor, branch);
        Long paid =
                jdbc.queryForObject(
                        "SELECT COALESCE(SUM(total_minor),0) FROM commerce_order WHERE tenant_id=? AND branch_id=? AND payment_status IN ('PAID','REFUNDING','REFUNDED')",
                        Long.class,
                        actor.tenantId(),
                        branch);
        Long refunded =
                jdbc.queryForObject(
                        "SELECT COALESCE(SUM(r.amount_minor),0) FROM commerce_refund r JOIN commerce_order o ON o.id=r.order_id WHERE o.tenant_id=? AND o.branch_id=? AND r.status='COMPLETED'",
                        Long.class,
                        actor.tenantId(),
                        branch);
        return Map.of(
                "paidMinor",
                paid,
                "refundedMinor",
                refunded,
                "netReceiptsMinor",
                paid - refunded,
                "definition",
                "已支付订单金额减已完成退款金额，不代表利润");
    }

    @Transactional
    public String aftersale(Actor actor, String id, AftersaleInput input) {
        get(actor, id);
        String request = id();
        jdbc.update(
                "INSERT INTO commerce_aftersale(id,order_id,user_id,reason,status) VALUES(?,?,?,?,'OPEN')",
                request,
                id,
                actor.userId(),
                CatalogService.text(input.reason(), 2000));
        return request;
    }

    public List<Map<String, Object>> aftersales(Actor actor, String id) {
        get(actor, id);
        return jdbc.queryForList(
                "SELECT id,reason,status,response,created_at AS createdAt FROM commerce_aftersale WHERE order_id=? ORDER BY created_at DESC LIMIT 100",
                id);
    }

    @Transactional
    public void resolve(Actor actor, String orderId, String requestId, AftersaleResolution input) {
        var order = get(actor, orderId);
        CatalogService.staff(actor, order.branchId());
        if (!Set.of("PROCESSING", "CLOSED").contains(input.status())) {
            throw new PlatformException(400, "售后状态不正确");
        }
        if (jdbc.update(
                        "UPDATE commerce_aftersale SET status=?,response=? WHERE id=? AND order_id=?",
                        input.status(),
                        CatalogService.text(input.response(), 2000),
                        requestId,
                        orderId)
                != 1) {
            throw new PlatformException(404, "售后不存在");
        }
    }

    static Order map(java.sql.ResultSet r, int n) throws java.sql.SQLException {
        return new Order(
                r.getString(1),
                r.getString(2),
                r.getString(3),
                r.getString(4),
                r.getString(5),
                r.getString(6),
                r.getString(7),
                r.getString(8),
                r.getLong(9),
                r.getInt(10),
                r.getLong(11),
                r.getInt(12),
                r.getString(13),
                r.getString(14),
                r.getString(15),
                r.getTimestamp(16).toInstant(),
                r.getString(17),
                r.getString(18),
                r.getString(19),
                r.getString(20));
    }
}
