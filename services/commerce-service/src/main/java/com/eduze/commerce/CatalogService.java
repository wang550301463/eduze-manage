package com.eduze.commerce;

import static com.eduze.commerce.CommerceModels.*;

import com.eduze.platform.runtime.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final JdbcTemplate jdbc;

    public CatalogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static String text(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new PlatformException(400, "输入长度不正确");
        }
        return value.trim();
    }

    static void staff(Actor actor, String branch) {
        actor.requireBranch(branch);
        actor.requirePermission("commerce:write");
    }

    @Transactional
    public Product save(Actor actor, ProductInput input) {
        staff(actor, input.branchId());
        if (!Set.of("COURSE", "ACTIVITY", "PHYSICAL").contains(input.type())
                || input.priceMinor() < 1
                || input.priceMinor() > 100000000L
                || input.stock() < 0
                || input.stock() > 1000000) {
            throw new PlatformException(400, "商品类型、价格或库存无效");
        }
        if ("COURSE".equals(input.type())
                && (input.courseId() == null
                        || input.courseId().isBlank()
                        || input.lessonUnits() < 1
                        || input.lessonUnits() > 10000)) {
            throw new PlatformException(400, "课程商品需要课程与课时数");
        }
        String id = input.id() == null ? UUID.randomUUID().toString() : text(input.id(), 64);
        var exists =
                jdbc.queryForList(
                        "SELECT tenant_id,branch_id,type,version FROM product WHERE id=? FOR UPDATE",
                        id);
        if (!exists.isEmpty()
                && (!actor.tenantId().equals(exists.get(0).get("tenant_id"))
                        || !input.branchId().equals(exists.get(0).get("branch_id"))
                        || !input.type().equals(exists.get(0).get("type")))) {
            throw new PlatformException(409, "商品归属和类型不可修改");
        }
        String title = text(input.title(), 160), description = text(input.description(), 20000);
        if (exists.isEmpty()) {
            if (input.version() != 0) {
                throw new PlatformException(409, "商品版本已变化，请刷新后再试");
            }
            jdbc.update(
                    "INSERT INTO product(id,tenant_id,branch_id,type,title,description,price_minor,stock,course_id,lesson_units,published) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                    id,
                    actor.tenantId(),
                    input.branchId(),
                    input.type(),
                    title,
                    description,
                    input.priceMinor(),
                    input.stock(),
                    input.courseId(),
                    input.lessonUnits(),
                    input.published());
        } else {
            if (input.version() != ((Number) exists.get(0).get("version")).intValue()) {
                throw new PlatformException(409, "商品版本已变化，请刷新库存后再试");
            }
            int updated =
                    jdbc.update(
                            "UPDATE product SET title=?,description=?,price_minor=?,stock=?,course_id=?,lesson_units=?,published=?,version=version+1 WHERE id=? AND version=?",
                            title,
                            description,
                            input.priceMinor(),
                            input.stock(),
                            input.courseId(),
                            input.lessonUnits(),
                            input.published(),
                            id,
                            input.version());
            if (updated != 1) {
                throw new PlatformException(409, "商品版本已变化，请刷新库存后再试");
            }
        }
        return get(id);
    }

    Product get(String id) {
        var rows =
                jdbc.query(
                        "SELECT id,branch_id,type,title,description,price_minor,stock,course_id,lesson_units,published,version FROM product WHERE id=?",
                        CatalogService::map,
                        id);
        if (rows.isEmpty()) {
            throw new PlatformException(404, "商品不存在");
        }
        return rows.get(0);
    }

    public List<Product> list(Actor actor, String branch, boolean publishedOnly) {
        if (!publishedOnly) {
            staff(actor, branch);
        }
        return jdbc.query(
                "SELECT id,branch_id,type,title,description,price_minor,stock,course_id,lesson_units,published,version FROM product WHERE branch_id=?"
                        + (publishedOnly ? " AND published=true" : " AND tenant_id=?")
                        + " ORDER BY title LIMIT 200",
                CatalogService::map,
                publishedOnly ? new Object[] {branch} : new Object[] {branch, actor.tenantId()});
    }

    private static Product map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new Product(
                rs.getString(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                rs.getLong(6),
                rs.getInt(7),
                rs.getString(8),
                rs.getInt(9),
                rs.getBoolean(10),
                rs.getInt(11));
    }
}
