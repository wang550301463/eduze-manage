# EduZE Manage —— 分组标签绑定可用时段与课时审计设计

> 文档路径：`docs/socrates/specs/2026-09-07-class-group-availability-binding-design.md`

---

## 0. 文档元信息

| 字段 | 值 |
|---|---|
| 文档版本 | v1.0 |
| 创建日期 | 2026-09-07 |
| 状态 | Accepted（2026-09-07 本地 18443 验收通过） |
| 文档类型 | 增量设计（Spec Patch） |
| 依据文档 | `docs/socrates/specs/2026-05-22-teacher-centric-scheduling-design.md` |
| 影响范围 | 分组标签（ClassGroup）、老师可用时段、订阅、排课生成、特殊课、学员主带、课时审计 |
| 后续文档 | 实施计划由 writing-plans 阶段产出 |

> 本文档在「老师为核心排课」已落地的前提下，把**分组标签**与**可用时段**重新闭环，并补充**学员历史课时审计流水**。

---

## 1. 背景与问题

### 1.1 现状

- 排课主轴已是：`TeacherAvailability` → 批量生成 `Lesson` + `LessonSubscription` → `LessonStudent`。
- `ClassGroup`（分组标签）仅有名称 / 课程 / 班主任 / 容量 / 标签色，**无时段字段**，与 availability **无外键**。
- 产品侧小学生场景：分组 = 固定班次，时段基本不变；「一个老师一个时间段只能一个分组」；补课 / 考级 / 比赛只能在正常课之外。
- 闭环缺口（已分析）：花名册与订阅双真相、未绑定时段仍可排正常课、特殊课无硬规则、换绑/停用未定义、上课老师与主带可能不一致、缺课时审计流水。

### 1.2 本次目标

1. 分组与老师可用时段 **1:1 绑定**。
2. 进组 / 调班与订阅、主带强制一致。
3. 正常批量排课仅针对已绑分组的时段。
4. 特殊课硬冲突隔离。
5. 学员历史课时**不可变审计流水**。

---

## 2. 关键决策（与用户共识）

| # | 决策 |
|---|---|
| D0 | 方案 A：在 `t_class_group` 上挂 `teacher_availability_id`（唯一） |
| D1 | 正常课名单以**有效订阅**为准；**取消订阅 = 出组**；**进组 = 默认开通订阅** |
| D2 | **未绑分组**的可用时段**不得**参与正常批量排课 |
| D3 | 特殊课（补课 / 考级 / 比赛）= 独立 `Lesson` + 专用 `source` + 与已占用正常窗口**硬冲突** |
| D4 | 换绑 / 停用：组内订阅批量迁移或先清学员；有未来课的时段禁止硬删 |
| D5 | 分组绑定时段的老师 **强制 = 班主任（headTeacher）= 主带（mentor）** |
| D6 | 新增学员历史课时记录表，只追加、纠错走冲正行；**课时包余额为余额真相，本表为审计流水** |
| D7 | 建分组：支持**选用已有时段**或**现场新建时段** |
| D8 | 容量以 **availability.capacity** 为准；分组 `capacity` 为只读镜像（创建 / 换绑时同步） |

---

## 3. 数据模型

### 3.1 `t_class_group` 增量

| 字段 | 说明 |
|---|---|
| `teacher_availability_id` | `BIGINT NULL`（迁移期旧数据可空；**新建分组必填**） |

约束：

- `uk_class_group_availability (tenant_id, teacher_availability_id, deleted_at)` — 一个可用时段最多一个分组。
- 应用层：绑定的 `TeacherAvailability.teacherId` 必须等于 `headTeacherId`。

### 3.2 既有表（逻辑约束，可不改列）

| 表 | 约束 / 行为 |
|---|---|
| `t_teacher_availability` | 未绑分组 → 不参与 `ScheduleGenerator` 正常生成（D2） |
| `t_lesson_subscription` | 进组写 `status=1`；取消时 `status=0` + `valid_to=today`，并出组（D1） |
| `t_lesson` | 正常课：`source=模板生成`，带 `teacher_availability_id`；特殊课：`teacher_availability_id=null`，`source∈{补课,考级,比赛}`（枚举值实现时落到现有 tinyint / 扩展约定） |
| `t_student` / mentor | 进组 / 调班时若主带 ≠ 时段老师 → **自动改主带并写 `t_student_mentor_history`**（D5） |

### 3.3 新表：`t_student_lesson_hour_ledger`（学员历史课时审计）

| 字段 | 类型 / 说明 |
|---|---|
| 继承 `BaseEntity` | `id`, `tenant_id`, `branch_id`, 审计列，软删按项目惯例（审计表**建议禁止业务软删**，仅系统保留） |
| `student_id` | NOT NULL |
| `lesson_id` | NULL（纯调账可空） |
| `lesson_student_id` | NULL |
| `package_id` | NULL（关联课时包，若有） |
| `event_type` | `ATTEND` / `ABSENT_DEDUCT` / `MAKEUP` / `ADJUST` / `VOID` |
| `minutes_delta` | 有符号整数（分钟；正=增加可用课时，负=消耗） |
| `balance_after_minutes` | 变更后课时包剩余分钟（写流水时快照） |
| `occurred_at` | 业务发生时间 |
| `operator_id` | 操作人 |
| `note` | 备注 |
| `related_ledger_id` | NULL；`VOID` 时指向被冲正行 |

索引：`(tenant_id, student_id, occurred_at DESC)`；`(tenant_id, lesson_id)`。

**原则：** 只 INSERT；纠错新增 `VOID` / `ADJUST` 行，不 UPDATE 历史业务字段。

**最小写入时机：**

- 出勤确认导致扣课时
- 补课入账
- 管理员手工调账
- 作废冲正

---

## 4. 不变量

1. 同一 `teacher_availability_id` 最多绑定一个未删除分组。
2. 分组 `headTeacherId` == 绑定时段 `teacherId` == 组内学员有效上课关系下的主带（进组时强制对齐）。
3. 组内有效成员集合 ≡ 对该分组绑定时段的有效订阅集合（D1 同步）。
4. `ScheduleGenerator` 仅处理：`status=1` 且已被某分组绑定的 availability。
5. 特殊课不得与「已绑分组时段」在老师维度上的展开窗口重叠（硬拒绝）；画室冲突沿用现有冲突策略（硬或软与现网一致，本期对老师维度至少硬拒绝）。
6. 存在未来未开始的模板课次时，禁止硬删该 availability；停用 / 换绑走规定流程（D4）。
7. 分组 `capacity` 始终等于绑定时段 `capacity`（镜像）。

---

## 5. 流程

### 5.1 创建分组

1. 选择校区 + 名称等基础字段。
2. **模式 A**：下拉选择「本校区、启用中、尚未被其他分组绑定」的 availability。  
   **模式 B**：选择老师 + 周几 + 起止时间 + 容量等 → 创建 `TeacherAvailability` → 再绑定。
3. `headTeacherId := availability.teacherId`；`capacity := availability.capacity`。
4. 写入 `teacher_availability_id`（UK 冲突则失败）。

### 5.2 进组

同事务：

1. 校验容量（按**有效订阅数** < capacity）。
2. 若学员主带 ≠ 时段老师 → 改主带 + mentor history（原因如「进组同步」）。
3. `StudentClassGroup` 插入（或恢复）。
4. 若无对该 `teacher_availability_id` 的有效订阅 → 创建订阅 `source=NORMAL`。

### 5.3 调班

同事务：结束旧组籍 + 旧订阅（`valid_to` / status=0）；对新组执行 §5.2。须同校区（沿用现有 transfer 校验）。

### 5.4 取消订阅 / 出组

停用订阅的同时结束组籍（D1）。UI 上「取消订阅」与「移出分组」合并为同一操作或互相级联。

### 5.5 批量生成课次

过滤条件增加：availability 存在绑定分组。其余沿用 `ScheduleGenerator`（订阅入名单）。

### 5.6 特殊课

- 入口：周课表 / 课次手工新建；类型三选一：补课 / 考级 / 比赛。
- `teacher_availability_id = null`；可不挂 `class_group_id`。
- 提交前：与所有「已绑分组的 availability」展开的周窗口（及已有 Lesson）做老师时间硬冲突；通过才创建。
- 名单：手工加减；不走正常订阅自动入列（除非产品后续另定试听规则，本期不做）。

### 5.7 换绑时段（D4）

1. 目标时段未绑定其他分组；目标老师仍满足 D5（即成为新班主任 / 主带对齐基准）。
2. 批量更新组内有效订阅的 `teacher_availability_id`（及 `teacher_id`）。
3. 更新分组 FK 与 `headTeacherId` / capacity 镜像；组内学员主带若不一致则批量对齐并写历史。
4. 旧时段：若无其他引用可停用，不可硬删若仍有未来课。
5. UI 提示：需按需**重新批量生成**未来课次（本期不自动删除旧未来课，避免误伤；实现计划可增「可选清理未开始模板课」开关）。

