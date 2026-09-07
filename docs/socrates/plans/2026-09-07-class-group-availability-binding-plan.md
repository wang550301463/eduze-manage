# 分组绑定可用时段 + 课时审计 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use socrates:subagent-driven-development (recommended) or socrates:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将分组标签与老师可用时段 1:1 绑定，进组/调班与订阅、主带强制对齐；正常批量排课仅处理已绑分组时段；特殊课硬冲突；并落地学员历史课时审计流水。

**Architecture:** 在现有 teacher-centric 模型上增量：`t_class_group.teacher_availability_id`（UK）为绑定点；成员变更与 `LessonSubscription` / mentor 同事务；`ScheduleGenerator` 过滤未绑定时段；特殊 `Lesson` 不走 availability；`t_student_lesson_hour_ledger` 只追加审计。

**Tech Stack:** Java 17 + Spring Boot 3.2 + MyBatis-Plus + Flyway + JUnit/Testcontainers；React 18 + Vite + TanStack Query + Tailwind。

**对应 spec：** `docs/socrates/specs/2026-09-07-class-group-availability-binding-design.md`

---

## 0. 文件结构（锁定边界）

```
src/main/resources/db/migration/
├─ V1.6.0__class_group_availability_bind.sql      (新)
├─ V1.6.1__student_lesson_hour_ledger.sql         (新)
└─ V9.4.0__class_group_bind_permissions.sql       (新: student:hour_adjust 等)

src/main/java/com/eduze/manage/
├─ course/
│  ├─ domain/ClassGroup.java                      (改: +teacherAvailabilityId)
│  ├─ dto/ClassGroupRequest.java / Response.java  (改)
│  ├─ service/ClassGroupService.java              (改: 绑定/新建时段/镜像 capacity)
│  └─ service/ClassGroupMemberService.java        (改: 进组/出组/调班 → 订阅+主带)
├─ teacher/
│  └─ service/TeacherAvailabilityService.java     (改: 删/停用守卫; 未绑定列表 API)
├─ lesson/
│  ├─ service/ScheduleGenerator.java              (改: 仅已绑分组时段)
│  ├─ service/ConflictService.java                (改: 特殊课 vs 正常窗口硬冲突)
│  ├─ service/LessonService.java + dto/LessonRequest.java (改: source 可选, classGroupId 可空)
│  └─ domain + service 课时 ledger（新包或放 student）
└─ student/ 或 ledger/
   ├─ domain/StudentLessonHourLedger.java
   ├─ mapper/...
   ├─ service/StudentLessonHourLedgerService.java
   └─ controller 挂在 StudentController 或独立

web/src/features/
├─ course/pages/ClassGroupListPage.tsx + ClassGroupFormDialog.tsx
├─ course/api.ts / types.ts
├─ student/components/TransferClassDialog.tsx     (展示时段; 已有基础可增强)
├─ teacher/pages/TeacherAvailabilityPage.tsx      (已绑定标识)
└─ student/… LessonHourLedgerTab（新）
```

---

## 1. 任务列表

### Task 1: Flyway — 分组绑定时段列 + UK

**Files:**
- Create: `src/main/resources/db/migration/V1.6.0__class_group_availability_bind.sql`
- Test: 启动 IT 或 `flyway` 迁移冒烟（`ClassGroupControllerIT` 仍可通过）

- [ ] **Step 1: 写迁移 SQL**

```sql
ALTER TABLE t_class_group
  ADD COLUMN teacher_availability_id BIGINT NULL COMMENT '绑定的老师可用时段' AFTER head_teacher_id;

-- MySQL 允许多行 NULL；非空值唯一。用生成列或条件唯一：
-- 方案：唯一索引包含 deleted_at，NULL availability 不冲突（InnoDB 多 NULL 允许多行）
CREATE UNIQUE INDEX uk_class_group_availability
  ON t_class_group (tenant_id, teacher_availability_id, deleted_at);
```

若当前表无 `deleted_at` 列名与 BaseEntity 不一致，先对照 `V1.2.0__init_course.sql` / `BaseEntity` 实际列名再写（必须与现网一致）。

- [ ] **Step 2: 本地/IT 跑迁移**

