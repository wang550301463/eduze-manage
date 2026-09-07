# 投产签字记录

**投产环境：** 单机构 / 多校区 / 单机 Docker Compose  
**Git 版本：** tag `v0.1.0-release-candidate` on branch `main` (local only, not pushed)
**投产负责人：** _待指定_  
**计划投产日期：** 2026-09-07（加固整改完成日）  
**审核模式：** Agent 自动门禁 + **人工签字仍为硬门禁**

## 自动门禁（Agent，2026-09-07）

详见 `docs/evidence/release-gate/hardening-2026-09-07.md`：

- `mvn verify` 91 IT PASS
- 前端 Vitest / build / E2E 9/9 PASS
- Flyway 16 迁移空库 PASS

**已完成（自动）：** HTTPS 全栈 Compose 冷启动 + 登录/学员/考勤/Prometheus + 备份恢复（见 `hardening-2026-09-07.md`）。  
**未完成（不得勾选为已放行）：** 正式 CA、人工签字。

## 分工

| 模块/阶段 | 主审人 | 交叉 Reviewer |
|-----------|--------|---------------|
| 00 全局基线 | Cursor Agent（自动） | _待人工_ |
| 01–07 | Cursor Agent（自动） | _待人工_ |

## 汇总

| 阶段/模块 | 检查项数 | 自动结果 | 人工签字 | 日期 |
|-----------|----------|----------|----------|------|
| 00–07 历史清单 | 84 | 需按本轮证据重评，禁止继承旧 PASS | 未签 | — |

## 投产放行

- [x] 自动门禁证据已更新且无伪装 PASS（Agent，2026-09-07）
- [x] HTTPS Compose 冷启动与业务冒烟已执行（Agent 本地演练，自签证书）
- [ ] 正式 CA 证书已安装
- [ ] 投产负责人签字（姓名 / 日期 / tag SHA）

**负责人签字：** ____________________  **日期：** ________