### 5.8 停用 / 删除时段

- 有未来未开始模板课 → 禁止硬删；可先停用（status=0）并停止后续生成。
- 仍有绑定分组 → 必须先换绑或解散分组（清学员）。

---

## 6. API 摘要

| 能力 | 建议 |
|---|---|
| 分组 CRUD | Request/Response 增加 `teacherAvailabilityId`；创建支持嵌套 `availability` 新建载荷 |
| 可选时段列表 | `GET` 本校区未绑定、启用中的 availabilities（供下拉） |
| 进组 / 调班 | 扩展现有 `/api/students/bulk/assign-class`、`/transfer-class`：内聚订阅 + 主带同步 |
| 出组 | 成员移除 API 级联停订阅 |
| 排课生成 | 服务端过滤未绑定时段（即使请求带了老师 id） |
| 特殊建课 | Lesson 创建增加 source 枚举校验 + 硬冲突 |
| 课时流水 | `GET /api/students/{id}/lesson-hour-ledger`；管理员 `POST` 调账（二期可做 UI） |

权限：沿用 `classgroup:*`、`teacher:availability:*`、`lesson:*`；调账需独立权限如 `student:hour_adjust`（实施时落库）。

---

## 7. UI 摘要

| 页面 | 变更 |
|---|---|
| 分组标签列表 | 列：时段（周几+时间）、老师；banner 更新为「分组绑定老师固定时段；排课仍由周课表生成」 |
| 新建 / 编辑分组 | 绑定时段：选择已有 / 现场新建；容量只读展示 |
| 学员详情「调班」 | 已接通的调班对话框展示目标组时段；成功后订阅与主带一致 |
| 老师可用时段 | 展示是否已绑分组；已绑定的删除受限提示 |
| 周课表 | 特殊课入口；生成仅含已绑分组时段 |
| 学员详情 | 课时流水 Tab（只读列表；调账可二期） |

---

## 8. 迁移

1. DDL：`teacher_availability_id` + UK；建 `t_student_lesson_hour_ledger`。
2. 旧分组：允许 `teacher_availability_id IS NULL`（只读告警：「待补绑」）；**新建必填**。
3. 数据修复脚本（可选运维）：按「班主任 + 唯一匹配时段」尝试自动绑定；失败留人工。
4. 不自动改写历史课次；从上线后新生成 / 新出勤开始写 ledger。

---

## 9. 验收标准

- [x] 同一 availability 无法绑两个分组（UK / API 冲突）。
- [x] 新建分组可选已有时段或现场建时段，列表显示时段与老师。
- [x] 进组后出现有效订阅；取消订阅后不在组内且不再进入新生成课次名单。
- [x] 调班后旧订阅结束、新订阅生效，主带 = 新组老师。（进组同步主带已验；调班走同一成员服务）
- [x] 未绑分组的时段不出现在正常批量生成结果中。（ScheduleGeneratorIT + 服务端过滤）
- [x] 特殊课与正常占用窗口老师时间重叠时创建失败。
- [x] 有未来课时段不可硬删 / 已绑定分组不可删。
- [x] 课时流水可追加（adjust API）；只追加不改历史行。
- [x] 分组 capacity 与绑定时段 capacity 一致。

---

## 10. 非目标（本期不做）

- 课时费结算、缴费 / 退费完整账务
- 特殊课运营闭环（报名缴费、证书）
- 自动清理换绑后的全部未来课（仅提示重生成；可选开关留给计划）
- 与 Mac Tahoe UI 视觉无关的大改版

---

## 11. 与既有文档关系

- 覆盖 / 细化 `2026-05-22-teacher-centric-scheduling-design.md` 中 K6「分组仅为命名」：本期升为**绑定固定时段的班次容器**，排课生成主轴仍是 availability。
- 不回退到「按班级周模板排课」旧模型；`class_group_id` 在模板生成课次上仍可为 null，花名册关系走成员表 + 订阅。

---

## 12. Spec 自检

- [x] 无 TBD / TODO 占位决策（实现细节留 plan）
- [x] D1–D8 与流程、验收一致
- [x] 特殊课与正常课边界已写硬冲突
- [x] 审计表与课时包余额职责分离已声明
- [x] 迁移期 NULL 与新建必填不矛盾
