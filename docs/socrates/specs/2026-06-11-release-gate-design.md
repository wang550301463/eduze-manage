# EduZE Manage 第一期 MVP 投产检查流程设计

**日期：** 2026-06-11  
**状态：** 已批准  
**范围：** 第一期 MVP 一次性投产门禁（非持续 CI 流程）  
**对应 spec：** `docs/socrates/specs/2026-05-09-eduze-manage-design.md` §4.1、§9  
**可执行清单：** `docs/operations/release-gate/`

---

## 1. 背景与目标

第一期 MVP 覆盖模块 0（账号/权限/校区）、学员与家长、课程与排课、签到与接送，以及横切全局体验。代码库已有约 38 个后端 `*IT.java` 集成测试、6 个 Playwright E2E spec、Docker Compose 部署与 `runbook.md`，但**缺少统一的投产门禁流程**。

本设计定义：

1. **谁审**：开发团队自审（主审人 + 交叉 Reviewer），无独立 QA
2. **怎么审**：按功能模块前后端分开验证，Markdown 清单人工勾选
3. **何时放行**：84 项 Blocker 全部 PASS + 投产负责人签字
4. **证据在哪**：`docs/operations/release-gate/sign-off-record.md`

---

## 2. 决策摘要

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 场景 | 第一期一次性投产门禁 | 用户明确 |
| 执行方 | 开发自审 + 交叉 Review | 无 QA 编制 |
| 放行标准 | 硬性门禁，无风险放行 | 少儿数据与教务核心，不容带病上线 |
| 自动化 | 文档清单为主，命令写在清单内人工执行 | 用户明确；第一期不引入 gate 脚本 |
| 文档结构 | 主 README + 分模块子清单 + 签字表 | 可并行、责任清晰、与代码库同版本 |

---

## 3. 流程总览

投产检查分 **5 个阶段**，前一阶段未全绿不得进入下一阶段。

```
阶段 0：准备（冻结范围、分工）
    ↓
阶段 1：全局基线（00-baseline.md，6 项）
    ↓
阶段 2：按模块审核（01–04，43 项，可并行）
    ↓
阶段 3：横切 + 非功能运维（05–06，24 项）
    ↓
阶段 4：生产演练（07-production-dry-run.md，11 项）
    ↓
阶段 5：签字放行（sign-off-record.md）
```

### 3.1 角色

| 角色 | 职责 |
|------|------|
| **主审人** | 熟悉该模块，按清单执行后端/前端步骤，填 PASS/FAIL 与证据 |
| **交叉 Reviewer** | 非主审该模块的同事，复现关键路径，签字确认 |
| **投产负责人** | 汇总各模块结果，组织生产演练，最终签字放行 |

### 3.2 硬性门禁规则

- 每一项仅 `PASS` / `FAIL`，本期全部为 **Blocker**
- 任一 `FAIL` → 登记缺陷 → 修复 → 重跑失败项及关联回归 → 再标 `PASS`
- 禁止「已知问题」「带风险放行」
- 模块 100% PASS 后模块负责人签字；全部 84 项 PASS 后投产负责人签字

### 3.3 缺陷处理

1. 在 `sign-off-record.md` 缺陷表登记：RC 编号、描述、负责人、状态
2. 修复提交注明 RC 编号
3. 主审重跑失败项；涉及核心流程时加跑对应 `*IT` 与 E2E spec
4. 交叉 Reviewer 复现通过后关闭，RC 改 PASS

---

## 4. 文档结构

```
docs/operations/release-gate/
├── README.md                   # 入口：流程说明与阶段索引
├── 00-baseline.md              # 阶段 1：全局基线（6 项）
├── 01-module-auth.md           # 模块 0：登录 / RBAC / 校区（10 项）
├── 02-module-student.md        # 模块 #2：学员与家长（10 项）
├── 03-module-course-schedule.md # 模块 #3：课程 / 排课 / 老师（13 项）
├── 04-module-attendance.md     # 模块 #4：签到 / 接送 / 请假（10 项）
├── 05-module-cross-cutting.md  # 横切体验（8 项）
├── 06-nfr-ops.md               # 非功能与运维（16 项）
├── 07-production-dry-run.md    # 生产演练剧本（11 项）
└── sign-off-record.md          # 汇总签字与缺陷追踪
```

**合计：84 项 Blocker。**

---

## 5. 检查项格式

每条检查项采用统一模板（见各子清单文件），必含：

| 字段 | 说明 |
|------|------|
| RC 编号 | `RC-{模块}-{序号}`，如 `RC-02-005` |
| Spec 依据 | 指向 design spec 章节 |
| 后端验证 | 可复现步骤 + 相关 `*IT` |
| 前端验证 | 路由/交互/权限 UI |
| 自动化证据 | 人工执行的测试命令 |
| E2E 参考 | 对应 `web/e2e/*.spec.ts`（如有） |
| 结果 | PASS / FAIL |
| 主审人 / 交叉 Review / 备注 | 签字与证据（截图、curl 输出、commit hash） |

**不能由两人独立复现的检查项视为未通过。**

---

## 6. 模块映射

| 清单 | 后端包 | 前端路由 | 主要 IT / E2E |
|------|--------|----------|---------------|
| 01-module-auth | `auth/`, `branch/` | `/login`, `/settings/*` | `AuthControllerIT`, `RbacIT`, `settings.spec.ts` |
| 02-module-student | `student/` | `/students` | `StudentControllerIT`, `StudentImportIT`, `students.spec.ts` |
| 03-module-course-schedule | `course/`, `lesson/`, `teacher/`, `curriculum/` | `/courses/*`, `/schedule`, `/teachers/*`, `/workbench` | `Lesson*IT`, `ConflictServiceIT`, `schedule.spec.ts` |
| 04-module-attendance | `attendance/` | `/attendance/*` | `Attendance*IT`, `LeaveServiceIT`, `attendance.spec.ts` |
| 05-module-cross-cutting | `search/`, `audit/` | `/`, Cmd+K | `SearchServiceIT`, `SpaFallbackIT` |
| 06-nfr-ops | 横切 | — | `RateLimitFilterIT`, `AuditAspectIT`, runbook |

---

## 7. 非功能要求（06-nfr-ops）

对照 `design §9`：

- **安全**：HTTPS、环境变量密钥、BCrypt、JWT 鉴权、登录限流、审计日志、手机号脱敏
- **性能**：学员列表 P95 < 500ms、周课表 P95 < 800ms（人工抽样）
- **可观测**：`/actuator/health`、日志目录、无未处理 ERROR
- **可用性**：备份/恢复脚本可用、Flyway 升级无 ERROR、单 Jar + SPA fallback
- **运维**：默认密码已修改（runbook 要求）

---

## 8. 生产演练（07-production-dry-run）

在与生产相同路径（Docker Compose + `docker/.env`）执行 11 步剧本：部署 → 建校区/账号 → 学员录入 → 排课 → 签到/请假 → 统计/搜索 → 备份恢复 → 模拟升级。详见 `07-production-dry-run.md`。

---

## 9. 签字放行

`sign-off-record.md` 汇总 84 项 PASS/FAIL 计数、各模块签字、投产环境、Git commit hash/tag。全部 PASS 后投产负责人签字方可上线。

---

## 10. 预估工作量

| 活动 | 人力 | 时长 |
|------|------|------|
| 阶段 0 准备 | 2 人 | 0.5h |
| 阶段 1 全局基线 | 1 人 | 1–2h |
| 阶段 2 模块审核（并行） | 2–3 人 | 1 天 |
| 阶段 3 非功能运维 | 1 人 | 0.5 天 |
| 阶段 4 生产演练 | 2 人 | 0.5 天 |
| 修复缓冲 | — | 0.5–1 天 |
| **合计** | | **约 2–3 个工作日** |

---

## 11. 不在本期范围

- 持续 per-PR 审核流程（未来可演进）
- `scripts/release-gate.sh` 自动化门禁脚本
- 独立 QA 用例库 / 测试管理平台
- 第二期功能（作品、支付、招生等）的检查项

---

## 12. 知识缺口（后续可补）

- 无 prior design 针对「投产门禁」；本 spec 为首次定义
- 性能 P95 目前靠人工抽样，无自动化压测基线
- 飞书/钉钉 ERROR 告警为可选项，本期不强制验证
