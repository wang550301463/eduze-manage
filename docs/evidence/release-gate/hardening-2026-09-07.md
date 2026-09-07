# EduZE 投产加固 — 发布验证证据（2026-09-07）

**目的：** 记录本轮「投产加固整改」自动门禁与全栈演练的真实结果。未执行项不得标 PASS。

## 自动门禁结果

| 门禁 | 结果 | 证据 |
|------|------|------|
| 后端 `./mvnw -Dskip.frontend.build=true verify` | **PASS** | 91 IT，0 Fail / 0 Error / 0 Skip |
| 前端 Vitest | **PASS** | 13 files / 22 tests |
| 前端 `tsc` + `vite build` | **PASS** | 路由懒加载后构建成功 |
| 前端 ESLint | **PASS*** | hooks 条件调用已修；允许少量既有 warning（`max-warnings 5`） |
| Playwright E2E | **PASS** | 9/9（系统 Chrome + Vite:5173 + App:8080） |
| Flyway 空库迁移 | **PASS** | 16 条迁移至 `v9.3.1`（IT 空库 + Compose 冷启动） |
| Actuator 指标保护 | **PASS** | health 公开；prometheus 未认证 401 / 认证后 200 |

\* lint：禁止 error；允许少量既有 react-refresh / exhaustive-deps warning。

## Compose HTTPS 全栈演练（2026-09-07 13:40+）

| 项 | 结果 | 说明 |
|----|------|------|
| `compose down -v && up -d` 冷启动 | **PASS** | mysql/redis/app/nginx 均 healthy |
| HTTPS 登录页 | **PASS** | `GET https://127.0.0.1:18443/login` → 200（自签证书） |
| HTTPS 登录（bootstrap 管理员） | **PASS** | `POST /api/auth/login` → 200 + accessToken |
| HTTPS 学员列表 | **PASS** | `GET /api/students` Bearer → 200 |
| HTTPS 校区 / 今日考勤 | **PASS** | `/api/branches`、`/api/attendance/today?branchId=1` → 200 |
| HTTPS Prometheus | **PASS** | 匿名 401；Bearer 200 |
| 备份 | **PASS** | `docker exec mysqldump` → `backups/eduze-2026-09-07-1343.sql.gz`（9276B）+ `.sha256` |
| 恢复 | **PASS** | 停 app → restore → 核心表计数一致（student/user/flyway）→ 重启后登录 200 |

**踩坑记录：** 宿主机 shell 若已 export 短 `DB_PASSWORD`，会覆盖 `docker/.env`（Compose 变量优先级）。冷启动前必须 `unset DB_PASSWORD`（及相关密钥变量）。

## 仍属环境放行（不得伪装为代码 PASS）

| 项 | 状态 | 说明 |
|----|------|------|
| 正式 CA 证书 | **环境责任** | 当前为自签 `docker/certs`；正式域名证书由运维签发 |
| 人工负责人签字 | **环境责任** | 见 `sign-off-record.md` |

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
3. 目标机按 runbook 再跑一遍冷启动/备份/恢复（可复用本证据流程）
