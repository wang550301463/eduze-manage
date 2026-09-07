# EduZE Manage 第一期 MVP 投产检查实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use socrates:subagent-driven-development (recommended) or socrates:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 `docs/operations/release-gate/` 清单完成 84 项 Blocker 投产审核，开发团队自审 + 交叉 Review，全部 PASS 后在 `sign-off-record.md` 签字放行。

**Architecture:** 五阶段串行门禁（准备 → 基线 → 模块审核 → 横切/NFR → 生产演练 → 签字）。模块 01–04 可并行；缺陷修复后仅重跑失败 RC 及关联 IT/E2E。不编写 gate 自动化脚本，证据以 Markdown 勾选 + 命令输出/截图路径为准。

**Tech Stack:** 现有栈不变（Spring Boot 3.2.5、React 18/Vite 6、MySQL 8、Redis 7、Docker Compose、JUnit+Testcontainers、Vitest、Playwright）

**对应 spec：** `docs/socrates/specs/2026-06-11-release-gate-design.md`  
**可执行清单：** `docs/operations/release-gate/`

---

## 0. 实施总览

### 0.1 阶段地图

| 阶段 | 任务 | 清单文件 | 项数 | 建议人力 | 预估 |
|------|------|----------|------|----------|------|
| **R0** | 投产准备 | `sign-off-record.md` | — | 2 人 | 0.5h |
| **R1** | 全局基线 | `00-baseline.md` | 6 | 1 人 | 1–2h |
| **R2** | 模块审核（可并行） | `01`–`04` | 43 | 2–3 人 | 1 天 |
| **R3** | 横切 + NFR | `05`–`06` | 24 | 1 人 | 0.5 天 |
| **R4** | 生产演练 | `07-production-dry-run.md` | 11 | 2 人 | 0.5 天 |
| **R5** | 签字放行 | `sign-off-record.md` | — | 投产负责人 | 0.5h |
| **缓冲** | 缺陷修复回归 | — | — | — | 0.5–1 天 |

### 0.2 涉及文件（本计划不修改业务代码）

| 文件 | 操作 |
|------|------|
| `docs/operations/release-gate/sign-off-record.md` | 填写分工、汇总、缺陷、签字 |
| `docs/operations/release-gate/00-baseline.md` … `07-production-dry-run.md` | 逐项标 PASS/FAIL、证据 |
| `docker/.env` | 生产演练用（**勿提交仓库**） |
| `docs/evidence/release-gate/`（可选） | 存放截图、curl 输出；可 `.gitignore` |

### 0.3 前置条件

- Docker Desktop 可用（`./mvnw verify` 需 Testcontainers）
- Node 18+、`pnpm` 已装（`cd web && pnpm install`）
- 本地或 CI 可访问 MySQL 8、Redis 7
- 至少 **2 名开发**：一人可兼多模块主审，但**交叉 Review 须换人**

### 0.4 缺陷修复回归矩阵

| FAIL 所在模块 | 最低回归命令 |
|---------------|--------------|
| RC-01-* | `./mvnw test -Dtest=AuthControllerIT,RbacIT,BranchControllerIT` + `cd web && pnpm e2e login.spec.ts settings.spec.ts` |
| RC-02-* | `./mvnw test -Dtest=StudentControllerIT,GuardianIT,StudentImportIT` + `students.spec.ts student-create.spec.ts` |
| RC-03-* | `./mvnw test -Dtest=CourseControllerIT,LessonGenerateIT,ConflictServiceIT,ScheduleServiceIT` + `schedule.spec.ts` |
| RC-04-* | `./mvnw test -Dtest=AttendanceControllerIT,LeaveServiceIT,PickupServiceIT` + `attendance.spec.ts` |
| RC-05-* | `./mvnw test -Dtest=SearchServiceFullIT,SpaFallbackIT` |
| RC-06-* | `./mvnw test -Dtest=RateLimitFilterIT,AuditAspectIT` + 相关 NFR 人工项 |
| RC-07-* | 重跑失败 D-xx 步骤 + 必要时 `./mvnw verify` |

---

## 阶段 R0：投产准备

### Task R0：分工与环境冻结

**Files:**
- Modify: `docs/operations/release-gate/sign-off-record.md`
- Read: `docs/socrates/specs/2026-05-09-eduze-manage-design.md` §4.1（范围冻结）

- [ ] **Step 1: 记录投产元信息**

在 `sign-off-record.md` 顶部填写：

```markdown
**投产环境：** staging / 生产 IP（待定）
**Git 版本：** `git rev-parse HEAD` 输出
**投产负责人：** <姓名>
**计划投产日期：** YYYY-MM-DD
```

执行并粘贴 commit hash：

```bash
cd /path/to/eduze-manage
git rev-parse HEAD
git status --short   # 应为干净或仅含 release-gate 证据目录
```

- [ ] **Step 2: 分配主审与交叉 Review**

在 `sign-off-record.md`「分工」表填写（示例 3 人团队）：

| 模块/阶段 | 主审人 | 交叉 Reviewer |
|-----------|--------|---------------|
| 00 全局基线 | 开发者 A | 开发者 B |
| 01 模块 0 | 开发者 A | 开发者 C |
| 02 学员 | 开发者 B | 开发者 A |
| 03 课程排课 | 开发者 C | 开发者 B |
| 04 签到 | 开发者 B | 开发者 C |
| 05 横切 | 开发者 A | 开发者 C |
| 06 非功能运维 | 开发者 C | 开发者 A |
| 07 生产演练 | 开发者 A + B | 互相复核 |

规则：主审人 ≠ 该模块交叉 Reviewer。

- [ ] **Step 3: 确认依赖服务**

```bash
docker --version
docker compose version
java -version          # 17
cd web && pnpm --version
./mvnw -v
```

- [ ] **Step 4: 安装前端依赖（若未装）**

```bash
cd web && pnpm install
```

Expected: `node_modules` 存在，无 install 错误。

- [ ] **Step 5: 创建证据目录（可选）**

```bash
mkdir -p docs/evidence/release-gate
echo 'docs/evidence/' >> .gitignore   # 若尚未忽略
```

**验收：** `sign-off-record.md` 分工表已填；Git hash 已记录；依赖命令均成功。

---

## 阶段 R1：全局基线（RC-00-001 – RC-00-006）

### Task R1：执行 `00-baseline.md`

**Files:**
- Modify: `docs/operations/release-gate/00-baseline.md`
- Modify: `docs/operations/release-gate/sign-off-record.md`（汇总表 00 行）

- [ ] **Step 1: RC-00-001 后端测试**

```bash
./mvnw verify
```

Expected: `BUILD SUCCESS`；所有 `*IT.java` 通过。  
失败则停止，登记缺陷，不进入后续阶段。

- [ ] **Step 2: RC-00-002 前端单元测试**

```bash
cd web && pnpm test
```

Expected: 全部 test files passed。

- [ ] **Step 3: RC-00-003 E2E 冒烟**

方式 A（推荐）：

```bash
./scripts/e2e-bootstrap.sh
```

方式 B（手动）：

```bash
docker compose -f docker/docker-compose.dev.yml --env-file docker/.env.example up -d
./mvnw spring-boot:run   # 另开终端
cd web && pnpm e2e
```

Expected: 6 specs 全绿：`login`, `students`, `student-create`, `schedule`, `attendance`, `settings`。

- [ ] **Step 4: RC-00-004 生产 Jar**

```bash
./mvnw -DskipTests package
jar tf target/eduze-manage-*.jar | grep -E 'BOOT-INF/classes/static/index.html'
```

Expected: 匹配到 `index.html`。

- [ ] **Step 5: RC-00-005 Flyway 空库**

```bash
# 使用临时库或 docker 新 volume
docker compose -f docker/docker-compose.dev.yml --env-file docker/.env.example down -v
docker compose -f docker/docker-compose.dev.yml --env-file docker/.env.example up -d mysql redis
# 等待 MySQL 就绪后
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
# 日志中搜索 Flyway：无 ERROR
```

Expected: 迁移全部 Success。

- [ ] **Step 6: RC-00-006 范围未越界**

对照 `docs/socrates/specs/2026-05-09-eduze-manage-design.md` §4.1「不做项」表，打开 `web/src/app/shell/nav-config.tsx` 与 `web/src/app/router.tsx`，确认无作品/支付/招生等导航入口。

- [ ] **Step 7: 更新签字表**

`00-baseline.md` 六项标 PASS；`sign-off-record.md` 汇总行「00 全局基线」PASS=6，主审与交叉 Review 签字。

**验收：** RC-00 全部 PASS 后方可开始 R2。

---

## 阶段 R2：模块审核（可并行）

> 四人团队可同时进行 01–04；两人团队建议顺序：01 → 02 → 03 → 04。

