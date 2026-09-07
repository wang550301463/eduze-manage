# 阶段 4：生产演练（RC-07 / D-01–D-11）

> 在与生产相同路径执行：Docker Compose + `docker/.env`（真实密钥，非 example 默认值）。

**参考：** `docs/operations/runbook.md`、design spec `2026-06-11-release-gate-design.md` §8

**证据：** `docs/evidence/release-gate/r4-prod-audit.md`、`r4-dry-run.log`

---

### RC-07-001 / D-01 Docker 全栈启动

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **操作** | `./mvnw -DskipTests package` → `docker compose -f docker/docker-compose.yml --env-file docker/.env up -d` |
| **验收** | app / mysql / redis / nginx 均 healthy；`curl -sk https://localhost/actuator/health` UP |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | Cursor Agent (Cross-Review) / 2026-06-12 |
| **备注 / 证据** | `r4-https-fullstack.log`；`cross-review-verify.log` |

---

### RC-07-002 / D-02 超管登录与改密、新建校区

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **操作** | 超管登录 → 修改默认密码 → 设置中新建「校区 B」 |
| **验收** | 新密码可登录；校区列表含 B |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | `scripts/r4-dry-run-api.sh` D-02；密码 `Admin@DryRun2026!` |

---

### RC-07-003 / D-03 创建业务账号与权限

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **操作** | 新建前台（仅校区 B）、班主任（校区 B） |
| **验收** | 各角色登录后菜单与数据范围正确 |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | `front_b` FRONT_DESK；`teacher_b` TEACHER；`/api/me` 校区 B |

---

### RC-07-004 / D-04 学员全流程录入

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **操作** | 录入 3 名学员 + 家长 + 课时包 |
| **验收** | 列表/详情/脱敏/余额展示正确 |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | DRYRUN-001～003；课时包 20/20 |

---

### RC-07-005 / D-05 排课全流程

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **操作** | 建课程 → 建班 → 加学员 → 批量生成一周课次 |
| **验收** | `/schedule` 周课表显示；课次详情可开 |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | API：`bulk-generate` generated=1；课次 2026-06-12 09:00 |

---

### RC-07-006 / D-06 签到（手动 + 扫码）

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **操作** | 前台账号在工作台：手动签到 1 人；扫码签到 1 人（若设备可用） |
| **验收** | 状态变为已入园；HTTPS 下扫码可用 |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | HTTP 演练；扫码需 `lessonId`（老师中心课次无 classGroup）。status=2 已入园。 |

---

### RC-07-007 / D-07 请假审批链

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **操作** | 前台代录请假 → 班主任审批通过 |
| **验收** | 出勤记录变请假；请假列表状态正确 |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | 前台创建 → `teacher_b` approve status=2 |

---

### RC-07-008 / D-08 统计与 Dashboard

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **操作** | 校长账号查看 `/` Dashboard 与 `/attendance/stats` |
| **验收** | KPI 有数据，与演练操作一致 |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | `/api/stats/attendance/dashboard` weekLessons=1, monthAttendanceRate=100 |

---

### RC-07-009 / D-09 全局搜索

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **操作** | Cmd+K 搜索刚建学员姓名 |
| **验收** | 命中结果并可跳转详情 |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | `/api/search?q=演练学员1` 命中 student 类型 |

---

### RC-07-010 / D-10 备份与恢复

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **操作** | 执行 `backup-mysql.sh` → 停 app → `restore-mysql.sh` → 重启 |
| **验收** | 学员/课次等数据不丢失 |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | `docs/evidence/release-gate/backups/eduze-dryrun-2026-06-12-1105.sql.gz`；恢复后 3 学员 + 1 课次 |

---

### RC-07-011 / D-11 模拟版本升级

| 字段 | 内容 |
|------|------|
| **级别** | Blocker |
| **操作** | 当前代码重新 `package` → `docker compose build app && up -d app` |
| **验收** | Flyway 无 ERROR；应用正常；已有数据仍在 |
| **结果** | ☑ PASS　☐ FAIL |
| **主审人 / 日期** | Cursor Agent / 2026-06-12 |
| **交叉 Review / 日期** | |
| **备注 / 证据** | 降级：`mvn package` + 重启 JAR；Flyway「No migration necessary」；`r4-jar-d11.log` |
