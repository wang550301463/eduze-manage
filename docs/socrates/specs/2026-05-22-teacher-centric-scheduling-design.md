# EduZE Manage —— 老师为核心的排课模型与罗恩菲尔德 778 体系设计

> 文档路径：`docs/socrates/specs/2026-05-22-teacher-centric-scheduling-design.md`

---

## 0. 文档元信息

| 字段 | 值 |
|---|---|
| 文档版本 | v1.1（v1.0 的增量修订） |
| 创建日期 | 2026-05-22 |
| 状态 | Draft（待用户审阅） |
| 文档类型 | 增量设计（Spec Patch） |
| 依据文档 | `docs/socrates/specs/2026-05-09-eduze-manage-design.md` v1.0 |
| 影响范围 | v1.0 spec 的 §4.3 / §4.4 / §4.6 / §5 / §10 / 附录 A / 附录 B |
| 后续文档 | 实施计划增量（由 writing-plans 阶段产出） |

> 本文档**不重写** v1.0 spec，而是对其指定章节做增量修订；落地时同步在 v1.0 文档头部加"v1.1 增量修订 by 本文档"的引用。

---

## 1. 范围与决策摘要

### 1.1 本次需求落点

| 需求（来自用户） | 本设计的落点 |
|---|---|
| 新增老师类型的角色和用户 | 现有 `TEACHER` 内置角色保留 + 新增 6 项细粒度权限；新增"老师工作台"入口与场景；保留"超级管理员 / 校长 / 课程顾问 / 前台"四个内置角色不变 |
| 新增学员需要关联到老师 | 学员必带 1 个**主带老师** `mentor_teacher_id`（NOT NULL）；变更必须留历史；列表 / 详情 / 表单 / 筛选全链路加字段 |
| 课程排期需要按老师排期并关联学生 | **以老师为核心的排课**：老师维护"周可用时段模板" → 校长 / 顾问按模板批量生成课次；学员"订阅"模板时段自动进入未来课次名单；逐课次可手动加减 |
| 初始化罗恩菲尔德 + 778 数据 | 新表 `t_curriculum_stage`（5 阶段）+ `t_curriculum_dimension`（22 维度）+ 5 个示例课程产品 + dev profile 下示例老师 / 学员 / 订阅 |

### 1.2 实施定位

- **本设计是 v1.0 spec 的增量修订**，文件命名 `2026-05-22-teacher-centric-scheduling-design.md`。
- 老 spec 不删，新 spec 中精确指出哪些章节被覆盖（详见 §6）、哪些不变。
- 配套修订 `2026-05-09-eduze-manage-phase1-plan.md` 的 F、G 两个阶段（学员 + 课程与排课），新增约 8–12 个任务节点。

### 1.3 关键决策（与用户共识）

| # | 决策点 | 选择 |
|---|---|---|
| K1 | 排课模型方向 | **B 模型 ── 老师为核心**：老师有可配置时间槽，学生直接报名到该老师某时段（≈小班 / 私教）。班级概念弱化为分组标签。 |
| K2 | 学员↔老师关系 | **1 个主带老师**（可换，留历史） |
| K3 | 老师时间槽 | **两层模型**：周可用时段模板 `TeacherAvailability` + 实际课次 `Lesson` |
| K4 | 学员进入课次名单的方式 | **订阅 + 逐课补丁**：默认走订阅自动入名单，个别试听 / 补课 / 调整走手工加减 |
| K5 | 778 体系定位 | **课程产品仅按阶段**（5 个），778 仅作为**学员阶段评估 / 成长报告**的维度标签，**不**作为课程主题 |
| K6 | 班级概念 | **降级为分组标签**：不再是排课载体，只是"同一老师同一时段学生集合"的命名 |
| K7 | 老师可见范围 | **同校区所有**：默认看"我的课表"，但有"全校"开关 |
| K8 | 778 维度内容 | 7 元素 + 7 设计原则 + 8 流派（印象 / 后印象 / 野兽 / 表现 / 立体 / 超现实 / 抽象表现 / 波普） |

### 1.4 术语补充（追加到 v1.0 spec 附录 A）

| 术语 | 含义 |
|---|---|
| **主带老师**（Mentor Teacher） | 学员的稳定指导老师，1 学员 = 1 主带，可更换并留历史 |
| **可用时段**（TeacherAvailability） | 老师的"周课表模板"——周几 / 起止分钟 / 容量 / 默认画室 |
| **订阅**（LessonSubscription） | 学员对老师某可用时段的常规报名；按可用时段生成课次时自动拉入名单 |
| **课次名单**（LessonStudent） | 单节课次实际参加学员清单（含来源：订阅 / 手工 / 试听 / 补课），与"出勤"`Attendance` 分离 |
| **阶段**（CurriculumStage） | 罗恩菲尔德阶段在本系统的实例：4–7 / 7–9 / 9–11 / 11–13 / 13–16 岁 |
| **维度**（CurriculumDimension） | 778 体系下的"7 元素 / 7 原则 / 8 流派"22 项能力标签，用于成长报告 |

### 1.5 不在本设计范围内（明确划线）

- 课时包销售 / 缴费 / 退费（仍属第三期）
- 课堂作品上传 / 作品集（仍属第二期）
- 老师课时费结算（仍属第三期；本设计的 `LessonStudent` 是其前置依赖）
- 阶段评估完整 UI（本设计仅建表 + 录入入口，完整报告留到第二期）
- 老师跨校区任教（假设 A1，本期不支持，临时代课走手动加学员）

---

## 2. 数据模型

### 2.1 ER 总览（聚焦本次修订的实体）

```
        Tenant ─< Branch
                    │
                    ├─< User ──< UserRole >─ Role (含 TEACHER)
                    │
                    │     ┌─────────────────────────────────────┐
                    │     ▼                                     │
                    │  TeacherAvailability  ───模板↓生成───→ Lesson  ─< LessonStudent >─ Student
                    │     (周可用时段模板)                       │            ▲
                    │                                            └─< Attendance (出勤)
                    │
        Student ─── mentor_teacher_id ─→ User (TEACHER 角色用户)
            │
            ├─< StudentMentorHistory       (主带变更历史)
            ├─< LessonSubscription ────→ TeacherAvailability
            ├─< StudentClassGroup          (现存：班级分组标签)
            ├─< StudentStageAssessment     (阶段评估，新表)
            └── current_stage_id ─→ CurriculumStage
                                          │
                                          └─< StageDimension >─ CurriculumDimension
                                                                (元素 / 原则 / 流派 共 22 项)
```

### 2.2 表清单（粗体 = 新表，斜体 = 字段调整）

| 表 | 关键字段 | 说明 |
|---|---|---|
| `t_student` *字段调整* | + `mentor_teacher_id BIGINT NOT NULL`<br>+ `current_stage_id BIGINT NULL` | mentor 软关联 `t_user.id`；stage 软关联 `t_curriculum_stage.id` |
| **`t_student_mentor_history`** | `student_id`, `from_teacher_id`（NULL=首次绑定）, `to_teacher_id`, `reason`, `changed_at`, `operator_id` | 不可删；索引 `(student_id, changed_at DESC)` |
| **`t_teacher_availability`** | `teacher_id`, `branch_id`, `day_of_week (1-7)`, `start_minute (0-1439)`, `end_minute`, `capacity`, `default_class_room_id NULL`, `valid_from DATE`, `valid_to DATE NULL`, `status (1 启用 / 0 停用)`, `note` | 一个老师可有多条；`valid_from / to` 支持"季节性课表"切换；UQ(`tenant_id, teacher_id, day_of_week, start_minute, valid_from, deleted_at`) |
| **`t_lesson_subscription`** | `student_id`, `teacher_id`, `teacher_availability_id`, `valid_from DATE`, `valid_to DATE NULL`, `status (1 启用 / 0 停用)`, `source ENUM(NORMAL, MAKEUP)`, `note` | 关闭时 `valid_to` 自动落今天；UQ(`tenant_id, student_id, teacher_availability_id, valid_from, deleted_at`) |
| `t_lesson` *字段调整* | - 弱化 `class_group_id`：保留为 nullable<br>+ `teacher_availability_id BIGINT NULL`<br>+ `source TINYINT NOT NULL DEFAULT 1`（1=模板生成 2=手动新建 3=补课 4=试听） | `teacher_id` 仍是必填；冲突检测以"teacher + 时段"和"class_room + 时段"为主键维度 |
| **`t_lesson_student`** | `lesson_id`, `student_id`, `subscription_id NULL`, `source ENUM(SUBSCRIPTION, MANUAL, TRIAL, MAKEUP)`, `status ENUM(BOOKED, CANCELLED)`, `note`, `removed_at NULL`, `removed_by NULL` | "课次报名名单"；与出勤 `t_attendance` 分离；UQ(`tenant_id, lesson_id, student_id, deleted_at`) |
| `t_class_group` *字段调整* | - 解除"必须挂课次"约束；`course_id` 改可空<br>+ `tag_color CHAR(7) NULL` | 第一期作为分组标签使用；保留 `head_teacher_id` |
| `t_attendance` *无字段调整* | — | 仍以 `(lesson_id, student_id)` 唯一；签到 / 出勤业务不变 |
| **`t_curriculum_stage`** | `code (UQ)`, `name`, `age_min`, `age_max`, `order_no`, `lorenfield_phase`, `description` | seed 5 条 |
| **`t_curriculum_dimension`** | `kind ENUM(ELEMENT, PRINCIPLE, MOVEMENT)`, `code (UQ)`, `name`, `description`, `order_no` | seed 7+7+8=22 条 |
| **`t_stage_dimension`** | `stage_id`, `dimension_id`, `weight TINYINT DEFAULT 1` | 阶段 ↔ 维度多对多，便于"每个阶段重点教哪些维度"配置；UQ(`stage_id, dimension_id`) |
| **`t_student_stage_assessment`** | `student_id`, `stage_id`, `assessed_at`, `assessed_by`, `scores_json`（{dimension_code: 1-5}）, `comment` | 第一期建表 + 后端 CRUD；UI 只在学员详情 Tab 加"成长评估"列表 |

### 2.3 关键约束 / 索引

- 所有新表都继承 v1.0 spec §5.2 的"通用列"模式：`id`, `tenant_id`, `branch_id`（适用时）, `created_at`, `updated_at`, `deleted_at`, `created_by`, `updated_by`, `version`
- `t_teacher_availability`：索引 `(tenant_id, branch_id, teacher_id, day_of_week)`
- `t_lesson_subscription`：索引 `(tenant_id, teacher_availability_id, status)`、`(tenant_id, student_id, status)`
- `t_lesson_student`：索引 `(tenant_id, lesson_id)`、`(tenant_id, student_id)`、`(tenant_id, subscription_id)`
- `t_student` 加索引 `(tenant_id, mentor_teacher_id)`、`(tenant_id, current_stage_id)`

### 2.4 老数据兼容 / 迁移决策

- 第一期目前尚无生产数据（A 阶段脚手架刚完成），`t_class_group.course_id` 改可空 + `t_lesson.class_group_id` 改可空属**Flyway 版本前向**改动；
- 已写好的 V1.1.0 / V1.2.0 不动，新增：
  - `V1.5.0__teacher_centric.sql`：DDL 改动（新表 + 老表 ALTER）
  - `V1.5.1__curriculum_tables.sql`：阶段 / 维度 / 阶段-维度 / 学员评估表
  - `V9.1.0__seed_curriculum.sql`：阶段 + 维度 + 阶段-维度推荐挂载 seed
  - `V9.1.1__add_teacher_centric_permissions.sql`：新权限 + 角色补丁
  - `V9.2.0__seed_demo_teachers.sql`：dev profile 示例数据（用 Flyway placeholders 控制条件执行）

---

## 3. 业务流程 + REST API 草案

### 3.1 核心流程

#### 流程 A：注册学员（必带主带老师）

```
[校长 / 顾问 / 前台]
  └─ 打开"新建学员"表单
       ├─ 填基础信息 + 健康 + 家长
       ├─ 选所属校区
       ├─ 选主带老师（按校区筛选 TEACHER 角色用户）── 必填
       ├─ （可选）勾选订阅老师的某条 TeacherAvailability ── 0..N 条
       └─ 提交
   ↓
后端：
  ├─ 写 t_student (mentor_teacher_id 必填)
  ├─ 写 t_student_mentor_history (from=NULL, to=新值, reason="首次绑定")
  ├─ 写 0..N 条 t_lesson_subscription
  └─ Toast: "学员已建档，已订阅 N 节常规课"
```

#### 流程 B：老师维护可用时段

```
[老师本人 或 校长]
  └─ 进"老师工作台" → "我的周课表模板"
       └─ 新增 / 修改 / 停用条目（周几 / 时段 / 容量 / 默认画室 / valid_from-to）
   ↓
后端校验：
  ├─ 同一 teacher_id 下时段不重叠（同一 day_of_week 起止区间冲突 → 拦截 + 弹窗）
  └─ 落库
```

#### 流程 C：批量生成下 N 周课次

```
[校长 / 顾问]
  └─ 进"排课总览" → "按周生成"
       ├─ 选 N 周（默认下 4 周，上限 8 周）
       ├─ 选要生成的老师范围（默认全部）
       └─ 预览：列出"将要创建的课次（按老师 × availability × 周）"
   ↓
后端：
  ├─ 对每条 valid 的 t_teacher_availability，按 day_of_week + start_minute 算出未来 N 周的具体 datetime
  ├─ 跳过已存在 (teacher_id, start_at) 的（幂等）
  ├─ 跳过节假日（第一期硬编码节假日列表 → 后续可配置）
  ├─ 对每条新生成的 lesson：
  │    ├─ 写 t_lesson (source=1 模板生成, teacher_availability_id=源)
  │    └─ 拉所有 status=1 且 valid_from <= lesson_date <= valid_to 的 t_lesson_subscription
  │        → 写 t_lesson_student (source=SUBSCRIPTION, status=BOOKED)
  ├─ 冲突软警告：检测同 teacher / 同 class_room / 同 student 时段冲突 → 弹窗显示
  └─ 返回生成报告（成功 N、跳过 M、冲突警告 K）
```

#### 流程 D：学员补课 / 试听 / 退订 / 换主带

| 动作 | 数据动作 |
|---|---|
| 补课 | 给目标 lesson 写 `t_lesson_student (source=MAKEUP, subscription_id=NULL)` |
| 试听 | 写 `t_lesson_student (source=TRIAL)` |
| 退订 | 把对应 `t_lesson_subscription.status=0`、`valid_to=today`；**已生成的未来课次的 `t_lesson_student` 批量软删除**（保留审计） |
| 换主带 | 写 `t_student_mentor_history` + 改 `t_student.mentor_teacher_id`；老老师现有订阅可选保留 / 取消（前端弹窗让用户决定） |

#### 流程 E：学员看到的"我的课表"

后端聚合：

```sql
SELECT l.*
FROM t_lesson_student ls
JOIN t_lesson l ON l.id = ls.lesson_id
WHERE ls.student_id = ? AND ls.status='BOOKED'
  AND l.start_at BETWEEN ? AND ?
ORDER BY l.start_at;
```

→ 时间轴展示。

### 3.2 REST API 新增 / 调整

> 风格沿用现有 `/api/**` 前缀；统一 `ApiResponse<T>` 包装；JWT 鉴权。

| Method | Path | 权限 | 描述 |
|---|---|---|---|
| GET | `/api/teachers` | `user:read` | 查 TEACHER 角色用户（按校区过滤），用于"主带老师下拉" |
| GET | `/api/teachers/{id}/availabilities` | `teacher:availability:read` | 看某老师所有可用时段 |
| POST | `/api/teachers/{id}/availabilities` | `teacher:availability:write` | 新增 |
| PUT | `/api/teacher-availabilities/{id}` | `teacher:availability:write` | 改 |
| DELETE | `/api/teacher-availabilities/{id}` | `teacher:availability:write` | 软删 / 停用 |
| GET | `/api/teacher-availabilities/conflicts?teacherId=&dayOfWeek=&start=&end=` | `teacher:availability:read` | 校验冲突，预提交用 |
| POST | `/api/students` *扩展* | `student:write` | body 增 `mentorTeacherId` 必填 + `initialSubscriptions[]` 可选 |
| PUT | `/api/students/{id}/mentor` | `student:mentor_assign` | 换主带老师；body: `{toTeacherId, reason, keepSubscriptions: bool}` |
| GET | `/api/students/{id}/mentor-history` | `student:read` | 学员主带变更历史 |
| GET | `/api/subscriptions?studentId=&teacherId=` | `subscription:read` | 查订阅 |
| POST | `/api/subscriptions` | `subscription:write` | 新增订阅 |
| PUT | `/api/subscriptions/{id}` | `subscription:write` | 改（一般用来 valid_to 收尾） |
| DELETE | `/api/subscriptions/{id}` | `subscription:write` | 退订（含级联取消未来名单） |
| POST | `/api/lessons/bulk-generate` *扩展* | `lesson:write` | 改为按 TeacherAvailability 生成；req 增 `teacherIds[] / weeks` |
| GET | `/api/lessons/{id}/students` | `lesson:read` | 课次名单 |
| POST | `/api/lessons/{id}/students` | `lesson:write` | 手动加学员（试听 / 补课） |
| DELETE | `/api/lessons/{id}/students/{studentId}` | `lesson:write` | 手动从名单移除 |
| GET | `/api/schedule/by-teacher?branchId=&weekStart=` *新* | `lesson:read` | "按老师 × 时段"周课表数据 |
| GET | `/api/schedule/my-week` *新* | `lesson:read` | 老师工作台用：我本周课表 |
| GET | `/api/curriculum/stages` | `course:read` | 阶段列表（含 778 维度推荐） |
| GET | `/api/curriculum/dimensions?kind=` | `course:read` | 维度列表 |
| POST | `/api/students/{id}/stage-assessments` | `student:write` | 录入阶段评估 |
| GET | `/api/students/{id}/stage-assessments` | `student:read` | 评估历史 |

### 3.3 权限补丁清单

```sql
INSERT INTO t_permission (id, code, name, module) VALUES
  (38, 'teacher:availability:read',  '老师可用时段查看', 'teacher'),
  (39, 'teacher:availability:write', '老师可用时段编辑', 'teacher'),
  (40, 'subscription:read',          '课程订阅查看',     'subscription'),
  (41, 'subscription:write',         '课程订阅编辑',     'subscription'),
  (42, 'student:mentor_assign',      '主带老师指派',     'student'),
  (43, 'lesson:teacher_view',        '按老师查看课表',   'lesson');

-- 角色补丁
-- TEACHER：teacher:availability:write、subscription:read、lesson:teacher_view、student:read（已有，确认）
-- ADVISOR：subscription:write、student:mentor_assign、lesson:teacher_view
-- PRINCIPAL：以上全部
-- SUPER_ADMIN：自动包含（其权限是 SELECT * FROM t_permission）
```

### 3.4 关键校验规则

- 学员的 `mentor_teacher_id` 必须是同 branch_id 下的 `TEACHER` 角色用户（否则 400）
- 新增订阅时：(`student.branch_id` == `availability.branch_id` == `teacher.branch_id`)（跨校区禁止订阅，第一期严格）
- 学员订阅同一 availability 不可重复有效（按 `valid_from` / `valid_to` 重叠判断）
- 课次冲突（**软警告**，弹窗提示但不阻止）：
  - 同 `teacher_id` 同 `(start_at, end_at)` 重叠
  - 同 `class_room_id` 同时段重叠
  - 同 `student` 同时段被排入两节课
- 批量生成范围上限：单次 ≤ 8 周（保护性兜底）

---

## 4. UI 改动

> 沿用 v1.0 spec §7 设计系统：Tailwind + Radix Primitives + Phosphor，色板 / 字体 / 圆角不变。

### 4.1 改动总览（按页面）

| 页面 | 改动 |
|---|---|
| **新建 / 编辑学员表单** | + 必填字段「主带老师」（按校区筛选的搜索下拉） + 可选区块「初始订阅」（multi-select 老师的 availability 卡片） |
| **学员列表** | 列增「主带老师」（缩略：头像 + 姓） + 增筛选「按主带老师」 |
| **学员详情 Sheet** | + 顶部「主带老师」徽标 + 按钮「更换主带」（弹 Dialog 填新老师 + 原因 + 是否迁移订阅） + Tab「订阅」 + Tab「主带变更记录」 + Tab「阶段评估」 |
| **班级页面（降级）** | 标题改"分组标签"；移除"排课入口"；保留"加成员 / 转出"；UI 上加说明 banner："分组标签用于花名册分类，不影响排课，排课在「老师课表」中进行" |
| **周课表视图（重写）** | 横轴 = **老师列**（按校区分组、可折叠），纵轴 = 半小时时段；单元格内显示「容量 X/Y + 课程阶段缩写 + 画室缩写」；点击 → Sheet 抽屉 |
| **课次详情 Sheet** | Tab「基础信息」「学员名单（订阅来源标签 + 手动加减）」「调课」「点名」 |
| **批量排课对话框** | 模板 = 老师的 availability；预览表格列出"老师 / 周几 / 时段 / 学员预计数（来自有效订阅） / 画室"；底部「冲突警告」摘要 |
| **老师工作台（新页面）** | 卡片 1「我的本周课表」 + 卡片 2「我的学员（主带）」 + 卡片 3「待批请假」 + 卡片 4「下周排课模板维护入口」 |
| **左侧导航** | 增菜单项「老师」（管理 TEACHER 角色用户 + 可用时段配置入口）；老师角色登录后默认落地「我的工作台」 |

### 4.2 关键页面线框（文字版）

#### 周课表（新）

```
┌─────────────────────────────────────────────────────────────────┐
│ 周课表  [< 2026-05-25 ~ 05-31 >]  [按老师▼] [按校区▼] [批量排课] │
├──────┬──────────┬──────────┬──────────┬──────────┬──────────┬──┤
│ 时段 │ 张老师    │ 王老师    │ 李老师    │ 周老师    │ 孙老师    │..│
├──────┼──────────┼──────────┼──────────┼──────────┼──────────┼──┤
│09:00 │ 周六•R1   │          │          │ 周六•R3   │          │  │
│      │ 启蒙 6/8  │          │          │ 探索 4/6  │          │  │
├──────┼──────────┼──────────┼──────────┼──────────┼──────────┼──┤
│10:30 │          │ 周六•R1   │          │          │ 周六•R2   │  │
│      │          │ 成长 5/8  │          │          │ 进阶 3/4  │  │
└──────┴──────────┴──────────┴──────────┴──────────┴──────────┴──┘

色块按"阶段"染色（5 种主色）；右上角小角标显示"模板生成 / 手动 / 补课"
```

#### 老师工作台

```
┌──────────────────────────────────────────────────────────┐
│ 张老师，下午好         [我的工作台]                       │
├──────────────┬──────────────┬──────────────┬─────────────┤
│ 📅 本周课次  │ 👶 主带学员  │ 📝 待批请假  │ 🕘 模板维护 │
│   8 节      │   12 人      │   3 条       │   →         │
├──────────────┴──────────────┴──────────────┴─────────────┤
│ 本周课表（横向滚）                                        │
│ [Mon 09:00 启蒙 6 人] [Mon 11:00 探索 4 人] [Sat 09:00..]│
├──────────────────────────────────────────────────────────┤
│ 我的主带学员（按阶段分组）                                 │
│  4-7 启蒙 ┃ 小李 小张 小王 ...                            │
│  7-9 探索 ┃ 小陈 小刘 ...                                 │
└──────────────────────────────────────────────────────────┘
```

### 4.3 移动端适配（沿用 v1.0 spec §7.6 断点）

- 周课表在 `< md` 切换为"以老师为纵向卡片堆 + 横向时段滚动"
- 老师工作台 4 个 KPI 卡片在 `< sm` 改为 2x2 网格

### 4.4 无障碍 / 快捷键

- 周课表单元格可键盘 Tab 聚焦，Enter 打开 Sheet
- 新增快捷键 `g t`（go teacher）= 跳转老师工作台
- 单元格 `aria-label="周六 09:00 张老师 启蒙阶段 6 人 / 8 人 容量"`

---

## 5. Seed 数据

> 全部写在 `V9.1.0__seed_curriculum.sql` + `V9.1.1__add_teacher_centric_permissions.sql` + `V9.2.0__seed_demo_teachers.sql`，主键预留固定段（stage 200000-、dim 210000-、demo user 1100-）避免冲突。

### 5.1 阶段（5 条，对应罗恩菲尔德 4–16 岁）

| code | name | age_min | age_max | order | lorenfield_phase | description |
|---|---|---|---|---|---|---|
| `STAGE_KMD` | 启蒙阶段 | 4 | 7 | 1 | Preschematic（样式化前期） | 涂鸦向符号化过渡；以色彩感知与基础造型启发为主 |
| `STAGE_TS` | 探索阶段 | 7 | 9 | 2 | Schematic（样式化期） | 用图式表达事物；引入线条 / 形状 / 色彩三元素 |
| `STAGE_CZ` | 成长阶段 | 9 | 11 | 3 | Dawning Realism（写实萌芽期） | 写实意识萌发；引入空间 / 明度 / 比例与构图原则 |
| `STAGE_JJ` | 进阶阶段 | 11 | 13 | 4 | Pseudo-Realistic（拟写实期） | 主动追求"像"；引入对比 / 节奏 / 统一与流派启蒙 |
| `STAGE_SB` | 思辨阶段 | 13 | 16 | 5 | Adolescent Art（决定期） | 风格意识自觉；以现代主义诸流派为媒介进行个人表达 |

### 5.2 维度（22 条 = 7 + 7 + 8）

**ELEMENT（艺术元素 7）**

| code | name |
|---|---|
| `EL_LINE` | 线条 (Line) |
| `EL_SHAPE` | 形状 (Shape) |
| `EL_FORM` | 形态 (Form) |
| `EL_SPACE` | 空间 (Space) |
| `EL_COLOR` | 色彩 (Color) |
| `EL_VALUE` | 明度 (Value) |
| `EL_TEXTURE` | 肌理 (Texture) |

**PRINCIPLE（设计原则 7）**

| code | name |
|---|---|
| `PR_BALANCE` | 平衡 (Balance) |
| `PR_CONTRAST` | 对比 (Contrast) |
| `PR_EMPHASIS` | 强调 (Emphasis) |
| `PR_RHYTHM` | 节奏 (Rhythm / Movement) |
| `PR_PATTERN` | 图案 (Pattern) |
| `PR_UNITY` | 统一 (Unity) |
| `PR_PROPORTION` | 比例 (Proportion) |

**MOVEMENT（艺术流派 8，按用户选定）**

| code | name |
|---|---|
| `MV_IMPRESSIONISM` | 印象派 (Impressionism) |
| `MV_POST_IMPRESSIONISM` | 后印象派 (Post-Impressionism) |
| `MV_FAUVISM` | 野兽派 (Fauvism) |
| `MV_EXPRESSIONISM` | 表现主义 (Expressionism) |
| `MV_CUBISM` | 立体主义 (Cubism) |
| `MV_SURREALISM` | 超现实主义 (Surrealism) |
| `MV_ABSTRACT_EXPRESSIONISM` | 抽象表现主义 (Abstract Expressionism) |
| `MV_POP_ART` | 波普艺术 (Pop Art) |

### 5.3 阶段 ↔ 维度推荐挂载（t_stage_dimension）

> 作为评估侧重提示；可在 UI 上覆盖。

| 阶段 | 重点维度（weight=2） | 一般维度（weight=1） |
|---|---|---|
| STAGE_KMD（4-7） | EL_LINE, EL_SHAPE, EL_COLOR | — |
| STAGE_TS（7-9） | EL_LINE, EL_SHAPE, EL_COLOR, EL_TEXTURE | PR_PATTERN |
| STAGE_CZ（9-11） | EL_SPACE, EL_VALUE, PR_PROPORTION, PR_BALANCE | EL_FORM |
| STAGE_JJ（11-13） | PR_CONTRAST, PR_RHYTHM, PR_UNITY, MV_IMPRESSIONISM, MV_POST_IMPRESSIONISM | PR_EMPHASIS |
| STAGE_SB（13-16） | MV_FAUVISM, MV_EXPRESSIONISM, MV_CUBISM, MV_SURREALISM, MV_ABSTRACT_EXPRESSIONISM, MV_POP_ART | PR_EMPHASIS, PR_UNITY |

### 5.4 示例课程产品（5 个，与阶段一对一）

> 写入 `t_course`，与 v1.0 spec 的 `t_course` 结构兼容。

| code | name | age_min | age_max | lesson_minutes | description |
|---|---|---|---|---|---|
| `KMD-90` | 启蒙·4–6 岁 | 4 | 6 | 90 | 罗恩菲尔德样式化前期阶段；启发色彩与基础造型感知 |
| `TS-90` | 探索·7–8 岁 | 7 | 8 | 90 | 样式化期；引入 7 大艺术元素的基础应用 |
| `CZ-120` | 成长·9–10 岁 | 9 | 10 | 120 | 写实萌芽期；引入空间 / 明度 / 比例与基础构图原则 |
| `JJ-120` | 进阶·11–12 岁 | 11 | 12 | 120 | 拟写实期；7 大设计原则系统训练 + 印象派 / 后印象派启蒙 |
| `SB-150` | 思辨·13–16 岁 | 13 | 16 | 150 | 决定期；以现代主义诸流派为媒介开展个人风格探索 |

### 5.5 示例老师 / 学员 / 订阅（仅 dev profile seed，prod 不 seed）

> 写在 `V9.2.0__seed_demo_teachers.sql`，仅在 dev profile 执行（Flyway 用 `placeholders.demoSeed=true/false` 控制条件 SQL）。

- 3 个示例老师：`teacher_zhang` / `teacher_wang` / `teacher_li`（密码统一 `teacher@123`，角色 TEACHER，归 branch_id=1）
- 每位老师 2 条 `t_teacher_availability`（如周六 09:00-10:30、周日 10:00-11:30）
- 5 个示例学员（每阶段 1 个），各绑定主带老师 + 1 条订阅
- 登录后立即看到一份"非空的周课表"

### 5.6 权限补丁 seed

```sql
-- V9.1.1__add_teacher_centric_permissions.sql
INSERT INTO t_permission (id, code, name, module) VALUES
  (38, 'teacher:availability:read',  '老师可用时段查看', 'teacher'),
  (39, 'teacher:availability:write', '老师可用时段编辑', 'teacher'),
  (40, 'subscription:read',          '课程订阅查看',     'subscription'),
  (41, 'subscription:write',         '课程订阅编辑',     'subscription'),
  (42, 'student:mentor_assign',      '主带老师指派',     'student'),
  (43, 'lesson:teacher_view',        '按老师查看课表',   'lesson')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- TEACHER 角色补：teacher:availability:write / subscription:read / lesson:teacher_view
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 4000 + p.id, 4, p.id FROM t_permission p
WHERE p.code IN ('teacher:availability:write','subscription:read','lesson:teacher_view');

-- ADVISOR 角色补：subscription:write / student:mentor_assign / lesson:teacher_view
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 3000 + p.id, 3, p.id FROM t_permission p
WHERE p.code IN ('subscription:write','student:mentor_assign','lesson:teacher_view');

-- PRINCIPAL 角色补：上述全部
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 2000 + p.id, 2, p.id FROM t_permission p
WHERE p.code IN ('teacher:availability:read','teacher:availability:write',
                 'subscription:read','subscription:write',
                 'student:mentor_assign','lesson:teacher_view');

-- SUPER_ADMIN 角色补：自动（已有 SELECT * FROM t_permission 的 seed 规则）
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 1000 + p.id, 1, p.id FROM t_permission p
WHERE p.code IN ('teacher:availability:read','teacher:availability:write',
                 'subscription:read','subscription:write',
                 'student:mentor_assign','lesson:teacher_view');
```

---

## 6. 对原 spec 与 plan 的修订位置

### 6.1 对 `docs/socrates/specs/2026-05-09-eduze-manage-design.md` 的修订

| 位置 | 修订动作 |
|---|---|
| §1 文档元信息 | 增"v1.1 增量修订 by 本文档"标注 + 指针 |
| §4.3 模块 #2 学员与家长 | 增"主带老师"字段说明；学员表单流增"初始订阅"步骤；详情 Sheet 增"订阅 / 主带变更 / 阶段评估"三个 Tab |
| §4.4 模块 #3 课程与排课 | **整节重写排课模型**：从"班级 → 课次"改为"老师 + 可用时段 → 课次 + 名单"；课程产品按阶段；班级降级为"分组标签"；保留冲突软警告策略 |
| §4.6 全局体验 | 增老师工作台、`g t` 快捷键 |
| §5 数据模型 | ER 图增 TeacherAvailability / LessonSubscription / LessonStudent / CurriculumStage / CurriculumDimension / StudentMentorHistory；§5.2 表清单追加；§5.3 tenant_id 策略不变 |
| §10 路线图 | 第二期"课堂与作品"基础上注明"阶段评估完整 UI 落地于第二期" |
| 附录 A 术语 | 追加 §1.4 中 6 个新术语 |
| 附录 B 取舍 | 增 B.10「为什么从班级排课切到老师排课」、B.11「为什么 778 是评估维度而非课程产品」 |

### 6.2 对 `docs/socrates/plans/2026-05-09-eduze-manage-phase1-plan.md` 的修订

| 阶段 | 任务调整 |
|---|---|
| **A（脚手架）** | 不变（已完成） |
| **B（测试基建 + 租户底座）** | 不变 |
| **C（设计系统）** | 不变 |
| **D（账号 / 权限 / 校区）** | 增 D9：补 6 项新权限到 seed + 角色补丁；增 D10：老师选择器 API（按 branch 查 TEACHER 用户） |
| **E（后台壳）** | 增 E8：基于登录用户角色判断默认落地页（老师 → 工作台）；增 E9：快捷键 `g t` |
| **F（学员与家长）** | F1–F5 不变；**F6 重写**学员表单（加 `mentorTeacherId` 必填 + 初始订阅）；新增 F11：换主带老师 + 历史 API；新增 F12：阶段评估建表 + CRUD |
| **G（课程与排课）—— 重写** | 删 G6（按班级周排课模板）；新增：<br>G6'：`t_teacher_availability` CRUD（后端）<br>G7'：`t_teacher_availability` 配置 UI<br>G8'：批量生成课次（基于 availability）<br>G9'：`t_lesson_subscription` CRUD<br>G10'：课次名单 `t_lesson_student` API<br>G11'：周课表视图（老师列）UI<br>G12'：老师工作台页面<br>G13'：班级页降级为分组标签 |
| **H（签到与接送）** | 出勤记录绑定 `lesson_id + student_id`，由 `t_lesson_student` 派生（而不是从 `t_class_group` 推），调整 1 个 SQL；其余不变 |
| **I（联调 / 部署）** | 增 I6：dev profile seed 示例老师 / 学员 / 订阅；增 I7：E2E 冒烟脚本覆盖"老师维度课表生成 + 出勤"链路 |
| 总任务数 | 70 → 约 78–82（取决于拆分粒度） |

### 6.3 实施顺序建议（拆给后续 plan 执行）

```
1. 先 DDL + seed：V1.5.0 + V1.5.1 + V9.1.0 + V9.1.1（一次 PR，可独立 review）
2. 再后端 service / API（按 D/F/G 改动顺序）（2-3 个 PR）
3. 再前端：
   a. 学员表单 + 主带字段（影响最广）
   b. 老师可用时段配置 + 工作台
   c. 周课表视图重写
   （3-4 个 PR）
4. 联调 / E2E
```

---

## 7. 假设 / 风险 / TBD

### 7.1 假设

| # | 假设 | 影响 |
|---|---|---|
| A1 | 老师不会跨多个校区任教（同一时刻只属于一个 branch） | TeacherAvailability 以 branch_id 区分；跨校区临时代课走"手动加学员"绕过 |
| A2 | 学员每周不超过 3 个常规订阅 | 容量预估、对账复杂度可控 |
| A3 | 节假日跳过用硬编码列表即可 | 第一期不接日历服务；后续 TBD |
| A4 | 阶段评估 UI 在第一期只做"录入 + 列表"，可视化报告在第二期 | 不阻塞主链路 |
| A5 | 8 大流派以本设计 §5.2 选定的为准（印象 / 后印象 / 野兽 / 表现 / 立体 / 超现实 / 抽象表现 / 波普） | 可由教研团队后续微调 seed |

### 7.2 风险

| # | 风险 | 缓解 |
|---|---|---|
| R1 | 学员订阅 → 批量生成课次的"幂等性 + 边界"逻辑复杂（valid_from / to 与 lesson_date 比较） | 抽 `ScheduleGenerator` 独立 service + 写 ≥ 10 个单测覆盖边界（学员订阅生效中段切入 / 退订当周 / 老师停用时段 / availability valid_to 过期等） |
| R2 | 周课表"按老师列"在校区老师多时（>15 列）UI 性能 / 可读性下降 | 老师列折叠 + 仅显示有课的老师 + 默认仅本周 |
| R3 | 主带老师变更级联：现有订阅是否跟着迁？ | 默认不跟，弹窗让操作者明确选择"保留旧订阅 / 迁移到新老师同时段"；走审计日志 |
| R4 | 老师可见范围 Q8=B 同校区全开，未来若想限制只看自己学员，需追加"个人范围"过滤器 | 把"是否限制个人范围"做成 RolePermission 上的可选附加标志，未来无需重构 |
| R5 | 778 维度 22 项，UI 评估面板放不下 | 默认按当前阶段过滤推荐维度（基于 `t_stage_dimension`），可点"显示全部 22 项"展开 |
| R6 | "班级降级"对已有班级语义的认知冲击（同事可能仍叫"排课到 X 班"） | 文档与 UI 文案统一："分组标签 = 班级"在 §4.1 顶部说明 banner |

### 7.3 TBD

| # | 项 | 何时确定 |
|---|---|---|
| T1 | 阶段评估的评分尺度（1-5 / 1-3 / 文字描述） | 实施 F12 时与教研团队确认 |
| T2 | 是否允许"试听课"自动转换为正式订阅（流程 D 的延伸） | 招生模块第四期细化 |
| T3 | 节假日日历是否接入第三方（如官方节假日 API） | 第二期视实际遗漏量决定 |
| T4 | 老师"代课"场景：临时换老师上某节课，是否影响该课次出勤 / 课时费归属 | 财务模块第三期定义 |
| T5 | 主带老师离职 → 学员主带批量迁移工具 | 视实际运营需求做轻量批量工具，本期不做 |

---

## 附录 A：补充的关键设计取舍

### A.1 为什么从"班级排课"切到"老师排课"

- 少儿美术机构常见经营是「老师品牌化」：家长记的是「张老师的课」，而不是「周六 A 班」；老师维度比班级维度更贴近真实业务认知；
- 老师变更频度（季度）远低于班级变更频度（每开新班、每分新生）：以更稳定的实体作为排课主轴，调度复杂度更低；
- 一对一 / 小班 / 试听 / 补课等场景在班级模型下都是"特例补丁"，在老师模型下是"正常订阅或单课名单"，模型本身解决了大量边界情况；
- 班级降级为「分组标签」保留了"花名册分类"这一真实需求，没有完全否定原模型。

### A.2 为什么 778 是评估维度而非课程产品

- 778 是「能力 / 风格 / 原则」三组分类，本质是评估学员成长的"标尺"，不是排课的"载体"；
- 如果把 778 × 5 阶段共 110 个组合都做成课程产品，会带来 22 × 5 = 110 行 `t_course` 数据 + 排课时下拉选择的灾难；
- 让课次「打主题标签」也比「绑定课程产品」更灵活：老师可以一节课同时打 `EL_COLOR + PR_BALANCE` 两个标签；
- 留给第二期：成长报告页可以基于历次评估生成「学员在 22 维度上的能力雷达图」，这是续费抓手。

### A.3 为什么订阅 + 名单 分两张表（而不是合一张）

- 订阅是「常规意图」（学员定期上某老师某时段），名单是「单节事实」（学员是否实际进了某节课）；
- 试听 / 补课 / 临时调整都是「有名单但无订阅」的场景；
- 订阅退订时，未来已生成的名单可以批量软删除（保留审计），订阅本体也保留历史，两者解耦清晰；
- 与 v1.0 spec 中"`t_attendance` 是单条出勤记录"语义一致——出勤是基于名单的进一步事实。

---

## 附录 B：实施清单（给后续 writing-plans 阶段消费）

下一步由 writing-plans 技能产出 `2026-05-22-teacher-centric-scheduling-plan.md`，本节列出关键交付项以便 plan 拆任务：

1. **Flyway 迁移脚本**
   - `V1.5.0__teacher_centric.sql`：DDL（新表 6 张 + 老表 ALTER 3 处）
   - `V1.5.1__curriculum_tables.sql`：阶段 / 维度 / 阶段-维度 / 学员评估表
   - `V9.1.0__seed_curriculum.sql`：阶段 + 维度 + 阶段-维度推荐挂载 + 5 个课程产品
   - `V9.1.1__add_teacher_centric_permissions.sql`：6 项新权限 + 角色补丁
   - `V9.2.0__seed_demo_teachers.sql`：dev profile 示例数据（Flyway placeholder 条件执行）
2. **后端**
   - `TeacherController` + `TeacherAvailabilityService`
   - `LessonSubscriptionService` + `LessonSubscriptionController`
   - `ScheduleGenerator` service（批量生成核心算法，含 ≥10 个单测）
   - `LessonStudentService` + 课次名单 API
   - `Student` 字段扩展 + `mentor` API + 历史 API
   - `StageAssessmentService` 最小 CRUD
   - `ScheduleByTeacherQuery` 聚合查询（周课表）
   - `CurriculumController`（阶段 / 维度只读 API）
3. **前端**
   - 学员表单加 `mentorTeacherId` + 初始订阅
   - 老师可用时段配置页
   - 周课表视图重写（按老师列）
   - 老师工作台页（新路由）
   - 班级页降级为分组标签
   - 学员详情 Sheet 三个新 Tab
4. **测试**
   - `ScheduleGenerator` 单测覆盖边界
   - 端到端冒烟：seed 老师 → 配置 availability → 创建学员 + 订阅 → 批量生成 → 出勤

---

**（文档结束）**
