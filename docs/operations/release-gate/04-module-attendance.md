# 模块 #4：签到与接送（RC-04）

**后端包：** `attendance/`  
**前端路由：** `/attendance`, `/attendance/leaves`, `/attendance/stats`  
**主要 IT：** `AttendanceControllerIT`, `PickupServiceIT`, `LeaveServiceIT`, `AbsenceJobIT`, `AttendanceStatIT`  
**E2E：** `web/e2e/attendance.spec.ts`

---

### RC-04-001 今日花名册

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.5 |
| **后端验证** | 按校区/时段筛选；`AttendanceControllerIT` |
| **前端验证** | `AttendanceWorkbenchPage` 花名册加载 |
| **自动化证据** | `./mvnw test -Dtest=AttendanceControllerIT` |
| **E2E 参考** | `attendance.spec.ts` |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-04-002 手动入园签到

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.5 |
| **后端验证** | check-in API；`CheckInMethod` 记录 |
| **前端验证** | `CheckInButton` 状态变「已入园」 |
| **自动化证据** | IT + E2E |
| **E2E 参考** | `attendance.spec.ts` |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-04-003 离园签到

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.5 |
| **后端验证** | check-out API |
| **前端验证** | 离园按钮与状态更新 |
| **自动化证据** | 人工 + IT |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-04-004 二维码扫码签到

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.5、phase1-plan R2 |
| **后端验证** | 扫码解析与签到逻辑 |
| **前端验证** | `StudentQrCodePanel`；localhost/HTTPS 下摄像头可用 |
| **自动化证据** | 人工扫码或模拟 token |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-04-005 接送人记录

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.5、`t_pickup_record` |
| **后端验证** | `PickupServiceIT`：入园/离园选人 |
| **前端验证** | `PickupSelectDialog` |
| **自动化证据** | `./mvnw test -Dtest=PickupServiceIT` |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-04-006 请假申请（前台代录）

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.5 |
| **后端验证** | `LeaveServiceIT`：创建待审批 |
| **前端验证** | `LeaveRequestFormDialog` |
| **自动化证据** | `./mvnw test -Dtest=LeaveServiceIT` |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-04-007 请假审批

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.5 |
| **后端验证** | approve/reject；批准后出勤变请假 |
| **前端验证** | `/attendance/leaves` + `LeaveApprovalActions` |
| **自动化证据** | `LeaveServiceIT` + 人工 |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-04-008 缺勤定时任务

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.5 |
| **后端验证** | `AbsenceJobIT`：课次结束未签到→缺勤 |
| **前端验证** | — |
| **自动化证据** | `./mvnw test -Dtest=AbsenceJobIT` |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-04-009 出勤统计

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.5 |
| **后端验证** | `AttendanceStatIT` |
| **前端验证** | `/attendance/stats`、校长 Dashboard KPI |
| **自动化证据** | `./mvnw test -Dtest=AttendanceStatIT` |
| **E2E 参考** | — |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |

---

### RC-04-010 签到状态枚举完整

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **Spec 依据** | design §4.5（未到/已入园/已离园/缺勤/请假） |
| **后端验证** | `AttendanceStatus` 五种状态 API 一致 |
| **前端验证** | `AttendanceStatusBadge` 显示正确 |
| **自动化证据** | 人工走查各状态 |
| **E2E 参考** | `attendance.spec.ts` |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | docs/evidence/release-gate/r2-r3-audit.md |
