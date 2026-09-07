# EduZE 投产加固 — 发布验证证据（2026-09-07）

**目的：** 记录本轮「投产加固整改」自动门禁的真实结果。未执行项不得标 PASS。

## 自动门禁结果

| 门禁 | 结果 | 证据 |
|------|------|------|
| 后端 `./mvnw -Dskip.frontend.build=true verify` | **PASS** | 91 IT，0 Fail / 0 Error / 0 Skip（`/tmp/eduze-verify3.log`） |
| 前端 Vitest | **PASS** | 13 files / 22 tests |
| 前端 `tsc` + `vite build` | **PASS** | 路由懒加载后构建成功；主 chunk 已拆分 |
| 前端 ESLint | **PASS*** | hooks 条件调用已修；保留 3 条既有 warning（`max-warnings 5`） |
| Playwright E2E | **PASS** | 9/9（系统 Chrome + 本地 Vite:5173 + App:8080；`/tmp/eduze-e2e.log`） |
| Flyway 空库迁移 | **PASS** | IT 空库应用 **16** 条迁移至 `v9.3.1`（日志：Successfully applied 16 migrations） |
| Actuator 指标保护 | **PASS** | `/actuator/health` 公开；`/actuator/prometheus` 未认证 401、认证后 200（`ActuatorSecurityIT` + 本地 curl） |
| 登录 / 学员 API 冒烟 | **PASS** | 本地 `dev` 栈 `POST /api/auth/login` + `GET /api/students` 200 |

\* lint 策略：禁止 error；允许少量既有 react-refresh / exhaustive-deps warning。

## 环境侧未完成（不得伪装为代码 PASS）

| 项 | 状态 | 说明 |
|----|------|------|
| 生产 Compose 全栈冷启动 + HTTPS 业务演练 | **未完成** | 已构建 `eduze-manage:release-verify` 镜像；本会话 Docker 高权限操作审批失败，未 `compose up` 全栈 |
| mysqldump 宿主机备份演练 | **未完成** | 脚本已加固（非空校验、`gunzip -t`、`.sha256`）；本机无 `mysqldump` CLI，需 `docker exec` 执行 |
| 正式 CA 证书 | **环境责任** | 仓库仅有自签 `docker/certs`；正式域名证书由运维签发 |
| 人工负责人签字 | **环境责任** | Agent 证据不可替代 `sign-off-record.md` 真人签字 |

## 本轮加固摘要

- 会话：`token_version`、Refresh Redis 轮换/注销、禁用/改密/角色校区变更立即失效、生产 bootstrap 管理员
- 校区：`BranchAccessGuard` + interceptor fail-closed；家长搜索分校过滤
- 业务：AbsenceJob 老师中心名单、导入强制 mentor、课次写前冲突校验
- 审计：敏感 API `@AuditAction`、traceId、500 堆栈日志、Prometheus 认证
- 数据：V9.3.0 `token_version`；V9.3.1 孤儿/重复 enroll 守卫；prod Flyway 显式化
- 运维：Compose Redis AUTH healthcheck、取消 DB/Redis 宿主机端口、备份/恢复确认、runbook 回滚/RPO/RTO

## 剩余放行条件（人工）

1. 投产负责人在 `docs/operations/release-gate/sign-off-record.md` 签字并填写本 tag SHA  
2. 正式 HTTPS 证书替换自签证书  
3. 在目标机执行一次：Compose 冷启动 → 备份 → 停服恢复 → HTTPS 冒烟
