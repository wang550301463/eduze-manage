# 模块 #2：学员与家长（RC-02）

**后端包：** `student/`  
**前端路由：** `/students`  
**主要 IT：** `StudentControllerIT`, `GuardianIT`, `CoursePackageIT`, `StudentImportIT`, `ClassGroupMemberIT`  
**E2E：** `web/e2e/students.spec.ts`, `student-create.spec.ts`

---

### RC-02-001 学员 CRUD

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.3 |
| **后端验证** | 创建/更新/软删除/状态变更；`StudentControllerIT` |
| **前端验证** | 列表、详情 Sheet、新建表单（Zod 校验） |
| **自动化证据** | `./mvnw test -Dtest=StudentControllerIT` |
| **E2E 参考** | `student-create.spec.ts` |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-02-002 入园编号租户内唯一

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §5.2 `UNIQUE(tenant_id, enroll_no)` |
| **后端验证** | 重复 `enroll_no` 返回冲突错误 |
| **前端验证** | 表单重复编号显示错误提示 |
| **自动化证据** | `StudentControllerIT` 或人工 API |
| **E2E 参考** | — |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-02-003 家长 CRUD 与关联

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.3（一户多生） |
| **后端验证** | `GuardianIT`：关联、关系字段 |
| **前端验证** | 学员详情「家长」Tab 增删改 |
| **自动化证据** | `./mvnw test -Dtest=GuardianIT` |
| **E2E 参考** | `students.spec.ts` |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-02-004 课时包录入与展示

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.3 |
| **后端验证** | `CoursePackageIT`：录入、余额计算 |
| **前端验证** | 详情 Tab 展示余额；≤5 节高亮提醒 |
| **自动化证据** | `./mvnw test -Dtest=CoursePackageIT` |
| **E2E 参考** | — |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-02-005 批量分班与调班

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.3 |
| **后端验证** | `ClassGroupMemberIT`, `StudentBulkController` |
| **前端验证** | `BulkAssignClassDialog` 批量操作生效 |
| **自动化证据** | `./mvnw test -Dtest=ClassGroupMemberIT` |
| **E2E 参考** | — |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-02-006 Excel 导入

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.3、phase1-plan R4（≤5000 行） |
| **后端验证** | `StudentImportIT`：模板、行数限制、错误反馈 |
| **前端验证** | 导入入口、成功/失败 Toast |
| **自动化证据** | `./mvnw test -Dtest=StudentImportIT` |
| **E2E 参考** | — |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-02-007 手机号脱敏

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §9.2 |
| **后端验证** | 列表 API 返回脱敏手机号 |
| **前端验证** | 列表显示 `138****1234` 格式 |
| **自动化证据** | API 响应 + UI 截图 |
| **E2E 参考** | — |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-02-008 敏感操作审计

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §9.2、`t_audit_log` |
| **后端验证** | 读学员明细写审计；`AuditAspectIT` |
| **前端验证** | — |
| **自动化证据** | `./mvnw test -Dtest=AuditAspectIT` + 查 `t_audit_log` |
| **E2E 参考** | — |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-02-009 学员详情 Tab 完整

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.3 UI |
| **后端验证** | 出勤/请假 API 有数据时返回正确 |
| **前端验证** | 基础/家长/课时包/出勤/请假 Tab 均可加载 |
| **自动化证据** | 人工走查 |
| **E2E 参考** | `students.spec.ts` |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |

---

### RC-02-010 列表筛选与分页

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.3、§9.1 P95 |
| **后端验证** | 校区/班级/状态/余额筛选 API 正确 |
| **前端验证** | 桌面 DataTable + 移动卡片；筛选生效 |
| **自动化证据** | 人工 + Network 抽样 P95 |
| **E2E 参考** | `students.spec.ts` |
| **结果** | ☐ PASS　☐ FAIL |
| **主审人 / 日期** | |
| **交叉 Review / 日期** | |
| **备注 / 证据** | |
