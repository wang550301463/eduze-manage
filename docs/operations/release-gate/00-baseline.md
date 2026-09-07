# 阶段 1：全局基线（RC-00）

> 全部 PASS 后方可进入模块审核。任一 FAIL 停止并修复。

---

### RC-00-001 后端单元与集成测试全绿

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §9.4、phase1-plan I5 |
| **后端验证** | 执行 `./mvnw verify`，含 Testcontainers `*IT.java` |
| **前端验证** | — |
| **自动化证据** | `./mvnw verify` 退出码 0 |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | Cursor Agent (Cross-Review) / 2026-06-12 |
| **备注 / 证据** | `cross-review-mvn-verify.log` |

---

### RC-00-002 前端单元测试全绿

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | phase1-plan C 阶段 |
| **后端验证** | — |
| **前端验证** | `cd web && pnpm test` 全绿 |
| **自动化证据** | `cd web && pnpm test` |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | Cursor Agent (Cross-Review) / 2026-06-12 |
| **备注 / 证据** | 22/22 `pnpm test` |

---

### RC-00-003 E2E 冒烟全绿

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | phase1-plan Task I5 |
| **后端验证** | 后端 health 可达 |
| **前端验证** | 6 个 spec 全绿：`login`, `students`, `student-create`, `schedule`, `attendance`, `settings` |
| **自动化证据** | `./scripts/e2e-bootstrap.sh` 或 README 手动流程后 `cd web && pnpm e2e` |
| **E2E 参考** | `web/e2e/*.spec.ts` |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-00-004 生产 Jar 可构建且含前端静态资源

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §6.1、§8.4 |
| **后端验证** | `./mvnw -DskipTests package` 成功；`jar tf target/*.jar \| grep static/index.html` 有输出 |
| **前端验证** | — |
| **自动化证据** | `./mvnw -DskipTests package` |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-00-005 Flyway 空库迁移无 ERROR

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §6.2、runbook 升级章节 |
| **后端验证** | 空库启动应用，日志无 Flyway migration ERROR；`t_flyway_schema_history` 记录完整 |
| **前端验证** | — |
| **自动化证据** | 空 MySQL + `./mvnw spring-boot:run` 或 Docker 首次启动 |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-00-006 第一期范围未越界

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.1「不做项」 |
| **后端验证** | 无作品/支付/招生/微信家长端等半成品 API 对普通角色开放 |
| **前端验证** | 导航无上述模块入口；`PlaceholderPage` 路由不可达 |
| **自动化证据** | 人工对照 §4.1 表格逐项确认 |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |
