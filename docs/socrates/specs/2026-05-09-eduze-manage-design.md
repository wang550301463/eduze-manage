# EduZE Manage —— 少儿美术教培管理系统 总体设计

> 文档路径：`docs/socrates/specs/2026-05-09-eduze-manage-design.md`

---

## 1. 文档元信息

| 字段 | 值 |
|---|---|
| 文档版本 | v1.0 |
| 创建日期 | 2026-05-09 |
| 状态 | Draft（待用户审阅） |
| 文档类型 | 总体设计（Spec） |
| 适用范围 | 整个 EduZE Manage 产品的长期愿景 + 第一期 MVP 详细设计 |
| 后续文档 | `2026-05-09-eduze-manage-phase1-plan.md`（实施计划，由 writing-plans 阶段产出） |

> 关键设计取舍（为什么这么选 / 不这么选）请见 §附录 B。

---

## 2. 业务背景与产品定位

### 2.1 机构画像

- **机构类型**：线下少儿美术启蒙
- **学员年龄**：4–12 岁
- **付费 / 沟通主体**：**家长**（孩子不上手机）
- **业务模式**：以**课时包销售**为主（10 / 20 / 50 节），按周排课
- **机构规模假设（第一期）**：1–5 个校区，单校区 100–500 名在读学员，机构全员在读规模 100–2000 人量级
- **业务命脉**：续费率 → 受作品质量、家长感知、阶段评估影响最大

### 2.2 用户角色与典型一天

| 角色 | 第一期是否上线 | 典型场景 |
|---|---|---|
| 超级管理员 | ✅ | 配置角色权限、新建校区、维护字典 |
| 校长 / 校区负责人 | ✅ | 看本校区数据看板、审核请假、调度老师 |
| 课程顾问 | ✅（弱） | 第一期主要负责录入新生、关联家长（招生模块第四期才完整） |
| 班主任 / 任课老师 | ✅ | 查本周课表、点名、记录请假 |
| 前台 | ✅ | 入园 / 离园签到、接送人核对、临时接待 |
| 财务 | 角色预留 | 第三期（财务与订单模块）启用 |

**北极星指标（NSM）**：**月度续费率**。
辅助指标：周出勤率、试听转化率、家长触达率、校区营收。

### 2.3 产品定位

- **第一期定位**：替代机构当前散落在 Excel、纸质点名册、微信群里的"教务日常运营"；让校长能看到一张随时正确的学员花名册和一张随时准确的周课表。
- **长期愿景**：覆盖少儿美术机构"招生 → 服务 → 续费"全生命周期；可扩展为 SaaS 多租户产品。

---

## 3. 长期愿景：7 模块全景

### 3.1 模块全景图

```
                ┌─────────────── 1️⃣ 招生与试听 ───────────┐
                │                                          ↓
   [底座]       2️⃣ 学员与家长（数据底座，必须先有）
                │            │            │            │
                ↓            ↓            ↓            ↓
          3️⃣ 课程与排课  7️⃣ 财务与订单  6️⃣ 家校沟通    │
                │                                       │
                ├──→ 4️⃣ 签到与接送  ──────────────────→│
                └──→ 5️⃣ 课堂与作品  ──────────────────→│
                                                        │
                                              （消息中心 / 家长触达）
```

### 3.2 七个模块一页简介

| # | 模块 | 核心场景 | 核心实体 | 上线期次 |
|---|---|---|---|---|
| 1 | 🎯 **招生与试听** | 线索登记 → 邀约试听 → 试听课 → 转化报名 | `Lead`、`TrialClass`、`TrialEnrollment` | 第四期 |
| 2 | 👶 **学员与家长** | 学员档案、家长 / 紧急联系人、家庭关系、课时包余额、合同 | `Student`、`Guardian`、`StudentGuardianRelation`、`CoursePackage`、`Contract` | **第一期** |
| 3 | 📅 **课程与排课** | 课程产品、班级、教师、画室、周课表、调课 / 补课 | `Course`、`ClassGroup`、`ClassRoom`、`Lesson`、`LessonStudent` | **第一期** |
| 4 | 📷 **签到与接送** | 入园 / 离园签到、接送人核对、缺勤 / 请假、出勤统计 | `Attendance`、`PickupRecord`、`LeaveRequest` | **第一期** |
| 5 | 🎨 **课堂与作品** | 课堂作品照片上传、老师点评、推送家长、学员作品集 | `Artwork`、`ArtworkComment`、`StudentPortfolio` | 第二期 |
| 6 | 💬 **家校沟通** | 公告、班级群消息、阶段成长报告、续费提醒 | `Announcement`、`GrowthReport`、`Message` | 第五期（轻量版伴随 #5 上线） |
| 7 | 💰 **财务与订单** | 课时包销售、缴费 / 退费、电子收据、对账、教师课时费结算 | `Order`、`Payment`、`Refund`、`TeacherPayroll` | 第三期 |

**横切关注点（不是独立模块，是底座）**：

- 🔐 **账号 / 角色 / 权限** —— RBAC + 校区数据范围
- 🏫 **校区维度** —— 第一期所有数据带 `branch_id`，但不做"多校区运营"独立模块
- 🌐 **租户维度** —— 所有表预留 `tenant_id`，第一期默认为 1，对外按单租户呈现
- 📊 **数据看板** —— 每个模块自带 KPI 卡，未来可叠加"校长视角"总览
- 📨 **消息中心** —— 第一期是接口预留 + 落库；第二期对接微信小程序订阅消息

---

## 4. 第一期 MVP 详细设计

### 4.1 范围

#### ✅ 第一期要做

