# EduZE Manage 第一期 MVP 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use socrates:subagent-driven-development (recommended) or socrates:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 从零搭建 EduZE Manage 第一期 MVP：覆盖账号 / 权限 / 校区底座、学员与家长、课程与排课、签到与接送四大业务模块，以及全局体验（设计系统、全局搜索、Toast、键盘快捷键、骨架屏、响应式、无障碍）。

**Architecture:** 单仓库 Spring Boot 3.2.5 + Java 17（根目录） + React 18 + Vite 6（`web/` 子目录），生产环境 Maven 联动 npm 构建产出 **单 Jar 同包部署**（`/api/**` 为 REST，其余 fallback 到 `index.html`）。所有业务表预留 `tenant_id`，第一期固定为 1，但拦截器、唯一键、字段注入按 SaaS 多租户结构落地，未来切租户零业务改动。

**Tech Stack:**
- 后端：Spring Boot 3.2.5、Spring Security 6 + JJWT、MyBatis-Plus 3.5.15 + P6Spy、MySQL 8、Redis 7、Flyway、Lombok、MapStruct 1.5.5、Hutool、FastJSON2、springdoc-openapi 2.x、JUnit 5 + Mockito + Testcontainers
- 前端：React 18 + TypeScript 5、Vite 6、Tailwind CSS 3、Radix UI Primitives、Phosphor Icons (duotone)、React Router DOM v6、TanStack Query v5 + Zustand、React Hook Form + Zod、TanStack Table v8、date-fns、Axios、Vitest + React Testing Library
- 字体（self-host）：IBM Plex Sans / Noto Serif / IBM Plex Mono
- 部署：Docker Compose（app + mysql + redis + nginx）

**对应 spec：** `docs/socrates/specs/2026-05-09-eduze-manage-design.md`（v1.0），第一期范围见 §4.1，技术栈见 §6，工程结构见 §8，数据模型见 §5，设计系统见 §7。

---

## 0. 实施总览

### 0.1 阶段地图（先地基 → 后业务）

| 阶段 | 目标 | 任务编号 | 累计任务数 |
|---|---|---|---|
| **A. 工程脚手架** | Maven + Spring Boot + Vite + Maven-npm 联动 + Flyway + 通用层 | A1–A8 | 8 |
| **B. 测试基建 + 租户底座** | Testcontainers + TenantContext + MyBatis-Plus 拦截器 + 审计 | B1–B4 | 12 |
| **C. 设计系统落地** | Tailwind / 字体 / `web/src/components/ui/` 通用组件库 | C1–C8 | 20 |
| **D. 模块 0：账号 / 权限 / 校区** | 后端 RBAC + JWT + 数据范围拦截器 | D1–D8 | 28 |
| **E. 后台壳** | 登录页 + Layout + 全局搜索弹窗 + Toast + 快捷键 + 移动端 | E1–E7 | 35 |
| **F. 模块 #2 学员与家长** | 学员 / 家长 / 关联 / 课时包 + 列表 + 详情 + 导入 | F1–F10 | 45 |
| **G. 模块 #3 课程与排课** | 课程 / 班级 / 画室 / 课次 + 周课表 + 调课 + 冲突检测 | G1–G10 | 55 |
| **H. 模块 #4 签到与接送** | 签到 / 接送 / 请假 + 工作台 + 扫码 + 出勤统计 | H1–H10 | 65 |
| **I. 联调 / 部署** | 全局搜索串接 + Docker Compose + 备份脚本 + E2E 冒烟 | I1–I5 | 70 |

总计 **70 个任务**，每个任务粒度 ≤ 1 个 PR / ≤ 1 天工作量。

### 0.2 文件结构（落地决策）

#### 0.2.1 后端目录（按业务能力分包，禁止 controller / service / dao 顶层包）

```
src/main/java/com/eduze/manage/
├─ EduzeManageApplication.java
├─ common/
│  ├─ web/             ApiResponse, ApiError, ResponseAdvice
│  ├─ exception/       BizException, ErrorCode, GlobalExceptionHandler
│  ├─ entity/          BaseEntity (id, tenant_id, branch_id, audit, version, deleted_at)
│  ├─ mybatis/         MetaObjectHandler, P6SpyConfig
│  └─ util/            JsonUtil, IdGenerator (Snowflake)
├─ config/
│  ├─ MybatisPlusConfig.java
│  ├─ RedisConfig.java
│  ├─ SecurityConfig.java
│  ├─ JwtFilter.java
│  ├─ WebMvcConfig.java   （SPA fallback）
│  └─ OpenApiConfig.java
├─ tenant/
│  ├─ TenantContext.java
│  ├─ TenantResolver.java
│  └─ TenantInterceptor.java
├─ infra/
│  ├─ storage/         FileStorage 接口 + LocalDiskFileStorage
│  └─ message/         MessageSender 接口 + LoggingMessageSender + t_outbox_message
├─ audit/
│  ├─ AuditLog.java（注解）+ AuditAspect.java + AuditLogMapper + t_audit_log 实体
├─ auth/
│  ├─ controller/AuthController, UserController, RoleController, PermissionController
│  ├─ service/AuthService, UserService, RoleService, PermissionService, JwtService, LoginAttemptService
│  ├─ domain/User, Role, Permission, UserRole, RolePermission, UserBranch
│  ├─ mapper/...
│  ├─ dto/LoginRequest, LoginResponse, RefreshRequest, ...
│  └─ security/CustomUserDetails, CustomUserDetailsService
├─ branch/
│  ├─ controller/BranchController
│  ├─ service/BranchService
│  ├─ domain/Branch
│  ├─ mapper/BranchMapper
│  └─ dto/BranchRequest, BranchResponse
├─ student/
│  ├─ controller/StudentController, GuardianController, CoursePackageController, StudentImportController
│  ├─ service/StudentService, GuardianService, CoursePackageService, StudentImportService
│  ├─ domain/Student, Guardian, StudentGuardianRelation, CoursePackage
│  ├─ mapper/...
│  └─ dto/...
├─ course/
│  ├─ controller/CourseController, ClassGroupController, ClassRoomController
│  ├─ service/CourseService, ClassGroupService, ClassRoomService
│  ├─ domain/Course, ClassGroup, ClassRoom, StudentClassGroup
│  ├─ mapper/...
│  └─ dto/...
├─ lesson/
│  ├─ controller/LessonController, ScheduleController
│  ├─ service/LessonService, ScheduleService, ConflictService
│  ├─ domain/Lesson, LessonChangeLog
│  ├─ mapper/...
│  └─ dto/...
├─ attendance/
│  ├─ controller/AttendanceController, PickupController, LeaveController
│  ├─ service/AttendanceService, PickupService, LeaveService, AttendanceStatService, AbsenceJob
│  ├─ domain/Attendance, PickupRecord, LeaveRequest
│  ├─ mapper/...
│  └─ dto/...
└─ search/
   ├─ controller/SearchController
   └─ service/SearchService（聚合学员 / 家长 / 班级 / 课次）

src/main/resources/
├─ application.yml
├─ application-dev.yml
├─ application-prod.yml
├─ db/migration/
│  ├─ V1.0.0__init_tenant_branch.sql
│  ├─ V1.0.1__init_auth.sql
│  ├─ V1.0.2__init_audit.sql
│  ├─ V1.1.0__init_student.sql
│  ├─ V1.2.0__init_course.sql
│  ├─ V1.3.0__init_attendance.sql
│  ├─ V1.4.0__init_outbox.sql
│  └─ V9.0.0__seed_builtin.sql
└─ static/                ← .gitignore，构建期由前端拷贝填充

src/test/
├─ java/com/eduze/manage/
│  ├─ AbstractITContainerTest.java（@Testcontainers 抽象基类）
│  ├─ auth/...
│  ├─ student/...
│  ├─ course/...
│  ├─ lesson/...
│  └─ attendance/...
```

#### 0.2.2 前端目录（按 feature 分文件夹，跨模块复用进 lib/）

```
web/
├─ package.json
├─ pnpm-lock.yaml
├─ vite.config.ts
├─ tsconfig.json
├─ tsconfig.node.json
├─ tailwind.config.ts
├─ postcss.config.js
├─ index.html
├─ public/
│  └─ fonts/             IBM Plex Sans / Noto Serif / IBM Plex Mono（self-host）
└─ src/
   ├─ main.tsx
   ├─ env.d.ts
   ├─ styles/
   │  ├─ globals.css     （Tailwind + CSS 变量 + focus-visible）
   │  └─ fonts.css       （@font-face）
   ├─ app/
   │  ├─ router.tsx
   │  ├─ providers/
   │  │  ├─ QueryProvider.tsx
   │  │  ├─ ToastProvider.tsx
   │  │  ├─ AuthProvider.tsx
   │  │  └─ ShortcutProvider.tsx
   │  └─ shell/
   │     ├─ AppLayout.tsx
   │     ├─ Header.tsx
   │     ├─ Sidebar.tsx
   │     ├─ MobileNav.tsx
   │     ├─ CommandPalette.tsx     （Ctrl/Cmd+K 全局搜索）
   │     └─ ShortcutHelpDialog.tsx （? 帮助弹窗）
   ├─ components/ui/
   │  ├─ Button.tsx
   │  ├─ Input.tsx
   │  ├─ Textarea.tsx
   │  ├─ Label.tsx
   │  ├─ Select.tsx
   │  ├─ Checkbox.tsx
   │  ├─ RadioGroup.tsx
   │  ├─ Switch.tsx
   │  ├─ DatePicker.tsx
   │  ├─ Dialog.tsx
   │  ├─ Sheet.tsx
   │  ├─ Drawer.tsx
   │  ├─ Tooltip.tsx
   │  ├─ DropdownMenu.tsx
   │  ├─ NavigationMenu.tsx
   │  ├─ Toast.tsx              （内部，由 ToastProvider 渲染）
   │  ├─ Skeleton.tsx
   │  ├─ EmptyState.tsx
   │  ├─ Pagination.tsx
   │  ├─ KPICard.tsx
   │  └─ DataTable.tsx          （TanStack Table v8 包装）
   ├─ features/
   │  ├─ auth/
   │  │  ├─ pages/LoginPage.tsx
   │  │  ├─ api.ts
   │  │  ├─ store.ts            （Zustand：accessToken / user / branchIds / permissions）
   │  │  └─ hooks/useAuth.ts
   │  ├─ student/
   │  │  ├─ pages/StudentListPage.tsx
   │  │  ├─ components/StudentDetailSheet.tsx
   │  │  ├─ components/StudentFormDialog.tsx
   │  │  ├─ components/GuardianSubForm.tsx
   │  │  ├─ components/CoursePackagePanel.tsx
   │  │  ├─ components/StudentImportDialog.tsx
   │  │  ├─ api.ts
   │  │  ├─ types.ts
   │  │  └─ schemas.ts          （Zod schema）
   │  ├─ course/
   │  │  ├─ pages/CourseListPage.tsx
   │  │  ├─ pages/ClassGroupListPage.tsx
   │  │  ├─ pages/ClassRoomListPage.tsx
   │  │  ├─ components/ClassGroupDetailSheet.tsx
   │  │  ├─ api.ts
   │  │  └─ types.ts
   │  ├─ lesson/
   │  │  ├─ pages/WeeklySchedulePage.tsx
   │  │  ├─ components/WeeklyGrid.tsx
   │  │  ├─ components/LessonDetailSheet.tsx
   │  │  ├─ components/RescheduleDialog.tsx
   │  │  ├─ components/BulkGenerateDialog.tsx
   │  │  ├─ api.ts
   │  │  └─ types.ts
   │  └─ attendance/
   │     ├─ pages/AttendanceWorkbenchPage.tsx
   │     ├─ pages/LeaveListPage.tsx
   │     ├─ pages/AttendanceStatsPage.tsx
   │     ├─ components/QrScanDialog.tsx
   │     ├─ components/PickupSelectDialog.tsx
   │     ├─ components/LeaveApprovalDialog.tsx
   │     ├─ api.ts
   │     └─ types.ts
   ├─ lib/
   │  ├─ axios.ts              （拦截器：JWT、401、Toast、refresh）
   │  ├─ queryClient.ts
   │  ├─ format.ts             （日期 / 手机号脱敏）
   │  ├─ permissions.ts
   │  └─ kbd.ts                （快捷键工具：matchHotkey）
   ├─ hooks/
   │  ├─ useShortcut.ts
   │  ├─ useDebouncedValue.ts
   │  ├─ usePagedQuery.ts
   │  └─ useUrlState.ts        （筛选条件 ↔ URL searchParams 同步）
   └─ types/
      └─ api.ts                （后端 OpenAPI 生成或手写）
```

#### 0.2.3 根目录与脚本

```
eduze-manage/
├─ pom.xml
├─ mvnw / mvnw.cmd
├─ .mvn/wrapper/maven-wrapper.properties
├─ .gitignore
├─ .editorconfig
├─ README.md
├─ docker/
│  ├─ docker-compose.yml
│  ├─ docker-compose.dev.yml      （只跑 mysql + redis 给本地开发）
│  ├─ Dockerfile
│  └─ nginx.conf
├─ scripts/
│  ├─ backup-mysql.sh
│  └─ restore-mysql.sh
└─ docs/socrates/
   ├─ specs/2026-05-09-eduze-manage-design.md
   └─ plans/2026-05-09-eduze-manage-phase1-plan.md  ← 本文件
```

### 0.2.4 本地开发环境实测值（dev profile 默认值）

> 本地开发机已就绪以下服务，A 阶段所有任务均直接连本机服务即可，无需 Docker。Docker Compose（I3）面向生产 / 单机部署。

| 类别 | 项 | 值 |
|---|---|---|
| **MySQL 8.4.9** | host | `127.0.0.1` |
| | port | `3306` |
| | database | `eduze`（utf8mb4，A4 任务前手工建库） |
| | user / password | `root` / `root123` |
| | 连接 URL | `jdbc:mysql://127.0.0.1:3306/eduze?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true` |
| **Redis 7.4.9** | host | `127.0.0.1` |
| | port | `6379` |
| | password | `redis123` |
| | database | `0` |
| | 持久化 | AOF 已开启（应用层无需关心） |
| **应用** | 端口 | `8080` |
| | 默认 profile | `dev` |
| | JWT secret (dev) | `dev-only-secret-change-me-in-prod-32bytes-min` |
| | 文件存储根目录 | `./storage`（相对工作目录） |

**强制要求：**
- 这些值仅作为 `application-dev.yml` 的 `${ENV:default}` 默认值出现，**不得**在 `application-prod.yml` 或代码常量里硬编码。
- prod profile 必须强制走环境变量：`DB_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET` 缺失即启动失败（A2 任务 `AppProperties.@PostConstruct` 校验）。
- 单元测试 / Testcontainers（B1）不依赖本机服务，自带容器随用随起，与上述值无关。

### 0.3 全局约定

- **提交习惯**：每个 Task 完成后立即 commit；commit message 用 `feat(<scope>): ...` / `chore(<scope>): ...` / `test(<scope>): ...`，scope 用任务前缀（`scaffold`、`tenant`、`auth`、`student`、`course`、`lesson`、`attendance`、`ui`、`shell`、`deploy`）。
- **TDD 约定**：所有业务 service / 拦截器 / 校验器先写 Testcontainers 集成测试或 MockMvc 测试，再写实现；通用 UI 组件用 Vitest + RTL 配合 Storybook 风格的渲染测试。
- **依赖版本锁定**：后端 `pom.xml` 用 `<dependencyManagement>` 集中锁定，所有版本号显式写明，禁止用 `LATEST`。前端 `package.json` 用 pnpm + `--frozen-lockfile`，commit `pnpm-lock.yaml`。
- **代码风格**：后端 Lombok + 不写 `@Data`（用 `@Getter @Setter` 显式控制）；前端 ESLint + Prettier 默认规则，禁用 `any`，全量 `strict: true`。
- **日志规范**：业务操作日志用 SLF4J `info`；异常用 `error`；调试用 `debug`；P6Spy 仅在 `dev` profile 启用。
- **国际化**：第一期界面文案全部中文硬编码，但保留 i18n 抽离能力（前端文案集中放到 `web/src/i18n/zh-CN.ts`，第一期可仅一份字典）。

---

## 阶段 A：工程脚手架

### Task A1：初始化 Maven 工程 + Spring Boot 主类

**Spec 章节：** §6.1（部署形态）、§6.2（后端栈）、§8.1（单仓库布局）

**前置任务：** 无（最起始任务）

**Files：**
- 新建：`pom.xml`、`mvnw`、`mvnw.cmd`、`.mvn/wrapper/maven-wrapper.properties`
- 新建：`src/main/java/com/eduze/manage/EduzeManageApplication.java`
- 新建：`src/main/resources/application.yml`、`src/main/resources/banner.txt`
- 新建：`.gitignore`、`.editorconfig`、`README.md`

**实施要点：**
- `pom.xml` 父 pom 用 `spring-boot-starter-parent:3.2.5`，`<java.version>17</java.version>`
- 引入：`spring-boot-starter-web`、`spring-boot-starter-validation`、`spring-boot-starter-actuator`、`lombok`（provided）、`mapstruct:1.5.5.Final` + `mapstruct-processor`、`hutool-all:5.8.27`、`fastjson2:2.0.60`
- `EduzeManageApplication`：`@SpringBootApplication` + `@MapperScan("com.eduze.manage.**.mapper")`（mapper 在 A5 引入）
- `application.yml`：`server.port=8080`、`spring.application.name=eduze-manage`、`management.endpoints.web.exposure.include=health,info,prometheus`
- `.gitignore`：忽略 `target/`、`logs/`、`*.iml`、`.idea/`、`web/node_modules/`、`web/dist/`、`src/main/resources/static/**`（前端构建产出由 Maven 生成，不入仓）

**验收标准：**
- `./mvnw clean compile` 成功
- `./mvnw spring-boot:run` 启动后 `curl http://localhost:8080/actuator/health` 返回 `{"status":"UP"}`
- `git status` 干净，构建产物全部被忽略

**预估粒度：** 0.5 天

---

### Task A2：多 profile 配置 + 环境变量映射

**Spec 章节：** §6.4（鉴权 secret 走环境变量）、§9.2（密钥从环境读取）、§9.3（部署）

**前置任务：** A1

**Files：**
- 修改：`src/main/resources/application.yml`
- 新建：`src/main/resources/application-dev.yml`、`src/main/resources/application-prod.yml`
- 新建：`src/main/java/com/eduze/manage/common/config/AppProperties.java`

**实施要点：**
- 默认 `spring.profiles.active=dev`，生产用 `SPRING_PROFILES_ACTIVE=prod` 注入
- `AppProperties` 用 `@ConfigurationProperties(prefix="eduze")`，包含：`jwt.secret`、`jwt.access-ttl-min`、`jwt.refresh-ttl-day`、`storage.local-root`、`tenant.default-id`
- 敏感配置（DB 密码、Redis 密码、JWT secret）用 `${ENV_NAME:default}` 占位，dev 给可用默认，prod 强制环境注入

**本地开发环境固化值（dev profile 默认值，已与本机服务对齐）：**

| 项 | 值 | 备注 |
|---|---|---|
| MySQL host | `127.0.0.1` | MySQL 8.4.9 |
| MySQL port | `3306` | |
| MySQL database | `eduze` | utf8mb4 |
| MySQL user | `root` | |
| MySQL password | `root123` | 仅 dev 默认；prod 必须走 `${DB_PASSWORD}` |
| Redis host | `127.0.0.1` | Redis 7.4.9，AOF 已开启 |
| Redis port | `6379` | |
| Redis password | `redis123` | 仅 dev 默认；prod 必须走 `${REDIS_PASSWORD}` |
| Redis database | `0` | |
| 服务端口 | `8080` | |

`application-dev.yml` 关键片段（A4 / A6 任务在此基础上分别补 datasource 与 redis 节）：

```yaml
spring:
  application:
    name: eduze-manage
  datasource:
    url: jdbc:mysql://${DB_HOST:127.0.0.1}:${DB_PORT:3306}/${DB_NAME:eduze}?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root123}
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:redis123}
      database: ${REDIS_DB:0}
      timeout: 3s
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2

eduze:
  tenant:
    default-id: 1
  jwt:
    secret: ${JWT_SECRET:dev-only-secret-change-me-in-prod-32bytes-min}
    access-ttl-min: 15
    refresh-ttl-day: 7
  storage:
    local-root: ${EDUZE_STORAGE_LOCAL_ROOT:./storage}
```

`application-prod.yml` 不写默认值，只列结构（`${DB_PASSWORD}`、`${REDIS_PASSWORD}`、`${JWT_SECRET}` 不带默认 → 缺失即启动失败）。

**注意 MySQL 8.4 兼容性：**
- 8.4 的默认认证插件是 `caching_sha2_password`，URL 中已带 `allowPublicKeyRetrieval=true` 必需保留。
- mysql-connector-j 用 `8.4.0`（A4 任务的依赖版本不变）即可对上 8.4 服务器。
- 字符集统一 `utf8mb4`（不是 `utf8`），URL 与建库语句都按此走。

**验收标准：**
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` 启动正常（连上本机 MySQL/Redis 不报错）
- `JWT_SECRET=test ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod` 启动时因缺 `DB_PASSWORD` 抛错（证明 prod 强制注入生效）
- 缺失 `JWT_SECRET` 时 prod profile 启动时抛 `IllegalStateException`（在 `AppProperties.@PostConstruct` 校验）

**预估粒度：** 0.5 天

---

### Task A3：通用响应 / 异常 / 实体基类

**Spec 章节：** §5.2（通用列）、§6.2（统一响应）

**前置任务：** A1

**Files：**
- 新建：`common/web/ApiResponse.java`、`common/web/ApiError.java`、`common/web/PageResult.java`
- 新建：`common/exception/ErrorCode.java`、`common/exception/BizException.java`、`common/exception/GlobalExceptionHandler.java`
- 新建：`common/entity/BaseEntity.java`
- 新建：`common/util/IdGenerator.java`（Hutool Snowflake 包一层）
- 测试：`src/test/java/com/eduze/manage/common/web/ApiResponseTest.java`

**实施要点：**
- `ApiResponse<T>`：`{ code: int, message: String, data: T, traceId: String }`，提供 `ok(data)` / `fail(ErrorCode)` 静态方法
- `ErrorCode` enum：`OK(0)`、`UNAUTHORIZED(401)`、`FORBIDDEN(403)`、`NOT_FOUND(404)`、`VALIDATION_FAILED(40001)`、`CONFLICT(40901)`、`TENANT_UNIQUE_VIOLATION(40902)`、`INTERNAL_ERROR(500)`
- `GlobalExceptionHandler`：拦截 `MethodArgumentNotValidException` → `VALIDATION_FAILED`（带字段级错误列表）、`BizException` → 对应 code、其余 → `INTERNAL_ERROR`
- `BaseEntity`：`id (Long)`、`tenantId (Long)`、`branchId (Long, nullable)`、`createdAt`、`updatedAt`、`deletedAt`、`createdBy`、`updatedBy`、`version (Integer)`，配合 MyBatis-Plus 注解（A5 任务真正生效）

**验收标准：**
- `ApiResponseTest`：`ApiResponse.ok("x").getCode() == 0`、`fail(ErrorCode.UNAUTHORIZED).getCode() == 401`
- 起一个临时 `@RestController` 抛 `BizException(ErrorCode.NOT_FOUND, "测试")`，调用返回 `{"code":404,"message":"测试",...}`

**预估粒度：** 0.5 天

---

### Task A4：MySQL + Flyway 接入 + 初始化骨架

**Spec 章节：** §6.2、§9.3

**前置任务：** A2

**Files：**
- 修改：`pom.xml`（加 `mysql-connector-j:8.4.0`、`flyway-core`、`flyway-mysql`、`spring-boot-starter-jdbc`）
- 修改：`application-dev.yml`（datasource、flyway 配置）
- 新建：`src/main/resources/db/migration/V0.0.1__placeholder.sql`（空表占位，仅证明 Flyway 通了）

**实施要点：**
- 沿用 A2 已定义的 `spring.datasource` 配置（`127.0.0.1:3306 / root / root123 / eduze / utf8mb4`），本任务只追加 `spring.flyway` 节并加依赖
- Flyway：`enabled=true`、`baseline-on-migrate=true`、`locations=classpath:db/migration`、`validate-on-migrate=true`、`table=flyway_schema_history`
- `V0.0.1__placeholder.sql`：建一张 `t_flyway_check (id BIGINT)` 占位（B 阶段会写真正的迁移）

**首次准备 MySQL（一次性，本机已装 MySQL 8.4.9 / root / root123）：**

```sql
-- 用 root@127.0.0.1:3306 登录后执行
CREATE DATABASE IF NOT EXISTS eduze
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 验证：
SHOW DATABASES LIKE 'eduze';
SELECT @@version;  -- 应为 8.4.x
```

**验收标准：**
- `eduze` 库存在，字符集 `utf8mb4`
- `./mvnw spring-boot:run` 启动后日志含 `Successfully applied 1 migration to schema "eduze"`
- 用 `mysql -uroot -proot123 -h127.0.0.1 eduze -e "SHOW TABLES"` 看到 `flyway_schema_history` 与 `t_flyway_check` 两张表
- `flyway_schema_history` 有一条 `V0.0.1` 记录、`success=1`
- 第二次启动日志含 `No migration necessary`（幂等）

**预估粒度：** 0.5 天

---

### Task A5：MyBatis-Plus + P6Spy + MetaObjectHandler

**Spec 章节：** §5.3（写入层注入 tenant_id）、§6.2

**前置任务：** A3、A4

**Files：**
- 修改：`pom.xml`（`mybatis-plus-spring-boot3-starter:3.5.15`、`p6spy:3.9.1`）
- 新建：`config/MybatisPlusConfig.java`
- 新建：`common/mybatis/AuditMetaObjectHandler.java`
- 新建：`src/main/resources/spy.properties`
- 修改：`application-dev.yml`（驱动改为 `com.p6spy.engine.spy.P6SpyDriver`，URL 前缀 `jdbc:p6spy:mysql://...`）

**实施要点：**
- `MybatisPlusConfig` 注册 `MybatisPlusInterceptor`，先加 `OptimisticLockerInnerInterceptor`、`PaginationInnerInterceptor(DbType.MYSQL)`，B3 任务会再插入 `TenantInterceptor`
- `AuditMetaObjectHandler`：插入时填 `tenantId`（默认 1，B2 任务接管）、`createdAt`、`updatedAt`、`createdBy`、`updatedBy`、`version=1`、`deletedAt=null`；更新时填 `updatedAt`、`updatedBy`
- `BaseEntity` 增加 MyBatis-Plus 注解：`@TableField(fill=FieldFill.INSERT)` / `INSERT_UPDATE`、`@Version`、`@TableLogic(value="0", delval="UNIX_TIMESTAMP()*1000")`
- `spy.properties` 用 `outagedetection=true`、`appender=com.p6spy.engine.spy.appender.Slf4JLogger`、只 dev profile 启用

**验收标准：**
- 任意写一个 `TestMapper` 测试一条 insert，日志能看到 P6Spy 打印的真实 SQL
- BaseEntity 字段在 insert 时被自动填充（日志中 `tenant_id=1, created_at=...`）

**预估粒度：** 0.5 天

---

### Task A6：Redis 接入 + RedisTemplate 配置

**Spec 章节：** §6.2、§4.2（登录失败计数 / JWT 黑名单 / 字典缓存）

**前置任务：** A2

**Files：**
- 修改：`pom.xml`（`spring-boot-starter-data-redis`、`commons-pool2`）
- 修改：`application-dev.yml`、`application-prod.yml`（spring.data.redis.host / port / password / database）
- 新建：`config/RedisConfig.java`
- 测试：`src/test/java/com/eduze/manage/config/RedisConfigTest.java`（用 Testcontainers，B1 完成后再补；先写一个 disabled 的占位）

**实施要点：**
- 沿用 A2 已定义的 `spring.data.redis` 配置（`127.0.0.1:6379 / redis123 / db=0`，AOF 由服务侧已开启，应用无需关心）
- `RedisConfig` 提供 `RedisTemplate<String, Object>` Bean，key 用 `StringRedisSerializer`，value 用 `Jackson2JsonRedisSerializer`（关闭默认类型信息以避免反序列化爆炸）
- 提供 `StringRedisTemplate` Bean（已默认）
- 客户端用 Lettuce + 连接池（`commons-pool2`），池大小见 A2 yml 节
- 启动时增加一次 `PING` 自检：`@PostConstruct` 内 `stringRedisTemplate.execute((RedisCallback<String>) c -> c.ping())`，失败抛 `IllegalStateException` 让启动 fail-fast

**验收标准：**
- 应用启动日志含 `Connected to Redis` 或不报连接异常
- 在临时 `@RestController` 写 `redisTemplate.opsForValue().set("eduze:smoke","ok", Duration.ofMinutes(1))`，调一次 `curl http://localhost:8080/__redis_smoke`
- 用 `redis-cli -h 127.0.0.1 -p 6379 -a redis123 get eduze:smoke` 返回 `"ok"`
- 故意把 `REDIS_PASSWORD` 改成错值后启动 → 应用启动失败（PING 自检触发）

**预估粒度：** 0.25 天

---

### Task A7：Vite + React + TypeScript 工程初始化（web/）

**Spec 章节：** §6.3、§8.1、§8.5

**前置任务：** A1

**Files：**
- 新建整套：`web/package.json`、`web/pnpm-workspace.yaml`（如有需要，第一期可省）
- 新建：`web/vite.config.ts`、`web/tsconfig.json`、`web/tsconfig.node.json`
- 新建：`web/index.html`、`web/src/main.tsx`、`web/src/env.d.ts`
- 新建：`web/src/styles/globals.css`（Tailwind C 阶段引入，先空 + reset）
- 新建：`web/.eslintrc.cjs`、`web/.prettierrc`、`web/.gitignore`

**实施要点：**
- 用 pnpm：`pnpm create vite@latest web -- --template react-ts`（后续手工调整）
- 锁定关键依赖（写到 `package.json`）：`react@^18.3.1`、`react-dom@^18.3.1`、`react-router-dom@^6.26.2`、`vite@^6.0.0`、`typescript@^5.5.0`
- 暂不安装业务依赖（C 阶段安装 Tailwind / Radix / TanStack 等）
- `vite.config.ts` 配置：`server.port=5173`、`server.proxy['/api']='http://localhost:8080'`、`build.outDir='dist'`
- `tsconfig.json`：`strict: true`、`noUnusedLocals: true`、`paths: { "@/*": ["./src/*"] }`

**验收标准：**
- `cd web && pnpm install` 成功
- `pnpm dev` 起 `:5173`，浏览器看到 Vite 默认页
- `pnpm build` 产出 `web/dist/`

**预估粒度：** 0.5 天

---

### Task A8：Maven-npm 联动 + SPA fallback

**Spec 章节：** §6.1（路由兜底）、§8.4（生产构建）

**前置任务：** A4、A7

**Files：**
- 修改：`pom.xml`（`frontend-maven-plugin:1.15.1` + `maven-resources-plugin`）
- 新建：`config/WebMvcConfig.java`
- 测试：`src/test/java/com/eduze/manage/config/SpaFallbackTest.java`

