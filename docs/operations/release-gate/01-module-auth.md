# 模块 0：登录 / RBAC / 校区（RC-01）

**后端包：** `auth/`, `branch/`  
**前端路由：** `/login`, `/settings/branches`, `/settings/users`, `/settings/roles`  
**主要 IT：** `AuthControllerIT`, `RbacIT`, `BranchControllerIT`, `LoginAttemptServiceIT`, `RateLimitFilterIT`, `MeControllerIT`  
**E2E：** `web/e2e/login.spec.ts`, `settings.spec.ts`

---

### RC-01-001 登录成功与 Token

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.2 |
| **后端验证** | `POST /api/auth/login` 返回 access + refresh；`AuthControllerIT` 全绿 |
| **前端验证** | 正确账号登录跳转 `/`；`login.spec.ts` admin 场景通过 |
| **自动化证据** | `./mvnw test -Dtest=AuthControllerIT` |
| **E2E 参考** | `login.spec.ts` |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-01-002 错误密码与登录失败

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.2 |
| **后端验证** | 错误凭证返回 401/业务错误 |
| **前端验证** | 停留 `/login` 并显示错误提示；`login.spec.ts` wrong password 场景 |
| **自动化证据** | E2E `login.spec.ts` |
| **E2E 参考** | `login.spec.ts` |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-01-003 登录失败锁定

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.2（5 次失败锁定 10 分钟） |
| **后端验证** | `LoginAttemptServiceIT` 全绿；连续失败后 Redis 锁定 |
| **前端验证** | 锁定期间登录失败提示与后端一致 |
| **自动化证据** | `./mvnw test -Dtest=LoginAttemptServiceIT` |
| **E2E 参考** | — |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-01-004 登出与 JWT 黑名单

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.2 |
| **后端验证** | 登出后旧 access token 请求 `/api/**` 返回 401 |
| **前端验证** | 用户菜单登出 → `/login`；刷新不可访问受保护页 |
| **自动化证据** | curl 登出前后对比 |
| **E2E 参考** | — |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-01-005 RBAC API 权限拦截

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.2 |
| **后端验证** | 无权限用户调用受保护 API 返回 403；`RbacIT` 全绿 |
| **前端验证** | `RequirePermission` / `ProtectedRoute` 拦截直链 |
| **自动化证据** | `./mvnw test -Dtest=RbacIT` |
| **E2E 参考** | — |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-01-006 校区数据范围

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.2、§5.3 |
| **后端验证** | 用户仅见绑定校区数据；`BranchScopeIT`, `TenantInterceptorIT` |
| **前端验证** | 筛选器仅展示授权校区 |
| **自动化证据** | `./mvnw test -Dtest=BranchScopeIT,TenantInterceptorIT` |
| **E2E 参考** | — |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-01-007 校区 CRUD

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.2 |
| **后端验证** | 仅超管可写；`BranchControllerIT` |
| **前端验证** | `/settings/branches` 增删改查；`settings.spec.ts` branch 场景 |
| **自动化证据** | `./mvnw test -Dtest=BranchControllerIT` |
| **E2E 参考** | `settings.spec.ts` |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-01-008 账号与角色管理

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.2 |
| **后端验证** | 创建用户、分配角色/校区后权限生效 |
| **前端验证** | `/settings/users`, `/settings/roles`；`settings.spec.ts` user/role 场景 |
| **自动化证据** | 人工 + `UserController` 相关 IT（如有） |
| **E2E 参考** | `settings.spec.ts` |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-01-009 个人中心改密

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.2 |
| **后端验证** | `MeControllerIT`：改密后旧密码不可登录 |
| **前端验证** | 改密流程 UI 可用 |
| **自动化证据** | `./mvnw test -Dtest=MeControllerIT` |
| **E2E 参考** | — |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-01-010 内置角色不可删除

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §5.2 `t_role.is_builtin` |
| **后端验证** | 删除内置角色返回业务错误 |
| **前端验证** | UI 禁用删除或提示不可删 |
| **自动化证据** | API 或 UI 人工验证 |
| **E2E 参考** | — |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |
