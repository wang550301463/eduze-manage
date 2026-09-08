package com.eduze.manage.identity;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.auth.security.CustomUserDetailsService;
import com.eduze.manage.branch.mapper.BranchMapper;
import com.eduze.manage.identity.IdentityDirectoryModels.BranchView;
import com.eduze.manage.identity.IdentityDirectoryModels.UserView;
import com.eduze.manage.tenant.BranchAccessGuard;
import com.eduze.platform.runtime.PlatformException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentityDirectoryService {
    private final Environment env;
    private final JdbcTemplate jdbc;
    private final UserMapper users;
    private final BranchMapper branches;
    private final CustomUserDetailsService details;
    private final BranchAccessGuard guard;

    public UserView user(Long id) {
        guard.requireUser();
        return view(users.selectById(id));
    }

    public List<UserView> batch(List<Long> ids) {
        guard.requireUser();
        validateIds(ids);
        return users.selectBatchIds(ids).stream().map(this::view).toList();
    }

    public List<UserView> teachers(Long branchId) {
        if (branchId != null) guard.requireBranchAccess(branchId);
        guard.requireUser();
        return users.selectList(Wrappers.<User>lambdaQuery().eq(User::getStatus, 1)).stream()
                .filter(
                        u ->
                                guard.requireUser().isSuperAdmin()
                                        || !Collections.disjoint(
                                                details.loadByUserId(u.getId()).getBranchIds(),
                                                guard.requireUser().getBranchIds()))
                .map(this::view)
                .filter(
                        u ->
                                u.roles().contains("TEACHER")
                                        && (branchId == null || u.branchIds().contains(branchId)))
                .toList();
    }

    public List<BranchView> branches() {
        guard.requireUser();
        return branches.selectList(null).stream()
                .filter(
                        b ->
                                guard.requireUser().isSuperAdmin()
                                        || guard.requireUser().getBranchIds().contains(b.getId()))
                .map(b -> new BranchView(b.getId(), b.getTenantId(), b.getName(), b.getCode()))
                .toList();
    }

    public BranchView branch(Long id) {
        guard.requireBranchAccess(id);
        var b = branches.selectById(id);
        if (b == null) throw new PlatformException(404, "校区不存在");
        return new BranchView(b.getId(), b.getTenantId(), b.getName(), b.getCode());
    }

    public List<BranchView> branchesBatch(List<Long> ids) {
        guard.requireUser();
        validateIds(ids);
        ids.forEach(guard::requireBranchAccess);
        return branches.selectBatchIds(ids).stream()
                .map(b -> new BranchView(b.getId(), b.getTenantId(), b.getName(), b.getCode()))
                .toList();
    }

    public List<Map<String, String>> recipients(List<String> userIds) {
        if (userIds == null || userIds.size() > 200) throw new PlatformException(400, "每批最多 200 人");
        if (userIds.isEmpty()) return List.of();
        String marks = String.join(",", Collections.nCopies(userIds.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(env.getRequiredProperty("eduze.tenant.default-id"));
        args.add(env.getProperty("eduze.wechat.app-id", ""));
        args.addAll(userIds);
        return jdbc.query(
                "SELECT u.id,w.open_id FROM wechat_identity w JOIN t_user u ON (u.id=w.user_id OR u.id=w.staff_user_id) AND u.tenant_id=w.tenant_id WHERE w.tenant_id=? AND w.app_id=? AND u.status=1 AND u.deleted_at=0 AND u.id IN ("
                        + marks
                        + ")",
                (rs, n) -> Map.of("userId", rs.getString(1), "openId", rs.getString(2)),
                args.toArray());
    }

    public Map<String, String> paymentIdentity(Long userId) {
        var actor = guard.requireUser();
        if (!actor.getUserId().equals(userId)) throw new PlatformException(403, "仅可使用当前账号支付");
        String appId = env.getProperty("eduze.wechat.app-id", "");
        if (appId.isBlank()) throw new PlatformException(503, "微信应用尚未配置");
        var rows =
                jdbc.query(
                        "SELECT w.open_id FROM wechat_identity w JOIN t_user u ON (u.id=w.user_id OR u.id=w.staff_user_id) AND u.tenant_id=w.tenant_id WHERE w.tenant_id=? AND w.app_id=? AND u.id=? AND u.status=1 AND u.deleted_at=0",
                        (rs, n) -> rs.getString(1),
                        actor.getTenantId(),
                        appId,
                        userId);
        if (rows.size() != 1) throw new PlatformException(403, "当前账号未绑定唯一可用的微信身份");
        return Map.of("openid", rows.get(0), "appId", appId);
    }

    private UserView view(User u) {
        if (u == null) throw new PlatformException(404, "员工不存在");
        var d = details.loadByUserId(u.getId());
        if (!guard.requireUser().isSuperAdmin()
                && Collections.disjoint(d.getBranchIds(), guard.requireUser().getBranchIds())
                && !u.getId().equals(guard.requireUser().getUserId()))
            throw new PlatformException(403, "无权访问员工");
        return new UserView(
                u.getId(),
                u.getTenantId(),
                u.getBranchId(),
                u.getName(),
                u.getUsername(),
                u.getStatus(),
                d.getBranchIds(),
                d.getRoleCodes());
    }

    private void validateIds(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 200)
            throw new PlatformException(400, "每批 1–200 个 ID");
    }
}