**实施要点：**
- `frontend-maven-plugin` 配置 Node 20.18.0 + pnpm 9.12.0，绑定到 `generate-resources` 阶段，依次执行 `install-node-and-pnpm`、`pnpm install --frozen-lockfile`、`pnpm build`，工作目录 `${project.basedir}/web`
- `maven-resources-plugin` 把 `web/dist` 拷贝到 `src/main/resources/static/`，绑定到 `process-resources`
- `WebMvcConfig`：实现 `WebMvcConfigurer.addResourceHandlers`，注册一个 `ResourceHandler` 把 `/**` 映射到 `classpath:/static/`，并加自定义 `PathResourceResolver` 重写 `getResource`：所有非 `/api/**`、非含点的路径（避免 `.js`/`.css` 错配）若文件不存在则返回 `index.html`
- 单测 `SpaFallbackTest`：`@SpringBootTest` + `MockMvc`，`GET /students` → 200 + content `index.html` 头尾片段；`GET /api/notexists` → 404

**验收标准：**
- `./mvnw clean package` 成功，产出单 jar
- `java -jar target/eduze-manage-*.jar` 启动后 `curl http://localhost:8080/` 返回 `index.html`，`curl http://localhost:8080/students` 也返回 `index.html`，`curl http://localhost:8080/api/__not_exist__` 返回 404 JSON
- `SpaFallbackTest` 通过

**预估粒度：** 1 天

---

## 阶段 B：测试基建 + 租户底座

### Task B1：Testcontainers 抽象基类（MySQL + Redis）

**Spec 章节：** §6.2（测试）

**前置任务：** A4、A5、A6

**Files：**
- 修改：`pom.xml`（`testcontainers-bom:1.20.4`、`testcontainers`、`testcontainers-mysql`、`junit-jupiter`、`spring-boot-testcontainers`）
- 新建：`src/test/java/com/eduze/manage/AbstractITContainerTest.java`
- 新建：`src/test/resources/application-test.yml`
- 修改：`config/RedisConfigTest.java`（启用，从基类继承）

**实施要点：**
- `AbstractITContainerTest`：`@SpringBootTest`、`@ActiveProfiles("test")`、`@Testcontainers`，静态字段 `MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")`、`GenericContainer<?> REDIS = new GenericContainer<>("redis:7.2-alpine").withExposedPorts(6379)`
- 用 `@DynamicPropertySource` 把容器实际端口覆盖到 `spring.datasource.url`、`spring.data.redis.host/port`
- `application-test.yml`：profile=test，关闭 P6Spy；Flyway 启用

**验收标准：**
- `./mvnw test -Dtest=RedisConfigTest` 拉起 Redis 容器跑通
- 任意继承基类的测试能拿到一个干净的 MySQL（Flyway 自动迁移）

**预估粒度：** 0.5 天

---

### Task B2：TenantContext + TenantResolver

**Spec 章节：** §5.3（tenant_id 预留策略）、附录 B.7

**前置任务：** A2

**Files：**
- 新建：`tenant/TenantContext.java`
- 新建：`tenant/TenantResolver.java`、`tenant/DefaultTenantResolver.java`
- 修改：`common/mybatis/AuditMetaObjectHandler.java`（接通 TenantContext）
- 测试：`src/test/java/com/eduze/manage/tenant/TenantContextTest.java`

**实施要点：**
- `TenantContext`：`ThreadLocal<Long>`，提供 `getTenantId()` / `setTenantId(Long)` / `clear()`，未 set 时返回 `eduze.tenant.default-id`（来自 `AppProperties`）
- `TenantResolver` 接口：`Long resolve(HttpServletRequest req)`，第一期 `DefaultTenantResolver` 永远返回 `eduze.tenant.default-id`（即 1）
- 注册一个 `OncePerRequestFilter` `TenantFilter`：进入时 `set`、`finally clear`
- `AuditMetaObjectHandler` 改为：`tenant_id` 取 `TenantContext.getTenantId()`

**验收标准：**
- `TenantContextTest` 验证：默认值、set 后获取、clear 后重置
- 起一个临时控制器返回 `TenantContext.getTenantId()`，curl 命中返回 `1`

**预估粒度：** 0.5 天

---

### Task B3：MyBatis-Plus TenantInterceptor

**Spec 章节：** §5.3（查询层 / 写入层）、§6.4（数据范围拦截器，部分能力延后到 D7）

**前置任务：** A5、B2

**Files：**
- 新建：`tenant/TenantInterceptor.java`
- 修改：`config/MybatisPlusConfig.java`（注册 `TenantLineInnerInterceptor` 提供给 MyBatis-Plus）
- 测试：`src/test/java/com/eduze/manage/tenant/TenantInterceptorIT.java`（继承 B1）

**实施要点：**
- 用 MyBatis-Plus 自带的 `TenantLineInnerInterceptor` + `TenantLineHandler`，`getTenantId()` 调 `TenantContext.getTenantId()`，`getTenantIdColumn()` 返回 `tenant_id`，`ignoreTable(tableName)` 对 `t_tenant`、`t_outbox_message`、`t_audit_log`、`flyway_schema_history` 返回 true
- 顺序：`TenantLineInnerInterceptor` 必须放在 `PaginationInnerInterceptor` **之前**
- IT 测试：建一张测试表 `t_tenant_demo`，插两行不同 tenant_id（手工绕过 metaObjectHandler，用原生 SQL），确认 `selectList` 在 `TenantContext=1` 时只返回 1 行；切到 2 时返回另 1 行

**验收标准：**
- IT 测试通过
- 插入时无需手工传 tenant_id，自动注入

**预估粒度：** 0.5 天

---

### Task B4：审计日志表 + AuditAspect 切面骨架

**Spec 章节：** §6.2（操作审计独立表 t_audit_log）、§9.2（操作审计要求）、§9.4（可观测性）

**前置任务：** A5、B2

**Files：**
- 新建：`src/main/resources/db/migration/V1.0.2__init_audit.sql`
- 新建：`audit/AuditLog.java`（实体）、`audit/AuditLogMapper.java`、`audit/AuditAction.java`（注解）、`audit/AuditAspect.java`、`audit/AuditLogService.java`
- 测试：`src/test/java/com/eduze/manage/audit/AuditAspectIT.java`

**实施要点：**
- 表 `t_audit_log`：`id`、`tenant_id`、`branch_id (nullable)`、`user_id`、`username`、`action (varchar 64)`、`entity_type (varchar 64)`、`entity_id (varchar 64)`、`ip (varchar 64)`、`user_agent (varchar 256)`、`request_path`、`status (success/fail)`、`error_msg`、`extra_json (json)`、`created_at`，索引 `(tenant_id, created_at)` 和 `(entity_type, entity_id)`
- `@AuditAction(action="STUDENT_READ", entityType="student", entityIdSpEL="#id")` 用于方法
- `AuditAspect`：环绕通知，前置不阻塞业务，`@AfterReturning` / `@AfterThrowing` 异步落表（`@Async`，简单 `Executor`，第一期可同步省 `@Async`）
- IT 测试：建一个 `@AuditAction` 标注的测试方法，调用后 `t_audit_log` 多一条记录

**验收标准：**
- 表结构通过 Flyway 落库
- IT 测试通过

**预估粒度：** 1 天

---

## 阶段 C：设计系统落地

### Task C1：Tailwind 配置 + CSS 变量 + 字体本地托管

**Spec 章节：** §7.1、§7.2、§7.3

**前置任务：** A7

**Files：**
- 修改：`web/package.json`（`tailwindcss@^3.4.0`、`postcss`、`autoprefixer`、`tailwindcss-animate`）
- 新建：`web/tailwind.config.ts`、`web/postcss.config.js`
- 修改：`web/src/styles/globals.css`
- 新建：`web/src/styles/fonts.css`
- 新建：`web/public/fonts/` 目录，放入 IBM Plex Sans (woff2 300/400/500/600)、Noto Serif (500/700)、IBM Plex Mono (400/500)
- 修改：`web/src/main.tsx`（`import './styles/globals.css'`）

**实施要点：**
- `tailwind.config.ts`（关键片段）：

```ts
import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary:    'hsl(var(--primary) / <alpha-value>)',
        'primary-fg': 'hsl(var(--primary-fg) / <alpha-value>)',
        secondary:  'hsl(var(--secondary) / <alpha-value>)',
        tertiary:   'hsl(var(--tertiary) / <alpha-value>)',
        accent:     'hsl(var(--accent) / <alpha-value>)',
        success:    'hsl(var(--success) / <alpha-value>)',
        warning:    'hsl(var(--warning) / <alpha-value>)',
        error:      'hsl(var(--error) / <alpha-value>)',
        info:       'hsl(var(--info) / <alpha-value>)',
        background: 'hsl(var(--background) / <alpha-value>)',
        foreground: 'hsl(var(--foreground) / <alpha-value>)',
        muted:      'hsl(var(--muted) / <alpha-value>)',
        'muted-fg': 'hsl(var(--muted-fg) / <alpha-value>)',
        border:     'hsl(var(--border) / <alpha-value>)',
      },
      borderRadius: {
        sm: '6px',
        md: '10px',
        lg: '14px',
        xl: '20px',
      },
      fontFamily: {
        sans: ['"IBM Plex Sans"', 'system-ui', 'sans-serif'],
        serif: ['"Noto Serif"', 'Georgia', 'serif'],
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'monospace'],
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
} satisfies Config
```

- `globals.css`：

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --primary: 12 82% 42%;
    --primary-fg: 0 0% 100%;
    --secondary: 12 72% 92%;
    --tertiary: 210 78% 44%;
    --accent: 250 60% 46%;
    --success: 142 70% 38%;
    --warning: 38 92% 50%;
    --error: 0 78% 50%;
    --info: 210 78% 50%;
    --background: 220 20% 97%;
    --foreground: 220 18% 14%;
    --muted: 220 14% 96%;
    --muted-fg: 220 9% 45%;
    --border: 220 13% 90%;
  }

  html { font-family: theme('fontFamily.sans'); }
  body { @apply bg-background text-foreground antialiased; }

  *:focus-visible {
    @apply outline-none ring-2 ring-primary/40 ring-offset-2 ring-offset-background;
  }
}
```

- `fonts.css`：标准 `@font-face` 本地引用 `/fonts/IBMPlexSans-*.woff2` 等

**验收标准：**
- `pnpm dev` 打开页面，DOM 字体落到 IBM Plex Sans（DevTools 检查 `font-family`）
- 临时在 `App.tsx` 写 `<button className="bg-primary text-primary-fg rounded-md px-4 py-2">主按钮</button>`，颜色和圆角符合 §7

**预估粒度：** 1 天

---

### Task C2：全局 Provider 骨架 + Router 占位

**Spec 章节：** §6.3（Query / Toast / Theme）、§4.2（后台壳）

**前置任务：** C1

**Files：**
- 修改：`web/package.json`（`@tanstack/react-query@^5.59.0`、`zustand@^5.0.0`、`react-router-dom@^6.26.2`）
- 新建：`web/src/app/router.tsx`、`web/src/app/providers/QueryProvider.tsx`、`web/src/app/providers/ToastProvider.tsx`（先空 stub，C6 实现）、`web/src/app/providers/AuthProvider.tsx`（先空 stub，E2 实现）、`web/src/app/providers/ShortcutProvider.tsx`（空 stub，E6 实现）
- 修改：`web/src/main.tsx`

**实施要点：**
- `router.tsx` 用 `createBrowserRouter`，先注册 `/login` 占位、`/` 占位（任意 div），未登录跳转交给 E 阶段处理
- `QueryProvider` 包 `QueryClientProvider`，`defaultOptions={ queries: { staleTime: 30_000, retry: 1 } }`
- `main.tsx` 嵌套顺序：`<QueryProvider><AuthProvider><ToastProvider><ShortcutProvider><RouterProvider router={router} /></ShortcutProvider></ToastProvider></AuthProvider></QueryProvider>`

**验收标准：**
- `pnpm dev` 访问 `/` 和 `/login` 都不白屏
- React Query DevTools（仅 dev）可挂载

**预估粒度：** 0.5 天

---

### Task C3：基础表单元素（Button / Input / Textarea / Label）

**Spec 章节：** §7.4

**前置任务：** C1

**Files：**
- 修改：`web/package.json`（`class-variance-authority@^0.7.0`、`clsx@^2.1.0`、`tailwind-merge@^2.5.0`）
- 新建：`web/src/lib/cn.ts`（`clsx + twMerge` 工具）
- 新建：`web/src/components/ui/Button.tsx`、`Input.tsx`、`Textarea.tsx`、`Label.tsx`
- 测试：`web/src/components/ui/__tests__/Button.test.tsx`（C 阶段开始用 Vitest + RTL）
- 修改：`web/package.json`（`vitest@^2.1.0`、`@testing-library/react@^16.0.0`、`@testing-library/jest-dom@^6.5.0`、`jsdom@^25.0.0`）
- 新建：`web/vitest.config.ts`、`web/src/test/setup.ts`

**实施要点：**
- `Button.tsx` 用 CVA：

```tsx
const buttonVariants = cva(
  'inline-flex items-center justify-center rounded-md font-medium transition-colors disabled:opacity-50 disabled:pointer-events-none',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-fg hover:bg-primary/90',
        secondary: 'bg-secondary text-foreground hover:bg-secondary/80',
        ghost: 'bg-transparent hover:bg-muted',
        danger: 'bg-error text-primary-fg hover:bg-error/90',
        link: 'underline text-primary hover:text-primary/80',
      },
      size: { sm: 'h-8 px-3 text-sm', md: 'h-10 px-4', lg: 'h-12 px-6 text-lg' },
    },
    defaultVariants: { variant: 'default', size: 'md' },
  },
)
```

- `Input` / `Textarea`：转发 `ref`、统一 `border border-border rounded-md focus-visible:ring-2 ring-primary/40`、错误态 `aria-invalid` 自动加红边

**验收标准：**
- `pnpm test` 跑通 `Button.test.tsx`：渲染 default / danger / size sm 三种变体快照不变
- 浏览器手动切换变体颜色正确

**预估粒度：** 0.75 天

---

### Task C4：表单选择类（Select / Checkbox / RadioGroup / Switch / DatePicker）

**Spec 章节：** §7.4

**前置任务：** C3

**Files：**
- 修改：`web/package.json`（`@radix-ui/react-select`、`@radix-ui/react-checkbox`、`@radix-ui/react-radio-group`、`@radix-ui/react-switch`、`react-day-picker@^9.0.0`、`date-fns@^4.1.0`、`@phosphor-icons/react@^2.1.7`）
- 新建：`Select.tsx`、`Checkbox.tsx`、`RadioGroup.tsx`、`Switch.tsx`、`DatePicker.tsx`
- 测试：每个组件一个 `__tests__/*.test.tsx`，最少 1 个交互测试

**实施要点：**
- 全部基于 Radix Primitives 包装，加 Tailwind 样式
- `DatePicker` 用 `react-day-picker` + `date-fns`，支持单选；中文 locale (`zhCN`)
- `Select` 提供 `Select.Trigger / Select.Content / Select.Item` 三件套或一个聚合 `<Select options={[...]} />` 简化 API
- Checkbox / Switch 必须支持 `aria-label` 和 `id` + `Label htmlFor`

**验收标准：**
- 5 个组件均通过测试（点击触发 onChange、键盘可达）
- 在 demo 页排开，外观符合 §7.3 圆角 / 阴影

**预估粒度：** 1 天

---

### Task C5：弹层组件（Dialog / Sheet / Drawer / Tooltip / DropdownMenu / NavigationMenu）

**Spec 章节：** §4.6（Sheet 抽屉、Drawer 移动筛选）、§7.4

**前置任务：** C3

**Files：**
- 修改：`web/package.json`（`@radix-ui/react-dialog`、`@radix-ui/react-tooltip`、`@radix-ui/react-dropdown-menu`、`@radix-ui/react-navigation-menu`、`vaul@^1.0.0`（Drawer））
- 新建：`Dialog.tsx`、`Sheet.tsx`、`Drawer.tsx`、`Tooltip.tsx`、`DropdownMenu.tsx`、`NavigationMenu.tsx`
- 测试：每个 1 个测试

**实施要点：**
- `Sheet` 基于 Radix Dialog + 自定义 `data-side="right"` 动画类（用 tailwindcss-animate 的 `slide-in-from-right-1/2`）
- `Drawer` 用 `vaul`（底部抽屉，移动端筛选）
- 所有弹层叠加层用 `bg-foreground/40 backdrop-blur-sm`
- `Sheet` 默认宽度 `w-full sm:w-[480px]`，标题区 + 内容区 + 底部按钮区三段插槽

**验收标准：**
- demo 页能依次打开 5 种弹层；Esc 关闭、外部点击关闭、focus trap 工作
- Sheet 在 `lg` 断点下右滑、`sm` 全屏

**预估粒度：** 1 天

---

### Task C6：Toast 系统（4.2s 自动消失 + 撤销）

**Spec 章节：** §4.6（Toast 通知）、§7.4

**前置任务：** C2

**Files：**
- 修改：`web/package.json`（`sonner@^1.7.0` 或 `@radix-ui/react-toast`，本期选 `sonner`）
- 修改：`web/src/app/providers/ToastProvider.tsx`
- 新建：`web/src/components/ui/Toast.tsx`（包装 sonner 的 `toast()`，提供 `toast.success / error / undo`）
- 新建：`web/src/lib/toast.ts`（统一导出）
- 测试：`web/src/components/ui/__tests__/Toast.test.tsx`

**实施要点：**
- `ToastProvider`：渲染 `<Toaster richColors duration={4200} closeButton position="top-right" />`
- `toast.undo({ message, onUndo })`：内部用 sonner 的 `toast(message, { action: { label: '撤销', onClick: onUndo } })`
- 4.2 秒后 `onAutoClose` 视为不撤销

**验收标准：**
- 测试：触发 `toast.success('已保存')`，4.2 秒内 DOM 含文案，之后消失（fake timers）
- 触发 `toast.undo`，点击"撤销"调用 `onUndo` 一次

**预估粒度：** 0.5 天

---

### Task C7：状态展示组件（Skeleton / EmptyState / Pagination / KPICard）

**Spec 章节：** §4.6（骨架屏、KPI 数字动画）、§7.4

**前置任务：** C3

**Files：**
- 修改：`web/package.json`（`react-countup@^6.5.0`）
- 新建：`Skeleton.tsx`、`EmptyState.tsx`、`Pagination.tsx`、`KPICard.tsx`
- 测试：4 个对应测试

**实施要点：**
- `Skeleton`：`bg-muted animate-pulse rounded-md`，支持 `className` 覆盖宽高
- `EmptyState`：图标（Phosphor `<Folders weight="duotone" />`） + 标题 + 描述 + 可选 CTA
- `Pagination`：受控 + 简化 API（`page / pageSize / total / onChange`），桌面端 `< 1 2 3 ... >`、移动端 `< 当前页 / 总页数 >`
- `KPICard`：标题 + 数值（用 `react-countup` 滚动） + 趋势小三角（可选）

**验收标准：**
- 4 个组件测试通过，KPI 卡渲染 `0 → 2500` 动画 1 秒内完成

**预估粒度：** 0.75 天

---

### Task C8：DataTable（TanStack Table v8 包装）

**Spec 章节：** §6.3（TanStack Table v8）、§4.6（响应式）、§7.4

**前置任务：** C3

**Files：**
- 修改：`web/package.json`（`@tanstack/react-table@^8.20.0`）
- 新建：`web/src/components/ui/DataTable.tsx`、`DataTable.types.ts`
- 测试：`web/src/components/ui/__tests__/DataTable.test.tsx`

**实施要点：**
- API：

```tsx
type DataTableProps<T> = {
  columns: ColumnDef<T>[]
  data: T[]
  loading?: boolean
  empty?: ReactNode
  pageState?: { page: number; pageSize: number; total: number; onChange: (next) => void }
  rowKey: (row: T) => string | number
  onRowClick?: (row: T) => void
  toolbar?: ReactNode      // 顶部筛选区
  stickyHeader?: boolean
  mobileCardRender?: (row: T) => ReactNode  // < md 断点用卡片
}
```

- 桌面端：`<table>`，sticky header `top-0 z-10 bg-background`，hover `bg-muted/40`
- 移动端：`< md` 断点切换为 `mobileCardRender` 渲染的 div 列表
- 加载态：`loading` 时表格渲染 5 行 `<Skeleton />`；空态时渲染 `empty` 或默认 `<EmptyState />`
- 集成 `Pagination`

**验收标准：**
- 测试：5 行 mock 数据渲染 5 行；点击行触发 `onRowClick`；切换 `loading` 显示骨架；移动端模拟（`window.innerWidth=375`）显示卡片
- 在临时 demo 页能看到一个完整可分页表格

**预估粒度：** 1 天

---

## 阶段 D：模块 0（账号 / 权限 / 校区）

### Task D1：建表 — 租户 + 校区 + 内置数据

**Spec 章节：** §5.2（t_tenant、t_branch）、§5.3、附录 A

**前置任务：** B3、B4

**Files：**
- 新建：`src/main/resources/db/migration/V1.0.0__init_tenant_branch.sql`

**实施要点（关键 DDL 片段）：**

```sql
CREATE TABLE t_tenant (
  id BIGINT NOT NULL PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  code VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at BIGINT NOT NULL DEFAULT 0,
  created_by BIGINT, updated_by BIGINT, version INT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_tenant_code (code, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_branch (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  name VARCHAR(128) NOT NULL,
  code VARCHAR(64) NOT NULL,
  address VARCHAR(256),
  phone VARCHAR(32),
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at BIGINT NOT NULL DEFAULT 0,
  created_by BIGINT, updated_by BIGINT, version INT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_branch_code (tenant_id, code, deleted_at),
  KEY idx_branch_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO t_tenant (id, name, code, status) VALUES (1, '默认机构', 'DEFAULT', 1);
INSERT INTO t_branch (id, tenant_id, name, code, address) VALUES (1, 1, '总校区', 'HQ', '');
```

- 软删用 `deleted_at BIGINT`（默认 0，删除时写时间戳毫秒），是 MyBatis-Plus `@TableLogic` 推荐做法，复合唯一键带上 `deleted_at` 避免软删后重复 code 冲突
- 所有业务表沿用此模板（B 阶段后续 D2 / F1 / G1 / H1 都重复此结构）

**验收标准：**
- Flyway 启动后 `t_tenant`、`t_branch` 落库，默认数据存在
- 重复执行启动不报错（Flyway 幂等）

**预估粒度：** 0.5 天

---

### Task D2：建表 — 用户 / 角色 / 权限 + 内置 Seed

**Spec 章节：** §4.2（5 个内置角色）、§5.2

**前置任务：** D1

**Files：**
- 新建：`src/main/resources/db/migration/V1.0.1__init_auth.sql`
- 新建：`src/main/resources/db/migration/V9.0.0__seed_builtin.sql`

**实施要点：**
- `t_user`（`username` 加 `tenant_id` 复合唯一）、`t_role`（`code` 加 `tenant_id` 复合唯一）、`t_permission`（全局非租户表，code 全局唯一）、`t_user_role`、`t_role_permission`、`t_user_branch`
- `V9.0.0` Seed：
  - 内置 5 个角色：`SUPER_ADMIN`、`PRINCIPAL`、`ADVISOR`、`TEACHER`、`FRONT_DESK`，`is_builtin=1`
  - 权限模板（每模块 read/write/delete）：`branch:read/write`、`user:read/write`、`role:read/write/assign`、`student:read/write/import/delete`、`guardian:read/write`、`coursepkg:read/write`、`course:read/write`、`classgroup:read/write/assign`、`classroom:read/write`、`lesson:read/write/reschedule/cancel`、`attendance:read/write/scan`、`pickup:read/write`、`leave:read/write/approve`、`stat:read`、`audit:read`、`search:read`
  - 角色-权限映射：`SUPER_ADMIN` 全部、`PRINCIPAL` 除 `user:write/role:*` 外全部、`ADVISOR` 限学员/家长/课时包/搜索读写、`TEACHER` 限自己班级 lesson/attendance 读 + 调课请求 + 请假审批、`FRONT_DESK` 限学员读、签到、接送、请假写
  - 默认超管：`username=admin`、`password_hash=BCrypt('admin@123')`（README 注明首次登录强制改密；改密由 D7 任务实现）
  - 给超管赋 `SUPER_ADMIN` 角色，绑定 `branch_id=1`

**验收标准：**
- 启动后表结构与种子数据全部就绪
- `SELECT count(*) FROM t_role WHERE is_builtin=1` 返回 5
- `SELECT count(*) FROM t_permission` 返回 ≥ 27
- 用 SQL 能查到 admin 用户拥有 SUPER_ADMIN 角色

**预估粒度：** 1 天

---

### Task D3：Spring Security + JWT 基础链路

**Spec 章节：** §6.4

**前置任务：** D2、A6

**Files：**
- 修改：`pom.xml`（`spring-boot-starter-security`、`io.jsonwebtoken:jjwt-api/impl/jackson:0.12.6`）
- 新建：`config/SecurityConfig.java`、`config/JwtFilter.java`
- 新建：`auth/security/CustomUserDetails.java`、`auth/security/CustomUserDetailsService.java`
- 新建：`auth/service/JwtService.java`
- 测试：`src/test/java/com/eduze/manage/auth/JwtServiceTest.java`

**实施要点：**
- `SecurityConfig`：CSRF 关、`SessionCreationPolicy.STATELESS`、`/api/auth/**` permitAll，其余 `authenticated`，注入 `JwtFilter` 在 `UsernamePasswordAuthenticationFilter` 之前
- `JwtService.generateAccess(userId, jti)` / `generateRefresh(userId, jti)`，HS256，secret 来自 `eduze.jwt.secret`，access 15 分钟、refresh 7 天，claim 含 `tid`（tenant_id）、`bids`（branchIds）、`auths`（权限 code 列表）
- `JwtFilter`：解析 `Authorization` Bearer，校验黑名单 Redis key `jwt:blacklist:<jti>`，注入 `SecurityContext`，同步 `TenantContext.setTenantId(claim.tid)`，请求结束 `clear()`
- `CustomUserDetails`：`getAuthorities()` 返回 `SimpleGrantedAuthority(perm)` 集合
- `JwtServiceTest` 单测：生成 → 解析能拿回 jti / userId / tenantId

**验收标准：**
- `JwtServiceTest` 通过
- 启动后 `curl /api/branch` 返回 401（未登录），`/actuator/health` 仍 200

**预估粒度：** 1 天

---

### Task D4：AuthController 登录 / 登出 / 刷新

**Spec 章节：** §4.2、§6.4

**前置任务：** D3

**Files：**
- 新建：`auth/controller/AuthController.java`
- 新建：`auth/dto/LoginRequest.java`、`LoginResponse.java`、`RefreshRequest.java`
- 新建：`auth/service/AuthService.java`、`auth/service/JwtBlacklistService.java`
- 测试：`src/test/java/com/eduze/manage/auth/AuthControllerIT.java`（继承 B1）

**实施要点：**
- `POST /api/auth/login`：`{username, password}` → 校验 BCrypt → 颁发 access+refresh，返回 `{accessToken, refreshToken, user: {id, name, roles, permissions, branches}}`
- `POST /api/auth/refresh`：`{refreshToken}` → 校验 → 颁发新 access（refresh 不轮转，第一期简化）
- `POST /api/auth/logout`：从 `Authorization` 取当前 jti，写 Redis `jwt:blacklist:<jti>`，TTL = access 剩余有效期
- `JwtBlacklistService` 用 `StringRedisTemplate.opsForValue().set(key, "1", ttl)`
- `AuthControllerIT`：登录成功 → 拿 token 带请求 → 登出 → 同一 token 再请求 401

**验收标准：**
- IT 测试通过
- 错密码返回 `code=401, message="账号或密码错误"`，不区分用户名错或密码错（防枚举）

**预估粒度：** 1 天

---

### Task D5：登录失败计数 + 限流

**Spec 章节：** §4.2（5 次失败锁 10 分钟）、§9.2（同 IP 1 分钟 60 次）

**前置任务：** D4

**Files：**
- 新建：`auth/service/LoginAttemptService.java`
- 新建：`common/web/RateLimitFilter.java`
- 修改：`config/SecurityConfig.java`（在 `JwtFilter` 之前注册 `RateLimitFilter`）
- 测试：`src/test/java/com/eduze/manage/auth/LoginAttemptServiceIT.java`、`RateLimitFilterIT.java`

**实施要点：**
- `LoginAttemptService`：
  - `incrementFail(username)`：`INCR login:fail:<tenantId>:<username>` + `EXPIRE 600`，返回当前值；≥ 5 时返回锁定标志
  - `isLocked(username)`：检查 key 存在且 ≥ 5
  - 登录成功时 `DEL` 重置
- `RateLimitFilter`：仅作用于 `/api/auth/login`，按 `ip + minute` 桶，`INCR ratelimit:login:<ip>:<minute>` + `EXPIRE 70`，>60 时返回 429 `{code:42900,message:"请求过于频繁"}`
- IT 测试：连续失败 5 次第 6 次返回 423 `{code:42301}`；同 IP 60+ 次返回 429

**验收标准：**
- 两个 IT 通过
- 锁定 10 分钟后自动解封（fake redis ttl 验证）

**预估粒度：** 0.5 天

---

### Task D6：RBAC 方法级注解 + UserDetailsService

**Spec 章节：** §4.2（RBAC 模型）、§6.4（@PreAuthorize）

**前置任务：** D3

**Files：**
- 修改：`config/SecurityConfig.java`（`@EnableMethodSecurity(prePostEnabled=true)`）
- 修改：`auth/security/CustomUserDetailsService.java`（聚合权限）
- 新建：`auth/service/PermissionAggregator.java`
- 新建：`auth/controller/UserController.java`、`RoleController.java`、`PermissionController.java`
- 测试：`src/test/java/com/eduze/manage/auth/RbacIT.java`

**实施要点：**
- `PermissionAggregator.aggregate(userId)`：JOIN `t_user_role + t_role_permission + t_permission`，返回 `Set<String> codes`，缓存到 Redis `perm:user:<userId>` TTL 5 分钟，更新角色 / 权限时 `DEL` 失效
- `UserController` 提供：
  - `GET /api/users`（`@PreAuthorize("hasAuthority('user:read')")`），分页 + 关键字搜索
  - `POST /api/users`（`hasAuthority('user:write')`）
  - `PUT /api/users/{id}`、`DELETE /api/users/{id}` 
  - `POST /api/users/{id}/roles`：分配角色（`role:assign`）
  - `POST /api/users/{id}/branches`：分配校区
- `RoleController`：CRUD + 内置角色不可删（`is_builtin=1` 抛 `BizException`）
- `PermissionController`：仅 `GET /api/permissions`（按 module 分组）
- `RbacIT`：用 `TEACHER` 角色登录调 `/api/users` → 403；用 `SUPER_ADMIN` → 200

**验收标准：**
- IT 通过
- 角色变更后下一次接口调用立即生效（缓存失效正确）

**预估粒度：** 1 天

---

### Task D7：数据范围拦截器（branch_id 自动追加） + 个人中心

**Spec 章节：** §4.2（数据范围）、§6.4

**前置任务：** D3、B3

**Files：**
- 新建：`tenant/BranchScopeInterceptor.java`
- 修改：`config/MybatisPlusConfig.java`（注册）
- 新建：`auth/controller/MeController.java`、`auth/dto/ChangePasswordRequest.java`、`UpdateProfileRequest.java`
- 测试：`src/test/java/com/eduze/manage/tenant/BranchScopeIT.java`

**实施要点：**
- `BranchScopeInterceptor` 实现 MyBatis-Plus 的 `InnerInterceptor.beforeQuery`，拿到 `MappedStatement`，仅当：
  1. SQL 是 SELECT
  2. 表名在白名单（带 `branch_id` 列的业务表，初期写死：`t_student`、`t_class_group`、`t_class_room`、`t_lesson`、`t_attendance`、`t_pickup_record`、`t_leave_request`）
  3. 当前用户非 `SUPER_ADMIN`
- 通过 JSqlParser 在 `WHERE` 上追加 `AND branch_id IN (?,?,...)`，参数从 `SecurityContext` 的 `CustomUserDetails.branchIds` 取
- 不影响插入 / 更新（MetaObjectHandler 不动 `branch_id`，由调用方传入）
- `MeController`：
  - `GET /api/me`：当前用户 + 角色 + 权限 + 校区
  - `POST /api/me/password`：旧密码 + 新密码（BCrypt 校验旧、写新）
  - `PATCH /api/me/profile`：仅手机号 / 邮箱 / 显示名

**验收标准：**
- `BranchScopeIT`：建 2 个校区各 1 个学生，给非超管用户绑校区 1，查询 `t_student` 只返回校区 1 的学生
- `MeController` 改密接口 IT 通过

**预估粒度：** 1 天

---

### Task D8：BranchController + 校区管理（仅超管）

**Spec 章节：** §4.2（校区管理仅超级管理员）

**前置任务：** D6

**Files：**
- 新建：`branch/controller/BranchController.java`
- 新建：`branch/service/BranchService.java`、`branch/domain/Branch.java`、`branch/mapper/BranchMapper.java`、`branch/dto/BranchRequest.java`、`BranchResponse.java`
- 测试：`src/test/java/com/eduze/manage/branch/BranchControllerIT.java`

**实施要点：**
- 全部接口加 `@PreAuthorize("hasAuthority('branch:write')")` 或 `branch:read`
- `code` 在租户内唯一，复用 D1 的唯一索引；service 层捕获 `DuplicateKeyException` → `BizException(TENANT_UNIQUE_VIOLATION)`
- 删除采用软删（@TableLogic）；删除前检查无关联学员 / 班级 / 课次

**验收标准：**
- IT：超管建 / 改 / 软删校区流程通过；课程顾问角色调用全部 403
- 删除有学员的校区返回 `BizException("校区下仍有学员，无法删除")`

**预估粒度：** 0.5 天

---

## 阶段 E：后台壳

### Task E1：Axios 拦截器 + Auth Store

**Spec 章节：** §6.3（HTTP 统一拦截器）、§6.4

**前置任务：** C2、D4

**Files：**
- 修改：`web/package.json`（`axios@^1.7.0`、`zustand@^5.0.0`）
- 新建：`web/src/lib/axios.ts`
- 新建：`web/src/features/auth/store.ts`、`web/src/features/auth/api.ts`
- 测试：`web/src/lib/__tests__/axios.test.ts`

**实施要点：**
- `store.ts`：Zustand persist 到 `localStorage`，state：`accessToken / refreshToken / user / branchIds / permissions`
- `axios.ts`：
  - `baseURL='/api'`，`timeout=15000`
  - request 拦截：从 store 取 `accessToken` → `Authorization: Bearer ...`
  - response 拦截：
    - `401` 且非 `/auth/refresh`：尝试一次 refresh，成功则重放原请求；失败则清空 store 跳 `/login`
    - `403` → `toast.error('没有权限')`
    - `2xx` 但 `body.code !== 0` → 抛 `ApiError(code, message)`，调用方决定是否 toast
    - 5xx / 网络异常 → `toast.error(message ?? '服务异常，请稍后再试')`
- `api.ts`：`login(req)` / `logout()` / `refresh()` / `getMe()`

**验收标准：**
- 测试用 axios-mock-adapter：401 触发 refresh 一次，refresh 成功后重放，最终 200
- 失败 refresh 时清空 store + 跳转 `/login`（用 `vi.spyOn(window.location, 'assign')` 验证）

**预估粒度：** 0.75 天

---

### Task E2：登录页

**Spec 章节：** §4.2（登录页 单页极简）

**前置任务：** E1、C3

**Files：**
- 新建：`web/src/features/auth/pages/LoginPage.tsx`、`web/src/features/auth/schemas.ts`
- 修改：`web/src/app/router.tsx`（注册 `/login` + 公共路由）

**实施要点：**
- 居中卡片、品牌橙红 logo（占位 SVG）+ Noto Serif "EduZE Manage" 标题
- 字段：用户名 / 密码、记住我（仅前端）、登录按钮
- 表单 RHF + Zod：用户名 4-32 字符、密码 6-32 字符
- 登录成功跳转 `/dashboard`（先临时占位，E3 接管）
- 错误：账号锁定、限流分别 toast 不同文案
- `aria-label="登录"`、按下 Enter 提交

**验收标准：**
- 登录成功后 store 含 user，跳到 `/`（dashboard）
- 5 次错误后再登录显示锁定提示

**预估粒度：** 0.5 天

---

### Task E3：AppLayout + Header + Sidebar

**Spec 章节：** §4.2（后台壳）、§4.6、§7.5

**前置任务：** C5、E2

**Files：**
- 新建：`web/src/app/shell/AppLayout.tsx`、`Header.tsx`、`Sidebar.tsx`
- 新建：`web/src/lib/permissions.ts`（`hasPermission(perm: string): boolean`）
- 新建：`web/src/features/auth/components/UserMenu.tsx`
- 修改：`web/src/app/router.tsx`（嵌套：`/` 走 ProtectedRoute → AppLayout）

**实施要点：**
- `AppLayout`：`flex` 左侧 `w-60` Sidebar，右侧 `flex-1`，顶部 `h-14` sticky Header
- `Header`：Logo、面包屑（占位）、全局搜索按钮（占位 `Ctrl/Cmd K`） + 消息铃铛（占位） + UserMenu
- `Sidebar`：模块清单（学员、课程、签到、校区/账号/角色），按权限过滤；折叠按钮（`collapsed: bool` 持久化到 store）；`aria-current="page"` 当前路由
- 移动端：`< lg` 隐藏 Sidebar，Header 显示汉堡按钮（E7 实现 MobileNav）
- "跳过导航"链接：聚焦时显示，跳到 `<main id="main">`

**验收标准：**
- 桌面端能看到 Header + Sidebar；点击 Sidebar 跳路由；UserMenu 中能登出
- 折叠态左侧仅图标
- 无 Authorization 时被 ProtectedRoute 拦截到 `/login`

**预估粒度：** 1 天

---

### Task E4：ProtectedRoute + 权限指令

**Spec 章节：** §4.2（RBAC 前端体现）、§6.4

**前置任务：** E3

**Files：**
- 新建：`web/src/app/router/ProtectedRoute.tsx`
- 新建：`web/src/components/auth/RequirePermission.tsx`
- 测试：`web/src/components/auth/__tests__/RequirePermission.test.tsx`

**实施要点：**
- `ProtectedRoute`：未登录 → `<Navigate to="/login" replace />`；带 `permissions={['student:read']}` 时检查不通过 → `<EmptyState>没有权限</EmptyState>`
- `<RequirePermission perm="student:write">`：内联包裹按钮 / 入口，无权时不渲染（用于 Sidebar 项、新建按钮）

**验收标准：**
- 测试：mock store 无权限时不渲染子节点

**预估粒度：** 0.25 天

---

### Task E5：CommandPalette（Cmd+K 全局搜索弹窗 UI）

**Spec 章节：** §4.6（全局搜索 Cmd+K）、§7.5

**前置任务：** C5、E3

**Files：**
- 修改：`web/package.json`（`cmdk@^1.0.0`）
- 新建：`web/src/app/shell/CommandPalette.tsx`、`web/src/app/shell/ShortcutHelpDialog.tsx`
- 新建：`web/src/lib/searchClient.ts`（先 stub，I1 接通真实接口）

**实施要点：**
- `cmdk` 风格的命令面板，触发器：Header 搜索按钮 + Cmd+K（E6 注册）
- 默认 tab：搜索；右上 `?` 入口打开快捷键帮助
- 结果分组：学员 / 家长 / 班级 / 课次（先静态空 + loading 骨架）
- 选中后 `navigate(result.url)` + 弹 Sheet（业务模块阶段接通）
- 键盘：上下选择、Enter 确认、Esc 关闭
- 支持中文输入法（监听 `compositionstart/end`，输入法期间不触发筛选）

**验收标准：**
- Cmd+K 打开弹窗，输入触发 stub onSearch
- ? 打开帮助弹窗，列出 K / N / ? / Esc 四个快捷键

**预估粒度：** 0.75 天

---

### Task E6：全局快捷键 Provider

**Spec 章节：** §4.6（键盘快捷键）

**前置任务：** E5

**Files：**
- 修改：`web/src/app/providers/ShortcutProvider.tsx`
- 新建：`web/src/hooks/useShortcut.ts`、`web/src/lib/kbd.ts`

**实施要点：**
- `useShortcut('mod+k', cb, { enableOnFormTags: true })` 模拟 react-hotkeys-hook 风格
- `mod` 在 macOS 映射 Cmd，其他映射 Ctrl
- 默认全局快捷键：
  - `mod+k`：打开 CommandPalette
  - `mod+n`：触发当前模块的"新建"事件（用 `window.dispatchEvent(new CustomEvent('app:new'))`，业务页监听）
  - `?`：打开 `ShortcutHelpDialog`
  - `Escape`：交给 Radix 自处理（不在此注册）
- 在 input/textarea/contenteditable 内忽略（除非显式 enableOnFormTags）

**验收标准：**
- 各快捷键全局生效
- 输入框内按 `mod+k` 仍可用，按 `?` 不触发（除非显式启用）

**预估粒度：** 0.5 天

---

### Task E7：移动端导航 + 响应式收尾

**Spec 章节：** §4.6（移动端导航 / 移动端筛选）、§7.6

**前置任务：** E3、C5

**Files：**
- 新建：`web/src/app/shell/MobileNav.tsx`
- 修改：`web/src/app/shell/Header.tsx`（汉堡按钮触发 MobileNav）
- 新建：`web/src/components/ui/FilterDrawer.tsx`（移动端通用筛选抽屉，业务页复用）

**实施要点：**
- `MobileNav` 用 Radix Sheet 左侧滑出，复用 Sidebar 的菜单项
- `FilterDrawer` 用 `vaul` 底部 Drawer，标准三段：标题 / 筛选项 / 底部"重置 + 应用"按钮
- Header 在 `< lg` 显示汉堡，Logo 居中

**验收标准：**
- 模拟移动端宽度（375px）：Sidebar 隐藏，汉堡可打开 MobileNav
- 任意业务页能直接 `<FilterDrawer>...</FilterDrawer>` 接入

**预估粒度：** 0.5 天

---

## 阶段 F：模块 #2 学员与家长

### Task F1：建表 — 学员 / 家长 / 关联 / 课时包

**Spec 章节：** §5.2

**前置任务：** D1

**Files：**
- 新建：`src/main/resources/db/migration/V1.1.0__init_student.sql`

**实施要点（核心字段）：**

```sql
CREATE TABLE t_student (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  branch_id BIGINT NOT NULL,
  enroll_no VARCHAR(64) NOT NULL,
  name VARCHAR(64) NOT NULL,
  gender TINYINT NOT NULL DEFAULT 0 COMMENT '0=未知 1=男 2=女',
  birthday DATE,
  enroll_date DATE,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1=在读 2=暂停 3=退学',
  allergy VARCHAR(512),
  health_note VARCHAR(1024),
  emergency_contact VARCHAR(128),
  emergency_phone VARCHAR(32),
  avatar_url VARCHAR(512),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at BIGINT NOT NULL DEFAULT 0,
  created_by BIGINT, updated_by BIGINT, version INT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_student_enroll (tenant_id, enroll_no, deleted_at),
  KEY idx_student_branch (tenant_id, branch_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- `t_guardian (id, tenant_id, name, phone, is_main_contact tinyint, can_pickup tinyint, qr_code varchar 64 unique nullable)`，`UNIQUE(tenant_id, phone, deleted_at)`
- `t_student_guardian_relation (id, tenant_id, student_id, guardian_id, relation varchar 32)`，`UNIQUE(tenant_id, student_id, guardian_id, deleted_at)`
- `t_course_package (id, tenant_id, branch_id, student_id, total_lessons int, remaining_lessons int, expire_date date, note varchar 256)`

**验收标准：**
- Flyway 通过；唯一键约束生效（手工尝试插入重复 enroll_no 报错）

**预估粒度：** 0.5 天

---

### Task F2：Student CRUD API + @TenantUnique 校验

**Spec 章节：** §4.3、§6.2（自定义 @TenantUnique）

**前置任务：** F1、D7

**Files：**
- 新建：`student/domain/Student.java`、`student/mapper/StudentMapper.java`、`student/service/StudentService.java`、`student/controller/StudentController.java`
- 新建：`student/dto/StudentRequest.java`、`StudentResponse.java`、`StudentQuery.java`
- 新建：`common/validation/TenantUnique.java`、`TenantUniqueValidator.java`
- 测试：`src/test/java/com/eduze/manage/student/StudentControllerIT.java`

**实施要点：**
- 接口：
  - `GET /api/students?keyword=&branchId=&classGroupId=&status=&pkgRemainingMax=&page=&size=`
  - `GET /api/students/{id}`
  - `POST /api/students`、`PUT /api/students/{id}`、`PATCH /api/students/{id}/status`（停学）、`DELETE`（软删）
- `@TenantUnique(table="t_student", column="enroll_no")`：在校验阶段查 DB（带 tenant_id），冲突时抛字段级错误
- 列表支持脱敏开关（`?mask=phone`）：`emergency_phone` 输出 `138****1234`

**验收标准：**
- IT 覆盖：CRUD + 重复 enroll_no 校验失败 + 软删后再查不到 + 数据范围拦截器（非 SUPER_ADMIN 看不到其他校区）

**预估粒度：** 1 天

---

### Task F3：Guardian + 学员-家长关联 API

**Spec 章节：** §4.3（一户多生）

**前置任务：** F2

**Files：**
- 新建：`student/domain/Guardian.java`、`StudentGuardianRelation.java`、对应 Mapper / Service / Controller
- 新建：`student/dto/GuardianRequest.java`、`GuardianResponse.java`、`LinkGuardianRequest.java`
- 测试：`src/test/java/com/eduze/manage/student/GuardianIT.java`

**实施要点：**
- `GuardianController`：`GET /api/guardians`（按 phone 搜索）、CRUD
- `StudentController`（扩展）：
  - `GET /api/students/{id}/guardians`、`POST /api/students/{id}/guardians/{guardianId}`（含 `relation` body）、`DELETE /api/students/{id}/guardians/{guardianId}`
  - `POST /api/students/{id}/guardians:upsert`：传家长全量信息 → 自动 upsert 家长 + 关联学员
- 一户多生：插入关联前不删除已存在的家长，仅新增关联
- 主联系人约束：每个学员最多 1 个 `is_main_contact=1` 的家长（service 层强制）

**验收标准：**
- IT：A 学员关联家长 X，B 学员也能关联同一个 X；主联系人切换正确

**预估粒度：** 1 天

---

### Task F4：CoursePackage CRUD + 余额提醒

**Spec 章节：** §4.3（课时包余额、≤5 高亮）

**前置任务：** F2

**Files：**
- 新建：`student/domain/CoursePackage.java`、`CoursePackageMapper.java`、`CoursePackageService.java`、`CoursePackageController.java`
- 新建：`student/dto/CoursePackageRequest.java`、`CoursePackageResponse.java`
- 测试：`src/test/java/com/eduze/manage/student/CoursePackageIT.java`

**实施要点：**
- 接口：
  - `GET /api/students/{id}/packages`
  - `POST /api/students/{id}/packages`：创建 `{totalLessons, remainingLessons, expireDate, note}`
  - `PUT /api/packages/{id}`、`DELETE /api/packages/{id}`
  - `GET /api/students?pkgRemainingMax=5`：用于 F2 列表筛选低余额
- `StudentResponse` 聚合 `totalRemaining = SUM(remaining_lessons WHERE expire_date >= today)`，`alertLow = totalRemaining <= 5`

**验收标准：**
- IT：插入 2 个包 → student 详情返回正确余额；过期包不计入

**预估粒度：** 0.5 天

---

### Task F5：学员批量导入 Excel

**Spec 章节：** §4.3（批量导入 Excel 模板）

**前置任务：** F3、F4

**Files：**
- 修改：`pom.xml`（`alibaba-easyexcel:4.0.3`）
- 新建：`student/controller/StudentImportController.java`、`student/service/StudentImportService.java`、`student/dto/StudentImportRow.java`、`StudentImportResult.java`
- 新建：`src/main/resources/templates/student-import-template.xlsx`（手工生成，Flyway 后放置）
- 测试：`src/test/java/com/eduze/manage/student/StudentImportIT.java`

**实施要点：**
- `GET /api/students/import/template`：下载 Excel 模板（流式输出）
- `POST /api/students/import`（multipart）：解析每一行，单行错误不中断，返回 `{successCount, failures: [{rowIndex, errors: [{column, message}]}]}`
- 字段：入园编号 / 姓名 / 性别 / 生日 / 入园日期 / 校区编码 / 主家长姓名 / 主家长手机 / 关系 / 过敏史 / 备注
- 主家长缺失时新建家长并关联（与 F3 upsert 逻辑一致）
- 整批用单事务包裹，单行失败回滚整批可选（第一期取"逐行落 + 收集失败"策略，更友好）

**验收标准：**
- IT：上传 5 行 Excel，3 行成功 / 2 行失败，结果正确返回；DB 中无半成品（成功 3 行已落，失败 2 行未落）

**预估粒度：** 1 天

---

### Task F6：学员搜索能力（接全局搜索预留）

**Spec 章节：** §4.6（全局搜索 跨四种实体）

**前置任务：** F2、F3

**Files：**
- 新建：`search/service/SearchService.java`（先只接学员 / 家长，I1 任务再加班级 / 课次）
- 新建：`search/controller/SearchController.java`、`search/dto/SearchHit.java`、`SearchQuery.java`
- 测试：`src/test/java/com/eduze/manage/search/SearchServiceIT.java`

**实施要点：**
- `GET /api/search?q=&types=student,guardian&limit=20`：返回 `{type, id, title, subtitle, url, branch}` 列表
- 学员：`name LIKE` 或 `enroll_no LIKE`，subtitle 显示班级和校区
- 家长：`name LIKE` 或 `phone LIKE`，subtitle 显示其学员
- 用 `LIMIT 20`，分类型混排，按相关性（精确匹配 enroll_no / phone 优先）
- 每类型最多 10 条

**验收标准：**
- IT：插入测试数据，搜 `张` 返回学员 + 家长各 N 条，精确匹配 enroll_no 排第一

**预估粒度：** 0.5 天

---

### Task F7：学员列表页（DataTable + 筛选 + URL 同步）

**Spec 章节：** §4.3（学员列表 UI）、§4.6、§7.6

**前置任务：** F2、C8、E3

**Files：**
- 新建：`web/src/features/student/api.ts`、`types.ts`、`schemas.ts`
- 新建：`web/src/features/student/pages/StudentListPage.tsx`、`components/StudentFilterBar.tsx`
- 新建：`web/src/hooks/useUrlState.ts`
- 修改：`web/src/app/router.tsx`

**实施要点：**
- 列：姓名 / 入园编号 / 性别 / 年龄（前端算）/ 班级（联表展示，F2 后端要在 `StudentResponse` 带 `classGroups: [{id, name}]`） / 课时余额（≤5 红色）/ 状态 / 操作
- 筛选：校区下拉、班级下拉、状态、低余额 checkbox
- `useUrlState` 把筛选条件双向同步到 `searchParams`，刷新页面保持
- 点击行 → 打开 StudentDetailSheet（F8 任务）
- 移动端用 `mobileCardRender`：卡片 2 行：第一行姓名 + 状态徽标；第二行 班级 + 课时余额
- 顶部按钮：新建学员（`mod+n` 触发） / 批量导入

**验收标准：**
- 浏览器看到 5 列分页表；筛选后 URL 含参数；移动端宽度看到卡片
- 余额 ≤5 高亮红色

**预估粒度：** 1 天

---

### Task F8：学员详情 Sheet + Tabs

**Spec 章节：** §4.3（详情 Sheet + Tabs）

**前置任务：** F7、C5

**Files：**
- 新建：`web/src/features/student/components/StudentDetailSheet.tsx`、`components/StudentBasicTab.tsx`、`StudentGuardiansTab.tsx`、`StudentPackagesTab.tsx`、`StudentAttendanceTab.tsx`、`StudentLeaveTab.tsx`
- 修改：`web/src/features/student/api.ts`

**实施要点：**
- 5 个 Tab：基础信息（只读 + 编辑入口）/ 家长（列表 + 新增 / 解绑）/ 课时包（列表 + 新增 / 编辑）/ 出勤记录（最近 30 天表格，H 阶段接通）/ 请假记录（H 阶段接通）
- 顶部操作：编辑、停学（确认弹窗）、调班（占位，G 阶段补）
- 加载用 `<Skeleton>` 占位
- 出勤 / 请假 Tab 在 H 阶段前显示空态 `<EmptyState>第 H 阶段后启用</EmptyState>` —— **第一期完成时不应保留此占位**，H10 完成后必须替换为真实数据。临时占位用 `TODO: H10` 注释

**验收标准：**
- 桌面端右滑出现 Sheet；Tab 切换流畅；ESC 关闭
- 移动端全屏

**预估粒度：** 1 天

---

### Task F9：新建 / 编辑学员表单（含家长 sub-flow）

**Spec 章节：** §4.3（RHF + Zod；现场新建家长；Toast 撤销）

**前置任务：** F7、F3、C4、C6

**Files：**
- 新建：`web/src/features/student/components/StudentFormDialog.tsx`、`components/GuardianSubForm.tsx`、`components/SectionStepper.tsx`
- 修改：`web/src/features/student/schemas.ts`

**实施要点：**
- 4 段：基础信息 → 健康 → 家长 → 班级（班级在 G 阶段后启用，第一期可保留"暂不分班"选项）
- 家长 sub-flow：可勾选已有家长（用 `Combobox` 输入手机号查询）或现场新建
- 提交成功 → toast.undo（4.2s 内点击撤销 → 调 DELETE 软删）
- 全字段 Zod：`name min(1).max(64)`、`enrollNo regex(/^[A-Za-z0-9-]{2,32}$/)`、`birthday date max today`、`emergencyPhone regex(/^1[3-9]\d{9}$/)`

**验收标准：**
- 必填项校验在前端 + 后端都报错；服务端 `TenantUnique` 错时定位到字段
- toast 撤销成功后列表立即移除该行

**预估粒度：** 1 天

---

### Task F10：状态切换 + 批量操作收尾

**Spec 章节：** §4.3（停学、批量分班 / 调班）

**前置任务：** F8、F9

**Files：**
- 修改：`web/src/features/student/components/StudentDetailSheet.tsx`（停学 / 复学按钮）
- 新建：`web/src/features/student/components/BulkActionBar.tsx`、`components/BulkAssignClassDialog.tsx`
- 修改：`web/src/features/student/pages/StudentListPage.tsx`（多选）
- 修改：`student/controller/StudentController.java`：`POST /api/students/bulk/assign-class`（依赖 G3）和 `POST /api/students/bulk/transfer-class`（G3 后启用）

**实施要点：**
- 列表多选 → 出现底部 sticky `BulkActionBar`
- 第一期支持的批量动作：批量分班、批量调班（依赖 G 阶段班级 API；先做接口骨架在 F10 调用，G3 完成后接通）
- 状态切换：停学 / 复学 → toast.undo

**验收标准：**
- 多选 3 个学员，批量分班调用一次接口；toast 撤销恢复
- 停学后状态徽标变灰、列表筛选 `status=停学` 能查到

**预估粒度：** 0.5 天（注意 G3 完成前 BulkAssignClassDialog 仅占位）

---

## 阶段 G：模块 #3 课程与排课

### Task G1：建表 — 课程 / 班级 / 画室 / 课次 / 调课日志

**Spec 章节：** §5.2

**前置任务：** D1、F1

**Files：**
- 新建：`src/main/resources/db/migration/V1.2.0__init_course.sql`

**实施要点：**
- `t_course (id, tenant_id, name, age_min int, age_max int, lesson_minutes int, cover_url, description text)`，`UNIQUE(tenant_id, name, deleted_at)`
- `t_class_group (id, tenant_id, branch_id, name, course_id, head_teacher_id (user), capacity, status)`
- `t_class_room (id, tenant_id, branch_id, name, capacity, note)`
- `t_student_class_group (id, tenant_id, student_id, class_group_id, joined_at, left_at)`
- `t_lesson (id, tenant_id, branch_id, class_group_id, class_room_id, teacher_id, start_at datetime, end_at datetime, status, note)`，索引 `(tenant_id, branch_id, start_at)`、`(class_group_id, start_at)`、`(teacher_id, start_at)`、`(class_room_id, start_at)`
- `t_lesson_change_log (id, tenant_id, lesson_id, change_type varchar 32, before_json json, after_json json, reason varchar 256, operator_id)`

**验收标准：**
- Flyway 通过；冲突检测所需索引齐全

**预估粒度：** 0.5 天

---

### Task G2：Course / ClassGroup / ClassRoom CRUD

**Spec 章节：** §4.4（课程产品 / 班级 / 画室）

**前置任务：** G1、D7

**Files：**
- 新建：`course/domain/*.java`、对应 Mapper / Service / Controller、`course/dto/*.java`
- 测试：`src/test/java/com/eduze/manage/course/CourseControllerIT.java`、`ClassGroupControllerIT.java`、`ClassRoomControllerIT.java`

**实施要点：**
- 三套 CRUD 接口（`/api/courses`、`/api/class-groups`、`/api/class-rooms`），权限点 `course:write` / `classgroup:write` / `classroom:write`
- `ClassGroupResponse` 聚合：班级名 + 课程名 + 班主任名 + 当前人数 + 容量
- 删除前校验：课程下无班级、班级下无活跃成员、画室下无未来课次

**验收标准：**
- 三套 IT 通过；删除约束生效

**预估粒度：** 1 天

---

### Task G3：班级成员管理（加入 / 转出 / 转班）

**Spec 章节：** §4.4（班级成员管理）

**前置任务：** G2、F2

**Files：**
- 新建：`course/controller/ClassGroupMemberController.java`、`course/service/ClassGroupMemberService.java`
- 测试：`src/test/java/com/eduze/manage/course/ClassGroupMemberIT.java`

**实施要点：**
- 接口：
  - `GET /api/class-groups/{id}/members`
  - `POST /api/class-groups/{id}/members`：批量加入 `{studentIds: [...]}`，超容报 `BizException`
  - `DELETE /api/class-groups/{id}/members/{studentId}`：转出（写 `left_at`）
  - `POST /api/students/bulk/assign-class`：F10 已埋接口
  - `POST /api/students/bulk/transfer-class`：`{studentIds, fromClassGroupId, toClassGroupId}`，事务内 leftA + joinB
- 容量计算：`COUNT(left_at IS NULL)`

**验收标准：**
- IT：加入 5 人到容量 10 班 OK；再加 6 人超容失败；转班后两边人数正确

**预估粒度：** 0.5 天

---

### Task G4：Lesson 单次新建 + 按周批量生成

**Spec 章节：** §4.4（单次新建、按周生成 N 周模板）

**前置任务：** G2、G3

**Files：**
- 新建：`lesson/domain/Lesson.java`、`LessonChangeLog.java`、对应 Mapper / Service / Controller
- 新建：`lesson/dto/LessonRequest.java`、`LessonResponse.java`、`BulkGenerateRequest.java`、`BulkGenerateResult.java`
- 测试：`src/test/java/com/eduze/manage/lesson/LessonGenerateIT.java`

**实施要点：**
- 接口：
  - `POST /api/lessons`：单次新建
  - `POST /api/lessons:bulk-generate`：`{classGroupId, weekdays:[1,3], startTime:"09:00", endTime:"10:30", durationMinutes:90, classRoomId, teacherId, weeks:8, fromDate:"2026-05-11"}` 生成 N 周课次
  - `GET /api/lessons?branchId=&classGroupId=&teacherId=&from=&to=`
  - `GET /api/lessons/{id}`、`DELETE /api/lessons/{id}`（取消，软删 + 写日志）
- 批量生成：返回 `{generated: int, conflicts: [{date, reason}]}`，冲突检测调用 G6 服务（先骨架，G6 接通）
- 时间存 UTC，前端按 `Asia/Shanghai` 显示

**验收标准：**
- IT：8 周双周三五 → 生成 16 节课次；冲突点跳过并报告

**预估粒度：** 1 天

---

### Task G5：调课 / 取消课次 + LessonChangeLog

**Spec 章节：** §4.4（调课、取消课次、变动写入日志）

**前置任务：** G4、B4

**Files：**
- 修改：`lesson/service/LessonService.java`
- 新建：`lesson/controller/LessonChangeController.java`（或并入 LessonController）
- 测试：`src/test/java/com/eduze/manage/lesson/LessonRescheduleIT.java`

**实施要点：**
- 接口：
  - `POST /api/lessons/{id}/reschedule`：`{startAt, endAt, teacherId, classRoomId, reason}`，触发冲突检测，记录 `before_json / after_json`
  - `POST /api/lessons/{id}/cancel`：`{reason}` 必填，写日志，状态置 `CANCELLED`
- `LessonChangeLog` 用 FastJSON2 序列化前后镜像
- 已完成的课次（`status=COMPLETED`）禁止调课

**验收标准：**
- IT：调课成功 → 日志一条；尝试调已完成课次 → 422
- 取消有 reason 必填校验

**预估粒度：** 0.5 天

---

### Task G6：冲突检测服务

**Spec 章节：** §4.4（同教师 / 画室 / 班级冲突弹窗警告）、§11.2 R1（软警告）

**前置任务：** G4

**Files：**
- 新建：`lesson/service/ConflictService.java`、`lesson/dto/ConflictReport.java`
- 测试：`src/test/java/com/eduze/manage/lesson/ConflictServiceIT.java`

**实施要点：**
- `check(LessonDraft draft) -> ConflictReport { teacher: Lesson?, classRoom: Lesson?, classGroup: Lesson? }`
- 三个维度的查询都使用区间相交：`start_at < newEnd AND end_at > newStart` 且 `status != CANCELLED`
- 只产出报告，**不强制阻塞**；调用方决定是否抛异常
- 单次新建 / 调课 / 批量生成都调用此服务
- API：`POST /api/lessons:check-conflict`（独立接口）供前端"调课确认"前预检

**验收标准：**
- IT 覆盖 4 种冲突场景（无冲突 / 教师冲 / 画室冲 / 班级冲）

**预估粒度：** 0.5 天

---

### Task G7：周课表查询 API

**Spec 章节：** §4.4（周课表横轴星期 / 纵轴时段）、§9.1（P95 < 800ms）

**前置任务：** G4

**Files：**
- 新建：`lesson/controller/ScheduleController.java`、`lesson/service/ScheduleService.java`、`lesson/dto/ScheduleQuery.java`、`ScheduleResponse.java`
- 测试：`src/test/java/com/eduze/manage/lesson/ScheduleServiceIT.java`

**实施要点：**
- `GET /api/schedule/week?branchId=&weekStart=2026-05-11`：返回 7 天课次列表，每条含课次基础字段 + 班级名 + 课程名 + 教师名缩写 + 画室缩写 + 颜色（按课程 hash 映射 §7 调色板）
- 单次查询 = 一条 SQL `WHERE start_at >= ? AND start_at < ? + 7 days`，已建索引
- 测试：插 30 节课次 → 查询返回 7 天分组、按 start_at 排序

**验收标准：**
- IT 通过；本地 1000 节课次量级响应 P95 < 200ms

**预估粒度：** 0.5 天

---

### Task G8：课程 / 班级 / 画室管理页面

**Spec 章节：** §4.4（班级列表 / 班级详情）

**前置任务：** G2、C8、E3

**Files：**
- 新建：`web/src/features/course/pages/CourseListPage.tsx`、`ClassGroupListPage.tsx`、`ClassRoomListPage.tsx`
- 新建：`web/src/features/course/components/CourseFormDialog.tsx`、`ClassGroupFormDialog.tsx`、`ClassGroupDetailSheet.tsx`、`ClassRoomFormDialog.tsx`、`ClassMemberPanel.tsx`
- 新建：`web/src/features/course/api.ts`、`types.ts`、`schemas.ts`
- 修改：`web/src/app/router.tsx`、`web/src/app/shell/Sidebar.tsx`

**实施要点：**
- 三个列表页用 DataTable，结构与学员列表一致
- `ClassGroupDetailSheet`：Tabs（基本信息 / 成员 / 即将到来的课次）
- 成员 Tab：使用 ClassMemberPanel 组件，支持添加（搜学员，多选）、转出（确认弹窗）、转班（选目标班级）
- 容量进度条：`<Progress value={current/capacity} />`，超容红色

**验收标准：**
- 三个页面 CRUD 全流程；成员管理交互流畅
- 移动端可用

**预估粒度：** 1 天

---

### Task G9：周课表视图（日历式）

**Spec 章节：** §4.4（周课表视图、可前后翻周、颜色按课程）

**前置任务：** G7、E3

**Files：**
- 新建：`web/src/features/lesson/pages/WeeklySchedulePage.tsx`、`components/WeeklyGrid.tsx`、`components/WeekNavigator.tsx`、`components/LessonCell.tsx`
- 新建：`web/src/features/lesson/api.ts`、`types.ts`
- 修改：`web/src/app/router.tsx`、`Sidebar.tsx`

**实施要点：**
- 横轴 7 天，纵轴 8:00–22:00 半小时颗粒，CSS Grid `grid-template-columns: 80px repeat(7, 1fr); grid-template-rows: repeat(28, 32px);`
- 每个课次定位：`grid-column: weekdayIndex+1; grid-row: startSlot / endSlot;`
- 翻周：`<` `本周` `>` 三按钮 + 日期范围显示
- 颜色：根据 `course.id` hash 映射到调色板（最多 12 色，超出回环）
- 点击课次 → 打开 LessonDetailSheet（G10）
- 移动端：`< md` 切换为"列表式"按天分组

**验收标准：**
- 桌面端能看到完整周视图；翻周流畅
- 课次卡显示 班级名 + 教师姓 + 画室缩写
- 颜色稳定（同一课程跨周保持一致）

**预估粒度：** 1.5 天（这是第一期视觉最重的任务，可拆 G9a 单元格 / G9b 翻周 + 数据接通）

---

### Task G10：课次详情 Sheet + 调课 / 取消 / 学员花名册

**Spec 章节：** §4.4（课次详情 Sheet + 调课操作）

**前置任务：** G9、G5、F8

**Files：**
- 新建：`web/src/features/lesson/components/LessonDetailSheet.tsx`、`components/RescheduleDialog.tsx`、`components/CancelLessonDialog.tsx`、`components/LessonRosterTable.tsx`、`components/BulkGenerateDialog.tsx`
- 新建：`web/src/features/lesson/components/ConflictWarningDialog.tsx`

**实施要点：**
- LessonDetailSheet 内容：基础信息 / 学员花名册（点名入口在 H 阶段接通）/ 调课 / 取消按钮 / 调课历史日志
- RescheduleDialog：先调用 `POST /api/lessons:check-conflict`，有冲突弹 ConflictWarningDialog（"软警告"，可继续）；用户确认后再调 reschedule
- CancelLessonDialog：必填原因
- BulkGenerateDialog：从班级详情入口触发，对应 G4 接口

**验收标准：**
- 调课冲突弹"软警告"对话框，"仍然继续"后写入成功
- 取消课次后周视图该格变灰 + 划线

**预估粒度：** 1 天

---

## 阶段 H：模块 #4 签到与接送

### Task H1：建表 — 出勤 / 接送 / 请假

**Spec 章节：** §5.2

**前置任务：** D1、F1、G1

**Files：**
- 新建：`src/main/resources/db/migration/V1.3.0__init_attendance.sql`

**实施要点：**
- `t_attendance (id, tenant_id, branch_id, lesson_id, student_id, status tinyint, check_in_at, check_out_at, check_in_method varchar 16 (manual/qr/face_reserved), note)`，`UNIQUE(tenant_id, lesson_id, student_id, deleted_at)`
- `t_pickup_record (id, tenant_id, branch_id, attendance_id, event_type varchar 8 (in/out), guardian_id, is_abnormal tinyint, abnormal_note, event_time)`
- `t_leave_request (id, tenant_id, branch_id, student_id, lesson_id BIGINT NULL, leave_start_date, leave_end_date, reason varchar 256, status tinyint (1=待审 2=已批 3=已拒), approved_by, approved_at)`
- 索引：`(tenant_id, branch_id, lesson_id)`、`(student_id, lesson_id)`、`(status, leave_start_date)`

**验收标准：**
- Flyway 通过；唯一键 `(lesson_id, student_id)` 防重复签到

**预估粒度：** 0.5 天

---

### Task H2：Attendance API（手动签到 + 二维码扫码）

**Spec 章节：** §4.5（前台手动 + 二维码扫码、人脸识别预留位）

**前置任务：** H1、G4、F3

**Files：**
- 新建：`attendance/domain/Attendance.java`、对应 Mapper / Service / Controller、`attendance/dto/CheckInRequest.java`、`AttendanceResponse.java`
- 测试：`src/test/java/com/eduze/manage/attendance/AttendanceControllerIT.java`

**实施要点：**
- 接口：
  - `GET /api/attendance/today?branchId=&period=morning|afternoon|evening`：今日所有应到学员（按 `t_lesson` 当日课次的成员展开），带状态
  - `POST /api/attendance/check-in`：`{lessonId, studentId, method:'manual'|'qr', guardianId?}`，写 attendance + pickup_record(in)；method=qr 时根据 `guardian.qr_code` 反查 guardian
  - `POST /api/attendance/check-out`：`{attendanceId, guardianId, isAbnormal, abnormalNote}`
  - `PATCH /api/attendance/{id}`：班主任更正状态（请假 / 缺勤）
  - `POST /api/guardians/{id}/qr`：生成二维码（保存 `guardian.qr_code = UUID`，前端展示二维码图片）
- `check_in_method` 枚举包含 `face_reserved`（永远不被业务赋值，仅占位扩展）
- 重复签到：唯一键约束抛 `DuplicateKeyException` → `BizException("学员已签到")`

**验收标准：**
- IT：今日花名册数量 = 当日所有课次成员之和；签到后状态正确
- QR 场景：先生成 QR → POST `qr_code` 触发签到成功

**预估粒度：** 1 天

---

### Task H3：PickupRecord API + 异常标记

**Spec 章节：** §4.5（接送人记录 / 异常）

**前置任务：** H2

**Files：**
- 新建：`attendance/domain/PickupRecord.java`、`PickupRecordMapper.java`、`PickupService.java`、`PickupController.java`
- 测试：`src/test/java/com/eduze/manage/attendance/PickupServiceIT.java`

**实施要点：**
- `GET /api/pickup-records?lessonId=&studentId=&isAbnormal=`
- `POST /api/pickup-records`：手动补录（前台漏勾时使用）
- 校验：guardian 必须在 student 的家长列表里且 `can_pickup=1`，否则 `is_abnormal=1` 但允许保存（带备注）
- `GET /api/pickup-records/abnormal?from=&to=`：异常列表

**验收标准：**
- IT：陌生家长接 → 默认 `isAbnormal=1`；正常接送人 → 0

**预估粒度：** 0.5 天

---

### Task H4：LeaveRequest 提交 / 审批

**Spec 章节：** §4.5（请假状态机）

**前置任务：** H2

**Files：**
- 新建：`attendance/domain/LeaveRequest.java`、`LeaveRequestMapper.java`、`LeaveService.java`、`LeaveController.java`
- 测试：`src/test/java/com/eduze/manage/attendance/LeaveServiceIT.java`

**实施要点：**
- 接口：
  - `POST /api/leaves`：前台代录入 `{studentId, lessonId?, leaveStartDate, leaveEndDate, reason}`，状态 `PENDING`
  - `GET /api/leaves?status=&studentId=&from=&to=`
  - `POST /api/leaves/{id}/approve`：班主任审批 → `APPROVED`，并把日期范围内涉及的 `t_attendance.status = LEAVE`（若已存在）；若签到记录尚未生成（课次未到）则在签到接口中读取已批请假优先置 LEAVE
  - `POST /api/leaves/{id}/reject`：`REJECTED`
- 权限：`leave:write` 录入、`leave:approve` 审批

**验收标准：**
- IT：批准请假后已存在的 attendance 状态变为 LEAVE；后续触发签到的课次自动 LEAVE

**预估粒度：** 1 天

---

### Task H5：自动缺勤定时任务

**Spec 章节：** §4.5（课次结束未签到自动缺勤）

**前置任务：** H2

**Files：**
- 新建：`attendance/service/AbsenceJob.java`
- 修改：`EduzeManageApplication.java`（`@EnableScheduling`）
- 测试：`src/test/java/com/eduze/manage/attendance/AbsenceJobIT.java`

**实施要点：**
- `@Scheduled(cron = "0 */15 * * * *")` 每 15 分钟扫描 1 小时前 `end_at` 的课次：
  - 对每节课次的所有班级成员，若无 attendance 记录且无 APPROVED leave 覆盖该日期 → 插入 `attendance status=ABSENT, method=auto`
- 用分布式锁防多实例（Redis SETNX `lock:absence-job` TTL 10 分钟）
- 超管手动触发接口 `POST /api/admin/jobs/absence:run`（`audit:read` 权限）

**验收标准：**
- IT：构造一节 1 小时前结束的课次 + 班级成员 → 调用 job 后 attendance 表新增 ABSENT
- 已请假学员不被标缺

**预估粒度：** 0.75 天

---

### Task H6：出勤统计 API

**Spec 章节：** §4.5（学员 / 班级 / 校区维度）

**前置任务：** H2、H5

**Files：**
- 新建：`attendance/service/AttendanceStatService.java`、`attendance/controller/AttendanceStatController.java`
- 新建：`attendance/dto/StudentAttendanceStat.java`、`ClassGroupAttendanceStat.java`、`BranchAttendanceStat.java`
- 测试：`src/test/java/com/eduze/manage/attendance/AttendanceStatIT.java`

**实施要点：**
- 接口：
  - `GET /api/stats/attendance/student/{id}?from=&to=`：返回 `{total, present, absent, leave, rate}`
  - `GET /api/stats/attendance/class-group/{id}?from=&to=`
  - `GET /api/stats/attendance/branch/{id}?from=&to=`：再聚合一层
- 出勤率定义：`present / (present + absent + leave)`，请假记入分母（spec §4.5 出勤统计含义按机构惯例确定，本期默认按"应到"=已签到+缺勤+请假；实施时如机构反馈可调）
- 默认时间范围：本月

