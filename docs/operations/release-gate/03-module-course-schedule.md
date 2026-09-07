# 模块 #3：课程与排课（RC-03）

**后端包：** `course/`, `lesson/`, `teacher/`, `curriculum/`  
**前端路由：** `/courses`, `/courses/class-groups`, `/schedule`, `/teachers/availabilities`, `/workbench`  
**主要 IT：** `CourseControllerIT`, `ClassGroupControllerIT`, `ClassRoomControllerIT`, `LessonGenerateIT`, `LessonRescheduleIT`, `ConflictServiceIT`, `ScheduleServiceIT`, `CurriculumSeedIT`  
**E2E：** `web/e2e/schedule.spec.ts`

---

### RC-03-001 课程产品 CRUD

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.4 |
| **后端验证** | `CourseControllerIT` |
| **前端验证** | `/courses` 列表与表单 |
| **自动化证据** | `./mvnw test -Dtest=CourseControllerIT` |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-03-002 班级 CRUD 与成员

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.4 |
| **后端验证** | `ClassGroupControllerIT`, `ClassGroupMemberIT` |
| **前端验证** | `/courses/class-groups` 成员管理 |
| **自动化证据** | `./mvnw test -Dtest=ClassGroupControllerIT,ClassGroupMemberIT` |
| **E2E 参考** | `schedule.spec.ts` |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-03-003 画室 CRUD

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.4 |
| **后端验证** | `ClassRoomControllerIT` |
| **前端验证** | 画室列表/表单页可访问 |
| **自动化证据** | `./mvnw test -Dtest=ClassRoomControllerIT` |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-03-004 单次课次创建

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.4 |
| **后端验证** | `lesson:write` 权限；课次落库 |
| **前端验证** | 课次详情 Sheet 新建 |
| **自动化证据** | `LessonController` 相关 IT |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-03-005 按周批量生成课次

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.4 |
| **后端验证** | `LessonGenerateIT`, `ScheduleGeneratorIT` |
| **前端验证** | `BulkGenerateDialog` 后周课表可见 |
| **自动化证据** | `./mvnw test -Dtest=LessonGenerateIT,ScheduleGeneratorIT` |
| **E2E 参考** | `schedule.spec.ts` |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-03-006 调课与变更日志

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.4、`t_lesson_change_log` |
| **后端验证** | `LessonRescheduleIT`：`LessonChangeLog` 落库 |
| **前端验证** | `RescheduleDialog` 成功后课表更新 |
| **自动化证据** | `./mvnw test -Dtest=LessonRescheduleIT` |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-03-007 取消课次

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.4 |
| **后端验证** | `lesson:cancel` 权限；原因记录 |
| **前端验证** | `CancelLessonDialog` |
| **自动化证据** | API 或 IT 验证 |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-03-008 冲突检测（软警告）

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.4、§11.2 R1 |
| **后端验证** | `ConflictServiceIT`：教师/画室/班级冲突 |
| **前端验证** | `ConflictWarningDialog` 弹出，可继续提交 |
| **自动化证据** | `./mvnw test -Dtest=ConflictServiceIT` |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-03-009 周课表加载性能

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §9.1（P95 < 800ms） |
| **后端验证** | `ScheduleServiceIT` |
| **前端验证** | `/schedule` 翻周、单元格点击开 Sheet；Network P95 抽样 |
| **自动化证据** | `./mvnw test -Dtest=ScheduleServiceIT` + 人工 P95 |
| **E2E 参考** | `schedule.spec.ts` |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-03-010 老师可用时段

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | teacher-centric scheduling design |
| **后端验证** | `TeacherAvailabilityController` + `MigrationV150IT` |
| **前端验证** | `/teachers/availabilities` CRUD |
| **自动化证据** | `./mvnw test -Dtest=MigrationV150IT` |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-03-011 老师工作台

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.4、`lesson:teacher_view` |
| **后端验证** | 老师仅见自己的课次 |
| **前端验证** | `/workbench` 权限门控；侧栏仅老师可见 |
| **自动化证据** | 老师账号人工验证 |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-03-012 778 课程体系

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | `2026-05-22-teacher-centric-scheduling-design.md` |
| **后端验证** | `CurriculumSeedIT`, `MigrationV151IT` |
| **前端验证** | 课程维度/阶段与 seed 数据一致 |
| **自动化证据** | `./mvnw test -Dtest=CurriculumSeedIT,MigrationV151IT` |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-03-013 课次花名册与点名入口

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.4 UI |
| **后端验证** | 课次详情 API 返回学员花名册 |
| **前端验证** | `LessonRosterTable` + 跳转签到入口 |
| **自动化证据** | 人工走查 |
| **E2E 参考** | `schedule.spec.ts` |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |
