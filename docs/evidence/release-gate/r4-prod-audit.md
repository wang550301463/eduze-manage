# R4 生产演练证据（2026-06-12）

## 环境说明（降级路径）

因 Docker Hub 拉取超时（`registry-1.docker.io` 连接超时），无法构建 `eduze-app` / 拉取 `nginx:1.27-alpine`。

**实际演练路径：**

- `docker compose -f docker/docker-compose.yml --env-file docker/.env up -d mysql redis`
- `java -jar target/eduze-manage.jar` + `SPRING_PROFILES_ACTIVE=prod`
- 密钥来自本地 `docker/.env`（已 gitignore，未提交）

## 证据文件

| 文件 | 说明 |
|------|------|
| `r4-dry-run.log` | API 演练脚本输出 |
| `r4-jar-prod.log` / `r4-jar-restart.log` / `r4-jar-d11.log` | prod JAR 启动与升级日志 |
| `backups/eduze-dryrun-2026-06-12-1105.sql.gz` | D-10 备份产物 |

## D-01～D-11 结果摘要

| RC | 结果 | 说明 |
|----|------|------|
| RC-07-001 / D-01 | **FAIL** | 无 app/nginx 容器；`curl -sk https://localhost` 未测 |
| RC-07-002 / D-02 | PASS | admin 改密 `Admin@DryRun2026!`；新建校区 B |
| RC-07-003 / D-03 | PASS | `front_b` / `teacher_b` 角色与校区范围正确 |
| RC-07-004 / D-04 | PASS | 3 学员 + 家长 + 课时包 |
| RC-07-005 / D-05 | PASS | 课程/班级/成员 + bulk-generate 1 课次 |
| RC-07-006 / D-06 | PASS | 手动签到 + 扫码签到（HTTP；扫码需传 `lessonId`，老师中心课次 `class_group_id` 为空） |
| RC-07-007 / D-07 | PASS | 前台代录请假 → 班主任审批 |
| RC-07-008 / D-08 | PASS | dashboard KPI（修复 `AttendanceStatService` null class_group_id NPE 后） |
| RC-07-009 / D-09 | PASS | 搜索「演练学员1」命中 |
| RC-07-010 / D-10 | PASS | mysqldump 备份 → 停 JAR → restore → 3 学员 + 1 课次仍在 |
| RC-07-011 / D-11 | PASS | 重新 `mvn package` + 重启 JAR；Flyway「No migration necessary」 |

## RC-06 补齐

| RC | 结果 | 说明 |
|----|------|------|
| RC-06-001 | **FAIL** | 同 D-01，nginx TLS 未启动 |
| RC-06-002 | PASS | `docker/.env` 非 example 默认值 |
| RC-06-013 | PASS | 同 D-10 |
| RC-06-016 | PASS | 默认 `admin@123` 已失效；新密码可登录 |

## 演练中修复的代码缺陷

- `AttendanceStatService.branchStat`：老师中心课次 `class_group_id` 为 NULL 时统计接口 500 → 跳过 NULL 分组。

## 未关闭 Blocker（投产前）

~~1. **RC-06-001 / RC-07-001**~~ **已关闭**（2026-06-12 下午）：经 DaoCloud 镜像源拉取 `nginx:1.27-alpine` / `eclipse-temurin:17-jre-jammy`，`docker compose up -d` 全栈 + `curl -sk https://localhost/actuator/health` → UP。见 `r4-https-fullstack.log`。

1. 交叉 Reviewer 与投产负责人签字（`sign-off-record.md`）

## 镜像拉取备忘（本地网络）

Docker Hub 直连超时时可用：

```bash
docker pull docker.m.daocloud.io/library/nginx:1.27-alpine
docker tag docker.m.daocloud.io/library/nginx:1.27-alpine nginx:1.27-alpine
docker pull docker.m.daocloud.io/library/eclipse-temurin:17-jre-jammy
docker tag docker.m.daocloud.io/library/eclipse-temurin:17-jre-jammy eclipse-temurin:17-jre-jammy
docker compose -f docker/docker-compose.yml --env-file docker/.env up -d --pull never
```