**验收标准：**
- IT：构造 10 节课次 / 8 出勤 / 1 缺 / 1 假 → 学员维度 rate=80%、班级维度求平均

**预估粒度：** 0.5 天

---

### Task H7：签到工作台前端

**Spec 章节：** §4.5（签到工作台 UI）

**前置任务：** H2、H3、E3、C8

**Files：**
- 新建：`web/src/features/attendance/pages/AttendanceWorkbenchPage.tsx`、`components/AttendanceRosterTable.tsx`、`components/PickupSelectDialog.tsx`、`components/CheckInButton.tsx`、`components/AttendanceStatusBadge.tsx`
- 新建：`web/src/features/attendance/api.ts`、`types.ts`
- 修改：`web/src/app/router.tsx`、`Sidebar.tsx`

**实施要点：**
- 顶部筛选：校区下拉 + 时段（上午 / 下午 / 晚上） + 日期（默认今天）
- 中间花名册：每行 学员名 + 班级 + 应到时段 + 状态色块（未到=灰 / 已入园=绿 / 已离园=蓝 / 缺勤=红 / 请假=黄）
- 签到入口：行右侧"签到"按钮，弹 PickupSelectDialog 选择接送家长（家长列表自动过滤 `can_pickup=1`）
- 离园：状态=已入园 时按钮变"离园"
- 顶部"扫码"按钮（H8 接通）
- 右上角实时计数：`X / Y 已入园`，自动每 30 秒刷新（TanStack Query `refetchInterval`）

**验收标准：**
- 签到流程一键走通；状态色块即时变化（乐观更新 + invalidate）
- 移动端：花名册转换为大卡片，签到按钮全宽

**预估粒度：** 1.5 天

---

### Task H8：二维码扫描组件

**Spec 章节：** §4.5（二维码扫码、调摄像头）

**前置任务：** H7

**Files：**
- 修改：`web/package.json`（`@zxing/browser@^0.1.5`、`@zxing/library@^0.21.3`）
- 新建：`web/src/features/attendance/components/QrScanDialog.tsx`、`components/CameraPermissionAlert.tsx`
- 新建：`web/src/features/attendance/components/StudentQrCodePanel.tsx`（在学员详情/家长详情显示家长 QR）
- 修改：`web/src/features/student/components/StudentGuardiansTab.tsx`（新增"显示二维码"按钮）

**实施要点：**
- `QrScanDialog`：调 `getUserMedia({ video: { facingMode: 'environment' } })`，用 `@zxing/browser` 持续解码
- 解码到 QR → 调 `/api/attendance/check-in`（method=qr，guardianId 由后端按 qr_code 反查），成功 toast，可继续扫
- 错误：摄像头被禁 → 显示 `CameraPermissionAlert` 提示
- 关闭弹窗：停止摄像头（`track.stop()`）
- 家长 QR 在 `StudentGuardiansTab` 用 `qrcode.react` 渲染（轻量）

**验收标准：**
- 桌面 / 移动端摄像头可调用
- 解码后自动签到，失败有提示
- 关闭弹窗摄像头释放（无设备指示灯常亮）

**预估粒度：** 1 天

---

### Task H9：请假管理 UI（列表 + 审批）

**Spec 章节：** §4.5（请假管理 列表 + 详情 + 审批）

**前置任务：** H4、E3

**Files：**
- 新建：`web/src/features/attendance/pages/LeaveListPage.tsx`、`components/LeaveDetailSheet.tsx`、`components/LeaveRequestFormDialog.tsx`、`components/LeaveApprovalActions.tsx`
- 修改：`web/src/app/router.tsx`、`Sidebar.tsx`

**实施要点：**
- 列表 Tab：待审批 / 已审批 / 已拒绝；筛选：校区 / 学员 / 班主任
- 详情 Sheet：学员信息 / 请假区间 / 原因 / 历史审批
- 审批：仅 `leave:approve` 权限可见按钮；批准 / 拒绝弹确认
- 录入：前台用 `LeaveRequestFormDialog`，可选择具体课次或日期范围

**验收标准：**
- 待审批列表实时更新；批准后状态变化 + Toast；
- F8 学员详情页"请假记录"Tab 接通真实数据

**预估粒度：** 1 天

---

### Task H10：出勤统计看板 + 接通 F8 / G10 占位