### Task R2a：模块 0 账号（`01-module-auth.md`，10 项）

**Files:**
- Modify: `docs/operations/release-gate/01-module-auth.md`
- Test: `src/test/java/com/eduze/manage/auth/AuthControllerIT.java` 等

- [ ] **Step 1: 批量跑模块 IT**

```bash
./mvnw test -Dtest=AuthControllerIT,MeControllerIT,RbacIT,BranchControllerIT,LoginAttemptServiceIT,RateLimitFilterIT,BranchScopeIT,TenantInterceptorIT
```

- [ ] **Step 2: 前端 E2E**

```bash
cd web && pnpm exec playwright test e2e/login.spec.ts e2e/settings.spec.ts
```

- [ ] **Step 3: 逐项人工验证 RC-01-001 – RC-01-010**

按 `01-module-auth.md` 每条执行后端/前端步骤。重点人工项：

| RC | 人工动作 |
|----|----------|
| RC-01-004 | 登录 → 登出 → `curl -H "Authorization: Bearer <old_token>" http://localhost:8080/api/auth/me` 期望 401 |
| RC-01-006 | 非超管账号登录，确认 API 仅返回绑定校区数据 |
| RC-01-008 | 个人中心改密流程 |
| RC-01-010 | 尝试删除 `SUPER_ADMIN` 等内置角色 |

- [ ] **Step 4: 交叉 Review**

Reviewer 独立复现：登录/登出、校区 CRUD、创建受限角色账号并验证 403。

- [ ] **Step 5: 更新 `sign-off-record.md` 01 行**

**验收：** 10/10 PASS。

---

### Task R2b：学员模块（`02-module-student.md`，10 项）

**Files:**
- Modify: `docs/operations/release-gate/02-module-student.md`
- Test: `StudentControllerIT`, `GuardianIT`, `CoursePackageIT`, `StudentImportIT`, `ClassGroupMemberIT`

- [ ] **Step 1: 批量跑模块 IT**

```bash
./mvnw test -Dtest=StudentControllerIT,GuardianIT,CoursePackageIT,StudentImportIT,ClassGroupMemberIT,AuditAspectIT
```

- [ ] **Step 2: 前端 E2E**

```bash
cd web && pnpm exec playwright test e2e/students.spec.ts e2e/student-create.spec.ts
```

- [ ] **Step 3: 逐项人工验证 RC-02-001 – RC-02-010**

重点：

| RC | 人工动作 |
|----|----------|
| RC-02-002 | 创建重复 `enroll_no` 学员，确认错误提示 |
| RC-02-006 | 下载模板，导入 ≤10 行样例 Excel |
| RC-02-007 | 列表页确认手机号 `138****1234` 格式 |
| RC-02-008 | 打开学员详情后查 `SELECT * FROM t_audit_log ORDER BY id DESC LIMIT 5` |
| RC-02-010 | 筛选校区/班级/状态，记录 Network 耗时 |

- [ ] **Step 4: 交叉 Review**

Reviewer 从零创建 1 名学员 + 1 名家长 + 课时包，检查详情 Tab。

- [ ] **Step 5: 更新签字表 02 行**

**验收：** 10/10 PASS。

---

### Task R2c：课程排课（`03-module-course-schedule.md`，13 项）

**Files:**
- Modify: `docs/operations/release-gate/03-module-course-schedule.md`

- [ ] **Step 1: 批量跑模块 IT**

```bash
./mvnw test -Dtest=CourseControllerIT,ClassGroupControllerIT,ClassRoomControllerIT,ClassGroupMemberIT,LessonGenerateIT,LessonRescheduleIT,ConflictServiceIT,ScheduleServiceIT,ScheduleGeneratorIT,MigrationV150IT,CurriculumSeedIT,MigrationV151IT
```

- [ ] **Step 2: 前端 E2E**

```bash
cd web && pnpm exec playwright test e2e/schedule.spec.ts
```

- [ ] **Step 3: 逐项人工验证 RC-03-001 – RC-03-013**

重点：

| RC | 人工动作 |
|----|----------|
| RC-03-005 | `/schedule` 批量生成一周，确认单元格出现 |
| RC-03-006 | 调课一次，查 `t_lesson_change_log` 有记录 |
| RC-03-008 | 故意排冲突时段，确认 `ConflictWarningDialog` 弹出且可继续 |
| RC-03-009 | DevTools Network 抽样周课表 API P95（目标 <800ms） |
| RC-03-010 | `/teachers/availabilities` CRUD |
| RC-03-011 | 老师账号登录 `/workbench`，仅见自己的课 |

- [ ] **Step 4: 交叉 Review**

Reviewer 完整走：建课程 → 建班 → 加学员 → 生成课次 → 调课。

- [ ] **Step 5: 更新签字表 03 行**

**验收：** 13/13 PASS。

---

### Task R2d：签到模块（`04-module-attendance.md`，10 项）

**Files:**
- Modify: `docs/operations/release-gate/04-module-attendance.md`

- [ ] **Step 1: 批量跑模块 IT**

```bash
./mvnw test -Dtest=AttendanceControllerIT,PickupServiceIT,LeaveServiceIT,AbsenceJobIT,AttendanceStatIT
```

- [ ] **Step 2: 前端 E2E**

```bash
cd web && pnpm exec playwright test e2e/attendance.spec.ts
```

- [ ] **Step 3: 逐项人工验证 RC-04-001 – RC-04-010**

重点：

| RC | 人工动作 |
|----|----------|
| RC-04-002/003 | 工作台手动入园、离园 |
| RC-04-004 | HTTPS 或 localhost 下扫码（无设备则记录「模拟 token」证据） |
| RC-04-005 | 入园/离园选择接送人 |
| RC-04-007 | 前台提请假 → 班主任 `/attendance/leaves` 审批 |
| RC-04-009 | `/attendance/stats` 与校长 Dashboard KPI |

- [ ] **Step 4: 交叉 Review**

Reviewer 以「前台 + 班主任」两个账号走完签到与请假链。

- [ ] **Step 5: 更新签字表 04 行**

**验收：** 10/10 PASS。

---

## 阶段 R3：横切与非功能（24 项）

### Task R3a：横切体验（`05-module-cross-cutting.md`，8 项）

- [ ] **Step 1: IT**

```bash
./mvnw test -Dtest=SearchServiceFullIT,SearchServiceIT,SpaFallbackIT
```

- [ ] **Step 2: 人工走查 RC-05-001 – RC-05-008**

| RC | 动作 |
|----|------|
| RC-05-001 | Cmd+K 搜学员名 → 跳转详情 |
| RC-05-002 | 浏览器刷新 `/students` 不 404 |
| RC-05-005 | `?` 打开快捷键帮助；`Esc` 关 Dialog |
| RC-05-006 | DevTools 375px 宽度看汉堡菜单 |
| RC-05-007 | Accessibility 面板抽查 `aria-label` |

- [ ] **Step 3: 交叉 Review + 签字表 05 行**

---

### Task R3b：非功能运维（`06-nfr-ops.md`，16 项）

**Files:**
- Modify: `docs/operations/release-gate/06-nfr-ops.md`
- Read: `docs/operations/runbook.md`, `docker/.env.example`

- [ ] **Step 1: 安全项 RC-06-001 – RC-06-007**

```bash
# RC-06-005
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/students
# Expected: 401

./mvnw test -Dtest=RateLimitFilterIT,AuditAspectIT
```

生产 HTTPS（RC-06-001）在 R4 演练时一并验证；若仅本地 dev，先标「待 R4 确认」不得最终签字。

- [ ] **Step 2: 性能 RC-06-008 – RC-06-009**

在接近真实数据量环境（或演练数据）：

```bash
# 示例：10 次请求计时（替换 TOKEN）
for i in $(seq 1 10); do
  curl -s -o /dev/null -w "%{time_total}\n" \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:8080/api/students?page=1&size=20"
done
```

记录 P95 填入 `06-nfr-ops.md` 备注。学员列表 <500ms，课表 <800ms。

- [ ] **Step 3: 运维 RC-06-010 – RC-06-016**

```bash
curl -s http://localhost:8080/actuator/health | jq .
DB_PASSWORD='***' ./scripts/backup-mysql.sh
# 记录备份文件路径
```

RC-06-013 恢复、RC-06-014 升级在 R4 执行。

- [ ] **Step 4: 交叉 Review + 签字表 06 行**

**验收：** 05 八项 + 06 十六项全 PASS（06 中与 R4 重叠项可在 R4 后补勾，但签字前必须全绿）。

---

## 阶段 R4：生产演练（RC-07 / D-01–D-11）

### Task R4：Docker 生产路径全流程

**Files:**
- Modify: `docs/operations/release-gate/07-production-dry-run.md`
- Create: `docker/.env`（从 example 复制，**不提交**）

- [ ] **Step 1: 准备生产配置**

```bash
cp docker/.env.example docker/.env
# 编辑：DB_PASSWORD、JWT_SECRET（≥32 字符，非 change-me）
# 配置 docker/certs/ 见 docker/certs/README.md
./mvnw -DskipTests package
docker compose -f docker/docker-compose.yml --env-file docker/.env up -d
```

- [ ] **Step 2: 执行 D-01 – D-11**

严格按 `07-production-dry-run.md` 每项操作与验收。建议两人分工：

| 角色 | 步骤 |
|------|------|
| 操作员 A | D-01–D-05 基建与排课 |
| 操作员 B | D-06–D-09 签到/请假/统计/搜索 |
| 共同 | D-10 备份恢复、D-11 升级 |

- [ ] **Step 3: 补齐 RC-06 生产项**

在同一环境确认：RC-06-001 HTTPS、RC-06-002 密钥、RC-06-013 恢复、RC-06-014 升级、RC-06-016 默认密码已改。

- [ ] **Step 4: 交叉 Review**

Reviewer 用**未参与操作的浏览器/账号**复现 D-04–D-09 至少各 1 次。

- [ ] **Step 5: 更新签字表 07 行（11/11 PASS）**

**验收：** 生产路径可走通，备份恢复后数据完整。

---

## 阶段 R5：签字放行

### Task R5：最终门禁

**Files:**
- Modify: `docs/operations/release-gate/sign-off-record.md`

- [ ] **Step 1: 汇总核对**

```bash
# 人工统计各 md 文件中 PASS 数量，合计必须为 84
grep -r "PASS" docs/operations/release-gate/0*.md | wc -l   # 参考，以人工勾选为准
```

核对 `sign-off-record.md` 汇总表：

| 阶段 | 项数 | 必须 PASS |
|------|------|-----------|
| 00 | 6 | 6 |
| 01 | 10 | 10 |
| 02 | 10 | 10 |
| 03 | 13 | 13 |
| 04 | 10 | 10 |
| 05 | 8 | 8 |
| 06 | 16 | 16 |
| 07 | 11 | 11 |
| **合计** | **84** | **84** |

- [ ] **Step 2: 缺陷表清零**

`sign-off-record.md` 缺陷追踪表无 OPEN 状态。

- [ ] **Step 3: 投产负责人签字**

勾选放行四项 checkbox 并签字、日期、环境、Git 版本。

- [ ] **Step 4: 归档证据（可选）**

将 `sign-off-record.md` 导出 PDF 或提交至内部 Wiki；截图存 `docs/evidence/release-gate/`（可不进 Git）。

**验收：** 投产负责人签字完成；机构可执行 `runbook.md` 首次部署。

---

## 5. Spec 覆盖度自检

| Spec 章节 | 覆盖任务 |
|-----------|----------|
| §3 五阶段流程 | R0–R5 |
| §4 文档结构 10 文件 | 各 Task 对应 md |
| §5 检查项格式 | 清单内已落地 |
| §6 模块映射 | R2a–R2d |
| §7 NFR §9 | R3b、R4 Step 3 |
| §8 生产演练 | R4 |
| §9 签字放行 | R5 |
| §10 工作量 | §0.1 表 |
| §11 不在范围（无 gate 脚本） | 本计划未引入脚本 |

## 6. 风险与缓解

| 风险 | 缓解 |
|------|------|
| E2E 环境不一致导致 flaky | 优先 `e2e-bootstrap.sh`；失败重跑 1 次，仍失败记 FAIL |
| 两人团队无法并行 | 顺序 01→04，工期 +1 天 |
| 扫码 RC-04-004 无硬件 | localhost 模拟 token + 注明证据；生产 HTTPS 下再验一次 |
| 性能 P95 超标 | 记 FAIL，优化索引/SQL 后重测 RC-06-008/009 |
| `docker/.env` 误提交 | `.gitignore` 已含；签字前 `git status` 确认 |

---

## 7. 完成定义（Definition of Done）

- [ ] `sign-off-record.md` 合计 84 PASS，0 FAIL
- [ ] 缺陷表无 OPEN 项
- [ ] 投产负责人已签字
- [ ] Git 版本与投产环境已记录
- [ ] `runbook.md` 首次部署步骤可由另一人仅凭文档复现