1. **模块 0：账号 / 权限 / 校区底座**
2. **模块 #2：学员与家长**
3. **模块 #3：课程与排课**
4. **模块 #4：签到与接送**
5. **跨模块的全局体验**（设计系统落地、全局搜索、Toast、键盘快捷键、骨架屏、响应式、无障碍）

#### ❌ 第一期明确不做（避免 scope creep）

| 不做项 | 何时做 | 第一期如何兜底 |
|---|---|---|
| 课堂作品上传 / 学员作品集 | 第二期 | — |
| 微信小程序家长端 | 第二期 | 后端预留消息中心接口 |
| 课时包销售 / 在线缴费 / 退费 | 第三期 | 课时包数据手工录入，余额展示只读 |
| 招生线索 / 试听课转化 | 第四期 | 新生由前台 / 顾问直接录入 |
| 家校沟通完整版（公告、群消息、成长报告） | 第五期 | 第一期签到通知靠"接口预留 + 短信兜底"，可不发 |
| 人脸识别签到 | 第二期及以后 | 前台手动签到 + 二维码扫码 |
| 教师课时费结算 | 第三期 | 课时数据落库，结算靠 Excel 导出 |
| 财务对账 / 电子收据 | 第三期 | — |
| 多租户 UI（租户切换、租户管理） | 视商业进展 | 数据库结构预留，UI 不暴露 |
| OSS 文件存储 | 第二期上作品时 | 第一期文件存本地磁盘（学员头像可缺省） |

---

### 4.2 模块 0：账号 / 权限 / 校区底座

#### 功能

- **登录**：账号 + 密码（BCrypt 加盐），登录失败次数限制（同一账号 5 次失败锁定 10 分钟，由 Redis 记录）
- **JWT 鉴权**：无状态 Token + Refresh Token；登出时把 JWT 写入 Redis 黑名单直至过期
- **角色管理**：内置 5 个角色（超级管理员、校长、课程顾问、班主任 / 任课老师、前台），支持自定义角色
- **权限模型**：RBAC（用户—角色—资源—操作）；资源粒度到"模块 + 操作"（如 `student:read`、`student:write`、`lesson:reschedule`）
- **数据范围**：每个用户绑定 1..N 个校区；MyBatis-Plus 拦截器自动给所有带 `branch_id` 的查询拼上 `WHERE branch_id IN (...)`
- **校区管理**：增删改查校区（仅超级管理员）
- **个人中心**：改密码、绑定手机号

#### UI

- **登录页**：单页极简，品牌橙红色按钮
- **后台壳**：
  - 顶部 Sticky Header：Logo + 全局搜索（Ctrl/Cmd+K） + 消息铃铛 + 用户菜单
  - 左侧侧栏：模块导航（图标 + 文案），可折叠
  - 主内容区：模块自由内容
  - 详情侧边 Sheet（用 Radix Sheet）

---

### 4.3 模块 #2：学员与家长

#### 功能

- **学员档案 CRUD**
  - 基础信息：姓名、性别、生日、入园日期、入园编号（机构编码，业务唯一）、状态（在读 / 暂停 / 退学）
  - 安全字段：**过敏史**、**特殊健康情况**、**紧急联系人**
  - 附件：头像（可选，第一期文件存本地）
  - 归属：所属校区、所属班级（多对多）
- **家长档案 CRUD**
  - 姓名、与学员关系（爸爸 / 妈妈 / 爷爷 / 奶奶 / 外公 / 外婆 / 其他）、手机号、是否接送人、是否主联系人
  - **支持一户多生**：一个家长可关联多个学员
- **课时包余额（只读）**
  - 第一期不做销售流程，由前台 / 校长**手工录入**："学员 X 的课时包：剩余 N 节，到期日 YYYY-MM-DD"
  - 显示在学员详情页和班级花名册
  - 当余额 ≤ 5 时高亮提醒（续费触发）
- **批量操作**
  - 批量导入学员（Excel 模板）
  - 批量分班、批量调班

#### UI

- **学员列表**：
  - 桌面端：`<DataTable>`（姓名 / 入园编号 / 性别 / 年龄 / 班级 / 课时余额 / 状态 / 操作）
  - 移动端：卡片列表（同字段，2 行布局）
  - 顶部筛选：校区、班级、状态、课时余额阈值
  - 行点击 → 右侧 Sheet 抽屉打开详情
- **学员详情 Sheet**：
  - Tab：基础信息 / 家长 / 课时包 / 出勤记录 / 请假记录
  - 顶部操作按钮：编辑、停学、调班
- **新建学员表单**：
  - React Hook Form + Zod 校验
  - 表单分段：基础信息 → 健康 → 家长（可现场新建家长）→ 班级
  - 提交后 Toast 通知（带"撤销" 4.2 秒）

---

### 4.4 模块 #3：课程与排课

#### 功能

- **课程产品**（`Course`）：课程名称（如"启蒙·4–6 岁"）、单节时长、年龄范围、封面图、简介
- **班级**（`ClassGroup`）：班级名（如"周六上午启蒙 A 班"）、所属课程、所属校区、班主任、容量
  - 班级成员管理：加入 / 转出 / 转班
- **画室 / 教室**（`ClassRoom`）：所属校区、容量、设备清单（备注）
- **教师**（`Teacher`，第一期复用账号体系，老师角色用户）
- **课次**（`Lesson`）：班级 + 日期 + 时段 + 教师 + 画室 + 状态（待开课 / 进行中 / 已完成 / 已取消）
- **周课表**：
  - 横轴星期一~日，纵轴时段（半小时颗粒度）
  - 单元格显示班级名 + 教师名缩写 + 画室缩写
  - 颜色按课程分类
  - 点击单元格 → Sheet 抽屉显示该课次详情、点名入口
