# R2–R3 模块审核证据（Agent 主审）

**日期：** 2026-06-12  
**Git：** `5e48e9f`（文档）+ 工作区未提交代码修复  
**主审：** Cursor Agent

## 自动化回归摘要

| 模块 | IT 命令 | 结果 |
|------|---------|------|
| RC-01 | `AuthControllerIT,RbacIT,BranchControllerIT,LoginAttemptServiceIT,RateLimitFilterIT,MeControllerIT,BranchScopeIT,TenantInterceptorIT` | PASS |
| RC-02 | `StudentControllerIT,GuardianIT,StudentImportIT,CoursePackageIT,ClassGroupMemberIT` | PASS |
| RC-03 | `CourseControllerIT,ClassGroupControllerIT,ClassRoomControllerIT,LessonGenerateIT,LessonRescheduleIT,ConflictServiceIT,ScheduleServiceIT,ScheduleGeneratorIT,CurriculumSeedIT,DemoTeacherSeedIT` | PASS |
| RC-04 | `AttendanceControllerIT,PickupServiceIT,LeaveServiceIT,AbsenceJobIT,AttendanceStatIT` | PASS |
| RC-05 | `SearchServiceIT,SearchServiceFullIT,SpaFallbackIT,AuditAspectIT` | PASS |
| E2E | `pnpm e2e` 9/9 | PASS |

## RC-00 补全

| RC | 证据 |
|----|------|
| RC-00-005 | 空库卷 Flyway 14 条 migration `success=1` |
| RC-00-006 | 导航无作品/支付/招生/微信入口；`PlaceholderPage` 未挂路由；后端无相关 API |

## 手工 / curl 抽检

| RC | 证据 |
|----|------|
| RC-01-004 | 登出后 token → `/api/users` 401；`AuthControllerIT.login_refresh_logout_flow` |
| RC-01-010 | `DELETE /api/roles/1` → 409「内置角色不可修改或删除」 |
| RC-06-003 | `t_user.password_hash` 前缀 `$2a$10$` |
| RC-06-005 | 无 Token `/api/users` → 401 |
| RC-06-008 | 学员列表 10 次 curl P95 ≈ 50ms（dev 空库） |
| RC-06-010 | `curl /actuator/health` → UP |
| RC-06-012 | `docker exec mysqldump` → `backups/eduze-docker-2026-06-12.sql.gz` |

## RC-05 横切 UI（代码走查）

| RC | 证据 |
|----|------|
| RC-05-003 | `PrincipalDashboardPage` 挂载于 `/` |
| RC-05-004 | `ToastProvider` + sonner `duration={4200}` |
| RC-05-005 | `CommandPalette` + Header `aria-label="全局搜索"` |
| RC-05-006 | `AppLayout` 汉堡菜单 `lg:hidden` |
| RC-05-007 | 登录/筛选/表格控件含 `aria-label` |

## 待 R4 生产演练（未勾选）

- RC-06-001 HTTPS（Nginx + 证书）
- RC-06-002 生产 `docker/.env` 真实密钥
- RC-06-013 恢复演练
- RC-06-016 默认 admin 密码已改
- RC-07-001 … RC-07-011 全流程 Docker 投产演练
