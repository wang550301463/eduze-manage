# EduZE Manage 第一期 MVP 投产检查清单

**设计文档：** [docs/socrates/specs/2026-06-11-release-gate-design.md](../../socrates/specs/2026-06-11-release-gate-design.md)

## 使用说明

1. 阅读设计文档 §3 流程，在 `sign-off-record.md` 填写分工（主审人 / 交叉 Reviewer / 投产负责人）
2. **按顺序**执行各阶段；前一阶段全部 PASS 再进入下一阶段
3. 每项仅标 `PASS` 或 `FAIL`；FAIL 登记到 `sign-off-record.md` 缺陷表
4. 全部 84 项 PASS 后，投产负责人在 `sign-off-record.md` 签字

## 阶段索引

| 顺序 | 文件 | 项数 | 说明 |
|------|------|------|------|
| 1 | [00-baseline.md](00-baseline.md) | 6 | 构建、测试、迁移基线 |
| 2a | [01-module-auth.md](01-module-auth.md) | 10 | 登录 / RBAC / 校区 / 设置 |
| 2b | [02-module-student.md](02-module-student.md) | 10 | 学员与家长 |
| 2c | [03-module-course-schedule.md](03-module-course-schedule.md) | 13 | 课程 / 排课 / 老师 |
| 2d | [04-module-attendance.md](04-module-attendance.md) | 10 | 签到 / 接送 / 请假 |
| 3a | [05-module-cross-cutting.md](05-module-cross-cutting.md) | 8 | 全局搜索 / 壳层体验 |
| 3b | [06-nfr-ops.md](06-nfr-ops.md) | 16 | 安全 / 性能 / 运维 |
| 4 | [07-production-dry-run.md](07-production-dry-run.md) | 11 | 生产路径全流程演练 |
| — | [sign-off-record.md](sign-off-record.md) | — | 汇总签字与缺陷 |

**合计：84 项 Blocker。**

## 快速命令参考

```bash
# 阶段 1 基线
./mvnw verify
cd web && pnpm test
./scripts/e2e-bootstrap.sh    # 或手动起服务后 cd web && pnpm e2e
./mvnw -DskipTests package

# 生产演练
cp docker/.env.example docker/.env   # 编辑密钥
./mvnw -DskipTests package
docker compose -f docker/docker-compose.yml --env-file docker/.env up -d
```

## 角色

- **主审人**：执行清单步骤，填结果与证据
- **交叉 Reviewer**：复现关键路径，签字
- **投产负责人**：汇总、组织演练、最终放行签字