- **排课操作**：
  - 单次新建课次
  - 按周生成（按"班级周固定时间"模板批量生成 N 周）
  - 调课（同班级改时间 / 改老师 / 改画室），变动需写入 `LessonChangeLog`
  - 取消课次（需备注原因）
  - **冲突检测**：同教师 / 同画室 / 同班级时段冲突需弹窗警告
- **补课**：第一期允许"插入额外课次"作为补课，但**不做"学员个体补课"**（学员个体补课跨班级，复杂度较高，留到第二期）

#### UI

- **周课表视图**（默认）：日历式，可前后翻周
- **班级列表 / 班级详情**：管理班级成员
- **课次详情 Sheet**：基础信息 + 学员花名册 + 调课操作

---

### 4.5 模块 #4：签到与接送

#### 功能

- **签到**：
  - 第一期签到方式：**前台手动勾选** + **二维码扫码**（家长出示绑定的二维码，前台扫描确认）
  - 人脸识别预留扩展位（接口、数据表都留好），第二期视设备和预算决定是否上线
  - 签到状态：未到 / 已入园 / 已离园 / 缺勤 / 请假
- **接送人记录**（`PickupRecord`）：
  - 入园时记录"由谁送的"（从该学员家长列表选）
  - 离园时记录"由谁接的"
  - 异常情况（陌生人接送、没人来接）由前台手工标记并备注
- **请假**（`LeaveRequest`）：
  - 家长（电话 / 微信）告诉前台 → 前台代录入
  - 状态：待审批 → 已批准 / 已拒绝（班主任审批）
  - 已批准的请假自动把当天的 `Attendance` 状态置为"请假"
- **缺勤**：
  - 课次结束未签到的学员自动置为"缺勤"
  - 缺勤可由班主任后续追溯改成"请假"（需备注）
- **出勤统计**：
  - 学员维度：本月出勤率
  - 班级维度：本月平均出勤率
  - 校区维度：本周缺勤人次

#### UI

- **签到工作台**（前台主用）：
  - 顶部按校区 + 时段筛选
  - 中间大张"今日入园 / 离园"花名册，每行一个学员，状态色块 + 一键签到按钮
  - 右上角"扫码"按钮（弹出扫码窗，调摄像头）
- **请假管理**：列表 + 详情 + 审批
- **出勤统计看板**：嵌入校长仪表盘

---

### 4.6 跨模块的"全局体验"

| 体验 | 实现方式 |
|---|---|
| **全局搜索弹窗**（Ctrl/Cmd+K） | Radix Dialog；后端 `/api/search?q=...` 跨"学员 / 家长 / 班级 / 课次"四种实体；点击结果直接跳转并打开对应 Sheet |
| **详情侧边抽屉** | Radix Sheet；统一从右侧滑出；含元数据、标签、操作按钮 |
| **移动端筛选** | Radix Drawer（底部上滑） |
| **移动端导航** | Radix Sheet（左侧抽屉） |
| **任务待办面板** | 校长 / 班主任主页右侧；复选框乐观更新；Toast 撤销 |
| **KPI 数字动画** | 模块切换时数字从 0 滚动到目标值（react-countup 或自写 Hook） |
| **骨架屏** | 切换模块时 720 ms 的骨架占位；统一用 `<Skeleton />` 组件 |
| **Toast 通知** | 4.2 秒自动消失，含"撤销" |
| **键盘快捷键** | Ctrl/Cmd+K 全局搜索、Ctrl/Cmd+N 新建当前模块对象、`?` 打开快捷键帮助、Esc 关闭弹窗 |
| **响应式** | 桌面端 `<table>`；移动端卡片；导航在 `lg` 断点以下折叠为汉堡菜单 |
| **无障碍** | 所有按钮 `aria-label`；`aria-current="page"` 标当前模块；`aria-pressed` 标任务态；跳过导航链接；全局 `focus-visible` 焦点环 |

---

## 5. 数据模型（第一期）

### 5.1 核心 ER 图（精简）

```
Tenant ─< Branch ─< User ─< UserRole >─ Role ─< RolePermission >─ Permission
                    │
                    └── (data-scope) ── Branch
Branch ─< Student ─< StudentGuardianRelation >─ Guardian
       │           ─< StudentClassGroup >── ClassGroup
       │           ─< CoursePackage（一学员可多个）
       │           ─< Attendance ─── Lesson
       │           ─< LeaveRequest ─ Lesson
       │           ─< PickupRecord ─ Lesson
       └── ClassRoom
       └── Course ─< ClassGroup ─< Lesson
       └── ClassGroup ─< Lesson ─< LessonChangeLog
```

### 5.2 表清单与关键字段

> **每张表都包含的"通用列"**：
> `id (BIGINT 主键, 雪花 ID)`、`tenant_id (BIGINT, 默认 1)`、`branch_id (BIGINT, 业务实体相关时)`、`created_at`、`updated_at`、`deleted_at (软删除)`、`created_by`、`updated_by`、`version (乐观锁)`。
> 下表只列出"非通用"的关键字段。

| 表名 | 关键字段 | 备注 |
|---|---|---|
| `t_tenant` | `name`, `code`, `status` | 第一期固定 1 条 |
| `t_branch` | `name`, `code`, `address`, `phone` | 校区 |
| `t_user` | `username (UQ)`, `password_hash`, `name`, `phone`, `email`, `status`, `last_login_at` | 密码 BCrypt |
| `t_role` | `code (UQ)`, `name`, `is_builtin` | 内置角色不可删 |
| `t_permission` | `code (UQ)`, `name`, `module` | 形如 `student:read` |
| `t_user_role` | `user_id`, `role_id` | |
| `t_role_permission` | `role_id`, `permission_id` | |
| `t_user_branch` | `user_id`, `branch_id` | 数据范围 |
| `t_student` | `enroll_no (UQ within tenant)`, `name`, `gender`, `birthday`, `enroll_date`, `status`, `allergy`, `health_note`, `avatar_url` | 入园编号机构内唯一 |
| `t_guardian` | `name`, `phone`, `is_main_contact`, `can_pickup` | |
| `t_student_guardian_relation` | `student_id`, `guardian_id`, `relation` | 一户多生 |
| `t_course_package` | `student_id`, `total_lessons`, `remaining_lessons`, `expire_date`, `note` | 第一期手工录入 |
| `t_course` | `name`, `age_min`, `age_max`, `lesson_minutes`, `cover_url`, `description` | 课程产品 |
| `t_class_group` | `name`, `course_id`, `head_teacher_id`, `capacity`, `status` | 班级 |
| `t_class_room` | `name`, `capacity`, `note` | 画室 |
| `t_student_class_group` | `student_id`, `class_group_id`, `joined_at`, `left_at` | 学员↔班级 |
| `t_lesson` | `class_group_id`, `class_room_id`, `teacher_id`, `start_at`, `end_at`, `status`, `note` | 单次课次 |
| `t_lesson_change_log` | `lesson_id`, `change_type`, `before_json`, `after_json`, `reason`, `operator_id` | 调课审计 |
| `t_attendance` | `lesson_id`, `student_id`, `status`, `check_in_at`, `check_out_at`, `check_in_method`, `note` | 出勤 |
| `t_pickup_record` | `attendance_id`, `event_type (in/out)`, `guardian_id`, `is_abnormal`, `abnormal_note`, `event_time` | 接送 |
| `t_leave_request` | `student_id`, `lesson_id (nullable)`, `leave_date_range`, `reason`, `status`, `approved_by`, `approved_at` | 请假 |

### 5.3 `tenant_id` 预留策略

- **存储层**：每张业务表都有 `tenant_id BIGINT NOT NULL DEFAULT 1`，索引上**所有联合唯一键以 `tenant_id` 打头**（如 `UNIQUE(tenant_id, enroll_no)`）。
- **查询层**：MyBatis-Plus 自定义拦截器，从 `SecurityContext` 取 `tenantId` 自动追加 `WHERE tenant_id = ?`。第一期 `SecurityContext` 永远返回 1。
- **写入层**：插入时自动注入 `tenant_id`（用 MyBatis-Plus 的 `MetaObjectHandler`）。
- **未来切换 SaaS 时**：只需要把"租户解析"接口（`TenantResolver`）从"返回 1"改为"按域名 / 子路径 / 请求头解析"，并在登录页加上租户切换 / 注册流程，**业务代码零改动**。

---

## 6. 技术架构

### 6.1 部署形态

```
┌──────────────────────────────────────────┐
│        eduze-manage.jar (单进程)          │
│                                          │
│  ┌──────────────┐    ┌────────────────┐ │
│  │ Spring Boot  │    │ static/        │ │
│  │  - REST API  │ ←─ │  index.html    │ │
│  │   /api/**    │    │  assets/*.js   │ │
│  │  - Fallback  │ ─→ │  assets/*.css  │ │
│  │   非/api/**  │    │  (Vite 构建产物) │ │
│  └──────┬───────┘    └────────────────┘ │
│         │                                │
└─────────┼────────────────────────────────┘
          │
   ┌──────┴──────┐
   │   MySQL 8   │
   │   Redis 7   │
   │ (本地磁盘文件) │
   └─────────────┘
```

- **单 Jar**：Spring Boot 同时托管 REST API（`/api/**`）和前端静态资源
- **路由兜底**：注册一个 `WebMvcConfigurer`，把所有非 `/api/**` 的 GET 请求全部转发到 `/index.html`，由 React Router 接管
- **前端构建**：Vite 输出到 `web/dist`，Maven 在 `package` 阶段把 `web/dist/*` 拷贝到 `src/main/resources/static/`
- **本地开发**：双进程
  - 后端：Spring Boot 跑在 `:8080`
  - 前端：Vite dev server 跑在 `:5173`，配置 `proxy.'/api'` 转发到 `:8080`
  - 这样前端可以热更新，后端可以独立 debug

### 6.2 后端栈

| 项 | 选型 | 备注 |
|---|---|---|
| JDK | **Java 17 LTS** | |
| 框架 | **Spring Boot 3.2.5** | 不引入 Spring Cloud（第一期单体，避免微服务复杂度） |
| ORM | **MyBatis-Plus 3.5.15** | 配套 P6Spy 打 SQL 日志 |
| 数据库 | **MySQL 8** + **Redis 7** | Redis 用于：登录失败计数、JWT 黑名单、缓存字典 |
| 安全 | **Spring Security 6** + **JWT** | jjwt 实现；密码 BCrypt |
| 工具 | Lombok, MapStruct 1.5.5, Hutool 5.8.27, FastJSON2 2.0.60 | 成熟 Java 工具生态 |
| 文件 | 第一期本地磁盘；抽象 `FileStorage` 接口，未来切 OSS / MinIO 不改业务代码 | |
| 校验 | Jakarta Validation + 自定义 `@TenantUnique` 校验器 | |
| 文档 | **springdoc-openapi 2.x**（Swagger UI） | |
| 日志 | Logback + 按天滚动 + 操作审计日志（独立表 `t_audit_log`） | |
| 测试 | JUnit 5 + Mockito + Testcontainers（MySQL / Redis） | |
| 构建 | Maven | |

**显式不引入**：
- ❌ Spring Cloud / Spring Cloud Alibaba（第一期单体）
- ❌ Nacos / Sentinel / Seata（无微服务即无需）
- ❌ XXL-Job 等独立调度（第一期用 Spring `@Scheduled` 即可，未来再换）

### 6.3 前端栈

| 项 | 选型 | 备注 |
|---|---|---|
| 框架 | **React 18 + TypeScript 5** | |
| 构建 | **Vite 6** | |
| 样式 | **Tailwind CSS 3** | 设计 token 写在 `tailwind.config.ts` 里，HSL 变量化 |
| 组件 | **Radix UI Primitives**（Dialog / Sheet / NavigationMenu / Drawer / Popover / DropdownMenu / Toast / Tooltip） | 不引入 shadcn/ui 整套，按需自包一层 |
| 图标 | **Phosphor Icons (duotone)** | |
| 路由 | **React Router DOM v6** | |
| 状态 | **TanStack Query v5**（服务端状态）+ **Zustand**（少量客户端状态如侧栏折叠） | 不引入 Redux |
| 表单 | **React Hook Form** + **Zod** | |
| 表格 | **TanStack Table v8** | 表头排序、列固定、可见列控制 |
| 日期 | **date-fns** | |
| HTTP | **Axios**（统一拦截器：JWT、错误转 Toast、401 跳登录） | |
| 单测 | **Vitest** + **React Testing Library** | |
| 字体 | IBM Plex Sans / Noto Serif / IBM Plex Mono（自托管） | |

### 6.4 鉴权设计

- **登录**：`POST /api/auth/login` → 返回 `accessToken` (15 min) + `refreshToken` (7 d)
- **接口鉴权**：`Authorization: Bearer <accessToken>`
- **刷新**：`POST /api/auth/refresh`
- **登出**：`POST /api/auth/logout` → 把 `accessToken` jti 写入 Redis 黑名单直至过期
- **方法级权限**：`@PreAuthorize("hasAuthority('student:write')")`
- **数据范围**：MyBatis-Plus 拦截器读 `SecurityContext.userDetails.branchIds`，自动 SQL 拼接

### 6.5 文件存储

```java
public interface FileStorage {
    String save(InputStream is, String originalName, String bizCategory);
    InputStream load(String path);
    void delete(String path);
    String publicUrl(String path);
}
```

第一期实现：`LocalDiskFileStorage`（写到 `${eduze.storage.local-root}` 目录）
未来：`OssFileStorage` / `MinioFileStorage`，切换无侵入

### 6.6 消息中心

```java
public interface MessageSender {
    void send(MessageRequest req); // type=SMS|WX_TEMPLATE|MINI_PROGRAM_SUBSCRIBE
}
```

第一期：`LoggingMessageSender`（**只落库 + 打日志，不真发**），表 `t_outbox_message` 记录待发送队列
第二期：实现 `WxMiniProgramSubscribeMessageSender`，对接微信小程序订阅消息

---

## 7. 设计系统

### 7.1 字体

| 用途 | 字体 | 字重 |
|---|---|---|
| 正文 | **IBM Plex Sans** | 300 / 400 / 500 / 600 |
| 标题装饰 | **Noto Serif** | 500 / 700 |
| 代码 / ID | **IBM Plex Mono** | 400 / 500 |

字体托管：本地 self-host（放在 `/web/public/fonts/`），不依赖 Google Fonts CDN（避免境内访问问题）。

### 7.2 色彩 Token（HSL）

```ts
// tailwind.config.ts 节选
colors: {
  primary:    'hsl(12 82% 42%)',   // 橙红 - 主按钮、品牌
  'primary-fg': 'hsl(0 0% 100%)',
  secondary:  'hsl(12 72% 92%)',   // 浅橙红 - 辅助、选中态
  tertiary:   'hsl(210 78% 44%)',  // 蓝 - 第三级强调
  accent:     'hsl(250 60% 46%)',  // 紫 - 作业 / 家校（第二期）
  success:    'hsl(142 70% 38%)',  // 绿
  warning:    'hsl(38 92% 50%)',   // 琥珀
  error:      'hsl(0 78% 50%)',    // 红
  info:       'hsl(210 78% 50%)',  // 蓝
  background: 'hsl(220 20% 97%)',  // 冷白
  foreground: 'hsl(220 18% 14%)',
  muted:      'hsl(220 14% 96%)',
  'muted-fg': 'hsl(220 9% 45%)',
  border:     'hsl(220 13% 90%)',
}
```

通过 `:root { --primary: 12 82% 42%; ... }` 写成 CSS 变量，方便日后切主题（家长端可能用更童趣的色板）。

### 7.3 圆角 / 间距 / 阴影

| Token | 值 | 用途 |
|---|---|---|
| `rounded-sm` | 6 px | 标签 / 小按钮 |
| `rounded-md` | **10 px** | 输入框 / 按钮（默认） |
| `rounded-lg` | **14 px** | 卡片 / Sheet |
| `rounded-xl` | 20 px | 大卡片 / 模态框 |
| `shadow-sm` | 极轻 | 卡片默认 |
| `shadow-md` | 中等 | hover / 选中 |
| `shadow-lg` | 显著 | 弹层 / 抽屉 |

整体风格：**圆润但克制**，避免拟物。

### 7.4 通用组件清单（自包装层）

```
web/src/components/ui/
├─ Button.tsx          (variant: default|secondary|ghost|danger|link, size: sm|md|lg)
├─ Input.tsx
├─ Textarea.tsx
├─ Select.tsx          (Radix Select + 设计 token)
├─ Checkbox.tsx
├─ RadioGroup.tsx
├─ Switch.tsx
├─ DatePicker.tsx
├─ Dialog.tsx
├─ Sheet.tsx           (右侧抽屉)
├─ Drawer.tsx          (底部抽屉，移动端筛选用)
├─ Toast.tsx
├─ Tooltip.tsx
├─ DropdownMenu.tsx
├─ NavigationMenu.tsx
├─ Skeleton.tsx
├─ DataTable.tsx       (TanStack Table 包装)
├─ KPICard.tsx
├─ EmptyState.tsx
└─ Pagination.tsx
```

### 7.5 布局原型

```
┌──────────────────────────────────────────────────────┐
│  [Logo]   学员  课程  签到  ┃  搜索🔍   📨  👤      │ ← Sticky Header
├──────┬───────────────────────────────────────────────┤
│      │  [模块切换子头：KPI / 状态筛选 / 时间 / 导出]   │
│ 模块 │  ┌────┐ ┌────┐ ┌────┐ ┌────┐                │
│ 导航 │  │KPI │ │KPI │ │KPI │ │KPI │                │
│      │  └────┘ └────┘ └────┘ └────┘                │
│      │                                              │
│ ⌘    │  ┌──────────────────────────────────────┐    │
│ ⌘    │  │  数据表格 / 卡片 / 时间线 / 看板     │    │
│ ⌘    │  └──────────────────────────────────────┘    │
│      │                                              │
└──────┴───────────────────────────────────────────────┘
                            ↑
              （右侧 Sheet 抽屉滑出 - 详情）
```

### 7.6 响应式与无障碍

- **断点**：`sm 640 / md 768 / lg 1024 / xl 1280 / 2xl 1536`
- **桌面端**（`lg+`）：表格 + 侧栏导航
- **平板**（`md`–`lg`）：表格 + 折叠侧栏
- **移动端**（< `md`）：卡片列表 + 汉堡菜单 Sheet + 底部 Drawer 筛选
- **无障碍**：
  - 所有交互按钮带 `aria-label`
  - `aria-current="page"` 标当前选中模块
  - `aria-pressed` 标任务完成态
  - 跳过导航链接（Skip to main content）
  - 全局 `focus-visible` 焦点环（用 Tailwind 的 `ring-2 ring-primary/40`）
  - Radix UI 原生支持的 ARIA 属性全部启用

---

## 8. 工程结构与构建

### 8.1 单仓库布局

```
eduze-manage/
├─ pom.xml                                ← 后端 Maven
├─ src/
│  ├─ main/
│  │  ├─ java/com/eduze/manage/
│  │  │  ├─ EduzeManageApplication.java
│  │  │  ├─ common/        ← 通用：响应包装、异常、拦截器、工具
│  │  │  ├─ config/        ← Security / Redis / MyBatis-Plus 配置
│  │  │  ├─ tenant/        ← TenantContext / TenantInterceptor
│  │  │  ├─ infra/         ← 基础设施：FileStorage / MessageSender
│  │  │  ├─ auth/          ← 登录 / JWT / 用户 / 角色 / 权限
│  │  │  ├─ branch/        ← 校区
│  │  │  ├─ student/       ← 学员 + 家长 + 课时包
│  │  │  ├─ course/        ← 课程 + 班级 + 画室
│  │  │  ├─ lesson/        ← 课次 + 排课
│  │  │  ├─ attendance/    ← 签到 + 接送 + 请假
│  │  │  └─ search/        ← 全局搜索
│  │  └─ resources/
│  │     ├─ application.yml
│  │     ├─ db/migration/  ← Flyway SQL
│  │     └─ static/        ← 自动接收前端构建产物（gitignore）
│  └─ test/
├─ web/                                   ← 前端子工程
│  ├─ package.json
│  ├─ vite.config.ts
│  ├─ tailwind.config.ts
│  ├─ tsconfig.json
│  ├─ index.html
│  ├─ public/fonts/
│  └─ src/
│     ├─ main.tsx
│     ├─ app/
│     │  ├─ router.tsx
│     │  ├─ shell/         ← 后台壳：Header / Sidebar / Layout
│     │  └─ providers/     ← Query / Toast / Theme
│     ├─ components/ui/    ← 通用组件
│     ├─ features/
│     │  ├─ auth/
│     │  ├─ student/
│     │  ├─ course/
│     │  ├─ lesson/
│     │  └─ attendance/
│     ├─ lib/              ← axios / dayjs / 工具
│     ├─ hooks/
│     ├─ types/            ← 由后端 OpenAPI 生成（可选）
│     └─ styles/
├─ docs/socrates/specs/                   ← 设计 spec
└─ scripts/                               ← 部署 / 备份脚本
```

### 8.2 后端模块划分原则

- 按"业务能力"分包，**不按"层"分包**（不要 `controller/service/dao` 顶层包）
- 每个业务包内：`controller / service / mapper / domain / dto / mapstruct`
- 跨包依赖只允许：通过 `service` 接口调用，禁止跨包直接 `mapper` 调用

### 8.3 前端目录划分原则

- `app/`：应用级（路由、壳、Providers）
- `components/ui/`：纯 UI 组件，无业务逻辑
- `features/<module>/`：业务功能，每个模块自包含 `pages / components / hooks / api / types`
- `lib/` / `hooks/`：跨模块复用工具

### 8.4 Maven 联动 npm（生产构建）

`pom.xml` 用 `frontend-maven-plugin` 在 `generate-resources` 阶段：
1. 安装 Node 20 + pnpm
2. `cd web && pnpm install --frozen-lockfile`
3. `cd web && pnpm build`
4. `maven-resources-plugin` 把 `web/dist/*` 拷贝到 `src/main/resources/static/`
5. 然后正常 `package` → 单 jar

### 8.5 本地开发模式

- **后端**：`./mvnw spring-boot:run`，跑在 `:8080`
- **前端**：`cd web && pnpm dev`，跑在 `:5173`，`vite.config.ts` 配置：
  ```ts
  server: { proxy: { '/api': 'http://localhost:8080' } }
  ```
- 前端开发改样式 / 改逻辑无需重启后端

---

## 9. 非功能需求

### 9.1 性能与容量目标（第一期）

| 指标 | 目标 |
|---|---|
| 在读学员数 | 5 校区 × 500 = 2500（设计上限留 5 倍冗余） |
| 单日课次数 | 5 校区 × 30 课次 = 150 |
| 单日签到数 | 2500 × 1 = 2500 |
| 后台并发用户 | 50（前台 / 老师 / 校长同时在线） |
| 列表页 P95 | < 500 ms |
| 周课表加载 P95 | < 800 ms |
| 数据库 | 单实例 MySQL 8 即可（无需主从） |

### 9.2 安全与少儿数据合规

- **密码**：BCrypt cost=10
- **传输**：HTTPS（部署层 Nginx 终结 TLS）
- **JWT**：HS256；Secret 从环境变量读取；Access Token 15 分钟，Refresh Token 7 天
- **少儿数据**：
  - 学员姓名 / 生日 / 家长手机号属于敏感信息，列表页可配置"脱敏开关"（如手机号 138****1234）
  - 操作审计：所有"读取学员明细"、"导出学员"操作进入 `t_audit_log`
  - 未来如做家长端，需明确《儿童个人信息网络保护规定》合规审查
- **接口防护**：限流（同 IP 1 分钟 60 次登录尝试）、防重放（关键接口加幂等 key）

### 9.3 可用性与部署

- **第一期部署**：单机 Docker Compose
  - `app`（eduze-manage.jar） + `mysql` + `redis` + `nginx`（TLS 终结、静态 gzip）
- **数据备份**：每天凌晨 mysqldump 到对象存储或独立硬盘，保留 30 天
- **未来升级路径**：
  - 单机 → 多实例（Token 鉴权天然无状态，文件改 OSS 即可水平扩展）
  - 单机 → K8s（无侵入，加 Helm Chart）

### 9.4 可观测性

- **日志**：Logback 输出到 `logs/`，按天滚动，保留 30 天；error 级别接入飞书 / 钉钉机器人（可选）
- **健康检查**：`/actuator/health`，`/actuator/info`
- **指标**：`/actuator/prometheus`（micrometer），第一期不强制接 Prometheus，但端口暴露好
- **审计日志**：表 `t_audit_log`（who / when / what action / on which entity / ip）

---

## 10. 路线图

| 期次 | 目标 | 主要交付 |
|---|---|---|
| **第一期**（本 spec） | 教务日常运营底座 | 模块 0 + #2 + #3 + #4 |
| **第二期** | **美术差异化 + 家长触达** | #5 课堂与作品 + 微信小程序家长端（看课表 / 看作品 / 收消息） + 人脸签到（可选） |
| **第三期** | **赚钱链路** | #7 财务与订单（课时包销售、缴费、退费、电子收据、对账、教师课时费结算） |
| **第四期** | **招生** | #1 招生与试听（线索池、试听课、转化漏斗） |
| **第五期** | **家校沟通完整版** | #6 公告 / 班级群消息 / 阶段成长报告 / 续费提醒；可视情况叠加运营自动化 |

每一期都会独立走 brainstorm → spec → plan → implementation 循环，本 spec 为"第一期 + 全局蓝图"。

---

## 11. 假设、风险、待定

### 11.1 假设（Assumptions）

| # | 假设 | 影响 |
|---|---|---|
| A1 | 机构网络环境稳定，本地部署即可 | 不需要 CDN / 多区域 |
| A2 | 家长用微信生态（公众号 / 小程序）触达 | 不做 App、不做短信营销 |
| A3 | 老师有智能手机或前台有 PC + 手持扫码设备 | 签到方案可用 |
| A4 | 第一期不超过 5 个校区 | 单 MySQL 实例够用 |
| A5 | 三年内不做 SaaS 商业化 | tenant_id 仅预留，UI 不暴露；若提前商业化需补 0.5–1 期 |

### 11.2 风险（Risks）

| # | 风险 | 缓解 |
|---|---|---|
| R1 | 排课冲突逻辑可能复杂（教师 / 画室 / 班级三维冲突） | 第一期实现"软警告"（弹窗提示但不强制阻止），第二期再做强校验 |
| R2 | 课时包数据手工录入易错 | 加入"批量导入 + 双人复核"流程；第三期上销售流程后自动化 |
| R3 | 二维码扫码签到依赖前台手持设备 | 第一期保留"手动勾选"作为兜底 |
| R4 | 少儿合规要求未来可能加严 | 数据模型预留软删除 + 审计；接到合规要求时可补做"被遗忘权"删除 |
| R5 | 前端 Vite 6 + React 18 与未来 Vite 7 / React 19 升级 | 第一期锁版本，升级走独立技术债任务 |

### 11.3 待定（TBD）

| # | 项 | 期望何时确定 |
|---|---|---|
| T1 | 是否需要打印纸质收据 / 报到证 | 第三期前 |
| T2 | 是否对接钉钉 / 企微作为内部沟通工具 | 第二期前 |
| T3 | 校长 / 老师是否需要自己的工作台首页 | 第一期实施前确定布局 |
| T4 | 是否需要"试听课"在第一期保留一个极简版（仅录入） | 第一期实施前确定 |
| T5 | OSS / MinIO 选型（第二期上作品时定） | 第二期 |

---

## 附录 A：术语表

| 术语 | 含义 |
|---|---|
| **机构 / 租户**（Tenant） | 一个独立的教培机构。第一期固定为 1 个。 |
| **校区**（Branch） | 一个机构下的物理校区。 |
| **课程产品**（Course） | 一种课程的元数据，如"启蒙·4–6 岁"。 |
| **班级**（ClassGroup） | 课程的具体开班实例，如"周六上午启蒙 A 班"。 |
| **画室**（ClassRoom） | 一个上课的物理教室。 |
| **课次**（Lesson） | 班级在某个具体日期 / 时段的一节课。 |
| **课时包**（CoursePackage） | 学员剩余课时的容器（10/20/50 节等）。 |
| **接送人**（Pickup Guardian） | 当天负责接送的家长 / 监护人。 |
| **入园编号**（Enroll No） | 机构内的学员业务编号，机构内唯一。 |

---

## 附录 B：关键设计取舍记录

记录本设计中的关键取舍，方便后续迭代和团队成员快速理解"**为什么这么做 / 不这么做**"。

### B.1 为什么定位"线下少儿美术启蒙"而非通用教培

- 美术教培和文化课教培业务流截然不同：美术核心是「作品」，文化课核心是「考分」
- 少儿美术付费 / 沟通主体是**家长**，与美考集训（学员自己）、成人兴趣（学员自己）不同，UI / 通知 / 角色全都不一样
- 通用化反而会让每个场景都不顺手；先做最聚焦的版本，后续按需扩展

### B.2 为什么"前后端不分离"采用单 Jar 同包部署，而非服务端模板渲染

- 服务端模板（Thymeleaf / Freemarker）在复杂交互（弹窗 / 抽屉 / 实时筛选）下体验和维护成本都不优
- 单 Jar 部署 = "工程上前后端不分离"（同一进程、同一构建产物、同一份 OPS）+ "代码上前后端分离"（前端独立 `web/` 子工程）
- 兼顾"不想运维两个进程"与"现代前端开发体验"

### B.3 为什么不引入 Spring Cloud / 微服务

- 第一期单体足够覆盖业务；微服务会大幅推高复杂度（注册中心、配置中心、链路追踪、分布式事务、服务间通信全要管）
- 中小教培机构的 QPS 离需要微服务还差几个数量级
- 业务边界先在单体里跑清楚，未来真要拆，按"业务能力"拆服务即可，不是不可逆决策

### B.4 为什么不做「线上课程」模块

- 本机构是**线下**少儿美术，"线上课程"短期内无业务场景
- 一旦引入会带来直播 / 推流 / 互动 / 录像等大量复杂度，与第一期目标不符
- 未来若上录播课作为线下补充，会作为独立期次单独评估

### B.5 为什么把「作业督学」重塑为「课堂与作品」

- 少儿美术不是"留作业回家做"，而是"**课堂上出作品**"
- 老师的核心动作不是"批改"，而是"**拍照 → 点评 → 推送家长**"
- 作品集是续费抓手（家长付钱不是为"上了 N 节课"，而是为"看到孩子作品在变好"）
- 因此整个模块的数据模型、UI、推送策略都需要按"作品"重新设计

### B.6 为什么人脸签到不放第一期

- 依赖硬件（人脸闸机或带摄像头的平板）和家长授权采集人脸数据
- 第一期"前台手动勾选 + 二维码扫码"已能覆盖签到刚需
- 数据模型、接口都预留扩展位，第二期视设备和预算决定是否上线
- 避免在 MVP 阶段被硬件采购 / 合规审批拖住进度

### B.7 为什么 `tenant_id` 数据库结构预留但 UI 不暴露

- 用户当前是"自用"，三年内未确定是否做 SaaS（详见 §11.1 假设 A5）
- 如果一开始按单租户写、未来再加多租户，等于重构数据库
- 反过来，按多租户结构 + 单租户 UI 实现，工程成本仅约 +5%（一个拦截器 + 一个 MetaObjectHandler），但保留了 SaaS 升级的逆向兼容性
- 这是"低成本买可选权"的典型决策

### B.8 为什么把 7 个模块拆成 5 期上线，而不是一次做完

- 少儿美术机构最痛的是"教务日常运营"（排课 + 签到 + 学员花名册），其他模块在没有系统时都有 Excel / 微信群兜底
- 第一期就把"日常运营"做扎实，机构每天都能用，是真正的 PMF
- "招生"、"财务"涉及外部集成（短信通道、支付通道）和合规审查，**不应阻塞**第一期上线
- "课堂与作品" + "家长端" 是第二期一起上的杀手锏，**不能拆开**——只做家长端没作品看 / 只做作品没家长端推送，都是浪费

### B.9 为什么前端不引入 shadcn/ui 整套，而是按需自包

- shadcn/ui 把 Radix Primitives + Tailwind 组合好，开箱即用，但样式风格偏硅谷工程师审美
- 本设计有强烈的品牌色彩 token（橙红主色 / 圆润但克制）和字体系统（IBM Plex + Noto Serif 混排），需要自己控制每个组件的样式
- 自包装层只引入 Radix Primitives（无样式行为层）+ 自己写 Tailwind 样式，包出符合 §7 设计 token 的组件，更可控、更轻量

---

**（文档结束）**