Run: `./mvnw -Dtest=ClassGroupControllerIT test`（或项目惯用 IT 配置）  
Expected: 迁移成功；旧 IT 仍绿（availability 可空）。

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V1.6.0__class_group_availability_bind.sql
git commit -m "db: add class_group.teacher_availability_id unique bind"
```

---

### Task 2: Flyway — 课时审计表 + 权限

**Files:**
- Create: `src/main/resources/db/migration/V1.6.1__student_lesson_hour_ledger.sql`
- Create: `src/main/resources/db/migration/V9.4.0__class_group_bind_permissions.sql`

- [ ] **Step 1: ledger 表**

```sql
CREATE TABLE t_student_lesson_hour_ledger (
  id            BIGINT PRIMARY KEY,
  tenant_id     BIGINT NOT NULL,
  branch_id     BIGINT NOT NULL,
  student_id    BIGINT NOT NULL,
  lesson_id     BIGINT NULL,
  lesson_student_id BIGINT NULL,
  package_id    BIGINT NULL,
  event_type    VARCHAR(32) NOT NULL,
  minutes_delta INT NOT NULL,
  balance_after_minutes INT NULL,
  occurred_at   DATETIME(3) NOT NULL,
  operator_id   BIGINT NULL,
  note          VARCHAR(512) NULL,
  related_ledger_id BIGINT NULL,
  created_at    DATETIME(3) NOT NULL,
  updated_at    DATETIME(3) NOT NULL,
  deleted_at    DATETIME(3) NULL,
  INDEX idx_ledger_student (tenant_id, student_id, occurred_at),
  INDEX idx_ledger_lesson (tenant_id, lesson_id)
);
```

列集必须与项目 `BaseEntity` / 雪花 ID 策略对齐（对照邻近业务表 DDL 复制惯用列）。

- [ ] **Step 2: 权限 seed**

插入 `student:hour_adjust`；赋予 SUPER_ADMIN / 校长角色（对照 `V9.1.1__add_teacher_centric_permissions.sql` 写法）。

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V1.6.1__student_lesson_hour_ledger.sql \
        src/main/resources/db/migration/V9.4.0__class_group_bind_permissions.sql
git commit -m "db: student lesson hour ledger and adjust permission"
```

---

### Task 3: 后端 — ClassGroup 绑定 / 现场建时段

**Files:**
- Modify: `ClassGroup.java`, `ClassGroupRequest.java`, `ClassGroupResponse.java`, `ClassGroupService.java`
- Modify: `TeacherAvailabilityService`（复用 create）或在 ClassGroupService 注入 Mapper
- Test: `ClassGroupControllerIT` 增补绑定用例

- [ ] **Step 1: 写失败 IT（绑定同一 availability 两次 → 409）**

在 `ClassGroupControllerIT` 增加：创建 avail → 创建 groupA 绑定 → 再创建 groupB 同 avail → expect CONFLICT。

- [ ] **Step 2: 跑测确认失败**

Run: `./mvnw -Dtest=ClassGroupControllerIT#create_duplicateAvailabilityBind_conflict test`  
Expected: FAIL（尚无字段/校验）

- [ ] **Step 3: 实现最小逻辑**

`ClassGroupRequest` 增加：

```java
/** 新建必填（绑定已有时段）。与 nestedAvailability 二选一。 */
private Long teacherAvailabilityId;
/** 现场新建时段载荷；非空时服务端先建 availability 再绑定。 */
private TeacherAvailabilityRequest nestedAvailability;
```

`ClassGroupService.create`:

1. 若 `nestedAvailability != null` → 创建 availability（老师/校区/时段），得到 id。  
2. 否则要求 `teacherAvailabilityId != null`（新建）。  
3. 加载 availability：校验校区、status=1、未被其他组绑定。  
4. `headTeacherId = avail.teacherId`；`capacity = avail.capacity`（忽略请求里冲突的 capacity 或强制覆盖）。  
5. 写入 `teacherAvailabilityId`。  
6. Response 带出时段摘要：`dayOfWeek/startMinute/endMinute/teacherName`。

更新同理；换绑走 Task 7 规则时可先只允许创建路径。

- [ ] **Step 4: IT 通过后 Commit**

```bash
git commit -m "feat: bind class group to teacher availability on create"
```

---

### Task 4: ScheduleGenerator 仅处理已绑分组时段（D2）

**Files:**
- Modify: `src/main/java/com/eduze/manage/lesson/service/ScheduleGenerator.java`
- Test: 新增或扩展 generator IT（若无则 `ScheduleGeneratorIT`）

- [ ] **Step 1: 失败测试** — 未绑分组的 avail 生成 weeks=1 → generated=0（或 skip）；已绑定则生成。

- [ ] **Step 2: 实现** — 加载 avail 后 join / 子查询 `t_class_group` where `teacher_availability_id = avail.id` and deleted 过滤；无绑定则 `continue`。

- [ ] **Step 3: Commit**

```bash
git commit -m "fix: bulk-generate only availabilities bound to a class group"
```

---

### Task 5: 进组 / 出组 / 调班 ↔ 订阅 + 主带（D1/D5）

**Files:**
- Modify: `ClassGroupMemberService.java`（`addMembers` / `removeMember` / `transfer`）
- Modify: 注入 `LessonSubscriptionMapper`、`StudentMentorService`、`StudentMapper`
- Test: `ClassGroupMemberIT` 扩展

- [ ] **Step 1: IT** — addMember 后存在 status=1 订阅指向该组 availability；removeMember 后订阅 status=0 且 leftAt 有值；transfer 旧订阅停、新订阅开；主带变为新组老师。

- [ ] **Step 2: 实现要点（同事务）**

`addMembers` 在 insert membership 后：

```text
require group.teacherAvailabilityId != null
avail = load(availId)
if student.mentorTeacherId != avail.teacherId:
  mentorService.change(..., reason="进组同步主带")
if no active sub for (student, availId):
  insert LessonSubscription NORMAL status=1 validFrom=today
```

`removeMember`：结束 membership + 停用该 avail 上有效订阅。

`transfer`：对每个学生 remove 旧逻辑 + add 新逻辑（不要只调 addMembers 导致双开订阅）。

容量校验：有效订阅数（或组员数，二者应一致）< capacity。

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: sync subscription and mentor on class group membership changes"
```

---

### Task 6: 特殊课 source + 硬冲突（D3）

**Files:**
- Modify: `LessonRequest.java` — `classGroupId` 改为可选；增加 `Integer source`（或枚举）
- Modify: `LessonService.create`
- Modify: `ConflictService`
- Test: Lesson IT

约定 source（与现有 tinyint 对齐，文档写死）：

| 值 | 含义 |
|----|------|
| 1 | 模板生成（仅 generator） |
| 2 | 手动正常（若仍保留） |
| 3 | 补课 MAKEUP |
| 4 | 试听 TRIAL（已有语义可复用） |
| 5 | 考级 EXAM |
| 6 | 比赛 CONTEST |

- [ ] **Step 1: IT** — 创建 source=5 的课与某已绑分组时段同老师同窗口重叠 → 400/409；不重叠 → 201。特殊课 `teacherAvailabilityId` 必须 null。

- [ ] **Step 2: 实现** — `ConflictService` 增加：加载所有已绑分组 availability，按 dayOfWeek+分钟展开到 `startAt` 当天窗口，与草稿老师时间区间相交则硬失败。

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: special lessons with hard conflict against bound group slots"
```

---

### Task 7: 时段停用/删除守卫 + 换绑（D4）

**Files:**
- Modify: `TeacherAvailabilityService` delete/update
- Modify: `ClassGroupService` rebind 方法或 update
- Test: Availability IT + ClassGroup IT

- [ ] **Step 1: IT** — 存在未来 `Lesson`（startAt>now, teacherAvailabilityId=avail）时 DELETE → 失败；无未来课且无绑定组可删/停用。

- [ ] **Step 2: 换绑** — `PUT` 分组换 `teacherAvailabilityId`：目标未绑定；批量更新组内有效订阅的 avail+teacher；对齐主带；镜像 capacity/headTeacher；**不**自动删旧未来课；API 响应 message 提示重新 bulk-generate。

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: guard availability delete and support group slot rebind"
```

---

### Task 8: 课时审计 ledger 服务 + 出勤挂钩（D6）

**Files:**
- Create: domain/mapper/service for `StudentLessonHourLedger`
- Modify: 出勤确认扣课时路径（定位现有 package 扣减代码；若尚无扣减则在「确认出勤」处插入 ledger 最小写入，余额字段可读课时包 remaining）
- Create: `GET /api/students/{id}/lesson-hour-ledger`；`POST .../lesson-hour-ledger/adjust`（`@PreAuthorize student:hour_adjust`）
- Test: Service IT — 写入后不可 update；VOID 新行

- [ ] **Step 1: 失败测试** — adjust 后 list 两条（ADJUST）；尝试 update 历史行的 mapper 不暴露 update API。

- [ ] **Step 2: 实现只 INSERT 的 service；出勤成功路径调用 `append(ATTEND, -lessonMinutes, ...)`。

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: append-only student lesson hour ledger"
```

---

### Task 9: 前端 — 分组表单 / 列表绑定时段

**Files:**
- Modify: `web/src/features/course/types.ts` — `teacherAvailabilityId`, 时段展示字段
- Modify: `web/src/features/course/api.ts`
- Modify: `ClassGroupFormDialog.tsx` — 模式：选已有 / 现场新建（老师+周几+时间）
- Modify: `ClassGroupListPage.tsx` — 列「时段」「老师」；更新 banner
- Add: API `listUnboundAvailabilities(branchId)`（后端 Task 3/7 暴露的 GET）

- [ ] **Step 1: 类型与 API**
- [ ] **Step 2: 表单 UI**（容量只读，来自所选时段）
- [ ] **Step 3: `npm run build` 通过
- [ ] **Step 4: Commit**

```bash
git commit -m "feat(web): bind availability when creating class groups"
```

---

### Task 10: 前端 — 调班展示时段 + 可用时段绑定态 + 特殊课入口

**Files:**
- Modify: `TransferClassDialog.tsx`（worktree 或 main：选项 label 含周几时段）
- Modify: `TeacherAvailabilityPage.tsx` — 列「分组」；已绑定禁用删除或提示
- Modify: 周课表手工建课 Dialog — source 选补课/考级/比赛

- [ ] **Step 1–3: 实现 + build**
- [ ] **Step 4: Commit**

```bash
git commit -m "feat(web): show bound slots on transfer and special lesson sources"
```

---

### Task 11: 前端 — 学员课时流水 Tab（只读）

**Files:**
- Create: `StudentLessonHourLedgerTab.tsx`
- Modify: `StudentDetailSheet.tsx` 增加 Tab「课时流水」
- Modify: `student/api.ts` list ledger

- [ ] **Step 1: 列表只读**
- [ ] **Step 2: build**
- [ ] **Step 3: Commit**

```bash
git commit -m "feat(web): student lesson hour ledger tab"
```

---

### Task 12: 联调验收 + 文档勾选

**Files:**
- Modify: spec §9 验收复选框（或 release note）
- Ops: 按 `docker` 既有流程构建部署（若用户环境需要）

- [ ] **Step 1: 按 spec §9 手工/API 验收清单逐条打勾**
- [ ] **Step 2: 修复发现的缺口（小补丁单独 commit）**
- [ ] **Step 3: Commit** docs 勾选状态

```bash
git commit -m "docs: mark class-group availability binding acceptance"
```

---

## 2. Spec 覆盖自检

| Spec 项 | Task |
|---------|------|
| D0 FK + UK | T1, T3 |
| D1 订阅↔组籍 | T5 |
| D2 未绑定不排课 | T4 |
| D3 特殊课硬冲突 | T6 |
| D4 删/换绑 | T7 |
| D5 主带=班主任=时段老师 | T3, T5 |
| D6 ledger | T2, T8, T11 |
| D7 选已有/现场建 | T3, T9 |
| D8 capacity 镜像 | T3, T7 |
| UI 列表/表单/调班/流水 | T9–T11 |
| 验收 | T12 |

## 3. Placeholder 扫描

无 TBD；source 枚举值已在 T6 表内写死。若与库中已有 source 含义冲突，实现时**以本表为准并在 PR 说明映射**，不得留空。

## 4. 类型一致性

- 后端 ID：Long；JSON 字符串（既有 Jackson 配置）。
- 前端：`teacherAvailabilityId: string`。
- 容量：number；镜像自 availability。

---

## 5. 执行说明

- 建议分支：`feature/class-group-availability-bind`（从最新 `main` 拉 worktree）。
- 与 Mac Tahoe UI 分支并行时：T9–T11 可在 Tahoe 分支上拣选合并，或先合 UI 再开本功能分支。
- >6 tasks：Agent-Driven 时用 batch-parallel（T1+T2 可并行；T3 依赖 T1；T4/T5/T6 在 T3 后可并行等）。
