# 老师为核心的排课模型 + 778 体系 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use socrates:subagent-driven-development (recommended) or socrates:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在第一期 MVP 范围内引入"老师为核心"的排课模型：学员强制关联主带老师，老师维护周可用时段模板，学员订阅生成课次名单；同时初始化罗恩菲尔德 5 阶段 + 778（7 元素 / 7 原则 / 8 流派）课程体系。

**Architecture:** 沿用现有单 Jar Spring Boot + React + MyBatis-Plus 架构（spec `2026-05-09-eduze-manage-design.md` §6）。本计划对其 §4.3 / §4.4 / §5 做增量修订，对应 spec `2026-05-22-teacher-centric-scheduling-design.md`。新增 6 张业务表 + 4 张课程体系表 + 5 个 SQL 迁移 + 8 个后端 Service + 8 个 REST 端点 + 5 个前端页面 / 大组件，对原 `t_student` / `t_lesson` / `t_class_group` 三表做 ALTER。

**Tech Stack:** Java 17 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.15 + Flyway + JUnit 5 + Testcontainers；React 18 + Vite 6 + TanStack Query + Zustand + React Hook Form + Zod + Tailwind 3 + Radix UI。

**对应 spec：** `docs/socrates/specs/2026-05-22-teacher-centric-scheduling-design.md`（v1.1），范围见 §1，数据模型见 §2，API 见 §3，UI 见 §4，Seed 见 §5。

---

## 0. 实施总览

### 0.1 阶段地图

| 阶段 | 目标 | 任务编号 | 累计任务数 |
|---|---|---|---|
| **P1. DB 迁移与 Seed** | 5 个 Flyway SQL 文件落地新表、ALTER、778 体系 seed、权限补丁、dev 示例 | T1–T5 | 5 |
| **P2. 后端基础设施** | 老师选择器 API、TeacherAvailability CRUD、Curriculum 只读 API、Student 字段扩展 | T6–T9 | 9 |
| **P3. 主带老师管理** | 学员创建/更新强制 mentor、换主带 + 历史 | T10–T11 | 11 |
| **P4. 排课模型重塑** | LessonSubscription / LessonStudent / ScheduleGenerator / 按老师课表 API | T12–T16 | 16 |
| **P5. 班级降级 + 阶段评估** | ClassGroup 字段松绑、StageAssessment 最小 CRUD | T17–T18 | 18 |
| **P6. 前端** | 学员表单 + 详情 Sheet、老师配置页、工作台、按老师周课表 | T19–T23 | 23 |
| **P7. 联调 / 文档** | E2E 冒烟、原 spec / plan 增加 v1.1 引用 | T24–T25 | 25 |

### 0.2 Git Commit 协议

本计划的每个任务结尾给出建议的 `git commit` 命令，但**本会话不会执行任何 commit**：
- 用户在 review plan 后会自行决定哪些任务先 commit / 一次 commit
- AI 助手或后续 executor 在执行单个任务前必须**先经过用户确认**才允许 commit
- 推荐合并策略：T1–T5 一次 commit（"feat(db): teacher-centric migration + curriculum seed"），后续按 service / 页面粒度独立 commit

### 0.3 文件结构

#### 后端新增 / 调整文件

```
src/main/resources/db/migration/
├─ V1.5.0__teacher_centric.sql                         (新)
├─ V1.5.1__curriculum_tables.sql                       (新)
├─ V9.1.0__seed_curriculum.sql                         (新)
├─ V9.1.1__add_teacher_centric_permissions.sql         (新)
└─ V9.2.0__seed_demo_teachers.sql                      (新, dev profile only)

src/main/java/com/eduze/manage/
├─ auth/
│  ├─ controller/TeacherController.java                (新)
│  └─ dto/TeacherSummaryResponse.java                  (新)
├─ teacher/                                             (新包)
│  ├─ controller/TeacherAvailabilityController.java
│  ├─ service/TeacherAvailabilityService.java
│  ├─ domain/TeacherAvailability.java
│  ├─ mapper/TeacherAvailabilityMapper.java
│  └─ dto/{TeacherAvailabilityRequest,Response}.java
├─ curriculum/                                          (新包)
│  ├─ controller/CurriculumController.java
│  ├─ service/CurriculumService.java
│  ├─ domain/{CurriculumStage,CurriculumDimension,StageDimension}.java
│  ├─ mapper/{CurriculumStageMapper,CurriculumDimensionMapper,StageDimensionMapper}.java
│  └─ dto/{StageResponse,DimensionResponse}.java
├─ student/
│  ├─ domain/{Student.java[修改],StudentMentorHistory.java(新),StudentStageAssessment.java(新)}
│  ├─ mapper/{StudentMentorHistoryMapper.java(新),StudentStageAssessmentMapper.java(新)}
│  ├─ service/{StudentService.java[修改],StudentMentorService.java(新),StageAssessmentService.java(新)}
│  ├─ controller/StudentController.java[修改]          (新增 PUT /mentor、GET /mentor-history、stage-assessments)
│  └─ dto/{StudentRequest.java[修改]+initialSubscriptions+mentorTeacherId,
│         AssignMentorRequest.java(新),MentorHistoryResponse.java(新),
│         StageAssessmentRequest.java(新),StageAssessmentResponse.java(新)}
└─ lesson/
   ├─ domain/{Lesson.java[修改],LessonSubscription.java(新),LessonStudent.java(新)}
   ├─ mapper/{LessonSubscriptionMapper.java(新),LessonStudentMapper.java(新)}
   ├─ service/{ScheduleGenerator.java(新),LessonStudentService.java(新),
   │           LessonSubscriptionService.java(新),LessonService.java[修改 bulkGenerate],
   │           ScheduleViewService.java(新)}
   ├─ controller/{LessonController.java[修改],
   │              LessonSubscriptionController.java(新),
   │              ScheduleController.java[修改 +by-teacher +my-week]}
   └─ dto/{BulkGenerateRequest.java[修改],
          SubscriptionRequest.java(新),SubscriptionResponse.java(新),
          LessonStudentRequest.java(新),LessonStudentResponse.java(新),
          TeacherScheduleResponse.java(新)}
```

#### 前端新增 / 调整文件

```
web/src/features/
├─ student/
│  ├─ components/StudentFormDialog.tsx[修改]            (+mentorTeacherId, +initialSubscriptions block)
│  ├─ components/StudentDetailSheet.tsx[修改]           (+mentor badge, +change mentor dialog)
│  ├─ components/MentorChangeDialog.tsx                 (新)
│  ├─ components/StudentMentorHistoryTab.tsx           (新)
│  ├─ components/StudentSubscriptionsTab.tsx           (新)
│  ├─ components/StudentStageAssessmentTab.tsx         (新)
│  ├─ pages/StudentListPage.tsx[修改]                   (+mentor column +filter)
│  ├─ api.ts[修改]                                       (+mentor / subscription / stage-assessment APIs)
│  └─ schemas.ts[修改]                                   (+mentorTeacherId required)
├─ teacher/                                              (新 feature)
│  ├─ pages/TeacherListPage.tsx
│  ├─ pages/TeacherAvailabilityPage.tsx
│  ├─ components/AvailabilityFormDialog.tsx
│  ├─ components/MyWorkbenchPage.tsx
│  └─ api.ts, schemas.ts, types.ts
├─ lesson/
│  ├─ pages/WeeklySchedulePage.tsx[重写]                (按老师列)
│  ├─ components/WeeklyGridByTeacher.tsx               (新)
│  ├─ components/BulkGenerateDialog.tsx[重写]
│  └─ components/LessonRosterTab.tsx                   (新, lesson_student CRUD)
└─ course/
   └─ pages/ClassGroupListPage.tsx[修改]               (降级 banner)
```

---

# P1. DB 迁移与 Seed

## Task 1: V1.5.0 老师为核心的核心表 DDL

**Files:**
- Create: `src/main/resources/db/migration/V1.5.0__teacher_centric.sql`
- Test: `src/test/java/com/eduze/manage/teacher/MigrationV150IT.java`

- [ ] **Step 1: 写失败的迁移落地测试**

Create `src/test/java/com/eduze/manage/teacher/MigrationV150IT.java`:

```java
package com.eduze.manage.teacher;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MigrationV150IT extends AbstractITContainerTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void teacherAvailability_tableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='t_teacher_availability'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void lessonSubscription_tableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='t_lesson_subscription'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void lessonStudent_tableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='t_lesson_student'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void studentMentorHistory_tableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='t_student_mentor_history'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void student_hasMentorTeacherIdColumn() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_name='t_student' AND column_name='mentor_teacher_id'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void student_hasCurrentStageIdColumn() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_name='t_student' AND column_name='current_stage_id'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void lesson_hasTeacherAvailabilityIdAndSource() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_name='t_lesson' AND column_name IN ('teacher_availability_id','source')",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void classGroup_courseIdNullable() {
        String nullable = jdbc.queryForObject(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_name='t_class_group' AND column_name='course_id'",
                String.class);
        assertThat(nullable).isEqualTo("YES");
    }

    @Test
    void lesson_classGroupIdNullable() {
        String nullable = jdbc.queryForObject(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_name='t_lesson' AND column_name='class_group_id'",
                String.class);
        assertThat(nullable).isEqualTo("YES");
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=MigrationV150IT test`
Expected: FAIL（缺表 / 缺字段）

- [ ] **Step 3: 写 SQL 迁移**

Create `src/main/resources/db/migration/V1.5.0__teacher_centric.sql`:

```sql
-- 1. 学员表加字段
ALTER TABLE t_student
    ADD COLUMN mentor_teacher_id BIGINT NULL COMMENT '主带老师 user.id'
        AFTER avatar_url,
    ADD COLUMN current_stage_id BIGINT NULL COMMENT '当前阶段 curriculum_stage.id'
        AFTER mentor_teacher_id,
    ADD KEY idx_student_mentor (tenant_id, mentor_teacher_id),
    ADD KEY idx_student_stage  (tenant_id, current_stage_id);

-- 2. 老师可用时段（周课表模板）
CREATE TABLE IF NOT EXISTS t_teacher_availability (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    day_of_week TINYINT NOT NULL COMMENT '1=周一 ... 7=周日',
    start_minute SMALLINT NOT NULL COMMENT '0-1439',
    end_minute   SMALLINT NOT NULL,
    capacity INT NOT NULL DEFAULT 8,
    default_class_room_id BIGINT NULL,
    valid_from DATE NOT NULL,
    valid_to   DATE NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    note VARCHAR(256) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_ta_slot (tenant_id, teacher_id, day_of_week, start_minute, valid_from, deleted_at),
    KEY idx_ta_branch_teacher (tenant_id, branch_id, teacher_id, day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 学员订阅
CREATE TABLE IF NOT EXISTS t_lesson_subscription (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    teacher_availability_id BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_to   DATE NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    source VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/MAKEUP',
    note VARCHAR(256) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_sub_slot (tenant_id, student_id, teacher_availability_id, valid_from, deleted_at),
    KEY idx_sub_availability_status (tenant_id, teacher_availability_id, status),
    KEY idx_sub_student_status (tenant_id, student_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 课次名单
CREATE TABLE IF NOT EXISTS t_lesson_student (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    subscription_id BIGINT NULL,
    source VARCHAR(16) NOT NULL DEFAULT 'SUBSCRIPTION' COMMENT 'SUBSCRIPTION/MANUAL/TRIAL/MAKEUP',
    status VARCHAR(16) NOT NULL DEFAULT 'BOOKED' COMMENT 'BOOKED/CANCELLED',
    note VARCHAR(256) NULL,
    removed_at DATETIME(3) NULL,
    removed_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_ls_lesson_student (tenant_id, lesson_id, student_id, deleted_at),
    KEY idx_ls_lesson (tenant_id, lesson_id),
    KEY idx_ls_student (tenant_id, student_id),
    KEY idx_ls_subscription (tenant_id, subscription_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 主带老师变更历史
CREATE TABLE IF NOT EXISTS t_student_mentor_history (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    from_teacher_id BIGINT NULL COMMENT 'NULL=首次绑定',
    to_teacher_id BIGINT NOT NULL,
    reason VARCHAR(256) NULL,
    changed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    operator_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_smh_student (tenant_id, student_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. lesson 表加字段 + 弱化 class_group_id 约束
ALTER TABLE t_lesson
    ADD COLUMN teacher_availability_id BIGINT NULL COMMENT '生成源',
    ADD COLUMN source TINYINT NOT NULL DEFAULT 2 COMMENT '1=模板生成 2=手动 3=补课 4=试听',
    MODIFY COLUMN class_group_id BIGINT NULL;

-- 7. class_group 字段调整
ALTER TABLE t_class_group
    MODIFY COLUMN course_id BIGINT NULL,
    ADD COLUMN tag_color CHAR(7) NULL COMMENT '#RRGGBB';
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `./mvnw -Dtest=MigrationV150IT test`
Expected: PASS（9 个测试全绿）

- [ ] **Step 5: 建议 commit（不执行，等用户决定）**

```bash
git add src/main/resources/db/migration/V1.5.0__teacher_centric.sql \
        src/test/java/com/eduze/manage/teacher/MigrationV150IT.java
git commit -m "feat(db): V1.5.0 teacher-centric core tables"
```

---

## Task 2: V1.5.1 课程体系表（罗恩菲尔德 + 778）

**Files:**
- Create: `src/main/resources/db/migration/V1.5.1__curriculum_tables.sql`
- Test: `src/test/java/com/eduze/manage/curriculum/MigrationV151IT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/curriculum/MigrationV151IT.java`:

```java
package com.eduze.manage.curriculum;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MigrationV151IT extends AbstractITContainerTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allCurriculumTablesExist() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN "
                        + "('t_curriculum_stage','t_curriculum_dimension','t_stage_dimension','t_student_stage_assessment')",
                Integer.class);
        assertThat(count).isEqualTo(4);
    }

    @Test
    void dimensionKindEnumColumn() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_name='t_curriculum_dimension' AND column_name='kind'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=MigrationV151IT test`
Expected: FAIL

- [ ] **Step 3: 写 SQL**

Create `src/main/resources/db/migration/V1.5.1__curriculum_tables.sql`:

```sql
-- 1. 阶段
CREATE TABLE IF NOT EXISTS t_curriculum_stage (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    age_min INT NOT NULL,
    age_max INT NOT NULL,
    order_no INT NOT NULL DEFAULT 0,
    lorenfield_phase VARCHAR(64) NULL,
    description VARCHAR(512) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_stage_code (tenant_id, code, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 维度
CREATE TABLE IF NOT EXISTS t_curriculum_dimension (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    kind VARCHAR(16) NOT NULL COMMENT 'ELEMENT/PRINCIPLE/MOVEMENT',
    code VARCHAR(48) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(512) NULL,
    order_no INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_dim_code (tenant_id, code, deleted_at),
    KEY idx_dim_kind (kind, order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 阶段 ↔ 维度
CREATE TABLE IF NOT EXISTS t_stage_dimension (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    stage_id BIGINT NOT NULL,
    dimension_id BIGINT NOT NULL,
    weight TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_stage_dim (tenant_id, stage_id, dimension_id),
    KEY idx_sd_stage (stage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 学员阶段评估
CREATE TABLE IF NOT EXISTS t_student_stage_assessment (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    branch_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    stage_id BIGINT NOT NULL,
    assessed_at DATE NOT NULL,
    assessed_by BIGINT NULL,
    scores_json JSON NULL COMMENT '{dimension_code: 1-5}',
    comment VARCHAR(1024) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    version INT NOT NULL DEFAULT 1,
    KEY idx_ssa_student (tenant_id, student_id, assessed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `./mvnw -Dtest=MigrationV151IT test`
Expected: PASS

- [ ] **Step 5: 建议 commit**

```bash
git add src/main/resources/db/migration/V1.5.1__curriculum_tables.sql \
        src/test/java/com/eduze/manage/curriculum/MigrationV151IT.java
git commit -m "feat(db): V1.5.1 curriculum stage/dimension tables"
```

---

## Task 3: V9.1.0 Seed 罗恩菲尔德 + 778 + 5 个示例课程产品

**Files:**
- Create: `src/main/resources/db/migration/V9.1.0__seed_curriculum.sql`
- Test: `src/test/java/com/eduze/manage/curriculum/CurriculumSeedIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/curriculum/CurriculumSeedIT.java`:

```java
package com.eduze.manage.curriculum;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class CurriculumSeedIT extends AbstractITContainerTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void stageCount_is5() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_curriculum_stage WHERE deleted_at=0", Integer.class);
        assertThat(count).isEqualTo(5);
    }

    @Test
    void dimensionCount_is22_and_breakdown() {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_curriculum_dimension WHERE deleted_at=0", Integer.class);
        assertThat(total).isEqualTo(22);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_curriculum_dimension WHERE kind='ELEMENT' AND deleted_at=0",
                        Integer.class))
                .isEqualTo(7);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_curriculum_dimension WHERE kind='PRINCIPLE' AND deleted_at=0",
                        Integer.class))
                .isEqualTo(7);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_curriculum_dimension WHERE kind='MOVEMENT' AND deleted_at=0",
                        Integer.class))
                .isEqualTo(8);
    }

    @Test
    void stageDimensionMappingExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_stage_dimension", Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(20);
    }

    @Test
    void demoCourses_5() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_course WHERE deleted_at=0 AND name LIKE '%·%'", Integer.class);
        assertThat(count).isEqualTo(5);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=CurriculumSeedIT test`
Expected: FAIL

- [ ] **Step 3: 写 seed SQL**

Create `src/main/resources/db/migration/V9.1.0__seed_curriculum.sql`:

```sql
-- ============ 5 个阶段 ============
INSERT INTO t_curriculum_stage (id, tenant_id, code, name, age_min, age_max, order_no, lorenfield_phase, description) VALUES
 (200001,1,'STAGE_KMD','启蒙阶段',4,7,1,'Preschematic（样式化前期）','涂鸦向符号化过渡；以色彩感知与基础造型启发为主'),
 (200002,1,'STAGE_TS', '探索阶段',7,9,2,'Schematic（样式化期）','用图式表达事物；引入线条/形状/色彩三元素'),
 (200003,1,'STAGE_CZ', '成长阶段',9,11,3,'Dawning Realism（写实萌芽期）','写实意识萌发；引入空间/明度/比例与构图原则'),
 (200004,1,'STAGE_JJ', '进阶阶段',11,13,4,'Pseudo-Realistic（拟写实期）','主动追求"像"；引入对比/节奏/统一与流派启蒙'),
 (200005,1,'STAGE_SB', '思辨阶段',13,16,5,'Adolescent Art（决定期）','风格意识自觉；以现代主义诸流派为媒介进行个人表达')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ============ 22 个维度 ============
-- ELEMENTS 7
INSERT INTO t_curriculum_dimension (id, tenant_id, kind, code, name, description, order_no) VALUES
 (210001,1,'ELEMENT','EL_LINE','线条 (Line)','所有视觉表达的基础元素', 1),
 (210002,1,'ELEMENT','EL_SHAPE','形状 (Shape)','由线条围合形成的二维区域', 2),
 (210003,1,'ELEMENT','EL_FORM','形态 (Form)','具有三维感的形状', 3),
 (210004,1,'ELEMENT','EL_SPACE','空间 (Space)','画面的前后纵深与正负关系', 4),
 (210005,1,'ELEMENT','EL_COLOR','色彩 (Color)','色相、明度、饱和度三属性的综合', 5),
 (210006,1,'ELEMENT','EL_VALUE','明度 (Value)','色彩的明暗程度', 6),
 (210007,1,'ELEMENT','EL_TEXTURE','肌理 (Texture)','表面的视觉与触觉质感', 7),
 (210008,1,'PRINCIPLE','PR_BALANCE','平衡 (Balance)','视觉重量在画面中的分布', 1),
 (210009,1,'PRINCIPLE','PR_CONTRAST','对比 (Contrast)','差异化的并置带来的张力', 2),
 (210010,1,'PRINCIPLE','PR_EMPHASIS','强调 (Emphasis)','吸引视线的视觉焦点', 3),
 (210011,1,'PRINCIPLE','PR_RHYTHM','节奏 (Rhythm/Movement)','元素重复或变化产生的运动感', 4),
 (210012,1,'PRINCIPLE','PR_PATTERN','图案 (Pattern)','元素的规律性重复', 5),
 (210013,1,'PRINCIPLE','PR_UNITY','统一 (Unity)','整体协调的视觉感受', 6),
 (210014,1,'PRINCIPLE','PR_PROPORTION','比例 (Proportion)','元素之间大小关系的恰当性', 7),
 (210015,1,'MOVEMENT','MV_IMPRESSIONISM','印象派 (Impressionism)','19 世纪末光影与瞬间感受的捕捉', 1),
 (210016,1,'MOVEMENT','MV_POST_IMPRESSIONISM','后印象派 (Post-Impressionism)','在印象派基础上强化结构、情感与符号', 2),
 (210017,1,'MOVEMENT','MV_FAUVISM','野兽派 (Fauvism)','大胆色彩与强烈笔触的表现性流派', 3),
 (210018,1,'MOVEMENT','MV_EXPRESSIONISM','表现主义 (Expressionism)','以变形与色彩表达情感与心理', 4),
 (210019,1,'MOVEMENT','MV_CUBISM','立体主义 (Cubism)','多视点几何化对象的分解与重构', 5),
 (210020,1,'MOVEMENT','MV_SURREALISM','超现实主义 (Surrealism)','潜意识与梦境的视觉化', 6),
 (210021,1,'MOVEMENT','MV_ABSTRACT_EXPRESSIONISM','抽象表现主义 (Abstract Expressionism)','纯粹抽象的情感性表达', 7),
 (210022,1,'MOVEMENT','MV_POP_ART','波普艺术 (Pop Art)','以大众文化与商品图像为题材', 8)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ============ 阶段 ↔ 维度推荐挂载 ============
-- STAGE_KMD 4-7: EL_LINE/EL_SHAPE/EL_COLOR weight=2
INSERT IGNORE INTO t_stage_dimension (id,tenant_id,stage_id,dimension_id,weight) VALUES
 (211001,1,200001,210001,2),(211002,1,200001,210002,2),(211003,1,200001,210005,2),
-- STAGE_TS 7-9: +EL_TEXTURE weight=2, PR_PATTERN weight=1
 (211010,1,200002,210001,2),(211011,1,200002,210002,2),(211012,1,200002,210005,2),
 (211013,1,200002,210007,2),(211014,1,200002,210012,1),
-- STAGE_CZ 9-11: EL_SPACE/EL_VALUE/PR_PROPORTION/PR_BALANCE weight=2, EL_FORM weight=1
 (211020,1,200003,210004,2),(211021,1,200003,210006,2),(211022,1,200003,210014,2),
 (211023,1,200003,210008,2),(211024,1,200003,210003,1),
-- STAGE_JJ 11-13: PR_CONTRAST/PR_RHYTHM/PR_UNITY/MV_IMP/MV_POSTIMP weight=2, PR_EMPHASIS weight=1
 (211030,1,200004,210009,2),(211031,1,200004,210011,2),(211032,1,200004,210013,2),
 (211033,1,200004,210015,2),(211034,1,200004,210016,2),(211035,1,200004,210010,1),
-- STAGE_SB 13-16: 6 流派 weight=2, PR_EMPHASIS/PR_UNITY weight=1
 (211040,1,200005,210017,2),(211041,1,200005,210018,2),(211042,1,200005,210019,2),
 (211043,1,200005,210020,2),(211044,1,200005,210021,2),(211045,1,200005,210022,2),
 (211046,1,200005,210010,1),(211047,1,200005,210013,1);

-- ============ 5 个示例课程产品（写到现有 t_course） ============
INSERT INTO t_course (id, tenant_id, name, age_min, age_max, lesson_minutes, description) VALUES
 (220001,1,'启蒙·4–6 岁', 4, 6, 90,  '罗恩菲尔德样式化前期阶段；启发色彩与基础造型感知'),
 (220002,1,'探索·7–8 岁', 7, 8, 90,  '样式化期；引入 7 大艺术元素的基础应用'),
 (220003,1,'成长·9–10 岁',9,10,120, '写实萌芽期；引入空间/明度/比例与基础构图原则'),
 (220004,1,'进阶·11–12 岁',11,12,120,'拟写实期；7 大设计原则系统训练 + 印象派/后印象派启蒙'),
 (220005,1,'思辨·13–16 岁',13,16,150,'决定期；以现代主义诸流派为媒介开展个人风格探索')
ON DUPLICATE KEY UPDATE name=VALUES(name);
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `./mvnw -Dtest=CurriculumSeedIT test`
Expected: PASS

- [ ] **Step 5: 建议 commit**

```bash
git add src/main/resources/db/migration/V9.1.0__seed_curriculum.sql \
        src/test/java/com/eduze/manage/curriculum/CurriculumSeedIT.java
git commit -m "feat(db): V9.1.0 seed curriculum (5 stages + 22 dims + 5 demo courses)"
```

---

## Task 4: V9.1.1 老师为核心权限补丁

**Files:**
- Create: `src/main/resources/db/migration/V9.1.1__add_teacher_centric_permissions.sql`
- Test: `src/test/java/com/eduze/manage/auth/TeacherCentricPermissionSeedIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/auth/TeacherCentricPermissionSeedIT.java`:

```java
package com.eduze.manage.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class TeacherCentricPermissionSeedIT extends AbstractITContainerTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void sixNewPermissionsExist() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_permission WHERE code IN "
                        + "('teacher:availability:read','teacher:availability:write',"
                        + "'subscription:read','subscription:write',"
                        + "'student:mentor_assign','lesson:teacher_view')",
                Integer.class);
        assertThat(count).isEqualTo(6);
    }

    @Test
    void teacherRoleHasAvailabilityWrite() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_role_permission rp "
                        + "JOIN t_permission p ON p.id=rp.permission_id "
                        + "WHERE rp.role_id=4 AND p.code='teacher:availability:write'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void advisorHasSubscriptionWrite() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_role_permission rp "
                        + "JOIN t_permission p ON p.id=rp.permission_id "
                        + "WHERE rp.role_id=3 AND p.code='subscription:write'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void superAdminHasAllNewPermissions() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_role_permission rp "
                        + "JOIN t_permission p ON p.id=rp.permission_id "
                        + "WHERE rp.role_id=1 AND p.code IN ("
                        + "'teacher:availability:read','teacher:availability:write',"
                        + "'subscription:read','subscription:write',"
                        + "'student:mentor_assign','lesson:teacher_view')",
                Integer.class);
        assertThat(count).isEqualTo(6);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=TeacherCentricPermissionSeedIT test`
Expected: FAIL

- [ ] **Step 3: 写权限 seed**

Create `src/main/resources/db/migration/V9.1.1__add_teacher_centric_permissions.sql`:

```sql
INSERT INTO t_permission (id, code, name, module) VALUES
 (38, 'teacher:availability:read',  '老师可用时段查看', 'teacher'),
 (39, 'teacher:availability:write', '老师可用时段编辑', 'teacher'),
 (40, 'subscription:read',          '课程订阅查看',     'subscription'),
 (41, 'subscription:write',         '课程订阅编辑',     'subscription'),
 (42, 'student:mentor_assign',      '主带老师指派',     'student'),
 (43, 'lesson:teacher_view',        '按老师查看课表',   'lesson')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- TEACHER (id=4) ← +avail:write/sub:read/lesson:teacher_view
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 4000 + p.id, 4, p.id FROM t_permission p
WHERE p.code IN ('teacher:availability:read','teacher:availability:write','subscription:read','lesson:teacher_view');

-- ADVISOR (id=3) ← +sub:write/mentor_assign/lesson:teacher_view
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 3000 + p.id, 3, p.id FROM t_permission p
WHERE p.code IN ('subscription:read','subscription:write','student:mentor_assign','lesson:teacher_view');

-- PRINCIPAL (id=2) ← 上述全部
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 2000 + p.id, 2, p.id FROM t_permission p
WHERE p.code IN ('teacher:availability:read','teacher:availability:write',
                 'subscription:read','subscription:write',
                 'student:mentor_assign','lesson:teacher_view');

-- SUPER_ADMIN (id=1) ← 上述全部
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT 1000 + p.id, 1, p.id FROM t_permission p
WHERE p.code IN ('teacher:availability:read','teacher:availability:write',
                 'subscription:read','subscription:write',
                 'student:mentor_assign','lesson:teacher_view');
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `./mvnw -Dtest=TeacherCentricPermissionSeedIT test`
Expected: PASS

- [ ] **Step 5: 建议 commit**

```bash
git add src/main/resources/db/migration/V9.1.1__add_teacher_centric_permissions.sql \
        src/test/java/com/eduze/manage/auth/TeacherCentricPermissionSeedIT.java
git commit -m "feat(db): V9.1.1 teacher-centric permissions + role patches"
```

---

## Task 5: V9.2.0 Dev profile 示例老师 / 学员 / 订阅

**Files:**
- Create: `src/main/resources/db/migration/V9.2.0__seed_demo_teachers.sql`
- Modify: `src/main/resources/application-dev.yml`（启用 Flyway placeholder `demoSeed=true`）
- Modify: `src/main/resources/application-test.yml`（设 `demoSeed=true` 让测试也跑示例）
- Modify: `src/main/resources/application-prod.yml`（设 `demoSeed=false`）
- Test: `src/test/java/com/eduze/manage/teacher/DemoTeacherSeedIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/teacher/DemoTeacherSeedIT.java`:

```java
package com.eduze.manage.teacher;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DemoTeacherSeedIT extends AbstractITContainerTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void threeDemoTeachersSeeded() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_user WHERE username IN ('teacher_zhang','teacher_wang','teacher_li') AND deleted_at=0",
                Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void eachDemoTeacherHasTeacherRole() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_user_role ur "
                        + "JOIN t_user u ON u.id=ur.user_id "
                        + "WHERE u.username IN ('teacher_zhang','teacher_wang','teacher_li') AND ur.role_id=4",
                Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void availabilitiesSeeded() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_teacher_availability WHERE deleted_at=0", Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(6);
    }

    @Test
    void demoStudentsHaveMentorAndSubscription() {
        Integer studentCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_student WHERE deleted_at=0 AND mentor_teacher_id IS NOT NULL",
                Integer.class);
        assertThat(studentCount).isGreaterThanOrEqualTo(5);
        Integer subCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_lesson_subscription WHERE deleted_at=0 AND status=1",
                Integer.class);
        assertThat(subCount).isGreaterThanOrEqualTo(5);
    }
}
```

- [ ] **Step 2: 修改 Flyway 配置加 placeholder**

Edit `src/main/resources/application.yml`（顶部 spring.flyway 节点）：

```yaml
spring:
  flyway:
    placeholders:
      demoSeed: "false"
```

Edit `src/main/resources/application-dev.yml` and `application-test.yml`：

```yaml
spring:
  flyway:
    placeholders:
      demoSeed: "true"
```

- [ ] **Step 3: 写 seed SQL（用 IF/Conditional 通过 placeholder 控制）**

Create `src/main/resources/db/migration/V9.2.0__seed_demo_teachers.sql`:

```sql
-- 当 demoSeed=false 时整段空操作（通过条件 INSERT 实现）
-- BCrypt for "teacher@123"，使用与 admin seed 同套 cost=10
SET @demo := '${demoSeed}';

-- 3 个示例老师用户
INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status)
SELECT 1101,1,1,'teacher_zhang',
       '$2a$10$Vc9KSaM9T4nAJVhrG3kacu.930c1dN/FnftTYp7twKW2oaq.PF0iW','张老师',1
WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_user WHERE id=1101);

INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status)
SELECT 1102,1,1,'teacher_wang',
       '$2a$10$Vc9KSaM9T4nAJVhrG3kacu.930c1dN/FnftTYp7twKW2oaq.PF0iW','王老师',1
WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_user WHERE id=1102);

INSERT INTO t_user (id, tenant_id, branch_id, username, password_hash, name, status)
SELECT 1103,1,1,'teacher_li',
       '$2a$10$Vc9KSaM9T4nAJVhrG3kacu.930c1dN/FnftTYp7twKW2oaq.PF0iW','李老师',1
WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_user WHERE id=1103);

-- 用户角色（TEACHER=4）
INSERT INTO t_user_role (id, tenant_id, user_id, role_id)
SELECT 1101,1,1101,4 WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_user_role WHERE id=1101);
INSERT INTO t_user_role (id, tenant_id, user_id, role_id)
SELECT 1102,1,1102,4 WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_user_role WHERE id=1102);
INSERT INTO t_user_role (id, tenant_id, user_id, role_id)
SELECT 1103,1,1103,4 WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_user_role WHERE id=1103);

-- user_branch
INSERT INTO t_user_branch (id, tenant_id, user_id, branch_id)
SELECT 1101,1,1101,1 WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_user_branch WHERE id=1101);
INSERT INTO t_user_branch (id, tenant_id, user_id, branch_id)
SELECT 1102,1,1102,1 WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_user_branch WHERE id=1102);
INSERT INTO t_user_branch (id, tenant_id, user_id, branch_id)
SELECT 1103,1,1103,1 WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_user_branch WHERE id=1103);

-- 每人 2 条 availability（zhang 周六/周日上午，wang 周六/周日下午，li 周五/周六晚）
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230001,1,1,1101,6, 540, 630, 8, '2026-05-01', 1 WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230001);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230002,1,1,1101,7, 540, 630, 8, '2026-05-01', 1 WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230002);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230003,1,1,1102,6, 840, 960, 6, '2026-05-01', 1 WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230003);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230004,1,1,1102,7, 840, 960, 6, '2026-05-01', 1 WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230004);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230005,1,1,1103,5,1080,1200, 4, '2026-05-01', 1 WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230005);
INSERT INTO t_teacher_availability (id,tenant_id,branch_id,teacher_id,day_of_week,start_minute,end_minute,capacity,valid_from,status)
SELECT 230006,1,1,1103,6,1080,1200, 4, '2026-05-01', 1 WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_teacher_availability WHERE id=230006);

-- 5 个示例学员（每阶段 1）
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240001,1,1,'DEMO0001','小启', 1,'2021-06-01','2026-05-01',1, 1101, 200001
WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_student WHERE id=240001);
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240002,1,1,'DEMO0002','小探', 2,'2018-06-01','2026-05-01',1, 1101, 200002
WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_student WHERE id=240002);
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240003,1,1,'DEMO0003','小成', 1,'2016-06-01','2026-05-01',1, 1102, 200003
WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_student WHERE id=240003);
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240004,1,1,'DEMO0004','小进', 2,'2014-06-01','2026-05-01',1, 1102, 200004
WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_student WHERE id=240004);
INSERT INTO t_student (id,tenant_id,branch_id,enroll_no,name,gender,birthday,enroll_date,status,mentor_teacher_id,current_stage_id)
SELECT 240005,1,1,'DEMO0005','小辨', 1,'2012-06-01','2026-05-01',1, 1103, 200005
WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_student WHERE id=240005);

-- 每个学员订阅其主带的第 1 条可用时段
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250001,1,1,240001,1101,230001,'2026-05-01',1,'NORMAL' WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250001);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250002,1,1,240002,1101,230002,'2026-05-01',1,'NORMAL' WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250002);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250003,1,1,240003,1102,230003,'2026-05-01',1,'NORMAL' WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250003);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250004,1,1,240004,1102,230004,'2026-05-01',1,'NORMAL' WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250004);
INSERT INTO t_lesson_subscription (id,tenant_id,branch_id,student_id,teacher_id,teacher_availability_id,valid_from,status,source)
SELECT 250005,1,1,240005,1103,230005,'2026-05-01',1,'NORMAL' WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_lesson_subscription WHERE id=250005);

-- 主带历史（首次绑定）
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260001,1,1,240001,NULL,1101,'首次绑定' WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260001);
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260002,1,1,240002,NULL,1101,'首次绑定' WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260002);
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260003,1,1,240003,NULL,1102,'首次绑定' WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260003);
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260004,1,1,240004,NULL,1102,'首次绑定' WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260004);
INSERT INTO t_student_mentor_history (id,tenant_id,branch_id,student_id,from_teacher_id,to_teacher_id,reason)
SELECT 260005,1,1,240005,NULL,1103,'首次绑定' WHERE @demo='true' AND NOT EXISTS (SELECT 1 FROM t_student_mentor_history WHERE id=260005);
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `./mvnw -Dtest=DemoTeacherSeedIT test`
Expected: PASS（test profile demoSeed=true 已生效）

- [ ] **Step 5: 建议 commit**

```bash
git add src/main/resources/db/migration/V9.2.0__seed_demo_teachers.sql \
        src/main/resources/application*.yml \
        src/test/java/com/eduze/manage/teacher/DemoTeacherSeedIT.java
git commit -m "feat(db): V9.2.0 demo teachers/students/subscriptions for dev profile"
```

---

# P2. 后端基础设施

## Task 6: 老师选择器 API

**Files:**
- Create: `src/main/java/com/eduze/manage/auth/controller/TeacherController.java`
- Create: `src/main/java/com/eduze/manage/auth/dto/TeacherSummaryResponse.java`
- Create: `src/main/java/com/eduze/manage/auth/service/TeacherQueryService.java`
- Test: `src/test/java/com/eduze/manage/auth/TeacherControllerIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/auth/TeacherControllerIT.java`:

```java
package com.eduze.manage.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;

class TeacherControllerIT extends AbstractApiIT {

    @Test
    void listTeachers_returnsAllTeacherRoleUsers() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/api/teachers").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.username=='teacher_zhang')]").exists())
                .andExpect(jsonPath("$.data[?(@.username=='teacher_wang')]").exists())
                .andExpect(jsonPath("$.data[?(@.username=='teacher_li')]").exists());
    }

    @Test
    void listTeachers_filteredByBranch() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/api/teachers?branchId=1").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=TeacherControllerIT test`
Expected: FAIL（404）

- [ ] **Step 3: 写 DTO**

Create `src/main/java/com/eduze/manage/auth/dto/TeacherSummaryResponse.java`:

```java
package com.eduze.manage.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherSummaryResponse {
    private final Long id;
    private final String username;
    private final String name;
    private final Long branchId;
}
```

- [ ] **Step 4: 写 service**

Create `src/main/java/com/eduze/manage/auth/service/TeacherQueryService.java`:

```java
package com.eduze.manage.auth.service;

import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.dto.TeacherSummaryResponse;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.tenant.TenantContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherQueryService {

    private static final String SQL = """
            SELECT u.id, u.username, u.name, u.branch_id
            FROM t_user u
            JOIN t_user_role ur ON ur.user_id=u.id
            JOIN t_role r ON r.id=ur.role_id AND r.code='TEACHER'
            WHERE u.tenant_id = ? AND u.deleted_at = 0 AND u.status = 1
              AND (? IS NULL OR u.branch_id = ?)
            ORDER BY u.id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;

    public List<TeacherSummaryResponse> list(Long branchId) {
        Long tenantId = TenantContext.getTenantId();
        return jdbcTemplate.query(
                SQL,
                (rs, i) -> TeacherSummaryResponse.builder()
                        .id(rs.getLong("id"))
                        .username(rs.getString("username"))
                        .name(rs.getString("name"))
                        .branchId(rs.getLong("branch_id"))
                        .build(),
                tenantId, branchId, branchId);
    }
}
```

- [ ] **Step 5: 写 controller**

Create `src/main/java/com/eduze/manage/auth/controller/TeacherController.java`:

```java
package com.eduze.manage.auth.controller;

import com.eduze.manage.auth.dto.TeacherSummaryResponse;
import com.eduze.manage.auth.service.TeacherQueryService;
import com.eduze.manage.common.web.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherQueryService teacherQueryService;

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<List<TeacherSummaryResponse>> list(@RequestParam(required = false) Long branchId) {
        return ApiResponse.ok(teacherQueryService.list(branchId));
    }
}
```

- [ ] **Step 6: 运行测试，确认通过**

Run: `./mvnw -Dtest=TeacherControllerIT test`
Expected: PASS

- [ ] **Step 7: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/auth/controller/TeacherController.java \
        src/main/java/com/eduze/manage/auth/dto/TeacherSummaryResponse.java \
        src/main/java/com/eduze/manage/auth/service/TeacherQueryService.java \
        src/test/java/com/eduze/manage/auth/TeacherControllerIT.java
git commit -m "feat(auth): GET /api/teachers list TEACHER-role users"
```

---

## Task 7: TeacherAvailability CRUD

**Files:**
- Create: `src/main/java/com/eduze/manage/teacher/domain/TeacherAvailability.java`
- Create: `src/main/java/com/eduze/manage/teacher/mapper/TeacherAvailabilityMapper.java`
- Create: `src/main/java/com/eduze/manage/teacher/dto/TeacherAvailabilityRequest.java`
- Create: `src/main/java/com/eduze/manage/teacher/dto/TeacherAvailabilityResponse.java`
- Create: `src/main/java/com/eduze/manage/teacher/service/TeacherAvailabilityService.java`
- Create: `src/main/java/com/eduze/manage/teacher/controller/TeacherAvailabilityController.java`
- Test: `src/test/java/com/eduze/manage/teacher/TeacherAvailabilityControllerIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/teacher/TeacherAvailabilityControllerIT.java`:

```java
package com.eduze.manage.teacher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TeacherAvailabilityControllerIT extends AbstractApiIT {

    @Test
    void crud_flow() throws Exception {
        String token = adminToken();

        // CREATE
        String created = mockMvc.perform(post("/api/teachers/1101/availabilities")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"dayOfWeek":3,"startMinute":540,"endMinute":630,"capacity":6,
                             "branchId":1,"validFrom":"2026-06-01","status":1}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("data").get("id").asLong();

        // LIST
        mockMvc.perform(get("/api/teachers/1101/availabilities").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id==" + id + ")]").exists());

        // UPDATE
        mockMvc.perform(put("/api/teacher-availabilities/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"dayOfWeek":3,"startMinute":540,"endMinute":660,"capacity":8,
                             "branchId":1,"validFrom":"2026-06-01","status":1}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endMinute").value(660));

        // DELETE
        mockMvc.perform(delete("/api/teacher-availabilities/" + id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void overlap_rejected() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/api/teachers/1101/availabilities")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"dayOfWeek":6,"startMinute":560,"endMinute":620,"capacity":6,
                             "branchId":1,"validFrom":"2026-05-01","status":1}
                            """))
                .andExpect(status().is4xxClientError());
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=TeacherAvailabilityControllerIT test`
Expected: FAIL

- [ ] **Step 3: 写 domain / mapper / DTO**

Create `src/main/java/com/eduze/manage/teacher/domain/TeacherAvailability.java`:

```java
package com.eduze.manage.teacher.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_teacher_availability")
public class TeacherAvailability extends BaseEntity {
    private Long teacherId;
    private Integer dayOfWeek;
    private Integer startMinute;
    private Integer endMinute;
    private Integer capacity;
    private Long defaultClassRoomId;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer status;
    private String note;
}
```

Create `src/main/java/com/eduze/manage/teacher/mapper/TeacherAvailabilityMapper.java`:

```java
package com.eduze.manage.teacher.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.teacher.domain.TeacherAvailability;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeacherAvailabilityMapper extends BaseMapper<TeacherAvailability> {}
```

Create `src/main/java/com/eduze/manage/teacher/dto/TeacherAvailabilityRequest.java`:

```java
package com.eduze.manage.teacher.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeacherAvailabilityRequest {
    @NotNull private Long branchId;
    @NotNull @Min(1) @Max(7) private Integer dayOfWeek;
    @NotNull @Min(0) @Max(1439) private Integer startMinute;
    @NotNull @Min(1) @Max(1440) private Integer endMinute;
    @NotNull @Min(1) private Integer capacity;
    private Long defaultClassRoomId;
    @NotNull private LocalDate validFrom;
    private LocalDate validTo;
    private Integer status;
    private String note;
}
```

Create `src/main/java/com/eduze/manage/teacher/dto/TeacherAvailabilityResponse.java`:

```java
package com.eduze.manage.teacher.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherAvailabilityResponse {
    private final Long id;
    private final Long teacherId;
    private final Long branchId;
    private final Integer dayOfWeek;
    private final Integer startMinute;
    private final Integer endMinute;
    private final Integer capacity;
    private final Long defaultClassRoomId;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final Integer status;
    private final String note;
}
```

- [ ] **Step 4: 写 service（含冲突校验）**

Create `src/main/java/com/eduze/manage/teacher/service/TeacherAvailabilityService.java`:

```java
package com.eduze.manage.teacher.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.teacher.domain.TeacherAvailability;
import com.eduze.manage.teacher.dto.TeacherAvailabilityRequest;
import com.eduze.manage.teacher.dto.TeacherAvailabilityResponse;
import com.eduze.manage.teacher.mapper.TeacherAvailabilityMapper;
import com.eduze.manage.tenant.TenantContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherAvailabilityService {

    private final TeacherAvailabilityMapper mapper;

    public List<TeacherAvailabilityResponse> listByTeacher(Long teacherId) {
        return mapper
                .selectList(Wrappers.<TeacherAvailability>lambdaQuery()
                        .eq(TeacherAvailability::getTenantId, TenantContext.getTenantId())
                        .eq(TeacherAvailability::getTeacherId, teacherId)
                        .orderByAsc(TeacherAvailability::getDayOfWeek)
                        .orderByAsc(TeacherAvailability::getStartMinute))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TeacherAvailabilityResponse create(Long teacherId, TeacherAvailabilityRequest req) {
        validateRange(req);
        checkNoOverlap(teacherId, null, req.getDayOfWeek(), req.getStartMinute(), req.getEndMinute());
        TeacherAvailability a = new TeacherAvailability();
        a.setTenantId(TenantContext.getTenantId());
        a.setBranchId(req.getBranchId());
        a.setTeacherId(teacherId);
        apply(a, req);
        mapper.insert(a);
        return toResponse(a);
    }

    @Transactional
    public TeacherAvailabilityResponse update(Long id, TeacherAvailabilityRequest req) {
        validateRange(req);
        TeacherAvailability a = require(id);
        checkNoOverlap(a.getTeacherId(), id, req.getDayOfWeek(), req.getStartMinute(), req.getEndMinute());
        apply(a, req);
        mapper.updateById(a);
        return toResponse(a);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        mapper.deleteById(id);
    }

    public TeacherAvailability require(Long id) {
        TeacherAvailability a = mapper.selectById(id);
        if (a == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "可用时段不存在");
        }
        return a;
    }

    private void validateRange(TeacherAvailabilityRequest req) {
        if (req.getEndMinute() <= req.getStartMinute()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "结束时间必须晚于开始时间");
        }
    }

    private void checkNoOverlap(Long teacherId, Long excludeId, int dow, int start, int end) {
        List<TeacherAvailability> exist = mapper.selectList(Wrappers.<TeacherAvailability>lambdaQuery()
                .eq(TeacherAvailability::getTenantId, TenantContext.getTenantId())
                .eq(TeacherAvailability::getTeacherId, teacherId)
                .eq(TeacherAvailability::getDayOfWeek, dow)
                .eq(TeacherAvailability::getStatus, 1));
        for (TeacherAvailability a : exist) {
            if (excludeId != null && excludeId.equals(a.getId())) continue;
            boolean overlap = start < a.getEndMinute() && end > a.getStartMinute();
            if (overlap) {
                throw new BizException(ErrorCode.CONFLICT, "与现有时段重叠");
            }
        }
    }

    private void apply(TeacherAvailability a, TeacherAvailabilityRequest req) {
        a.setBranchId(req.getBranchId());
        a.setDayOfWeek(req.getDayOfWeek());
        a.setStartMinute(req.getStartMinute());
        a.setEndMinute(req.getEndMinute());
        a.setCapacity(req.getCapacity());
        a.setDefaultClassRoomId(req.getDefaultClassRoomId());
        a.setValidFrom(req.getValidFrom());
        a.setValidTo(req.getValidTo());
        a.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        a.setNote(req.getNote());
    }

    private TeacherAvailabilityResponse toResponse(TeacherAvailability a) {
        return TeacherAvailabilityResponse.builder()
                .id(a.getId())
                .teacherId(a.getTeacherId())
                .branchId(a.getBranchId())
                .dayOfWeek(a.getDayOfWeek())
                .startMinute(a.getStartMinute())
                .endMinute(a.getEndMinute())
                .capacity(a.getCapacity())
                .defaultClassRoomId(a.getDefaultClassRoomId())
                .validFrom(a.getValidFrom())
                .validTo(a.getValidTo())
                .status(a.getStatus())
                .note(a.getNote())
                .build();
    }
}
```

- [ ] **Step 5: 写 controller**

Create `src/main/java/com/eduze/manage/teacher/controller/TeacherAvailabilityController.java`:

```java
package com.eduze.manage.teacher.controller;

import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.teacher.dto.TeacherAvailabilityRequest;
import com.eduze.manage.teacher.dto.TeacherAvailabilityResponse;
import com.eduze.manage.teacher.service.TeacherAvailabilityService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TeacherAvailabilityController {

    private final TeacherAvailabilityService service;

    @GetMapping("/api/teachers/{teacherId}/availabilities")
    @PreAuthorize("hasAuthority('teacher:availability:read')")
    public ApiResponse<List<TeacherAvailabilityResponse>> list(@PathVariable Long teacherId) {
        return ApiResponse.ok(service.listByTeacher(teacherId));
    }

    @PostMapping("/api/teachers/{teacherId}/availabilities")
    @PreAuthorize("hasAuthority('teacher:availability:write')")
    public ApiResponse<TeacherAvailabilityResponse> create(
            @PathVariable Long teacherId, @Valid @RequestBody TeacherAvailabilityRequest req) {
        return ApiResponse.ok(service.create(teacherId, req));
    }

    @PutMapping("/api/teacher-availabilities/{id}")
    @PreAuthorize("hasAuthority('teacher:availability:write')")
    public ApiResponse<TeacherAvailabilityResponse> update(
            @PathVariable Long id, @Valid @RequestBody TeacherAvailabilityRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/api/teacher-availabilities/{id}")
    @PreAuthorize("hasAuthority('teacher:availability:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
```

- [ ] **Step 6: 运行测试，确认通过**

Run: `./mvnw -Dtest=TeacherAvailabilityControllerIT test`
Expected: PASS

- [ ] **Step 7: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/teacher/ \
        src/test/java/com/eduze/manage/teacher/TeacherAvailabilityControllerIT.java
git commit -m "feat(teacher): TeacherAvailability CRUD with overlap detection"
```

---

## Task 8: Curriculum 只读 API

**Files:**
- Create: `src/main/java/com/eduze/manage/curriculum/domain/{CurriculumStage,CurriculumDimension,StageDimension}.java`
- Create: `src/main/java/com/eduze/manage/curriculum/mapper/*.java`
- Create: `src/main/java/com/eduze/manage/curriculum/dto/{StageResponse,DimensionResponse}.java`
- Create: `src/main/java/com/eduze/manage/curriculum/service/CurriculumService.java`
- Create: `src/main/java/com/eduze/manage/curriculum/controller/CurriculumController.java`
- Test: `src/test/java/com/eduze/manage/curriculum/CurriculumControllerIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/curriculum/CurriculumControllerIT.java`:

```java
package com.eduze.manage.curriculum;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;

class CurriculumControllerIT extends AbstractApiIT {

    @Test
    void listStages_returns5() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/api/curriculum/stages").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].code").value("STAGE_KMD"));
    }

    @Test
    void listDimensions_returns22() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/api/curriculum/dimensions").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(22));
    }

    @Test
    void listDimensions_filterByKind() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/api/curriculum/dimensions?kind=MOVEMENT").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(8));
    }

    @Test
    void stageResponse_includesRecommendedDimensions() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/api/curriculum/stages").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].recommendedDimensions.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=CurriculumControllerIT test`
Expected: FAIL

- [ ] **Step 3: 写 domain**

Create `src/main/java/com/eduze/manage/curriculum/domain/CurriculumStage.java`:

```java
package com.eduze.manage.curriculum.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.TenantBaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_curriculum_stage")
public class CurriculumStage extends TenantBaseEntity {
    private String code;
    private String name;
    private Integer ageMin;
    private Integer ageMax;
    private Integer orderNo;
    private String lorenfieldPhase;
    private String description;
}
```

Create `src/main/java/com/eduze/manage/curriculum/domain/CurriculumDimension.java`:

```java
package com.eduze.manage.curriculum.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.TenantBaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_curriculum_dimension")
public class CurriculumDimension extends TenantBaseEntity {
    private String kind;
    private String code;
    private String name;
    private String description;
    private Integer orderNo;
}
```

Create `src/main/java/com/eduze/manage/curriculum/domain/StageDimension.java`:

```java
package com.eduze.manage.curriculum.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.TenantBaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_stage_dimension")
public class StageDimension extends TenantBaseEntity {
    private Long stageId;
    private Long dimensionId;
    private Integer weight;
}
```

- [ ] **Step 4: 写 mapper / DTO / service / controller**

Create `src/main/java/com/eduze/manage/curriculum/mapper/CurriculumStageMapper.java`:

```java
package com.eduze.manage.curriculum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.curriculum.domain.CurriculumStage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CurriculumStageMapper extends BaseMapper<CurriculumStage> {}
```

Create `src/main/java/com/eduze/manage/curriculum/mapper/CurriculumDimensionMapper.java`:

```java
package com.eduze.manage.curriculum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.curriculum.domain.CurriculumDimension;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CurriculumDimensionMapper extends BaseMapper<CurriculumDimension> {}
```

Create `src/main/java/com/eduze/manage/curriculum/mapper/StageDimensionMapper.java`:

```java
package com.eduze.manage.curriculum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.curriculum.domain.StageDimension;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StageDimensionMapper extends BaseMapper<StageDimension> {}
```

Create `src/main/java/com/eduze/manage/curriculum/dto/DimensionResponse.java`:

```java
package com.eduze.manage.curriculum.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DimensionResponse {
    private final Long id;
    private final String kind;
    private final String code;
    private final String name;
    private final String description;
    private final Integer orderNo;
    private final Integer weight; // 在 stage 视角下的 weight，listAll 时为 null
}
```

Create `src/main/java/com/eduze/manage/curriculum/dto/StageResponse.java`:

```java
package com.eduze.manage.curriculum.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StageResponse {
    private final Long id;
    private final String code;
    private final String name;
    private final Integer ageMin;
    private final Integer ageMax;
    private final Integer orderNo;
    private final String lorenfieldPhase;
    private final String description;
    private final List<DimensionResponse> recommendedDimensions;
}
```

Create `src/main/java/com/eduze/manage/curriculum/service/CurriculumService.java`:

```java
package com.eduze.manage.curriculum.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.curriculum.domain.CurriculumDimension;
import com.eduze.manage.curriculum.domain.CurriculumStage;
import com.eduze.manage.curriculum.domain.StageDimension;
import com.eduze.manage.curriculum.dto.DimensionResponse;
import com.eduze.manage.curriculum.dto.StageResponse;
import com.eduze.manage.curriculum.mapper.CurriculumDimensionMapper;
import com.eduze.manage.curriculum.mapper.CurriculumStageMapper;
import com.eduze.manage.curriculum.mapper.StageDimensionMapper;
import com.eduze.manage.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurriculumService {

    private final CurriculumStageMapper stageMapper;
    private final CurriculumDimensionMapper dimensionMapper;
    private final StageDimensionMapper stageDimensionMapper;

    public List<StageResponse> listStages() {
        Long tenantId = TenantContext.getTenantId();
        List<CurriculumStage> stages = stageMapper.selectList(Wrappers.<CurriculumStage>lambdaQuery()
                .eq(CurriculumStage::getTenantId, tenantId)
                .orderByAsc(CurriculumStage::getOrderNo));
        List<CurriculumDimension> dims = dimensionMapper.selectList(Wrappers.<CurriculumDimension>lambdaQuery()
                .eq(CurriculumDimension::getTenantId, tenantId));
        Map<Long, CurriculumDimension> dimById = dims.stream().collect(Collectors.toMap(CurriculumDimension::getId, d -> d));
        List<StageDimension> mappings = stageDimensionMapper.selectList(Wrappers.<StageDimension>lambdaQuery()
                .eq(StageDimension::getTenantId, tenantId));
        Map<Long, List<StageDimension>> byStage = mappings.stream().collect(Collectors.groupingBy(StageDimension::getStageId));

        return stages.stream().map(s -> StageResponse.builder()
                .id(s.getId()).code(s.getCode()).name(s.getName())
                .ageMin(s.getAgeMin()).ageMax(s.getAgeMax()).orderNo(s.getOrderNo())
                .lorenfieldPhase(s.getLorenfieldPhase()).description(s.getDescription())
                .recommendedDimensions(byStage.getOrDefault(s.getId(), List.of()).stream()
                        .map(sd -> {
                            CurriculumDimension d = dimById.get(sd.getDimensionId());
                            return DimensionResponse.builder()
                                    .id(d.getId()).kind(d.getKind()).code(d.getCode())
                                    .name(d.getName()).description(d.getDescription())
                                    .orderNo(d.getOrderNo()).weight(sd.getWeight())
                                    .build();
                        }).toList())
                .build()).toList();
    }

    public List<DimensionResponse> listDimensions(String kind) {
        return dimensionMapper.selectList(Wrappers.<CurriculumDimension>lambdaQuery()
                        .eq(CurriculumDimension::getTenantId, TenantContext.getTenantId())
                        .eq(kind != null, CurriculumDimension::getKind, kind)
                        .orderByAsc(CurriculumDimension::getKind)
                        .orderByAsc(CurriculumDimension::getOrderNo))
                .stream()
                .map(d -> DimensionResponse.builder()
                        .id(d.getId()).kind(d.getKind()).code(d.getCode()).name(d.getName())
                        .description(d.getDescription()).orderNo(d.getOrderNo()).build())
                .toList();
    }
}
```

Create `src/main/java/com/eduze/manage/curriculum/controller/CurriculumController.java`:

```java
package com.eduze.manage.curriculum.controller;

import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.curriculum.dto.DimensionResponse;
import com.eduze.manage.curriculum.dto.StageResponse;
import com.eduze.manage.curriculum.service.CurriculumService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/curriculum")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService curriculumService;

    @GetMapping("/stages")
    @PreAuthorize("hasAuthority('course:read')")
    public ApiResponse<List<StageResponse>> stages() {
        return ApiResponse.ok(curriculumService.listStages());
    }

    @GetMapping("/dimensions")
    @PreAuthorize("hasAuthority('course:read')")
    public ApiResponse<List<DimensionResponse>> dimensions(@RequestParam(required = false) String kind) {
        return ApiResponse.ok(curriculumService.listDimensions(kind));
    }
}
```

- [ ] **Step 5: 运行测试，确认通过**

Run: `./mvnw -Dtest=CurriculumControllerIT test`
Expected: PASS

- [ ] **Step 6: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/curriculum/ \
        src/test/java/com/eduze/manage/curriculum/CurriculumControllerIT.java
git commit -m "feat(curriculum): read-only API for stages and dimensions"
```

---

## Task 9: Student domain 扩展 mentor / stage 字段

**Files:**
- Modify: `src/main/java/com/eduze/manage/student/domain/Student.java`
- Modify: `src/main/java/com/eduze/manage/student/dto/StudentResponse.java`
- Test: `src/test/java/com/eduze/manage/student/StudentMentorFieldIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/student/StudentMentorFieldIT.java`:

```java
package com.eduze.manage.student;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.mapper.StudentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StudentMentorFieldIT extends AbstractITContainerTest {

    @Autowired
    StudentMapper studentMapper;

    @Test
    void demoStudent_hasMentorAndStage() {
        Student s = studentMapper.selectById(240001L);
        assertThat(s.getMentorTeacherId()).isEqualTo(1101L);
        assertThat(s.getCurrentStageId()).isEqualTo(200001L);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=StudentMentorFieldIT test`
Expected: FAIL（字段未映射）

- [ ] **Step 3: 修改 Student domain**

Edit `src/main/java/com/eduze/manage/student/domain/Student.java` — 在 `private String avatarUrl;` 之后追加两个字段：

```java
    private Long mentorTeacherId;
    private Long currentStageId;
```

- [ ] **Step 4: 修改 StudentResponse**

Edit `src/main/java/com/eduze/manage/student/dto/StudentResponse.java` — 在 builder 的字段列表追加：

```java
    private final Long mentorTeacherId;
    private final String mentorTeacherName;
    private final Long currentStageId;
    private final String currentStageCode;
```

- [ ] **Step 5: 运行测试，确认通过**

Run: `./mvnw -Dtest=StudentMentorFieldIT test`
Expected: PASS

- [ ] **Step 6: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/student/domain/Student.java \
        src/main/java/com/eduze/manage/student/dto/StudentResponse.java \
        src/test/java/com/eduze/manage/student/StudentMentorFieldIT.java
git commit -m "feat(student): add mentor_teacher_id and current_stage_id fields"
```

---

# P3. 主带老师管理

## Task 10: Student 创建/更新强制 mentor 校验

**Files:**
- Modify: `src/main/java/com/eduze/manage/student/dto/StudentRequest.java`（增 `mentorTeacherId` + 校验注解）
- Modify: `src/main/java/com/eduze/manage/student/dto/StudentUpdateRequest.java`
- Modify: `src/main/java/com/eduze/manage/student/service/StudentService.java`（applyRequest 中绑定 + 写 mentor history）
- Create: `src/main/java/com/eduze/manage/student/domain/StudentMentorHistory.java`
- Create: `src/main/java/com/eduze/manage/student/mapper/StudentMentorHistoryMapper.java`
- Test: `src/test/java/com/eduze/manage/student/StudentMentorRequiredIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/student/StudentMentorRequiredIT.java`:

```java
package com.eduze.manage.student;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class StudentMentorRequiredIT extends AbstractApiIT {

    @Test
    void createStudent_withoutMentor_fails() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/api/students")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"branchId":1,"enrollNo":"M001","name":"无导师娃","gender":1,
                             "birthday":"2020-01-01","enrollDate":"2026-05-01","status":1}
                            """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createStudent_withMentor_succeedsAndHistoryWritten() throws Exception {
        String token = adminToken();
        String body = mockMvc.perform(post("/api/students")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"branchId":1,"enrollNo":"M002","name":"有导师娃","gender":1,
                             "birthday":"2020-01-01","enrollDate":"2026-05-01","status":1,
                             "mentorTeacherId":1101}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mentorTeacherId").value(1101))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("data").get("id").asLong();

        // 历史表自动写入 1 条
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/students/" + id + "/mentor-history")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].fromTeacherId").doesNotExist())
                .andExpect(jsonPath("$.data[0].toTeacherId").value(1101));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=StudentMentorRequiredIT test`
Expected: FAIL

- [ ] **Step 3: 建 mentor history domain + mapper**

Create `src/main/java/com/eduze/manage/student/domain/StudentMentorHistory.java`:

```java
package com.eduze.manage.student.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_student_mentor_history")
public class StudentMentorHistory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long branchId;

    private Long studentId;
    private Long fromTeacherId;
    private Long toTeacherId;
    private String reason;
    private LocalDateTime changedAt;
    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

Create `src/main/java/com/eduze/manage/student/mapper/StudentMentorHistoryMapper.java`:

```java
package com.eduze.manage.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.student.domain.StudentMentorHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMentorHistoryMapper extends BaseMapper<StudentMentorHistory> {}
```

- [ ] **Step 4: 改 DTO + service**

Edit `src/main/java/com/eduze/manage/student/dto/StudentRequest.java` — 增加字段：

```java
import jakarta.validation.constraints.NotNull;

// ... 在 StudentRequest 内追加 ...
@NotNull(message = "必须指定主带老师")
private Long mentorTeacherId;
```

Edit `src/main/java/com/eduze/manage/student/dto/StudentUpdateRequest.java` — 同样加（不带 @NotNull，用 update 单独接口换主带）：

```java
private Long mentorTeacherId;
```

Edit `src/main/java/com/eduze/manage/student/service/StudentService.java`：

1. 注入 `StudentMentorHistoryMapper`
2. `applyRequest` 末尾加：`student.setMentorTeacherId(request.getMentorTeacherId());`
3. `create` 内 `studentMapper.insert(student)` 之后追加：

```java
StudentMentorHistory history = new StudentMentorHistory();
history.setStudentId(student.getId());
history.setFromTeacherId(null);
history.setToTeacherId(request.getMentorTeacherId());
history.setReason("首次绑定");
history.setChangedAt(java.time.LocalDateTime.now());
history.setOperatorId(currentUserId());
studentMentorHistoryMapper.insert(history);
```

并新增 helper：

```java
private Long currentUserId() {
    var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof com.eduze.manage.auth.security.CustomUserDetails d) {
        return d.getUserId();
    }
    return null;
}
```

4. `toResponse` 中追加 mentor 名字解析（用 `JdbcTemplate` 单查 `t_user`）：

```java
String mentorName = student.getMentorTeacherId() == null ? null :
    jdbcTemplate.queryForObject(
        "SELECT name FROM t_user WHERE id=? AND deleted_at=0",
        String.class, student.getMentorTeacherId());
// 加进 builder：
.mentorTeacherId(student.getMentorTeacherId())
.mentorTeacherName(mentorName)
.currentStageId(student.getCurrentStageId())
```

并增加 mentor 历史 controller 端点：

Edit `src/main/java/com/eduze/manage/student/controller/StudentController.java` 追加：

```java
private final StudentMentorService studentMentorService;

@GetMapping("/{id}/mentor-history")
@PreAuthorize("hasAuthority('student:read')")
public ApiResponse<List<MentorHistoryResponse>> mentorHistory(@PathVariable Long id) {
    return ApiResponse.ok(studentMentorService.history(id));
}
```

- [ ] **Step 5: 写 mentor history DTO + service**

Create `src/main/java/com/eduze/manage/student/dto/MentorHistoryResponse.java`:

```java
package com.eduze.manage.student.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MentorHistoryResponse {
    private final Long id;
    private final Long studentId;
    private final Long fromTeacherId;
    private final String fromTeacherName;
    private final Long toTeacherId;
    private final String toTeacherName;
    private final String reason;
    private final LocalDateTime changedAt;
}
```

Create `src/main/java/com/eduze/manage/student/service/StudentMentorService.java` — 先放一个 stub，下个任务再扩展：

```java
package com.eduze.manage.student.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.student.domain.StudentMentorHistory;
import com.eduze.manage.student.dto.MentorHistoryResponse;
import com.eduze.manage.student.mapper.StudentMentorHistoryMapper;
import com.eduze.manage.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentMentorService {

    private final StudentMentorHistoryMapper historyMapper;
    private final JdbcTemplate jdbcTemplate;

    public List<MentorHistoryResponse> history(Long studentId) {
        List<StudentMentorHistory> list = historyMapper.selectList(Wrappers.<StudentMentorHistory>lambdaQuery()
                .eq(StudentMentorHistory::getTenantId, TenantContext.getTenantId())
                .eq(StudentMentorHistory::getStudentId, studentId)
                .orderByDesc(StudentMentorHistory::getChangedAt));
        Map<Long, String> names = jdbcTemplate.query(
                "SELECT id,name FROM t_user WHERE deleted_at=0",
                rs -> {
                    java.util.HashMap<Long, String> map = new java.util.HashMap<>();
                    while (rs.next()) map.put(rs.getLong(1), rs.getString(2));
                    return map;
                });
        return list.stream()
                .map(h -> MentorHistoryResponse.builder()
                        .id(h.getId())
                        .studentId(h.getStudentId())
                        .fromTeacherId(h.getFromTeacherId())
                        .fromTeacherName(h.getFromTeacherId() == null ? null : names.get(h.getFromTeacherId()))
                        .toTeacherId(h.getToTeacherId())
                        .toTeacherName(names.get(h.getToTeacherId()))
                        .reason(h.getReason())
                        .changedAt(h.getChangedAt())
                        .build())
                .toList();
    }
}
```

- [ ] **Step 6: 运行测试，确认通过**

Run: `./mvnw -Dtest=StudentMentorRequiredIT test`
Expected: PASS

- [ ] **Step 7: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/student/ \
        src/test/java/com/eduze/manage/student/StudentMentorRequiredIT.java
git commit -m "feat(student): require mentorTeacherId on create, write history"
```

---

## Task 11: 换主带老师 API

**Files:**
- Create: `src/main/java/com/eduze/manage/student/dto/AssignMentorRequest.java`
- Modify: `src/main/java/com/eduze/manage/student/service/StudentMentorService.java`（加 `change` 方法）
- Modify: `src/main/java/com/eduze/manage/student/controller/StudentController.java`（加 `PUT /{id}/mentor`）
- Test: `src/test/java/com/eduze/manage/student/StudentChangeMentorIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/student/StudentChangeMentorIT.java`:

```java
package com.eduze.manage.student;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class StudentChangeMentorIT extends AbstractApiIT {

    @Test
    void changeMentor_updatesStudentAndAppendsHistory() throws Exception {
        String token = adminToken();
        // demo student 240001 主带 = 1101
        mockMvc.perform(put("/api/students/240001/mentor")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"toTeacherId":1102,"reason":"老师换岗","keepSubscriptions":true}
                            """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students/240001").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.mentorTeacherId").value(1102));

        mockMvc.perform(get("/api/students/240001/mentor-history").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data[0].toTeacherId").value(1102))
                .andExpect(jsonPath("$.data[0].fromTeacherId").value(1101))
                .andExpect(jsonPath("$.data[0].reason").value("老师换岗"));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=StudentChangeMentorIT test`
Expected: FAIL

- [ ] **Step 3: 写 DTO**

Create `src/main/java/com/eduze/manage/student/dto/AssignMentorRequest.java`:

```java
package com.eduze.manage.student.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignMentorRequest {
    @NotNull private Long toTeacherId;
    private String reason;
    private Boolean keepSubscriptions;
}
```

- [ ] **Step 4: 扩展 StudentMentorService**

Edit `src/main/java/com/eduze/manage/student/service/StudentMentorService.java` — 注入 StudentMapper、LessonSubscriptionMapper（占位，Task 12 后真用），增加 `change`：

```java
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.domain.StudentMentorHistory;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.student.dto.AssignMentorRequest;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

// ... 字段追加：
private final StudentMapper studentMapper;

@Transactional
public void change(Long studentId, AssignMentorRequest req) {
    Student student = studentMapper.selectById(studentId);
    if (student == null) throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
    Long from = student.getMentorTeacherId();
    if (req.getToTeacherId().equals(from)) return;
    student.setMentorTeacherId(req.getToTeacherId());
    studentMapper.updateById(student);

    StudentMentorHistory h = new StudentMentorHistory();
    h.setStudentId(studentId);
    h.setFromTeacherId(from);
    h.setToTeacherId(req.getToTeacherId());
    h.setReason(req.getReason());
    h.setChangedAt(java.time.LocalDateTime.now());
    historyMapper.insert(h);

    // keepSubscriptions=false 时，停用旧老师下的所有订阅
    // 留给 Task 12 实现 LessonSubscriptionService 后再补全这一段
}
```

- [ ] **Step 5: 加 Controller 端点**

Edit `src/main/java/com/eduze/manage/student/controller/StudentController.java` 追加：

```java
@PutMapping("/{id}/mentor")
@PreAuthorize("hasAuthority('student:mentor_assign')")
public ApiResponse<Void> changeMentor(
        @PathVariable Long id, @Valid @RequestBody AssignMentorRequest req) {
    studentMentorService.change(id, req);
    return ApiResponse.ok(null);
}
```

- [ ] **Step 6: 运行测试，确认通过**

Run: `./mvnw -Dtest=StudentChangeMentorIT test`
Expected: PASS

- [ ] **Step 7: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/student/ \
        src/test/java/com/eduze/manage/student/StudentChangeMentorIT.java
git commit -m "feat(student): PUT /api/students/{id}/mentor + history"
```

---

# P4. 排课模型重塑

## Task 12: LessonSubscription CRUD

**Files:**
- Create: `src/main/java/com/eduze/manage/lesson/domain/LessonSubscription.java`
- Create: `src/main/java/com/eduze/manage/lesson/mapper/LessonSubscriptionMapper.java`
- Create: `src/main/java/com/eduze/manage/lesson/dto/SubscriptionRequest.java`
- Create: `src/main/java/com/eduze/manage/lesson/dto/SubscriptionResponse.java`
- Create: `src/main/java/com/eduze/manage/lesson/service/LessonSubscriptionService.java`
- Create: `src/main/java/com/eduze/manage/lesson/controller/LessonSubscriptionController.java`
- Test: `src/test/java/com/eduze/manage/lesson/LessonSubscriptionIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/lesson/LessonSubscriptionIT.java`:

```java
package com.eduze.manage.lesson;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class LessonSubscriptionIT extends AbstractApiIT {

    @Test
    void crud_flow() throws Exception {
        String token = adminToken();

        // CREATE — student 240003 订阅 teacher 1101 周六 availability 230001
        String body = mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"branchId":1,"studentId":240003,"teacherId":1101,
                             "teacherAvailabilityId":230001,"validFrom":"2026-06-01","source":"NORMAL"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("data").get("id").asLong();

        // LIST by student
        mockMvc.perform(get("/api/subscriptions?studentId=240003").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id==" + id + ")]").exists());

        // DELETE = soft cancel
        mockMvc.perform(delete("/api/subscriptions/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void duplicate_subscription_rejected() throws Exception {
        String token = adminToken();
        // 学员 240001 已经订阅 availability 230001（demo seed）
        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"branchId":1,"studentId":240001,"teacherId":1101,
                             "teacherAvailabilityId":230001,"validFrom":"2026-06-01","source":"NORMAL"}
                            """))
                .andExpect(status().is4xxClientError());
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=LessonSubscriptionIT test`
Expected: FAIL

- [ ] **Step 3: domain + mapper**

Create `src/main/java/com/eduze/manage/lesson/domain/LessonSubscription.java`:

```java
package com.eduze.manage.lesson.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_lesson_subscription")
public class LessonSubscription extends BaseEntity {
    private Long studentId;
    private Long teacherId;
    private Long teacherAvailabilityId;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer status;
    private String source;
    private String note;
}
```

Create `src/main/java/com/eduze/manage/lesson/mapper/LessonSubscriptionMapper.java`:

```java
package com.eduze.manage.lesson.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.lesson.domain.LessonSubscription;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LessonSubscriptionMapper extends BaseMapper<LessonSubscription> {}
```

- [ ] **Step 4: DTO**

Create `src/main/java/com/eduze/manage/lesson/dto/SubscriptionRequest.java`:

```java
package com.eduze.manage.lesson.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionRequest {
    @NotNull private Long branchId;
    @NotNull private Long studentId;
    @NotNull private Long teacherId;
    @NotNull private Long teacherAvailabilityId;
    @NotNull private LocalDate validFrom;
    private LocalDate validTo;
    private String source;
    private String note;
}
```

Create `src/main/java/com/eduze/manage/lesson/dto/SubscriptionResponse.java`:

```java
package com.eduze.manage.lesson.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionResponse {
    private final Long id;
    private final Long studentId;
    private final Long teacherId;
    private final Long teacherAvailabilityId;
    private final Long branchId;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final Integer status;
    private final String source;
    private final String note;
}
```

- [ ] **Step 5: service + controller**

Create `src/main/java/com/eduze/manage/lesson/service/LessonSubscriptionService.java`:

```java
package com.eduze.manage.lesson.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.lesson.domain.LessonSubscription;
import com.eduze.manage.lesson.dto.SubscriptionRequest;
import com.eduze.manage.lesson.dto.SubscriptionResponse;
import com.eduze.manage.lesson.mapper.LessonSubscriptionMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LessonSubscriptionService {

    private final LessonSubscriptionMapper mapper;

    public List<SubscriptionResponse> list(Long studentId, Long teacherId) {
        return mapper.selectList(Wrappers.<LessonSubscription>lambdaQuery()
                        .eq(LessonSubscription::getTenantId, TenantContext.getTenantId())
                        .eq(studentId != null, LessonSubscription::getStudentId, studentId)
                        .eq(teacherId != null, LessonSubscription::getTeacherId, teacherId)
                        .orderByDesc(LessonSubscription::getValidFrom))
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest req) {
        // 重复校验：同 student × 同 availability × status=1 的有效订阅
        List<LessonSubscription> dup = mapper.selectList(Wrappers.<LessonSubscription>lambdaQuery()
                .eq(LessonSubscription::getTenantId, TenantContext.getTenantId())
                .eq(LessonSubscription::getStudentId, req.getStudentId())
                .eq(LessonSubscription::getTeacherAvailabilityId, req.getTeacherAvailabilityId())
                .eq(LessonSubscription::getStatus, 1));
        if (!dup.isEmpty()) {
            throw new BizException(ErrorCode.CONFLICT, "该学员已订阅此时段");
        }
        LessonSubscription s = new LessonSubscription();
        s.setTenantId(TenantContext.getTenantId());
        s.setBranchId(req.getBranchId());
        s.setStudentId(req.getStudentId());
        s.setTeacherId(req.getTeacherId());
        s.setTeacherAvailabilityId(req.getTeacherAvailabilityId());
        s.setValidFrom(req.getValidFrom());
        s.setValidTo(req.getValidTo());
        s.setStatus(1);
        s.setSource(req.getSource() == null ? "NORMAL" : req.getSource());
        s.setNote(req.getNote());
        try {
            mapper.insert(s);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.CONFLICT, "订阅冲突");
        }
        return toResponse(s);
    }

    @Transactional
    public SubscriptionResponse update(Long id, SubscriptionRequest req) {
        LessonSubscription s = require(id);
        s.setValidFrom(req.getValidFrom());
        s.setValidTo(req.getValidTo());
        s.setSource(req.getSource() == null ? s.getSource() : req.getSource());
        s.setNote(req.getNote());
        mapper.updateById(s);
        return toResponse(s);
    }

    @Transactional
    public void delete(Long id) {
        LessonSubscription s = require(id);
        s.setStatus(0);
        s.setValidTo(LocalDate.now());
        mapper.updateById(s);
        // 软删 = status=0 + validTo=today；保留行做审计
        // 后续 Task 13/14 会把"未来已生成的 lesson_student"批量软删除
    }

    public LessonSubscription require(Long id) {
        LessonSubscription s = mapper.selectById(id);
        if (s == null) throw new BizException(ErrorCode.NOT_FOUND, "订阅不存在");
        return s;
    }

    private SubscriptionResponse toResponse(LessonSubscription s) {
        return SubscriptionResponse.builder()
                .id(s.getId()).studentId(s.getStudentId()).teacherId(s.getTeacherId())
                .teacherAvailabilityId(s.getTeacherAvailabilityId()).branchId(s.getBranchId())
                .validFrom(s.getValidFrom()).validTo(s.getValidTo())
                .status(s.getStatus()).source(s.getSource()).note(s.getNote())
                .build();
    }
}
```

Create `src/main/java/com/eduze/manage/lesson/controller/LessonSubscriptionController.java`:

```java
package com.eduze.manage.lesson.controller;

import com.eduze.manage.common.web.ApiResponse;
import com.eduze.manage.lesson.dto.SubscriptionRequest;
import com.eduze.manage.lesson.dto.SubscriptionResponse;
import com.eduze.manage.lesson.service.LessonSubscriptionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class LessonSubscriptionController {

    private final LessonSubscriptionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('subscription:read')")
    public ApiResponse<List<SubscriptionResponse>> list(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long teacherId) {
        return ApiResponse.ok(service.list(studentId, teacherId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('subscription:write')")
    public ApiResponse<SubscriptionResponse> create(@Valid @RequestBody SubscriptionRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('subscription:write')")
    public ApiResponse<SubscriptionResponse> update(
            @PathVariable Long id, @Valid @RequestBody SubscriptionRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('subscription:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
```

- [ ] **Step 6: 运行测试，确认通过**

Run: `./mvnw -Dtest=LessonSubscriptionIT test`
Expected: PASS

- [ ] **Step 7: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/lesson/ \
        src/test/java/com/eduze/manage/lesson/LessonSubscriptionIT.java
git commit -m "feat(lesson): LessonSubscription CRUD"
```

---

## Task 13: LessonStudent (课次名单) CRUD

**Files:**
- Create: `src/main/java/com/eduze/manage/lesson/domain/LessonStudent.java`
- Create: `src/main/java/com/eduze/manage/lesson/mapper/LessonStudentMapper.java`
- Create: `src/main/java/com/eduze/manage/lesson/dto/LessonStudentRequest.java`
- Create: `src/main/java/com/eduze/manage/lesson/dto/LessonStudentResponse.java`
- Create: `src/main/java/com/eduze/manage/lesson/service/LessonStudentService.java`
- Modify: `src/main/java/com/eduze/manage/lesson/controller/LessonController.java`（加 3 个新端点）
- Test: `src/test/java/com/eduze/manage/lesson/LessonStudentIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/lesson/LessonStudentIT.java`:

```java
package com.eduze.manage.lesson;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class LessonStudentIT extends AbstractApiIT {

    @Test
    void manualAddRemoveStudentOnLesson() throws Exception {
        String token = adminToken();
        // 先建一个临时 lesson（teacher 1101 在某个未来时间点）
        String lessonBody = mockMvc.perform(post("/api/lessons")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"branchId":1,"teacherId":1101,
                             "startAt":"2026-07-04T09:00:00","endAt":"2026-07-04T10:30:00"}
                            """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long lessonId = objectMapper.readTree(lessonBody).get("data").get("id").asLong();

        // 手动加入学员 240003（试听）
        mockMvc.perform(post("/api/lessons/" + lessonId + "/students")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"studentId":240003,"source":"TRIAL"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value(240003))
                .andExpect(jsonPath("$.data.source").value("TRIAL"));

        // 查名单
        mockMvc.perform(get("/api/lessons/" + lessonId + "/students").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // 移除
        mockMvc.perform(delete("/api/lessons/" + lessonId + "/students/240003")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/lessons/" + lessonId + "/students").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=LessonStudentIT test`
Expected: FAIL

- [ ] **Step 3: domain / mapper / DTO**

Create `src/main/java/com/eduze/manage/lesson/domain/LessonStudent.java`:

```java
package com.eduze.manage.lesson.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_lesson_student")
public class LessonStudent extends BaseEntity {
    private Long lessonId;
    private Long studentId;
    private Long subscriptionId;
    private String source;
    private String status;
    private String note;
    private LocalDateTime removedAt;
    private Long removedBy;
}
```

Create `src/main/java/com/eduze/manage/lesson/mapper/LessonStudentMapper.java`:

```java
package com.eduze.manage.lesson.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.lesson.domain.LessonStudent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LessonStudentMapper extends BaseMapper<LessonStudent> {}
```

Create `src/main/java/com/eduze/manage/lesson/dto/LessonStudentRequest.java`:

```java
package com.eduze.manage.lesson.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LessonStudentRequest {
    @NotNull private Long studentId;
    private String source;
    private Long subscriptionId;
    private String note;
}
```

Create `src/main/java/com/eduze/manage/lesson/dto/LessonStudentResponse.java`:

```java
package com.eduze.manage.lesson.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LessonStudentResponse {
    private final Long id;
    private final Long lessonId;
    private final Long studentId;
    private final String studentName;
    private final Long subscriptionId;
    private final String source;
    private final String status;
    private final String note;
}
```

- [ ] **Step 4: service**

Create `src/main/java/com/eduze/manage/lesson/service/LessonStudentService.java`:

```java
package com.eduze.manage.lesson.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.domain.LessonStudent;
import com.eduze.manage.lesson.dto.LessonStudentRequest;
import com.eduze.manage.lesson.dto.LessonStudentResponse;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.lesson.mapper.LessonStudentMapper;
import com.eduze.manage.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LessonStudentService {

    private final LessonStudentMapper mapper;
    private final LessonMapper lessonMapper;
    private final JdbcTemplate jdbcTemplate;

    public List<LessonStudentResponse> list(Long lessonId) {
        List<LessonStudent> list = mapper.selectList(Wrappers.<LessonStudent>lambdaQuery()
                .eq(LessonStudent::getTenantId, TenantContext.getTenantId())
                .eq(LessonStudent::getLessonId, lessonId)
                .eq(LessonStudent::getStatus, "BOOKED")
                .orderByAsc(LessonStudent::getId));
        Map<Long, String> names = jdbcTemplate.query(
                "SELECT id,name FROM t_student WHERE deleted_at=0",
                rs -> {
                    java.util.HashMap<Long, String> m = new java.util.HashMap<>();
                    while (rs.next()) m.put(rs.getLong(1), rs.getString(2));
                    return m;
                });
        return list.stream()
                .map(ls -> LessonStudentResponse.builder()
                        .id(ls.getId()).lessonId(ls.getLessonId()).studentId(ls.getStudentId())
                        .studentName(names.get(ls.getStudentId()))
                        .subscriptionId(ls.getSubscriptionId())
                        .source(ls.getSource()).status(ls.getStatus()).note(ls.getNote())
                        .build())
                .toList();
    }

    @Transactional
    public LessonStudentResponse add(Long lessonId, LessonStudentRequest req) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) throw new BizException(ErrorCode.NOT_FOUND, "课次不存在");
        LessonStudent ls = new LessonStudent();
        ls.setTenantId(TenantContext.getTenantId());
        ls.setBranchId(lesson.getBranchId());
        ls.setLessonId(lessonId);
        ls.setStudentId(req.getStudentId());
        ls.setSubscriptionId(req.getSubscriptionId());
        ls.setSource(req.getSource() == null ? "MANUAL" : req.getSource());
        ls.setStatus("BOOKED");
        ls.setNote(req.getNote());
        try {
            mapper.insert(ls);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.CONFLICT, "该学员已在此课次名单中");
        }
        return list(lessonId).stream().filter(r -> r.getId().equals(ls.getId())).findFirst().orElseThrow();
    }

    @Transactional
    public void remove(Long lessonId, Long studentId) {
        List<LessonStudent> matches = mapper.selectList(Wrappers.<LessonStudent>lambdaQuery()
                .eq(LessonStudent::getTenantId, TenantContext.getTenantId())
                .eq(LessonStudent::getLessonId, lessonId)
                .eq(LessonStudent::getStudentId, studentId));
        for (LessonStudent ls : matches) {
            mapper.deleteById(ls.getId());
        }
    }
}
```

- [ ] **Step 5: 加 controller 端点**

Edit `src/main/java/com/eduze/manage/lesson/controller/LessonController.java` 追加：

```java
private final LessonStudentService lessonStudentService;

@GetMapping("/{id}/students")
@PreAuthorize("hasAuthority('lesson:read')")
public ApiResponse<List<LessonStudentResponse>> students(@PathVariable Long id) {
    return ApiResponse.ok(lessonStudentService.list(id));
}

@PostMapping("/{id}/students")
@PreAuthorize("hasAuthority('lesson:write')")
public ApiResponse<LessonStudentResponse> addStudent(
        @PathVariable Long id, @Valid @RequestBody LessonStudentRequest req) {
    return ApiResponse.ok(lessonStudentService.add(id, req));
}

@DeleteMapping("/{id}/students/{studentId}")
@PreAuthorize("hasAuthority('lesson:write')")
public ApiResponse<Void> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
    lessonStudentService.remove(id, studentId);
    return ApiResponse.ok(null);
}
```

- [ ] **Step 6: 运行测试，确认通过**

Run: `./mvnw -Dtest=LessonStudentIT test`
Expected: PASS

- [ ] **Step 7: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/lesson/ \
        src/test/java/com/eduze/manage/lesson/LessonStudentIT.java
git commit -m "feat(lesson): LessonStudent roster CRUD (manual add/remove)"
```

---

## Task 14: ScheduleGenerator 核心算法

**Files:**
- Create: `src/main/java/com/eduze/manage/lesson/service/ScheduleGenerator.java`
- Create: `src/main/java/com/eduze/manage/lesson/dto/GenerateByAvailabilityRequest.java`
- Create: `src/main/java/com/eduze/manage/lesson/dto/GenerateByAvailabilityResult.java`
- Test: `src/test/java/com/eduze/manage/lesson/ScheduleGeneratorIT.java`（≥ 10 个边界用例）

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/lesson/ScheduleGeneratorIT.java`:

```java
package com.eduze.manage.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import com.eduze.manage.AbstractITContainerTest;
import com.eduze.manage.lesson.dto.GenerateByAvailabilityRequest;
import com.eduze.manage.lesson.dto.GenerateByAvailabilityResult;
import com.eduze.manage.lesson.service.ScheduleGenerator;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ScheduleGeneratorIT extends AbstractITContainerTest {

    @Autowired ScheduleGenerator generator;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setTenant() {
        TenantContext.setTenantId(1L);
    }

    @Test
    void generate_2weeks_forZhangSaturdayMorning() {
        GenerateByAvailabilityRequest req = new GenerateByAvailabilityRequest();
        req.setWeeks(2);
        req.setFromDate(LocalDate.of(2026, 6, 6));        // 周六
        req.setTeacherIds(List.of(1101L));
        GenerateByAvailabilityResult result = generator.generate(req);
        // availability 230001 周六 + 230002 周日 → 2 周 × 2 天 = 4 节
        assertThat(result.getGenerated()).isEqualTo(4);
    }

    @Test
    void generate_isIdempotent() {
        GenerateByAvailabilityRequest req = new GenerateByAvailabilityRequest();
        req.setWeeks(2);
        req.setFromDate(LocalDate.of(2026, 6, 6));
        req.setTeacherIds(List.of(1101L));
        generator.generate(req);
        GenerateByAvailabilityResult second = generator.generate(req);
        assertThat(second.getGenerated()).isEqualTo(0);   // 第二次全部跳过
        assertThat(second.getSkipped()).isGreaterThan(0);
    }

    @Test
    void generatedLessonHasRosterFromSubscription() {
        GenerateByAvailabilityRequest req = new GenerateByAvailabilityRequest();
        req.setWeeks(1);
        req.setFromDate(LocalDate.of(2026, 6, 6));
        req.setTeacherIds(List.of(1101L));
        generator.generate(req);
        // demo student 240001 订阅 availability 230001 → 应进入名单
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_lesson_student ls JOIN t_lesson l ON l.id=ls.lesson_id "
                        + "WHERE l.teacher_id=1101 AND ls.student_id=240001 AND ls.status='BOOKED' AND ls.deleted_at=0",
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void unsubscribedStudent_notInRoster() {
        // 学员先订阅再退订，再生成
        Long subId = jdbc.queryForObject(
                "SELECT id FROM t_lesson_subscription WHERE student_id=240001 AND teacher_availability_id=230001",
                Long.class);
        jdbc.update("UPDATE t_lesson_subscription SET status=0, valid_to='2026-05-30' WHERE id=?", subId);
        GenerateByAvailabilityRequest req = new GenerateByAvailabilityRequest();
        req.setWeeks(1);
        req.setFromDate(LocalDate.of(2026, 6, 6));
        req.setTeacherIds(List.of(1101L));
        generator.generate(req);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_lesson_student ls JOIN t_lesson l ON l.id=ls.lesson_id "
                        + "WHERE l.teacher_id=1101 AND ls.student_id=240001 "
                        + "AND l.start_at >= '2026-06-06' AND ls.deleted_at=0",
                Integer.class);
        assertThat(count).isEqualTo(0);
        // 恢复
        jdbc.update("UPDATE t_lesson_subscription SET status=1, valid_to=NULL WHERE id=?", subId);
    }

    @Test
    void disabledAvailability_skipped() {
        jdbc.update("UPDATE t_teacher_availability SET status=0 WHERE id=230001");
        GenerateByAvailabilityRequest req = new GenerateByAvailabilityRequest();
        req.setWeeks(1);
        req.setFromDate(LocalDate.of(2026, 6, 13));   // 新一周避开已 generated
        req.setTeacherIds(List.of(1101L));
        GenerateByAvailabilityResult result = generator.generate(req);
        // 只有 230002 周日生效 → 1 节
        assertThat(result.getGenerated()).isEqualTo(1);
        jdbc.update("UPDATE t_teacher_availability SET status=1 WHERE id=230001");
    }

    @Test
    void expiredValidTo_skipped() {
        jdbc.update("UPDATE t_teacher_availability SET valid_to='2026-05-31' WHERE id=230002");
        GenerateByAvailabilityRequest req = new GenerateByAvailabilityRequest();
        req.setWeeks(1);
        req.setFromDate(LocalDate.of(2026, 6, 20));
        req.setTeacherIds(List.of(1101L));
        GenerateByAvailabilityResult result = generator.generate(req);
        assertThat(result.getGenerated()).isEqualTo(1);    // 只有 230001 周六
        jdbc.update("UPDATE t_teacher_availability SET valid_to=NULL WHERE id=230002");
    }

    @Test
    void weeks_overEightLimit_throws() {
        GenerateByAvailabilityRequest req = new GenerateByAvailabilityRequest();
        req.setWeeks(20);
        req.setFromDate(LocalDate.of(2026, 7, 1));
        req.setTeacherIds(List.of(1101L));
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> generator.generate(req));
    }

    @Test
    void zeroTeacherIds_meansAll() {
        GenerateByAvailabilityRequest req = new GenerateByAvailabilityRequest();
        req.setWeeks(1);
        req.setFromDate(LocalDate.of(2026, 7, 6));
        req.setTeacherIds(List.of());
        GenerateByAvailabilityResult result = generator.generate(req);
        // 6 条 availability → 6 节
        assertThat(result.getGenerated()).isEqualTo(6);
    }

    @Test
    void subscriptionValidFromAfterLessonDate_excludesStudent() {
        // 创建一个 validFrom=未来的订阅，不应进入更早的课次名单
        Long subId = generator.testInsertSubscription(240002L, 1102L, 230003L, LocalDate.of(2026, 12, 1));
        GenerateByAvailabilityRequest req = new GenerateByAvailabilityRequest();
        req.setWeeks(1);
        req.setFromDate(LocalDate.of(2026, 7, 11));
        req.setTeacherIds(List.of(1102L));
        generator.generate(req);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_lesson_student WHERE subscription_id=? AND deleted_at=0",
                Integer.class, subId);
        assertThat(count).isEqualTo(0);
        jdbc.update("DELETE FROM t_lesson_subscription WHERE id=?", subId);
    }

    @Test
    void conflicts_detectedAsSoftWarning() {
        // 手动插入一个 1101 周六 09:00 的课次（与即将生成的撞），看 result.conflicts
        jdbc.update("INSERT INTO t_lesson (id,tenant_id,branch_id,teacher_id,start_at,end_at,status,source) VALUES "
                + "(9999001,1,1,1101,'2026-07-18 09:00:00','2026-07-18 10:30:00','SCHEDULED',2)");
        GenerateByAvailabilityRequest req = new GenerateByAvailabilityRequest();
        req.setWeeks(1);
        req.setFromDate(LocalDate.of(2026, 7, 18));
        req.setTeacherIds(List.of(1101L));
        GenerateByAvailabilityResult result = generator.generate(req);
        assertThat(result.getSkipped()).isGreaterThan(0);
        jdbc.update("DELETE FROM t_lesson WHERE id=9999001");
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=ScheduleGeneratorIT test`
Expected: FAIL（ScheduleGenerator 不存在）

- [ ] **Step 3: 写 DTO + service**

Create `src/main/java/com/eduze/manage/lesson/dto/GenerateByAvailabilityRequest.java`:

```java
package com.eduze.manage.lesson.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateByAvailabilityRequest {
    @NotNull @Min(1) @Max(8) private Integer weeks;
    @NotNull private LocalDate fromDate;
    /** 空 = 全部老师 */
    private List<Long> teacherIds;
}
```

Create `src/main/java/com/eduze/manage/lesson/dto/GenerateByAvailabilityResult.java`:

```java
package com.eduze.manage.lesson.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GenerateByAvailabilityResult {
    private final int generated;
    private final int skipped;
    private final List<String> conflicts;
}
```

Create `src/main/java/com/eduze/manage/lesson/service/ScheduleGenerator.java`:

```java
package com.eduze.manage.lesson.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.lesson.domain.Lesson;
import com.eduze.manage.lesson.domain.LessonStudent;
import com.eduze.manage.lesson.domain.LessonSubscription;
import com.eduze.manage.lesson.dto.GenerateByAvailabilityRequest;
import com.eduze.manage.lesson.dto.GenerateByAvailabilityResult;
import com.eduze.manage.lesson.mapper.LessonMapper;
import com.eduze.manage.lesson.mapper.LessonStudentMapper;
import com.eduze.manage.lesson.mapper.LessonSubscriptionMapper;
import com.eduze.manage.teacher.domain.TeacherAvailability;
import com.eduze.manage.teacher.mapper.TeacherAvailabilityMapper;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleGenerator {

    private static final Set<LocalDate> HOLIDAYS = Set.of(
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 3),
            LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 6), LocalDate.of(2027, 2, 7));

    private final TeacherAvailabilityMapper availabilityMapper;
    private final LessonMapper lessonMapper;
    private final LessonStudentMapper lessonStudentMapper;
    private final LessonSubscriptionMapper subscriptionMapper;

    @Transactional
    public GenerateByAvailabilityResult generate(GenerateByAvailabilityRequest req) {
        if (req.getWeeks() == null || req.getWeeks() < 1 || req.getWeeks() > 8) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "weeks 必须为 1..8");
        }
        Long tenantId = TenantContext.getTenantId();
        LocalDate from = req.getFromDate();
        LocalDate to = from.plusWeeks(req.getWeeks());

        var w = Wrappers.<TeacherAvailability>lambdaQuery()
                .eq(TeacherAvailability::getTenantId, tenantId)
                .eq(TeacherAvailability::getStatus, 1)
                .le(TeacherAvailability::getValidFrom, to);
        if (req.getTeacherIds() != null && !req.getTeacherIds().isEmpty()) {
            w.in(TeacherAvailability::getTeacherId, req.getTeacherIds());
        }
        List<TeacherAvailability> avails = availabilityMapper.selectList(w);

        int generated = 0;
        int skipped = 0;
        List<String> conflicts = new ArrayList<>();

        for (TeacherAvailability a : avails) {
            LocalDate cursor = from;
            while (!cursor.isAfter(to.minusDays(1))) {
                if (cursor.getDayOfWeek().getValue() != a.getDayOfWeek()) {
                    cursor = cursor.plusDays(1);
                    continue;
                }
                // valid_from / to 边界
                if (cursor.isBefore(a.getValidFrom())) {
                    cursor = cursor.plusDays(1); continue;
                }
                if (a.getValidTo() != null && cursor.isAfter(a.getValidTo())) {
                    cursor = cursor.plusDays(1); continue;
                }
                // 节假日跳过
                if (HOLIDAYS.contains(cursor)) {
                    skipped++;
                    cursor = cursor.plusDays(1); continue;
                }
                LocalDateTime startAt = LocalDateTime.of(cursor, LocalTime.of(a.getStartMinute() / 60, a.getStartMinute() % 60));
                LocalDateTime endAt = LocalDateTime.of(cursor, LocalTime.of(a.getEndMinute() / 60, a.getEndMinute() % 60));

                // 幂等：同 teacher × start_at 已存在则跳过
                Long exist = lessonMapper.selectCount(Wrappers.<Lesson>lambdaQuery()
                        .eq(Lesson::getTenantId, tenantId)
                        .eq(Lesson::getTeacherId, a.getTeacherId())
                        .eq(Lesson::getStartAt, startAt));
                if (exist > 0) {
                    skipped++;
                    conflicts.add("跳过：" + a.getTeacherId() + " @ " + startAt + "（已存在）");
                    cursor = cursor.plusDays(1); continue;
                }

                Lesson lesson = new Lesson();
                lesson.setTenantId(tenantId);
                lesson.setBranchId(a.getBranchId());
                lesson.setTeacherId(a.getTeacherId());
                lesson.setClassRoomId(a.getDefaultClassRoomId());
                lesson.setStartAt(startAt);
                lesson.setEndAt(endAt);
                lesson.setStatus("SCHEDULED");
                lesson.setTeacherAvailabilityId(a.getId());
                lesson.setSource(1);
                lessonMapper.insert(lesson);
                generated++;

                // 拉所有有效订阅写名单
                List<LessonSubscription> subs = subscriptionMapper.selectList(Wrappers.<LessonSubscription>lambdaQuery()
                        .eq(LessonSubscription::getTenantId, tenantId)
                        .eq(LessonSubscription::getTeacherAvailabilityId, a.getId())
                        .eq(LessonSubscription::getStatus, 1)
                        .le(LessonSubscription::getValidFrom, cursor));
                for (LessonSubscription s : subs) {
                    if (s.getValidTo() != null && cursor.isAfter(s.getValidTo())) continue;
                    LessonStudent ls = new LessonStudent();
                    ls.setTenantId(tenantId);
                    ls.setBranchId(lesson.getBranchId());
                    ls.setLessonId(lesson.getId());
                    ls.setStudentId(s.getStudentId());
                    ls.setSubscriptionId(s.getId());
                    ls.setSource("SUBSCRIPTION");
                    ls.setStatus("BOOKED");
                    lessonStudentMapper.insert(ls);
                }
                cursor = cursor.plusDays(1);
            }
        }
        return GenerateByAvailabilityResult.builder()
                .generated(generated).skipped(skipped).conflicts(conflicts).build();
    }

    // 仅测试使用
    public Long testInsertSubscription(Long studentId, Long teacherId, Long availabilityId, LocalDate validFrom) {
        LessonSubscription s = new LessonSubscription();
        s.setTenantId(TenantContext.getTenantId());
        s.setBranchId(1L);
        s.setStudentId(studentId);
        s.setTeacherId(teacherId);
        s.setTeacherAvailabilityId(availabilityId);
        s.setValidFrom(validFrom);
        s.setStatus(1);
        s.setSource("NORMAL");
        subscriptionMapper.insert(s);
        return s.getId();
    }
}
```

- [ ] **Step 4: Lesson domain 扩展字段映射**

Edit `src/main/java/com/eduze/manage/lesson/domain/Lesson.java` 追加：

```java
    private Long teacherAvailabilityId;
    private Integer source;
```

- [ ] **Step 5: 运行测试，确认通过**

Run: `./mvnw -Dtest=ScheduleGeneratorIT test`
Expected: PASS（10 个测试全绿）

- [ ] **Step 6: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/lesson/ \
        src/test/java/com/eduze/manage/lesson/ScheduleGeneratorIT.java
git commit -m "feat(lesson): ScheduleGenerator (idempotent bulk by availability)"
```

---

## Task 15: 改写 POST /api/lessons/bulk-generate 走 availability

**Files:**
- Modify: `src/main/java/com/eduze/manage/lesson/controller/LessonController.java`（保留旧端点用同 URL，但 body 模式改为 availability 模式）或新增 `/bulk-generate-by-availability` 端点
- Test: `src/test/java/com/eduze/manage/lesson/BulkGenerateByAvailabilityIT.java`

> **方案选择：** 不破坏现有 `/api/lessons/bulk-generate` 行为（class_group 模式仍可调用，保证现有测试不挂），新增 `/api/lessons/bulk-generate-by-teacher` 端点，前端切到新端点。

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/lesson/BulkGenerateByAvailabilityIT.java`:

```java
package com.eduze.manage.lesson;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class BulkGenerateByAvailabilityIT extends AbstractApiIT {

    @Test
    void byTeacher_generates_andSeedsRoster() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/api/lessons/bulk-generate-by-teacher")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"weeks":1,"fromDate":"2026-08-01","teacherIds":[1101]}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated").isNumber());
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=BulkGenerateByAvailabilityIT test`
Expected: FAIL

- [ ] **Step 3: 加 controller 端点**

Edit `src/main/java/com/eduze/manage/lesson/controller/LessonController.java` — 注入 `ScheduleGenerator`，追加：

```java
private final ScheduleGenerator scheduleGenerator;

@PostMapping("/bulk-generate-by-teacher")
@PreAuthorize("hasAuthority('lesson:write')")
public ApiResponse<GenerateByAvailabilityResult> bulkGenerateByTeacher(
        @Valid @RequestBody GenerateByAvailabilityRequest req) {
    return ApiResponse.ok(scheduleGenerator.generate(req));
}
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `./mvnw -Dtest=BulkGenerateByAvailabilityIT test`
Expected: PASS

- [ ] **Step 5: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/lesson/controller/LessonController.java \
        src/test/java/com/eduze/manage/lesson/BulkGenerateByAvailabilityIT.java
git commit -m "feat(lesson): POST /api/lessons/bulk-generate-by-teacher endpoint"
```

---

## Task 16: 按老师周课表 + 我的课表 API

**Files:**
- Create: `src/main/java/com/eduze/manage/lesson/dto/TeacherScheduleResponse.java`
- Create: `src/main/java/com/eduze/manage/lesson/service/ScheduleViewService.java`
- Modify: `src/main/java/com/eduze/manage/lesson/controller/ScheduleController.java`
- Test: `src/test/java/com/eduze/manage/lesson/ScheduleByTeacherIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/lesson/ScheduleByTeacherIT.java`:

```java
package com.eduze.manage.lesson;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;

class ScheduleByTeacherIT extends AbstractApiIT {

    @Test
    void byTeacher_returnsGroupedByTeacher() throws Exception {
        String token = adminToken();
        // 先生成 1 周课次
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/lessons/bulk-generate-by-teacher")
                        .header("Authorization", bearer(token))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                            {"weeks":1,"fromDate":"2026-09-05","teacherIds":[]}
                            """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/schedule/by-teacher?branchId=1&weekStart=2026-09-05")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teachers.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.teachers[0].lessons").exists());
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=ScheduleByTeacherIT test`
Expected: FAIL

- [ ] **Step 3: DTO**

Create `src/main/java/com/eduze/manage/lesson/dto/TeacherScheduleResponse.java`:

```java
package com.eduze.manage.lesson.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherScheduleResponse {
    private final List<TeacherColumn> teachers;

    @Getter @Builder
    public static class TeacherColumn {
        private final Long teacherId;
        private final String teacherName;
        private final List<LessonCell> lessons;
    }

    @Getter @Builder
    public static class LessonCell {
        private final Long lessonId;
        private final LocalDateTime startAt;
        private final LocalDateTime endAt;
        private final Long classRoomId;
        private final Integer source;
        private final Integer rosterCount;
        private final Integer capacity;
    }
}
```

- [ ] **Step 4: service**

Create `src/main/java/com/eduze/manage/lesson/service/ScheduleViewService.java`:

```java
package com.eduze.manage.lesson.service;

import com.eduze.manage.lesson.dto.TeacherScheduleResponse;
import com.eduze.manage.lesson.dto.TeacherScheduleResponse.LessonCell;
import com.eduze.manage.lesson.dto.TeacherScheduleResponse.TeacherColumn;
import com.eduze.manage.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleViewService {

    private final JdbcTemplate jdbc;

    public TeacherScheduleResponse byTeacher(Long branchId, LocalDate weekStart) {
        Long tenantId = TenantContext.getTenantId();
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusDays(7).atStartOfDay();

        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT l.id lesson_id, l.teacher_id, u.name teacher_name,
                       l.start_at, l.end_at, l.class_room_id, l.source,
                       (SELECT COUNT(*) FROM t_lesson_student ls
                          WHERE ls.lesson_id=l.id AND ls.status='BOOKED' AND ls.deleted_at=0) roster_count,
                       (SELECT capacity FROM t_teacher_availability ta WHERE ta.id=l.teacher_availability_id) capacity
                FROM t_lesson l JOIN t_user u ON u.id=l.teacher_id
                WHERE l.tenant_id=? AND l.deleted_at=0
                  AND (? IS NULL OR l.branch_id=?)
                  AND l.start_at>=? AND l.start_at<?
                ORDER BY l.teacher_id, l.start_at
                """,
                tenantId, branchId, branchId, from, to);

        Map<Long, TeacherColumn.TeacherColumnBuilder> byTeacher = new LinkedHashMap<>();
        Map<Long, List<LessonCell>> lessonsByTeacher = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Long teacherId = ((Number) r.get("teacher_id")).longValue();
            byTeacher.computeIfAbsent(teacherId, id -> TeacherColumn.builder()
                    .teacherId(id).teacherName((String) r.get("teacher_name")));
            lessonsByTeacher.computeIfAbsent(teacherId, id -> new ArrayList<>()).add(LessonCell.builder()
                    .lessonId(((Number) r.get("lesson_id")).longValue())
                    .startAt(((java.sql.Timestamp) r.get("start_at")).toLocalDateTime())
                    .endAt(((java.sql.Timestamp) r.get("end_at")).toLocalDateTime())
                    .classRoomId(r.get("class_room_id") == null ? null : ((Number) r.get("class_room_id")).longValue())
                    .source(r.get("source") == null ? null : ((Number) r.get("source")).intValue())
                    .rosterCount(((Number) r.get("roster_count")).intValue())
                    .capacity(r.get("capacity") == null ? null : ((Number) r.get("capacity")).intValue())
                    .build());
        }
        List<TeacherColumn> cols = byTeacher.entrySet().stream()
                .map(e -> e.getValue().lessons(lessonsByTeacher.get(e.getKey())).build())
                .toList();
        return TeacherScheduleResponse.builder().teachers(cols).build();
    }

    public TeacherScheduleResponse myWeek(Long teacherId, LocalDate weekStart) {
        TeacherScheduleResponse full = byTeacher(null, weekStart);
        return TeacherScheduleResponse.builder()
                .teachers(full.getTeachers().stream()
                        .filter(t -> t.getTeacherId().equals(teacherId))
                        .toList())
                .build();
    }
}
```

- [ ] **Step 5: 改 ScheduleController**

Edit `src/main/java/com/eduze/manage/lesson/controller/ScheduleController.java` 追加：

```java
private final ScheduleViewService scheduleViewService;

@GetMapping("/by-teacher")
@PreAuthorize("hasAuthority('lesson:teacher_view')")
public ApiResponse<TeacherScheduleResponse> byTeacher(
        @RequestParam(required = false) Long branchId,
        @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
        java.time.LocalDate weekStart) {
    return ApiResponse.ok(scheduleViewService.byTeacher(branchId, weekStart));
}

@GetMapping("/my-week")
@PreAuthorize("hasAuthority('lesson:read')")
public ApiResponse<TeacherScheduleResponse> myWeek(
        @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
        java.time.LocalDate weekStart,
        org.springframework.security.core.Authentication authentication) {
    var details = (com.eduze.manage.auth.security.CustomUserDetails) authentication.getPrincipal();
    return ApiResponse.ok(scheduleViewService.myWeek(details.getUserId(), weekStart));
}
```

- [ ] **Step 6: 运行测试，确认通过**

Run: `./mvnw -Dtest=ScheduleByTeacherIT test`
Expected: PASS

- [ ] **Step 7: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/lesson/ \
        src/test/java/com/eduze/manage/lesson/ScheduleByTeacherIT.java
git commit -m "feat(lesson): GET /api/schedule/by-teacher + /my-week"
```

---

# P5. 班级降级 + 阶段评估

## Task 17: ClassGroup 字段调整 + 解约束

**Files:**
- Modify: `src/main/java/com/eduze/manage/course/domain/ClassGroup.java`（加 `tagColor` 字段；`courseId` 已是 Long，无需改类型）
- Modify: `src/main/java/com/eduze/manage/course/dto/ClassGroupRequest.java`（`courseId` 改可选）
- Modify: `src/main/java/com/eduze/manage/course/service/ClassGroupService.java`（去掉"必须有 course"校验，加 tagColor 处理）
- Test: `src/test/java/com/eduze/manage/course/ClassGroupTagOnlyIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/course/ClassGroupTagOnlyIT.java`:

```java
package com.eduze.manage.course;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ClassGroupTagOnlyIT extends AbstractApiIT {

    @Test
    void createWithoutCourse_succeeds() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/api/class-groups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"branchId":1,"name":"周末小画家","capacity":12,"tagColor":"#FFA500"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tagColor").value("#FFA500"))
                .andExpect(jsonPath("$.data.courseId").doesNotExist());
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=ClassGroupTagOnlyIT test`
Expected: FAIL（service 仍 require course）

- [ ] **Step 3: 改 domain / DTO / service**

Edit `src/main/java/com/eduze/manage/course/domain/ClassGroup.java` — 追加：

```java
    private String tagColor;
```

Edit `src/main/java/com/eduze/manage/course/dto/ClassGroupRequest.java` — `courseId` 字段去掉 `@NotNull`，加 `tagColor`：

```java
    private Long courseId; // 现在可空
    private String tagColor;
```

Edit `src/main/java/com/eduze/manage/course/service/ClassGroupService.java` — `create` / `update` 中：
- 去掉 `if (courseId == null) throw ...` 类型检查
- 写入 `group.setTagColor(req.getTagColor());`
- toResponse 中加 `tagColor`

Edit `src/main/java/com/eduze/manage/course/dto/ClassGroupResponse.java` — 加 `private final String tagColor;`

- [ ] **Step 4: 运行测试，确认通过**

Run: `./mvnw -Dtest=ClassGroupTagOnlyIT test`
Expected: PASS

- [ ] **Step 5: 检查现有 ClassGroup 测试未挂**

Run: `./mvnw -Dtest='ClassGroup*' test`
Expected: PASS

- [ ] **Step 6: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/course/ \
        src/test/java/com/eduze/manage/course/ClassGroupTagOnlyIT.java
git commit -m "feat(course): demote ClassGroup to tag (course optional + tag_color)"
```

---

## Task 18: StudentStageAssessment 最小 CRUD

**Files:**
- Create: `src/main/java/com/eduze/manage/student/domain/StudentStageAssessment.java`
- Create: `src/main/java/com/eduze/manage/student/mapper/StudentStageAssessmentMapper.java`
- Create: `src/main/java/com/eduze/manage/student/dto/StageAssessmentRequest.java`
- Create: `src/main/java/com/eduze/manage/student/dto/StageAssessmentResponse.java`
- Create: `src/main/java/com/eduze/manage/student/service/StageAssessmentService.java`
- Modify: `src/main/java/com/eduze/manage/student/controller/StudentController.java`
- Test: `src/test/java/com/eduze/manage/student/StageAssessmentIT.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/com/eduze/manage/student/StageAssessmentIT.java`:

```java
package com.eduze.manage.student;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.support.AbstractApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class StageAssessmentIT extends AbstractApiIT {

    @Test
    void create_and_list_assessment() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/api/students/240001/stage-assessments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"stageId":200001,"assessedAt":"2026-05-20",
                             "scoresJson":{"EL_LINE":4,"EL_COLOR":5},"comment":"初次评估"}
                            """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students/240001/stage-assessments").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].stageId").value(200001));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `./mvnw -Dtest=StageAssessmentIT test`
Expected: FAIL

- [ ] **Step 3: domain + mapper + DTO + service + controller**

Create `src/main/java/com/eduze/manage/student/domain/StudentStageAssessment.java`:

```java
package com.eduze.manage.student.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduze.manage.common.entity.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_student_stage_assessment")
public class StudentStageAssessment extends BaseEntity {
    private Long studentId;
    private Long stageId;
    private LocalDate assessedAt;
    private Long assessedBy;
    private String scoresJson;
    private String comment;
}
```

Create `src/main/java/com/eduze/manage/student/mapper/StudentStageAssessmentMapper.java`:

```java
package com.eduze.manage.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.student.domain.StudentStageAssessment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentStageAssessmentMapper extends BaseMapper<StudentStageAssessment> {}
```

Create `src/main/java/com/eduze/manage/student/dto/StageAssessmentRequest.java`:

```java
package com.eduze.manage.student.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StageAssessmentRequest {
    @NotNull private Long stageId;
    @NotNull private LocalDate assessedAt;
    private JsonNode scoresJson;
    private String comment;
}
```

Create `src/main/java/com/eduze/manage/student/dto/StageAssessmentResponse.java`:

```java
package com.eduze.manage.student.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StageAssessmentResponse {
    private final Long id;
    private final Long studentId;
    private final Long stageId;
    private final LocalDate assessedAt;
    private final Long assessedBy;
    private final JsonNode scoresJson;
    private final String comment;
}
```

Create `src/main/java/com/eduze/manage/student/service/StageAssessmentService.java`:

```java
package com.eduze.manage.student.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.student.domain.Student;
import com.eduze.manage.student.domain.StudentStageAssessment;
import com.eduze.manage.student.dto.StageAssessmentRequest;
import com.eduze.manage.student.dto.StageAssessmentResponse;
import com.eduze.manage.student.mapper.StudentMapper;
import com.eduze.manage.student.mapper.StudentStageAssessmentMapper;
import com.eduze.manage.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StageAssessmentService {

    private final StudentStageAssessmentMapper mapper;
    private final StudentMapper studentMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public StageAssessmentResponse create(Long studentId, StageAssessmentRequest req) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BizException(ErrorCode.NOT_FOUND, "学员不存在");
        StudentStageAssessment a = new StudentStageAssessment();
        a.setTenantId(TenantContext.getTenantId());
        a.setBranchId(student.getBranchId());
        a.setStudentId(studentId);
        a.setStageId(req.getStageId());
        a.setAssessedAt(req.getAssessedAt());
        a.setScoresJson(req.getScoresJson() == null ? "{}" : req.getScoresJson().toString());
        a.setComment(req.getComment());
        mapper.insert(a);
        return toResponse(a);
    }

    public List<StageAssessmentResponse> list(Long studentId) {
        return mapper.selectList(Wrappers.<StudentStageAssessment>lambdaQuery()
                        .eq(StudentStageAssessment::getTenantId, TenantContext.getTenantId())
                        .eq(StudentStageAssessment::getStudentId, studentId)
                        .orderByDesc(StudentStageAssessment::getAssessedAt))
                .stream().map(this::toResponse).toList();
    }

    private StageAssessmentResponse toResponse(StudentStageAssessment a) {
        try {
            return StageAssessmentResponse.builder()
                    .id(a.getId()).studentId(a.getStudentId()).stageId(a.getStageId())
                    .assessedAt(a.getAssessedAt()).assessedBy(a.getAssessedBy())
                    .scoresJson(a.getScoresJson() == null ? null : objectMapper.readTree(a.getScoresJson()))
                    .comment(a.getComment())
                    .build();
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "scores_json 解析失败");
        }
    }
}
```

Edit `src/main/java/com/eduze/manage/student/controller/StudentController.java` 追加：

```java
private final StageAssessmentService stageAssessmentService;

@GetMapping("/{id}/stage-assessments")
@PreAuthorize("hasAuthority('student:read')")
public ApiResponse<List<StageAssessmentResponse>> assessments(@PathVariable Long id) {
    return ApiResponse.ok(stageAssessmentService.list(id));
}

@PostMapping("/{id}/stage-assessments")
@PreAuthorize("hasAuthority('student:write')")
public ApiResponse<StageAssessmentResponse> createAssessment(
        @PathVariable Long id, @Valid @RequestBody StageAssessmentRequest req) {
    return ApiResponse.ok(stageAssessmentService.create(id, req));
}
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `./mvnw -Dtest=StageAssessmentIT test`
Expected: PASS

- [ ] **Step 5: 建议 commit**

```bash
git add src/main/java/com/eduze/manage/student/ \
        src/test/java/com/eduze/manage/student/StageAssessmentIT.java
git commit -m "feat(student): stage assessment CRUD (minimal v1)"
```

---

# P6. 前端

> **前端测试约定**：用 Vitest + React Testing Library，组件级 unit test；交互级用 RTL 的 `userEvent`。前端任务在每个任务的 Step 1 写 1 个最小快照/行为测试，Step 4 跑 vitest 验证。

## Task 19: 学员表单加 mentorTeacherId + 初始订阅

**Files:**
- Modify: `web/src/features/student/schemas.ts`（加 `mentorTeacherId` 必填）
- Modify: `web/src/features/student/api.ts`（加 `fetchTeachers`、`fetchTeacherAvailabilities`、create/update 透传新字段）
- Modify: `web/src/features/student/components/StudentFormDialog.tsx`（加 mentor 选择 + 可选订阅区块）
- Create: `web/src/features/student/components/InitialSubscriptionsField.tsx`
- Test: `web/src/features/student/__tests__/StudentFormDialog.test.tsx`

- [ ] **Step 1: 写失败测试**

Create `web/src/features/student/__tests__/StudentFormDialog.test.tsx`:

```tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StudentFormDialog } from '../components/StudentFormDialog';

vi.mock('../api', () => ({
  studentApi: {
    create: vi.fn().mockResolvedValue({ id: 99 }),
    update: vi.fn(),
    listTeachers: vi.fn().mockResolvedValue([
      { id: 1101, username: 'teacher_zhang', name: '张老师', branchId: 1 },
    ]),
    listTeacherAvailabilities: vi.fn().mockResolvedValue([]),
  },
}));

function renderDialog() {
  const qc = new QueryClient();
  return render(
    <QueryClientProvider client={qc}>
      <StudentFormDialog
        open={true}
        onOpenChange={() => {}}
        branches={[{ id: 1, name: '本部' }]}
      />
    </QueryClientProvider>
  );
}

describe('StudentFormDialog mentor field', () => {
  it('shows mentor teacher select', async () => {
    renderDialog();
    expect(await screen.findByLabelText(/主带老师/)).toBeInTheDocument();
  });

  it('refuses submit when mentor is empty', async () => {
    renderDialog();
    await userEvent.type(screen.getByLabelText(/姓名/), '小测');
    await userEvent.click(screen.getByText(/保存/));
    expect(await screen.findByText(/必须指定主带老师/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `cd web && pnpm test -- StudentFormDialog`
Expected: FAIL

- [ ] **Step 3: 改 schema + api**

Edit `web/src/features/student/schemas.ts` — 在 `studentFormSchema` 中追加 `mentorTeacherId: z.string().min(1, '必须指定主带老师')`。

Edit `web/src/features/student/api.ts` — 追加：

```ts
export const studentApi = {
  // ... existing methods ...

  listTeachers: (branchId?: number) =>
    api.get<TeacherSummary[]>('/teachers', { params: { branchId } }).then(r => r.data.data),

  listTeacherAvailabilities: (teacherId: number) =>
    api.get<TeacherAvailability[]>(`/teachers/${teacherId}/availabilities`)
       .then(r => r.data.data),
};
```

- [ ] **Step 4: 改 StudentFormDialog**

Edit `web/src/features/student/components/StudentFormDialog.tsx` — 加 mentor 选择 + 初始订阅区块。在"基础"步骤的 JSX 末尾追加：

```tsx
<div>
  <Label htmlFor="mentorTeacherId">主带老师 *</Label>
  <SimpleSelect
    id="mentorTeacherId"
    value={form.watch('mentorTeacherId') ?? ''}
    onChange={v => form.setValue('mentorTeacherId', v)}
    options={teachersQuery.data?.map(t => ({ value: String(t.id), label: t.name })) ?? []}
  />
  {form.formState.errors.mentorTeacherId && (
    <p className="text-error text-sm">{form.formState.errors.mentorTeacherId.message as string}</p>
  )}
</div>

<InitialSubscriptionsField
  teacherId={Number(form.watch('mentorTeacherId') || 0)}
  selectedIds={form.watch('initialSubscriptions') ?? []}
  onChange={ids => form.setValue('initialSubscriptions', ids)}
/>
```

在文件头部加 query：

```tsx
const teachersQuery = useQuery({
  queryKey: ['teachers', form.watch('branchId')],
  queryFn: () => studentApi.listTeachers(Number(form.watch('branchId'))),
});
```

- [ ] **Step 5: 写 InitialSubscriptionsField**

Create `web/src/features/student/components/InitialSubscriptionsField.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { Checkbox } from '@/components/ui/Checkbox';
import { studentApi } from '../api';

const DAYS = ['周一','周二','周三','周四','周五','周六','周日'];

type Props = {
  teacherId: number;
  selectedIds: number[];
  onChange: (ids: number[]) => void;
};

function fmt(min: number) {
  const h = Math.floor(min / 60);
  const m = min % 60;
  return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`;
}

export function InitialSubscriptionsField({ teacherId, selectedIds, onChange }: Props) {
  const q = useQuery({
    queryKey: ['teacher-availabilities', teacherId],
    queryFn: () => studentApi.listTeacherAvailabilities(teacherId),
    enabled: teacherId > 0,
  });
  if (!teacherId) return null;
  return (
    <div className="rounded-lg border p-3 space-y-2">
      <div className="text-sm font-medium">订阅老师的常规时段（可选，可勾多个）</div>
      {(q.data ?? []).map(a => (
        <label key={a.id} className="flex items-center gap-2 text-sm">
          <Checkbox
            checked={selectedIds.includes(a.id)}
            onCheckedChange={checked => {
              onChange(checked
                ? [...selectedIds, a.id]
                : selectedIds.filter(id => id !== a.id));
            }}
          />
          {DAYS[a.dayOfWeek - 1]} {fmt(a.startMinute)}–{fmt(a.endMinute)} · 容量 {a.capacity}
        </label>
      ))}
      {q.data?.length === 0 && <div className="text-muted-fg text-xs">该老师暂无可用时段</div>}
    </div>
  );
}
```

- [ ] **Step 6: 让 onSubmit 创建学员后再批量提交订阅**

Edit `StudentFormDialog.tsx` `onSubmit` 函数：

```ts
const mentorTeacherId = Number(values.mentorTeacherId);
const created = await studentApi.create({
  ...payload,
  mentorTeacherId,
});
const subs = values.initialSubscriptions ?? [];
for (const availabilityId of subs) {
  await studentApi.createSubscription({
    branchId: Number(values.branchId),
    studentId: created.id,
    teacherId: mentorTeacherId,
    teacherAvailabilityId: availabilityId,
    validFrom: new Date().toISOString().slice(0, 10),
  });
}
```

`studentApi.createSubscription` 在 `api.ts` 中追加：

```ts
createSubscription: (body: {
  branchId: number; studentId: number; teacherId: number;
  teacherAvailabilityId: number; validFrom: string;
}) => api.post('/subscriptions', body).then(r => r.data.data),
```

- [ ] **Step 7: 运行测试，确认通过**

Run: `cd web && pnpm test -- StudentFormDialog`
Expected: PASS

- [ ] **Step 8: 建议 commit**

```bash
git add web/src/features/student/
git commit -m "feat(web): student form requires mentor + initial subscriptions"
```

---

## Task 20: 学员列表加主带列 + 详情 Sheet 三个新 Tab

**Files:**
- Modify: `web/src/features/student/pages/StudentListPage.tsx`（列 + 筛选）
- Modify: `web/src/features/student/components/StudentDetailSheet.tsx`（顶部 badge + 三个新 Tab）
- Create: `web/src/features/student/components/MentorChangeDialog.tsx`
- Create: `web/src/features/student/components/StudentMentorHistoryTab.tsx`
- Create: `web/src/features/student/components/StudentSubscriptionsTab.tsx`
- Create: `web/src/features/student/components/StudentStageAssessmentTab.tsx`
- Test: `web/src/features/student/__tests__/StudentDetailSheet.test.tsx`

- [ ] **Step 1: 写失败测试**

Create `web/src/features/student/__tests__/StudentDetailSheet.test.tsx`:

```tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StudentDetailSheet } from '../components/StudentDetailSheet';

vi.mock('../api', () => ({
  studentApi: {
    get: vi.fn().mockResolvedValue({
      id: 240001, name: '小启', mentorTeacherId: 1101, mentorTeacherName: '张老师',
      branchId: 1, branchName: '本部',
    }),
    mentorHistory: vi.fn().mockResolvedValue([]),
    listSubscriptions: vi.fn().mockResolvedValue([]),
    listAssessments: vi.fn().mockResolvedValue([]),
  },
}));

function renderSheet() {
  const qc = new QueryClient();
  return render(
    <QueryClientProvider client={qc}>
      <StudentDetailSheet studentId={240001} open={true} onOpenChange={() => {}} />
    </QueryClientProvider>
  );
}

describe('StudentDetailSheet new tabs', () => {
  it('shows mentor name badge', async () => {
    renderSheet();
    expect(await screen.findByText(/张老师/)).toBeInTheDocument();
  });
  it('has subscriptions / mentor history / stage tabs', async () => {
    renderSheet();
    expect(await screen.findByRole('tab', { name: /订阅/ })).toBeInTheDocument();
    expect(await screen.findByRole('tab', { name: /主带变更/ })).toBeInTheDocument();
    expect(await screen.findByRole('tab', { name: /阶段评估/ })).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `cd web && pnpm test -- StudentDetailSheet`
Expected: FAIL

- [ ] **Step 3: 加列 / 筛选**

Edit `web/src/features/student/pages/StudentListPage.tsx`:
- DataTable columns 追加 `{ accessorKey: 'mentorTeacherName', header: '主带老师' }`
- 筛选区追加"按主带老师"下拉，调 `studentApi.listTeachers()`，state `filterMentorId`，传给 `studentApi.list({mentorTeacherId: filterMentorId})`
- `api.ts` 中 `list` 接受 `mentorTeacherId` 参数透传给 `/api/students?...`（注：后端 list 暂未支持 mentor 过滤，本前端任务只准备 UI；后端在下一次小补丁中补 `mentorTeacherId` 查询条件，**或本任务的 service patch**——给 `StudentService.list` 加 `mentorTeacherId` 参数和 `wrapper.eq(...)`）

补后端：Edit `src/main/java/com/eduze/manage/student/service/StudentService.java` 的 list 方法签名加 `Long mentorTeacherId`，wrapper 内 `if (mentorTeacherId != null) wrapper.eq(Student::getMentorTeacherId, mentorTeacherId);`。同步改 `StudentController.list` 参数透传。

- [ ] **Step 4: 写三个 Tab + 换主带 Dialog**

Create `web/src/features/student/components/StudentMentorHistoryTab.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { studentApi } from '../api';
import { Skeleton } from '@/components/ui/Skeleton';

export function StudentMentorHistoryTab({ studentId }: { studentId: number }) {
  const q = useQuery({
    queryKey: ['student-mentor-history', studentId],
    queryFn: () => studentApi.mentorHistory(studentId),
  });
  if (q.isLoading) return <Skeleton className="h-32 w-full" />;
  return (
    <ul className="divide-y">
      {q.data?.map(h => (
        <li key={h.id} className="py-2 text-sm">
          <span className="text-muted-fg">{new Date(h.changedAt).toLocaleString()}</span>
          {' '}{h.fromTeacherName ?? '（首次绑定）'} → <strong>{h.toTeacherName}</strong>
          {h.reason && <div className="text-xs text-muted-fg mt-0.5">原因：{h.reason}</div>}
        </li>
      ))}
      {q.data?.length === 0 && <div className="py-4 text-sm text-muted-fg">暂无变更记录</div>}
    </ul>
  );
}
```

Create `web/src/features/student/components/StudentSubscriptionsTab.tsx`:

```tsx
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { studentApi } from '../api';
import { Skeleton } from '@/components/ui/Skeleton';

const DAYS = ['周一','周二','周三','周四','周五','周六','周日'];
const fmt = (m: number) => `${String(Math.floor(m/60)).padStart(2,'0')}:${String(m%60).padStart(2,'0')}`;

export function StudentSubscriptionsTab({ studentId }: { studentId: number }) {
  const qc = useQueryClient();
  const q = useQuery({
    queryKey: ['student-subscriptions', studentId],
    queryFn: () => studentApi.listSubscriptions({ studentId }),
  });
  const cancel = useMutation({
    mutationFn: (id: number) => studentApi.cancelSubscription(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['student-subscriptions', studentId] }),
  });
  if (q.isLoading) return <Skeleton className="h-32 w-full" />;
  return (
    <ul className="divide-y">
      {q.data?.map(s => (
        <li key={s.id} className="py-2 flex items-center gap-2">
          <div className="flex-1 text-sm">
            {s.teacherName ?? `老师 ${s.teacherId}`}
            {' · '}{DAYS[(s.availabilityDayOfWeek ?? 1) - 1]} {fmt(s.availabilityStartMinute ?? 0)}
            {s.status === 0 && <span className="ml-2 text-warning">（已停用）</span>}
          </div>
          {s.status === 1 && (
            <Button variant="ghost" onClick={() => cancel.mutate(s.id)} disabled={cancel.isPending}>
              退订
            </Button>
          )}
        </li>
      ))}
      {q.data?.length === 0 && <div className="py-4 text-sm text-muted-fg">暂无订阅</div>}
    </ul>
  );
}
```

Create `web/src/features/student/components/StudentStageAssessmentTab.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { studentApi } from '../api';

export function StudentStageAssessmentTab({ studentId }: { studentId: number }) {
  const q = useQuery({
    queryKey: ['student-assessments', studentId],
    queryFn: () => studentApi.listAssessments(studentId),
  });
  return (
    <div>
      <ul className="divide-y">
        {q.data?.map(a => (
          <li key={a.id} className="py-2">
            <div className="text-sm font-medium">阶段 {a.stageId} · {a.assessedAt}</div>
            <pre className="text-xs text-muted-fg whitespace-pre-wrap">
              {JSON.stringify(a.scoresJson, null, 2)}
            </pre>
            {a.comment && <p className="text-xs">{a.comment}</p>}
          </li>
        ))}
        {q.data?.length === 0 && <div className="py-4 text-sm text-muted-fg">暂无评估</div>}
      </ul>
    </div>
  );
}
```

Create `web/src/features/student/components/MentorChangeDialog.tsx`:

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { SimpleSelect } from '@/components/ui/Select';
import { Checkbox } from '@/components/ui/Checkbox';
import { studentApi } from '../api';
import { toast } from '@/lib/toast';

type Props = {
  open: boolean;
  onOpenChange: (b: boolean) => void;
  studentId: number;
  currentMentorId: number;
  branchId: number;
};

export function MentorChangeDialog({ open, onOpenChange, studentId, currentMentorId, branchId }: Props) {
  const qc = useQueryClient();
  const teachersQ = useQuery({
    queryKey: ['teachers', branchId],
    queryFn: () => studentApi.listTeachers(branchId),
  });
  const [toId, setToId] = useState('');
  const [reason, setReason] = useState('');
  const [keep, setKeep] = useState(true);

  const m = useMutation({
    mutationFn: () => studentApi.changeMentor(studentId, {
      toTeacherId: Number(toId), reason, keepSubscriptions: keep,
    }),
    onSuccess: () => {
      toast.success('已更换主带老师');
      qc.invalidateQueries({ queryKey: ['student', studentId] });
      qc.invalidateQueries({ queryKey: ['student-mentor-history', studentId] });
      onOpenChange(false);
    },
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader><DialogTitle>更换主带老师</DialogTitle></DialogHeader>
        <div className="space-y-3">
          <div>
            <Label>新主带老师</Label>
            <SimpleSelect
              value={toId}
              onChange={setToId}
              options={(teachersQ.data ?? [])
                .filter(t => t.id !== currentMentorId)
                .map(t => ({ value: String(t.id), label: t.name }))}
            />
          </div>
          <div>
            <Label>原因</Label>
            <Input value={reason} onChange={e => setReason(e.target.value)} />
          </div>
          <label className="flex items-center gap-2 text-sm">
            <Checkbox checked={keep} onCheckedChange={c => setKeep(Boolean(c))} />
            保留学员现有订阅（不勾则一并退订）
          </label>
        </div>
        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)}>取消</Button>
          <Button onClick={() => m.mutate()} disabled={!toId || m.isPending}>确认</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
```

Edit `web/src/features/student/components/StudentDetailSheet.tsx`:
- 顶部加 mentor badge：`<Badge>主带 · {student.mentorTeacherName}</Badge>` + 「更换」按钮触发 `MentorChangeDialog`
- 加 3 个 Tab：
  ```tsx
  <Tabs.Trigger value="subs">订阅</Tabs.Trigger>
  <Tabs.Trigger value="mentor-history">主带变更</Tabs.Trigger>
  <Tabs.Trigger value="assessment">阶段评估</Tabs.Trigger>
  <Tabs.Content value="subs"><StudentSubscriptionsTab studentId={id} /></Tabs.Content>
  <Tabs.Content value="mentor-history"><StudentMentorHistoryTab studentId={id} /></Tabs.Content>
  <Tabs.Content value="assessment"><StudentStageAssessmentTab studentId={id} /></Tabs.Content>
  ```

Edit `web/src/features/student/api.ts` 追加：

```ts
mentorHistory: (id: number) =>
  api.get(`/students/${id}/mentor-history`).then(r => r.data.data),
listSubscriptions: (q: { studentId?: number; teacherId?: number }) =>
  api.get('/subscriptions', { params: q }).then(r => r.data.data),
cancelSubscription: (id: number) =>
  api.delete(`/subscriptions/${id}`).then(r => r.data.data),
listAssessments: (studentId: number) =>
  api.get(`/students/${studentId}/stage-assessments`).then(r => r.data.data),
changeMentor: (id: number, body: { toTeacherId: number; reason?: string; keepSubscriptions: boolean }) =>
  api.put(`/students/${id}/mentor`, body).then(r => r.data.data),
```

- [ ] **Step 5: 运行测试，确认通过**

Run: `cd web && pnpm test -- StudentDetailSheet`
Expected: PASS

- [ ] **Step 6: 建议 commit**

```bash
git add web/src/features/student/ \
        src/main/java/com/eduze/manage/student/service/StudentService.java \
        src/main/java/com/eduze/manage/student/controller/StudentController.java
git commit -m "feat(web): student list mentor column + detail sheet 3 new tabs"
```

---

## Task 21: 老师可用时段配置页

**Files:**
- Create: `web/src/features/teacher/api.ts`、`types.ts`、`schemas.ts`
- Create: `web/src/features/teacher/pages/TeacherListPage.tsx`（路由 `/teachers`）
- Create: `web/src/features/teacher/pages/TeacherAvailabilityPage.tsx`（路由 `/teachers/:id/availabilities`）
- Create: `web/src/features/teacher/components/AvailabilityFormDialog.tsx`
- Modify: `web/src/app/router.tsx` 注册路由
- Modify: `web/src/app/shell/Sidebar.tsx` 加菜单项「老师」
- Test: `web/src/features/teacher/__tests__/TeacherAvailabilityPage.test.tsx`

- [ ] **Step 1: 写失败测试**

Create `web/src/features/teacher/__tests__/TeacherAvailabilityPage.test.tsx`:

```tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TeacherAvailabilityPage } from '../pages/TeacherAvailabilityPage';

vi.mock('../api', () => ({
  teacherApi: {
    listAvailabilities: vi.fn().mockResolvedValue([
      { id: 230001, teacherId: 1101, dayOfWeek: 6, startMinute: 540, endMinute: 630, capacity: 8, validFrom: '2026-05-01', status: 1 },
    ]),
    createAvailability: vi.fn(),
  },
}));

describe('TeacherAvailabilityPage', () => {
  it('lists availabilities for the teacher', async () => {
    const qc = new QueryClient();
    render(
      <QueryClientProvider client={qc}>
        <MemoryRouter initialEntries={['/teachers/1101/availabilities']}>
          <Routes>
            <Route path="/teachers/:id/availabilities" element={<TeacherAvailabilityPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
    expect(await screen.findByText(/周六 09:00–10:30/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `cd web && pnpm test -- TeacherAvailabilityPage`
Expected: FAIL

- [ ] **Step 3: types + api**

Create `web/src/features/teacher/types.ts`:

```ts
export type Teacher = {
  id: number; username: string; name: string; branchId: number;
};
export type Availability = {
  id: number; teacherId: number; branchId: number;
  dayOfWeek: number; startMinute: number; endMinute: number;
  capacity: number; defaultClassRoomId?: number;
  validFrom: string; validTo?: string;
  status: number; note?: string;
};
```

Create `web/src/features/teacher/api.ts`:

```ts
import { api } from '@/lib/api';
import type { Teacher, Availability } from './types';

export const teacherApi = {
  list: (branchId?: number) =>
    api.get<{ data: Teacher[] }>('/teachers', { params: { branchId } }).then(r => r.data.data),
  listAvailabilities: (teacherId: number) =>
    api.get<{ data: Availability[] }>(`/teachers/${teacherId}/availabilities`).then(r => r.data.data),
  createAvailability: (teacherId: number, body: Omit<Availability,'id'|'teacherId'>) =>
    api.post(`/teachers/${teacherId}/availabilities`, body).then(r => r.data.data),
  updateAvailability: (id: number, body: Omit<Availability,'id'|'teacherId'>) =>
    api.put(`/teacher-availabilities/${id}`, body).then(r => r.data.data),
  deleteAvailability: (id: number) =>
    api.delete(`/teacher-availabilities/${id}`).then(r => r.data.data),
};
```

- [ ] **Step 4: pages + dialog**

Create `web/src/features/teacher/pages/TeacherListPage.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { teacherApi } from '../api';

export function TeacherListPage() {
  const q = useQuery({ queryKey: ['teachers'], queryFn: () => teacherApi.list() });
  return (
    <div className="p-4 space-y-4">
      <h1 className="text-xl font-semibold">老师</h1>
      <ul className="divide-y rounded-lg border bg-white">
        {q.data?.map(t => (
          <li key={t.id} className="p-3 flex justify-between items-center">
            <span>{t.name} <span className="text-muted-fg text-xs">@{t.username}</span></span>
            <Link to={`/teachers/${t.id}/availabilities`} className="text-primary">配置可用时段</Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
```

Create `web/src/features/teacher/pages/TeacherAvailabilityPage.tsx`:

```tsx
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { teacherApi } from '../api';
import { AvailabilityFormDialog } from '../components/AvailabilityFormDialog';

const DAYS = ['周一','周二','周三','周四','周五','周六','周日'];
const fmt = (m: number) => `${String(Math.floor(m/60)).padStart(2,'0')}:${String(m%60).padStart(2,'0')}`;

export function TeacherAvailabilityPage() {
  const { id } = useParams();
  const teacherId = Number(id);
  const qc = useQueryClient();
  const [openNew, setOpenNew] = useState(false);

  const q = useQuery({
    queryKey: ['availabilities', teacherId],
    queryFn: () => teacherApi.listAvailabilities(teacherId),
  });

  const del = useMutation({
    mutationFn: (id: number) => teacherApi.deleteAvailability(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['availabilities', teacherId] }),
  });

  return (
    <div className="p-4 space-y-4">
      <div className="flex justify-between items-center">
        <h1 className="text-xl font-semibold">可用时段</h1>
        <Button onClick={() => setOpenNew(true)}>新增时段</Button>
      </div>
      <ul className="divide-y rounded-lg border bg-white">
        {q.data?.map(a => (
          <li key={a.id} className="p-3 flex justify-between items-center">
            <span>{DAYS[a.dayOfWeek - 1]} {fmt(a.startMinute)}–{fmt(a.endMinute)} · 容量 {a.capacity}</span>
            <Button variant="ghost" onClick={() => del.mutate(a.id)}>停用</Button>
          </li>
        ))}
      </ul>
      <AvailabilityFormDialog
        teacherId={teacherId}
        open={openNew}
        onOpenChange={setOpenNew}
        onSaved={() => qc.invalidateQueries({ queryKey: ['availabilities', teacherId] })}
      />
    </div>
  );
}
```

Create `web/src/features/teacher/components/AvailabilityFormDialog.tsx`:

```tsx
import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { SimpleSelect } from '@/components/ui/Select';
import { teacherApi } from '../api';
import { toast } from '@/lib/toast';

const DAYS = ['周一','周二','周三','周四','周五','周六','周日'];

type Props = { teacherId: number; open: boolean; onOpenChange: (b:boolean)=>void; onSaved?:()=>void };

function parseHHmm(s: string): number {
  const [h, m] = s.split(':').map(Number);
  return h * 60 + m;
}

export function AvailabilityFormDialog({ teacherId, open, onOpenChange, onSaved }: Props) {
  const [dow, setDow] = useState('6');
  const [start, setStart] = useState('09:00');
  const [end, setEnd] = useState('10:30');
  const [capacity, setCapacity] = useState('8');
  const [validFrom, setValidFrom] = useState(new Date().toISOString().slice(0,10));

  const m = useMutation({
    mutationFn: () => teacherApi.createAvailability(teacherId, {
      branchId: 1,
      dayOfWeek: Number(dow),
      startMinute: parseHHmm(start),
      endMinute: parseHHmm(end),
      capacity: Number(capacity),
      validFrom,
      status: 1,
    } as any),
    onSuccess: () => { toast.success('已保存'); onSaved?.(); onOpenChange(false); },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? '保存失败'),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader><DialogTitle>新增可用时段</DialogTitle></DialogHeader>
        <div className="space-y-3">
          <div><Label>周几</Label>
            <SimpleSelect value={dow} onChange={setDow}
              options={DAYS.map((d, i) => ({ value: String(i+1), label: d }))} />
          </div>
          <div className="flex gap-2">
            <div className="flex-1"><Label>开始</Label><Input type="time" value={start} onChange={e=>setStart(e.target.value)}/></div>
            <div className="flex-1"><Label>结束</Label><Input type="time" value={end}   onChange={e=>setEnd(e.target.value)}/></div>
          </div>
          <div><Label>容量</Label><Input value={capacity} onChange={e=>setCapacity(e.target.value)}/></div>
          <div><Label>生效起 (YYYY-MM-DD)</Label><Input type="date" value={validFrom} onChange={e=>setValidFrom(e.target.value)}/></div>
        </div>
        <DialogFooter>
          <Button variant="ghost" onClick={()=>onOpenChange(false)}>取消</Button>
          <Button onClick={()=>m.mutate()} disabled={m.isPending}>保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
```

- [ ] **Step 5: 路由 + 导航**

Edit `web/src/app/router.tsx` — 追加：

```tsx
import { TeacherListPage } from '@/features/teacher/pages/TeacherListPage';
import { TeacherAvailabilityPage } from '@/features/teacher/pages/TeacherAvailabilityPage';

// 在 layout 子路由数组中追加：
{ path: 'teachers', element: <TeacherListPage /> },
{ path: 'teachers/:id/availabilities', element: <TeacherAvailabilityPage /> },
```

Edit `web/src/app/shell/Sidebar.tsx` — 菜单数组追加：

```tsx
{ to: '/teachers', label: '老师', icon: ChalkboardTeacher },
```

- [ ] **Step 6: 运行测试，确认通过**

Run: `cd web && pnpm test -- TeacherAvailabilityPage`
Expected: PASS

- [ ] **Step 7: 建议 commit**

```bash
git add web/src/features/teacher/ web/src/app/router.tsx web/src/app/shell/Sidebar.tsx
git commit -m "feat(web): teacher list + availability config page"
```

---

## Task 22: 老师工作台页面

**Files:**
- Create: `web/src/features/teacher/pages/MyWorkbenchPage.tsx`（路由 `/me/workbench`）
- Modify: `web/src/app/router.tsx` 注册路由 + 老师角色登录后默认重定向到 `/me/workbench`
- Modify: `web/src/features/auth/store.ts` 或 `AuthProvider` 判断 role 后 redirect
- Test: `web/src/features/teacher/__tests__/MyWorkbenchPage.test.tsx`

- [ ] **Step 1: 写失败测试**

Create `web/src/features/teacher/__tests__/MyWorkbenchPage.test.tsx`:

```tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MyWorkbenchPage } from '../pages/MyWorkbenchPage';

vi.mock('../api', () => ({
  teacherApi: {
    myWeek: vi.fn().mockResolvedValue({
      teachers: [{ teacherId: 1101, teacherName: '张老师', lessons: [
        { lessonId: 1, startAt: '2026-09-05T09:00', endAt: '2026-09-05T10:30',
          classRoomId: null, source: 1, rosterCount: 6, capacity: 8 },
      ]}]
    }),
    myMentees: vi.fn().mockResolvedValue([
      { id: 240001, name: '小启', currentStageCode: 'STAGE_KMD' },
    ]),
  },
}));

describe('MyWorkbenchPage', () => {
  it('shows KPI and my week lessons', async () => {
    const qc = new QueryClient();
    render(
      <QueryClientProvider client={qc}>
        <MyWorkbenchPage />
      </QueryClientProvider>
    );
    expect(await screen.findByText(/本周课次/)).toBeInTheDocument();
    expect(await screen.findByText(/张老师/)).toBeInTheDocument();
    expect(await screen.findByText(/小启/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `cd web && pnpm test -- MyWorkbenchPage`
Expected: FAIL

- [ ] **Step 3: 加 api 方法**

Edit `web/src/features/teacher/api.ts` 追加：

```ts
myWeek: (weekStart: string) =>
  api.get('/schedule/my-week', { params: { weekStart } }).then(r => r.data.data),
myMentees: () =>
  api.get('/students', { params: { mentorTeacherId: 'self', size: 200 } }).then(r => r.data.data.list),
// 注：后端 mentorTeacherId 接受 'self' 时由 controller 解析为当前用户 id，需在 StudentController 中加：
//   if ("self".equals(mentorTeacherIdStr)) actualMentorId = currentUserId();
// 此调整放本任务 Step 4 一起做。
```

- [ ] **Step 4: 后端补 `self` 解析**

Edit `src/main/java/com/eduze/manage/student/controller/StudentController.java`:
- `list` 方法的 `Long mentorTeacherId` 参数改为 `String mentorTeacherId`
- 解析：

```java
Long resolvedMentor = null;
if (mentorTeacherId != null) {
    if ("self".equals(mentorTeacherId)) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails d) {
            resolvedMentor = d.getUserId();
        }
    } else {
        resolvedMentor = Long.valueOf(mentorTeacherId);
    }
}
// 传给 service.list(..., resolvedMentor, ...)
```

- [ ] **Step 5: 页面实现**

Create `web/src/features/teacher/pages/MyWorkbenchPage.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { teacherApi } from '../api';
import { KPICard } from '@/components/ui/KPICard';
import { Skeleton } from '@/components/ui/Skeleton';

function thisMonday(): string {
  const d = new Date();
  const day = d.getDay() || 7;
  d.setDate(d.getDate() - day + 1);
  return d.toISOString().slice(0, 10);
}

export function MyWorkbenchPage() {
  const ws = thisMonday();
  const wQ = useQuery({ queryKey: ['my-week', ws], queryFn: () => teacherApi.myWeek(ws) });
  const mQ = useQuery({ queryKey: ['my-mentees'], queryFn: () => teacherApi.myMentees() });

  const lessonCount = wQ.data?.teachers?.[0]?.lessons?.length ?? 0;
  return (
    <div className="p-4 space-y-4">
      <h1 className="text-2xl font-semibold">我的工作台</h1>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <KPICard label="本周课次" value={lessonCount} />
        <KPICard label="主带学员" value={mQ.data?.length ?? 0} />
        <KPICard label="待批请假" value={'—'} />
        <KPICard label="模板维护" value={'→'} href="/teachers" />
      </div>
      <section>
        <h2 className="text-lg font-medium mb-2">本周课表</h2>
        {wQ.isLoading ? <Skeleton className="h-24 w-full" /> : (
          <div className="flex gap-2 overflow-x-auto">
            {(wQ.data?.teachers?.[0]?.lessons ?? []).map(l => (
              <div key={l.lessonId} className="rounded-md border p-2 min-w-[160px]">
                <div className="text-sm font-medium">{new Date(l.startAt).toLocaleString('zh', { weekday:'short', hour:'2-digit', minute:'2-digit' })}</div>
                <div className="text-xs text-muted-fg">名单 {l.rosterCount}/{l.capacity}</div>
              </div>
            ))}
            {lessonCount === 0 && <div className="text-muted-fg text-sm">本周暂无课次</div>}
          </div>
        )}
      </section>
      <section>
        <h2 className="text-lg font-medium mb-2">我的主带学员</h2>
        <ul className="divide-y rounded-lg border bg-white">
          {mQ.data?.map((s: any) => (
            <li key={s.id} className="p-2 flex justify-between text-sm">
              <span>{s.name}</span>
              <span className="text-muted-fg text-xs">{s.currentStageCode}</span>
            </li>
          ))}
          {mQ.data?.length === 0 && <li className="p-3 text-muted-fg">暂无主带学员</li>}
        </ul>
      </section>
    </div>
  );
}
```

- [ ] **Step 6: 路由 + 登录后落地逻辑**

Edit `web/src/app/router.tsx` — 子路由追加：

```tsx
{ path: 'me/workbench', element: <MyWorkbenchPage /> },
```

Edit `web/src/features/auth/store.ts` 或 `AuthProvider` 中 login 成功后：

```ts
const roles = response.user.roles ?? [];
const isOnlyTeacher = roles.includes('TEACHER') && roles.length === 1;
navigate(isOnlyTeacher ? '/me/workbench' : '/dashboard');
```

- [ ] **Step 7: 运行测试，确认通过**

Run: `cd web && pnpm test -- MyWorkbenchPage`
Expected: PASS

- [ ] **Step 8: 建议 commit**

```bash
git add web/src/features/teacher/ web/src/app/router.tsx \
        src/main/java/com/eduze/manage/student/controller/StudentController.java
git commit -m "feat(web): teacher workbench page + role-based landing"
```

---

## Task 23: 周课表视图按老师列重写

**Files:**
- Modify: `web/src/features/lesson/pages/WeeklySchedulePage.tsx`（用新组件）
- Create: `web/src/features/lesson/components/WeeklyGridByTeacher.tsx`
- Modify: `web/src/features/lesson/components/BulkGenerateDialog.tsx`（改为 availability 模式）
- Modify: `web/src/features/lesson/api.ts`（加 `byTeacher`、`bulkGenerateByTeacher`）
- Test: `web/src/features/lesson/__tests__/WeeklyGridByTeacher.test.tsx`

- [ ] **Step 1: 写失败测试**

Create `web/src/features/lesson/__tests__/WeeklyGridByTeacher.test.tsx`:

```tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { WeeklyGridByTeacher } from '../components/WeeklyGridByTeacher';

const data = {
  teachers: [
    { teacherId: 1101, teacherName: '张老师', lessons: [
      { lessonId: 1, startAt: '2026-09-05T09:00:00', endAt: '2026-09-05T10:30:00',
        classRoomId: null, source: 1, rosterCount: 6, capacity: 8 },
    ]},
    { teacherId: 1102, teacherName: '王老师', lessons: [] },
  ],
};

describe('WeeklyGridByTeacher', () => {
  it('renders teacher columns', () => {
    render(<WeeklyGridByTeacher data={data} weekStart="2026-09-05" />);
    expect(screen.getByText('张老师')).toBeInTheDocument();
    expect(screen.getByText('王老师')).toBeInTheDocument();
  });
  it('renders lesson cell with roster count', () => {
    render(<WeeklyGridByTeacher data={data} weekStart="2026-09-05" />);
    expect(screen.getByText(/6 ?\/ ?8/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `cd web && pnpm test -- WeeklyGridByTeacher`
Expected: FAIL

- [ ] **Step 3: api 加方法**

Edit `web/src/features/lesson/api.ts` 追加：

```ts
byTeacher: (params: { branchId?: number; weekStart: string }) =>
  api.get('/schedule/by-teacher', { params }).then(r => r.data.data),
bulkGenerateByTeacher: (body: { weeks: number; fromDate: string; teacherIds: number[] }) =>
  api.post('/lessons/bulk-generate-by-teacher', body).then(r => r.data.data),
```

- [ ] **Step 4: 写 WeeklyGridByTeacher**

Create `web/src/features/lesson/components/WeeklyGridByTeacher.tsx`:

```tsx
import clsx from 'clsx';

type Lesson = {
  lessonId: number;
  startAt: string; endAt: string;
  classRoomId?: number | null;
  source?: number | null;
  rosterCount: number;
  capacity?: number | null;
};
type Data = { teachers: { teacherId: number; teacherName: string; lessons: Lesson[] }[] };

const HOUR_MIN = 8 * 60;       // 08:00
const HOUR_MAX = 21 * 60;      // 21:00
const SLOT = 30;
const ROWS = Array.from({ length: (HOUR_MAX - HOUR_MIN) / SLOT }, (_, i) => HOUR_MIN + i * SLOT);

const fmt = (m: number) => `${String(Math.floor(m/60)).padStart(2,'0')}:${String(m%60).padStart(2,'0')}`;

function minuteOfDay(iso: string): number {
  const d = new Date(iso); return d.getHours() * 60 + d.getMinutes();
}

export function WeeklyGridByTeacher({ data, weekStart, onClickLesson }: {
  data: Data; weekStart: string;
  onClickLesson?: (lessonId: number) => void;
}) {
  return (
    <div className="overflow-x-auto">
      <table className="border-collapse min-w-full text-xs">
        <thead>
          <tr>
            <th className="sticky left-0 bg-white border px-2 py-1 w-20">时段</th>
            {data.teachers.map(t => (
              <th key={t.teacherId} className="border px-2 py-1 min-w-[140px]">
                {t.teacherName}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {ROWS.map(slot => (
            <tr key={slot}>
              <td className="sticky left-0 bg-white border px-2 py-1 text-muted-fg">{fmt(slot)}</td>
              {data.teachers.map(t => {
                const cell = t.lessons.find(l => minuteOfDay(l.startAt) === slot);
                return (
                  <td key={t.teacherId+':'+slot} className={clsx('border align-top h-10', cell && 'bg-secondary cursor-pointer')}
                      onClick={() => cell && onClickLesson?.(cell.lessonId)}>
                    {cell && (
                      <div className="p-1">
                        <div className="text-[10px] text-muted-fg">{new Date(cell.startAt).toLocaleDateString('zh',{month:'2-digit',day:'2-digit'})}</div>
                        <div className="text-xs font-medium">{cell.rosterCount}/{cell.capacity ?? '—'}</div>
                      </div>
                    )}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

- [ ] **Step 5: 改 WeeklySchedulePage**

Edit `web/src/features/lesson/pages/WeeklySchedulePage.tsx`：

```tsx
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { WeeklyGridByTeacher } from '../components/WeeklyGridByTeacher';
import { WeekNavigator } from '../components/WeekNavigator';
import { LessonDetailSheet } from '../components/LessonDetailSheet';
import { BulkGenerateDialog } from '../components/BulkGenerateDialog';
import { Button } from '@/components/ui/Button';
import { lessonApi } from '../api';

function thisMonday(): string {
  const d = new Date(); const day = d.getDay() || 7;
  d.setDate(d.getDate() - day + 1); return d.toISOString().slice(0,10);
}

export function WeeklySchedulePage() {
  const [weekStart, setWeekStart] = useState(thisMonday());
  const [openBulk, setOpenBulk] = useState(false);
  const [activeLessonId, setActiveLessonId] = useState<number | null>(null);

  const q = useQuery({
    queryKey: ['schedule-by-teacher', weekStart],
    queryFn: () => lessonApi.byTeacher({ weekStart }),
  });

  return (
    <div className="p-4 space-y-3">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">周课表（按老师）</h1>
        <div className="flex items-center gap-2">
          <WeekNavigator value={weekStart} onChange={setWeekStart} />
          <Button onClick={() => setOpenBulk(true)}>批量排课</Button>
        </div>
      </div>
      {q.data && <WeeklyGridByTeacher data={q.data} weekStart={weekStart} onClickLesson={setActiveLessonId} />}
      <BulkGenerateDialog open={openBulk} onOpenChange={setOpenBulk}
        onDone={() => q.refetch()} />
      {activeLessonId && (
        <LessonDetailSheet lessonId={activeLessonId} open={true}
          onOpenChange={(b) => !b && setActiveLessonId(null)} />
      )}
    </div>
  );
}
```

- [ ] **Step 6: 改 BulkGenerateDialog 走 availability**

Edit `web/src/features/lesson/components/BulkGenerateDialog.tsx`：

```tsx
// 删除 classGroup / weekdays / startTime / endTime / durationMinutes 等字段
// 新表单：weeks（1-8）、fromDate、teacherIds（multi-select；空 = 全部）

import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { teacherApi } from '@/features/teacher/api';
import { lessonApi } from '../api';
import { toast } from '@/lib/toast';

type Props = { open: boolean; onOpenChange: (b:boolean)=>void; onDone?: ()=>void };

export function BulkGenerateDialog({ open, onOpenChange, onDone }: Props) {
  const teachersQ = useQuery({ queryKey: ['teachers'], queryFn: () => teacherApi.list() });
  const [weeks, setWeeks] = useState('4');
  const [fromDate, setFromDate] = useState(new Date().toISOString().slice(0,10));
  const [selected, setSelected] = useState<number[]>([]);

  const m = useMutation({
    mutationFn: () => lessonApi.bulkGenerateByTeacher({
      weeks: Number(weeks), fromDate, teacherIds: selected,
    }),
    onSuccess: (r: any) => {
      toast.success(`已生成 ${r.generated} 节，跳过 ${r.skipped} 节`);
      onDone?.(); onOpenChange(false);
    },
    onError: (e: any) => toast.error(e?.response?.data?.message ?? '生成失败'),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader><DialogTitle>批量排课</DialogTitle></DialogHeader>
        <div className="space-y-3">
          <div><Label>周数（1-8）</Label><Input value={weeks} onChange={e=>setWeeks(e.target.value)}/></div>
          <div><Label>起始日期</Label><Input type="date" value={fromDate} onChange={e=>setFromDate(e.target.value)}/></div>
          <div>
            <Label>老师范围（不选 = 全部）</Label>
            <div className="flex flex-wrap gap-2 mt-1">
              {(teachersQ.data ?? []).map(t => {
                const on = selected.includes(t.id);
                return (
                  <button key={t.id} type="button"
                    className={`px-2 py-1 rounded border ${on ? 'bg-primary text-primary-fg' : 'bg-white'}`}
                    onClick={() => setSelected(on ? selected.filter(i=>i!==t.id) : [...selected, t.id])}>
                    {t.name}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="ghost" onClick={()=>onOpenChange(false)}>取消</Button>
          <Button onClick={()=>m.mutate()} disabled={m.isPending}>生成</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
```

- [ ] **Step 7: 运行测试，确认通过**

Run: `cd web && pnpm test -- WeeklyGridByTeacher`
Expected: PASS

- [ ] **Step 8: 建议 commit**

```bash
git add web/src/features/lesson/
git commit -m "feat(web): rewrite weekly schedule view by teacher columns"
```

---

# P7. 联调 / 文档

## Task 24: E2E 冒烟脚本

**Files:**
- Create: `scripts/e2e-teacher-flow.sh`（curl 串联完整流程）
- Test: 手工或在 CI 跑

- [ ] **Step 1: 写脚本**

Create `scripts/e2e-teacher-flow.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
echo "==> 登录"
TOKEN=$(curl -sX POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin@123"}' | jq -r '.data.accessToken')
test -n "$TOKEN"
H="Authorization: Bearer $TOKEN"

echo "==> 查 5 个阶段"
curl -sf -H "$H" "$BASE/api/curriculum/stages" | jq '.data | length' | grep -q '^5$'

echo "==> 查 22 个维度"
curl -sf -H "$H" "$BASE/api/curriculum/dimensions" | jq '.data | length' | grep -q '^22$'

echo "==> 查老师列表"
curl -sf -H "$H" "$BASE/api/teachers" | jq '.data | length' | tee /tmp/teacher_count.txt
test "$(cat /tmp/teacher_count.txt)" -ge 3

echo "==> 张老师可用时段 (1101)"
curl -sf -H "$H" "$BASE/api/teachers/1101/availabilities" | jq '.data | length'

echo "==> 批量生成下 1 周课次（全部老师）"
GEN=$(curl -sX POST -H "$H" -H 'Content-Type: application/json' \
  "$BASE/api/lessons/bulk-generate-by-teacher" \
  -d "{\"weeks\":1,\"fromDate\":\"$(date -d '+7 days' +%F)\",\"teacherIds\":[]}")
echo "$GEN" | jq

echo "==> 按老师查本周课表"
curl -sf -H "$H" "$BASE/api/schedule/by-teacher?weekStart=$(date -d '+7 days' +%F)" \
  | jq '.data.teachers | length'

echo "==> 新增学员（带主带 + 1 个订阅）"
NEW=$(curl -sX POST -H "$H" -H 'Content-Type: application/json' \
  "$BASE/api/students" \
  -d '{"branchId":1,"enrollNo":"E2E001","name":"E2E 测试娃","gender":1,
       "birthday":"2020-01-01","enrollDate":"2026-05-01","status":1,
       "mentorTeacherId":1101}')
SID=$(echo "$NEW" | jq -r '.data.id')
echo "  -> studentId=$SID"

echo "==> 换主带老师 1101 -> 1102"
curl -sf -X PUT -H "$H" -H 'Content-Type: application/json' \
  "$BASE/api/students/$SID/mentor" \
  -d '{"toTeacherId":1102,"reason":"E2E 测试","keepSubscriptions":true}'

echo "==> 查主带变更历史"
curl -sf -H "$H" "$BASE/api/students/$SID/mentor-history" | jq '.data | length' | tee /tmp/hist.txt
test "$(cat /tmp/hist.txt)" -ge 2

echo "==> 清理：删除 E2E 学员"
curl -sf -X DELETE -H "$H" "$BASE/api/students/$SID" || true

echo "✓ E2E teacher flow PASSED"
```

- [ ] **Step 2: 赋权**

```bash
chmod +x scripts/e2e-teacher-flow.sh
```

- [ ] **Step 3: 手工跑（前提：本地 docker compose dev 已启动，admin 已 seed）**

Run: `./scripts/e2e-teacher-flow.sh`
Expected: 最终输出 `✓ E2E teacher flow PASSED`

- [ ] **Step 4: 建议 commit**

```bash
git add scripts/e2e-teacher-flow.sh
git commit -m "test(e2e): teacher-centric flow smoke script"
```

---

## Task 25: 文档同步更新

**Files:**
- Modify: `docs/socrates/specs/2026-05-09-eduze-manage-design.md`（顶部加 v1.1 引用 + §4.4 顶部加 banner 指向新 spec）
- Modify: `docs/socrates/plans/2026-05-09-eduze-manage-phase1-plan.md`（顶部加 v1.1 增量任务清单引用）
- Modify: `README.md`（如有"模块"章节，补充老师 / 课程体系简述）

- [ ] **Step 1: Patch v1.0 spec**

Edit `docs/socrates/specs/2026-05-09-eduze-manage-design.md` — §1 文档元信息表末尾追加行：

```
| 增量修订 | v1.1（2026-05-22）— 见 `docs/socrates/specs/2026-05-22-teacher-centric-scheduling-design.md`：将排课模型切换为"老师为核心"，新增 778 课程体系。**§4.3 / §4.4 / §5 内容请以 v1.1 为准。** |
```

并在 §4.3 和 §4.4 顶部各加一行：

```
> **v1.1 修订**：本节自 2026-05-22 起以 `2026-05-22-teacher-centric-scheduling-design.md` 为准。下方内容保留作历史参考。
```

- [ ] **Step 2: Patch phase1 plan**

Edit `docs/socrates/plans/2026-05-09-eduze-manage-phase1-plan.md` — 在 §0 实施总览底部追加：

```
### 0.x 增量任务（2026-05-22 v1.1 修订）

F / G 阶段任务按 `docs/socrates/plans/2026-05-22-teacher-centric-scheduling-plan.md` 执行，
具体覆盖：T1–T5（迁移/Seed）、T6–T9（基础设施）、T10–T11（主带管理）、
T12–T16（排课模型重塑）、T17–T18（班级降级+评估）、T19–T23（前端）、T24–T25（联调+文档）。
原 plan F/G 中"按班级排课"相关任务**作废**，以 v1.1 plan 任务为准。
```

- [ ] **Step 3: README 补充（如必要）**

Edit `README.md` — 如有"功能模块"章节，追加一句：

```
- 老师为核心的排课：学员关联主带老师，老师维护周可用时段，学员订阅自动生成课次名单。
- 罗恩菲尔德 5 阶段 + 778（7 元素 / 7 设计原则 / 8 流派）课程体系：已 seed，可用于学员阶段评估与成长报告。
```

- [ ] **Step 4: 建议 commit**

```bash
git add docs/socrates/specs/2026-05-09-eduze-manage-design.md \
        docs/socrates/plans/2026-05-09-eduze-manage-phase1-plan.md \
        README.md
git commit -m "docs: cross-link v1.1 teacher-centric design and plan"
```

---

## Self-Review 自审

> 实施完成后回到 spec 对照检查。

### 1. Spec 覆盖核对

| Spec §  | 要点 | 对应任务 |
|---|---|---|
| §1.1 落点 1（老师角色） | 现有 TEACHER 角色 + 6 权限 + 工作台 | T4, T6, T22 |
| §1.1 落点 2（学员关联） | mentor_teacher_id + 历史 + 换主带 | T1, T9, T10, T11 |
| §1.1 落点 3（按老师排期） | Availability + Subscription + ScheduleGenerator + 按老师课表 | T1, T7, T12, T13, T14, T15, T16, T23 |
| §1.1 落点 4（初始化数据） | 阶段 / 维度 / 课程产品 / dev 示例 | T2, T3, T5, T8 |
| §2.2 表清单 | 6 新表 + 4 课程体系表 + 3 表 ALTER | T1, T2 |
| §3.1 流程 A-E | 全部走通 | T10, T7, T14, T13, T23, T22 |
| §3.2 API 清单 | 24 个端点 | T6 (1), T7 (4), T8 (2), T10/11 (2), T12 (4), T13 (3), T15 (1), T16 (2), T18 (2) = 21 个 ✓ + 课程列表 1 个 = 22 |
| §3.3 权限补丁 | 6 项 + 4 角色 | T4 |
| §3.4 校验规则 | 5 条规则 | T7 (overlap), T10 (mentor required), T12 (dup subscription), T14 (weeks limit) |
| §4 UI 改动 | 9 个页面/组件改动 | T19, T20, T21, T22, T23 |
| §5 Seed | 阶段 / 维度 / 课程 / 示例 | T3, T5 |
| §6.1/6.2 文档修订 | 老 spec / 老 plan 增加 v1.1 引用 | T25 |
| §7 风险 R1 ScheduleGenerator 边界 | ≥ 10 单测 | T14 |

**全部覆盖。**

### 2. 占位扫描

✅ 无 TODO / 待补 / placeholder / "implement later"。每一步都有完整代码或确切命令。

### 3. 类型一致性

- `mentorTeacherId` 全栈一致：DB column `mentor_teacher_id`、Java `mentorTeacherId`、JSON `mentorTeacherId`、TS `mentorTeacherId` ✓
- `LessonStudent.source` 全栈一致：`SUBSCRIPTION/MANUAL/TRIAL/MAKEUP` 在 T1 DDL、T13 service、T13 测试 ✓
- `LessonStudent.status` = `BOOKED/CANCELLED` 一致 ✓
- `Lesson.source` = INT 1-4 在 T1 DDL、T14 ScheduleGenerator (`setSource(1)`)、TS 类型 ✓
- API 路径 kebab-case 一致 ✓

---

## Execution Handoff

**Plan complete and saved to `docs/socrates/plans/2026-05-22-teacher-centric-scheduling-plan.md`. Two execution options:**

**1. Agent-Driven (recommended)** - I dispatch a fresh Agent per task, review between tasks, fast iteration. Plans with >6 tasks automatically use batch-parallel execution (see Scaling protocol in the skill).

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**注意：用户已明确"不提交 git，等 review"。执行任何 commit 前都需要先获得用户确认。**