**Spec 章节：** §4.5（出勤统计看板）、§4.3（学员详情出勤记录）、§4.4（课次花名册）

**前置任务：** H6、H7、F8、G10

**Files：**
- 新建：`web/src/features/attendance/pages/AttendanceStatsPage.tsx`、`components/StudentAttendanceTrend.tsx`、`components/ClassAttendanceCard.tsx`、`components/BranchAttendanceKpis.tsx`
- 修改：`web/src/features/student/components/StudentAttendanceTab.tsx`（接通 H6 学员维度）
- 修改：`web/src/features/lesson/components/LessonRosterTable.tsx`（在课次详情显示已到 / 未到 + 行内签到入口）
- 新增 Dashboard：`web/src/features/dashboard/pages/PrincipalDashboardPage.tsx`（KPICard 看板嵌入校长仪表盘，第一期把它放到 `/`）

**实施要点：**
- 看板 KPI：本周缺勤人次 / 本月平均出勤率 / 本周新生数（从学员表 `enroll_date >= weekStart`）/ 本周课次数
- 学员维度趋势：4 周柱状图（用简单 SVG 或 `recharts@^2.13.0`，新增依赖）
- 课次花名册：和 H7 工作台共用 AttendanceRosterTable，但只读 + 行内"签到"按钮（前台用）
- 替换 F8 / G10 阶段的占位 EmptyState

**验收标准：**
- `/` 路径展示校长看板，KPI 数字滚动动画正常
- F8 学员详情"出勤"Tab 显示该学员近期出勤记录
- G10 课次详情花名册显示状态色块

**预估粒度：** 1 天

---

## 阶段 I：联调 / 部署

### Task I1：全局搜索后端聚合（接通班级 / 课次）

**Spec 章节：** §4.6（搜索跨学员 / 家长 / 班级 / 课次）

**前置任务：** F6、G2、G4

**Files：**
- 修改：`search/service/SearchService.java`（增加 ClassGroup、Lesson）
- 修改：`search/dto/SearchHit.java`（type 枚举增加 `class_group`、`lesson`）
- 测试：`src/test/java/com/eduze/manage/search/SearchServiceFullIT.java`

**实施要点：**
- 班级：`name LIKE` → subtitle 课程名 + 校区
- 课次：`class_group.name LIKE` 或 `start_at` 文本（如"5 月 11 日"模糊匹配较复杂，第一期仅匹配班级名 + 教师名 + start_at 日期串）
- 各类型最多 10 条；有 `branch_id` 的实体走数据范围拦截器

**验收标准：**
- IT：搜 `周六` 同时返回班级 + 课次；权限为单校区时不返回其他校区结果

**预估粒度：** 0.5 天

---

### Task I2：CommandPalette 接通真实搜索 + 跳转

**Spec 章节：** §4.6

**前置任务：** I1、E5、F8、G10

**Files：**
- 修改：`web/src/app/shell/CommandPalette.tsx`、`web/src/lib/searchClient.ts`
- 新建：`web/src/lib/openEntity.ts`（按 `SearchHit.type + id` 跳转并打开对应 Sheet）

**实施要点：**
- 输入防抖 300ms，调 `/api/search`；loading 显示骨架
- 4 类结果分组渲染（学员 / 家长 / 班级 / 课次），每组带图标
- 选中后：
  - 学员 → `navigate('/students?openId=' + id)`，列表页监听 `openId` 自动打开 Sheet
  - 家长 → `navigate('/students?openGuardianId=' + id)`
  - 班级 → `/courses/class-groups?openId=`
  - 课次 → `/schedule?openLessonId=&date=`，周课表页打开 LessonDetailSheet 并跳到对应周

**验收标准：**
- Cmd+K 搜索 → 选中 → 准确跳到目标实体并打开对应 Sheet

**预估粒度：** 0.75 天

---

### Task I3：Docker Compose（app + mysql + redis + nginx）

**Spec 章节：** §6.1（部署形态）、§9.3（单机 Docker Compose）

**前置任务：** A8、D1（任意一个 Flyway 任务即可，但实操放在最后）

**Files：**
- 新建：`docker/Dockerfile`、`docker/docker-compose.yml`、`docker/docker-compose.dev.yml`、`docker/nginx.conf`、`docker/.env.example`
- 修改：`README.md`

**实施要点：**
- `Dockerfile` 多阶段：阶段一 `eclipse-temurin:17-jdk` 跑 `./mvnw -DskipTests package`（实际生产 CI 中提前 build，镜像内只 COPY jar）；本期采用"先本地 mvn package，再 COPY jar"的简洁模式
- `docker-compose.yml` 服务：
  - `app`：image `eduze-manage:latest`，env：`SPRING_PROFILES_ACTIVE=prod`、`JWT_SECRET`、`DB_*`、`REDIS_*`、`EDUZE_STORAGE_LOCAL_ROOT=/data/storage`，挂载 `./storage:/data/storage`、`./logs:/app/logs`，依赖 mysql / redis
  - `mysql`：`mysql:8.0.36`，挂载数据卷
  - `redis`：`redis:7.2-alpine`
  - `nginx`：`nginx:1.27`，挂载 `nginx.conf`、TLS 证书目录占位 `./certs:/etc/nginx/certs`
- `docker-compose.dev.yml`：仅 mysql + redis（开发用），方便本地起后端
- `nginx.conf`：80 → 443 跳转、443 反代 `app:8080`、静态 gzip、`add_header Strict-Transport-Security`
- `.env.example`：所有必需环境变量列出

**验收标准：**
- `docker compose up -d` 启动后，访问 `https://localhost`（自签证书）能看到登录页
- `docker compose logs app` 无 ERROR
- 关闭后再起，数据持久化（学员仍在）

**预估粒度：** 1 天

---

### Task I4：备份 / 恢复脚本 + 部署文档

**Spec 章节：** §9.3（每天 mysqldump）

**前置任务：** I3

**Files：**
- 新建：`scripts/backup-mysql.sh`、`scripts/restore-mysql.sh`、`scripts/cron.example`
- 修改：`README.md`、新建 `docs/operations/runbook.md`

**实施要点：**
- `backup-mysql.sh`：`mysqldump --single-transaction --routines --events eduze | gzip > /backups/eduze-$(date +%F-%H%M).sql.gz`，保留最近 30 个文件（`find ... -mtime +30 -delete`）
- `restore-mysql.sh`：参数 `${BACKUP_FILE}`，`gunzip -c | mysql eduze`
- `cron.example`：`0 3 * * * /opt/eduze/scripts/backup-mysql.sh`
- `runbook.md` 覆盖：首次部署、升级版本、备份恢复、查日志、改 JWT secret 滚动、新增校区超管账号

**验收标准：**
- 在 docker compose 环境跑一次 backup → 产生 .sql.gz；restore 到全新库后数据恢复一致

**预估粒度：** 0.5 天

---

### Task I5：端到端冒烟测试（Vitest + Playwright）

**Spec 章节：** §9.1（性能目标）、§9.4

**前置任务：** I3、E2、F7、G9、H7

**Files：**
- 修改：`web/package.json`（`@playwright/test@^1.48.0`）
- 新建：`web/playwright.config.ts`、`web/e2e/login.spec.ts`、`students.spec.ts`、`schedule.spec.ts`、`attendance.spec.ts`
- 新建：`scripts/e2e-bootstrap.sh`（启动 docker compose + 等服务就绪 + 跑 playwright）

**实施要点：**
- Playwright 用 `webServer: { command: 'pnpm dev', port: 5173 }` + 后端独立 `./mvnw spring-boot:run` 通过 GitHub Actions 串联（可选）
- 4 个 spec 覆盖：
  - login：admin 登录成功 / 错误密码失败
  - students：登录后建一个学员、关联家长、看到列表
  - schedule：建班、批量生成一周、周课表显示
  - attendance：在工作台签到一节课次，状态变化
- 数据隔离：每个 spec 用独立租户？第一期单租户，spec 间用唯一 `enroll_no` 后缀（`E2E-${nanoid()}`）

**验收标准：**
- `pnpm e2e` 4 个 spec 全绿
- 单 spec 时间 < 30 秒

**预估粒度：** 1 天

---

## 5. Spec 覆盖度自检

| Spec 章节 | 覆盖任务 |
|---|---|
| §4.1 第一期范围 | 全计划 |
| §4.1 第一期不做 | 严格未涉及（无作品 / 微信小程序 / 销售 / 招生 / 完整家校沟通 / 人脸识别 / 教师课时费结算 / 财务 / 多租户 UI / OSS） |
| §4.2 模块 0 | D1–D8、E1–E4 |
| §4.3 模块 #2 学员与家长 | F1–F10 |
| §4.4 模块 #3 课程与排课 | G1–G10 |
| §4.5 模块 #4 签到与接送 | H1–H10 |
| §4.6 全局体验（搜索 / Sheet / Drawer / Toast / 快捷键 / KPI 动画 / 骨架屏 / 响应式 / 无障碍） | C5–C8、E5–E7、F8、G9、H10 |
| §5.1–§5.2 数据模型 | D1、D2、F1、G1、H1、B4 |
| §5.3 tenant_id 预留 | B2、B3、A5（MetaObjectHandler） |
| §6.1 单 Jar 部署 + SPA fallback | A8、I3 |
| §6.2 后端栈 | A1–A6、B1、B4 |
| §6.3 前端栈 | A7、C1–C8 |
| §6.4 鉴权 | D3–D7、E1 |
| §6.5 文件存储 | infra/storage 在 D 阶段提供本地实现（可在 D8 后补 Task D8b：FileStorage 接口 + LocalDiskFileStorage + 学员头像上传 → 已合并到 F2 任务的"avatar_url"字段，第一期允许缺省，文件上传接口在 F9 表单可选实现） |
| §6.6 消息中心 | infra/message 在 D8 后任意时机加（B4 审计日志已建立切面骨架，消息中心依赖较小，可作为 I 阶段尾部 Task I0 增加；本期默认未单列，留作小附加项；如严格 spec 要求则必须实现） |
| §7.1–§7.6 设计系统 | C1–C8 |
| §8.1–§8.5 工程结构 | A1、A7、A8 |
| §9.1 性能目标 | G7（索引设计）、I5（端到端） |
| §9.2 安全 | D5（限流 + 锁定）、F2（脱敏开关）、B4（审计）、A2（secret 走环境变量） |
| §9.3 可用性 / 部署 | I3、I4 |
| §9.4 可观测性 | A1（actuator）、B4（审计）、A5（P6Spy） |

### 已识别的需要补做的小任务（未单独列入主线，需在执行时插入）

> **必须补充：** Task **F2.5：FileStorage 接口 + LocalDiskFileStorage + 学员头像上传接口**。Files：`infra/storage/FileStorage.java`、`LocalDiskFileStorage.java`、`StudentAvatarController.java`，前端 `StudentFormDialog` 增加头像上传字段。建议位于 F2 之后、F9 之前。预估 0.5 天。
>
> **必须补充：** Task **D8.5：MessageSender 接口 + LoggingMessageSender + t_outbox_message 表**。建表合入 `V1.4.0__init_outbox.sql`（可见于 0.2.1 节）；接口骨架供未来第二期对接微信小程序订阅消息。建议位于 D8 之后、E1 之前。预估 0.5 天。

把以上两条加入后总任务数为 **72**。

---

## 6. 执行顺序图（依赖关系，文字版）

```
A1 → A2 → A4 → A5 → B3 → D1 → D2 → D3 → D4 → D5 → D6 → D7 → D8 → D8.5 → E1 → E2 → E3 → E4 → E5 → E6 → E7
A1 → A3
A1 → A6
A1 → A7 → A8
A4/A5/A6 → B1
A2 → B2 → B3
B4 ← (A5, B2)
C1 ← A7
C2 ← C1
C3/C4/C5/C6/C7/C8 ← C3 ← C1
F1 ← D1; F2 ← F1+D7; F2.5 ← F2; F3 ← F2; F4 ← F2; F5 ← F3+F4; F6 ← F2+F3; F7 ← F2+C8+E3
F8 ← F7+C5; F9 ← F7+F3+C4+C6; F10 ← F8+F9
G1 ← D1+F1; G2 ← G1+D7; G3 ← G2+F2; G4 ← G2+G3; G5 ← G4+B4; G6 ← G4; G7 ← G4
G8 ← G2+C8+E3; G9 ← G7+E3; G10 ← G9+G5+F8
H1 ← D1+F1+G1; H2 ← H1+G4+F3; H3 ← H2; H4 ← H2; H5 ← H2; H6 ← H2+H5
H7 ← H2+H3+E3+C8; H8 ← H7; H9 ← H4+E3; H10 ← H6+H7+F8+G10
I1 ← F6+G2+G4; I2 ← I1+E5+F8+G10; I3 ← A8+D1; I4 ← I3; I5 ← I3+E2+F7+G9+H7
```

---

## 7. 风险与缓解（来自 spec §11.2 + 实施补充）

| # | 风险 | 缓解 |
|---|---|---|
| R1 | 前端 G9 周课表性能（大量 cell 同时渲染） | 用 CSS Grid 一次绘制；课次数目 > 200 时上 `react-window` 虚拟化（第二期再加） |
| R2 | H8 摄像头权限在 HTTP 下被浏览器禁用 | 本地开发用 `localhost`（豁免）；生产强制 HTTPS，I3 nginx 配置已含 |
| R3 | G6 冲突检测在大量课次下 SQL 慢 | G1 已加复合索引 `(teacher_id, start_at)`、`(class_room_id, start_at)`；I5 用 1000 节课次量级冒烟 |
| R4 | F5 Excel 导入超大文件（>10000 行）OOM | EasyExcel 流式解析；服务端限制单次 ≤ 5000 行；超出引导分批 |
| R5 | D7 数据范围拦截器漏拦（第二期新增表忘记加白名单） | 在 `BranchScopeInterceptor` 用注解扫描而非硬编码白名单：要拦截的实体加 `@BranchScoped` → 由 mapper 解析；本期可作为 D7 的实现细节 |
| R6 | 多人开发并发修改 `application.yml` | 各 Task 修改时仅追加 / 替换自己 section，避免全文重写；冲突在 PR 阶段解决 |
| R7 | A8 frontend-maven-plugin 在 CI 拉 Node 慢 | CI 缓存 `~/.cache/pnpm` + node_modules；本地开发常态用 dev 模式不走 mvn package |

---

## 8. 自我复核结论

经对照 spec §4.1 列举的"第一期要做"5 大块逐项扫描：

- ✅ 模块 0（账号 / 权限 / 校区底座）：D1–D8 完整覆盖；前端入口在 E3 Sidebar、E4 ProtectedRoute、`/me` 个人中心由 D7 提供
- ✅ 模块 #2（学员与家长）：F1–F10 完整覆盖
- ✅ 模块 #3（课程与排课）：G1–G10 完整覆盖；§11.2 R1 软警告已在 G6 落实
- ✅ 模块 #4（签到与接送）：H1–H10 完整覆盖；人脸识别预留在 H1 表的 `check_in_method=face_reserved` + H2 service 不赋值，符合 §4.5
- ✅ 跨模块全局体验：C 阶段设计系统 + E 阶段后台壳 + I2 全局搜索接通
- ✅ 严格未触碰 §4.1 排除项（作品 / 微信家长端 / 销售 / 招生 / 家校完整版 / 人脸 / 财务 / 多租户 UI / OSS）

唯一需要在执行阶段插入的两个未编号任务：F2.5（FileStorage + 头像上传）、D8.5（MessageSender 接口骨架）。建议在执行计划时将其作为 §0.1 阶段地图中第 D / F 阶段的标准 0.5 天任务并入。

总任务数 **72**，预估总工作量 **约 60 个工作日**（部分任务粒度 0.25–0.75 天，最重的 G9 / H7 各 1.5 天），按单人全职计算约 **12 周（3 个月）** 可交付第一期 MVP。

---

**（实施计划文档结束）**
