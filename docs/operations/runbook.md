# EduZE Manage 运维手册

投产前请完成 [release-gate 检查清单](release-gate/README.md)（84 项 Blocker 全 PASS + **人工签字**）。

## 人工签字门禁

Agent 自审 / 交叉复验**不能替代**真人签字。正式上线前必须：

1. 各模块主审与交叉 Reviewer 在对应清单与 `sign-off-record.md` 签字。
2. 投产负责人确认 84 项 Blocker 全 PASS，在 `sign-off-record.md` 勾选放行项并签字、填写日期、环境、Git tag/SHA。
3. 未完成人工签字不得切换生产流量。

## RPO / RTO

| 指标 | 目标 | 说明 |
|------|------|------|
| **RPO**（恢复点目标） | ≤ 24 小时 | 每日凌晨 mysqldump（见 `scripts/cron.example`），保留 30 天；故障最多丢失自上一次成功备份以来的数据 |
| **RTO**（恢复时间目标） | ≤ 2 小时 | 含：停止应用 → 校验备份 → restore → 启动应用 → 冒烟验证核心路径 |

演练时按 RTO 计时，并把备份文件路径与 `.sha256` 记入证据。

## 首次部署

1. 安装 Docker 与 Docker Compose v2。
2. 构建前端并打包 Jar：
   ```bash
   ./mvnw -DskipTests package
   ```
3. 复制环境变量：`cp docker/.env.example docker/.env`，至少修改：
   - `DB_PASSWORD`、`REDIS_PASSWORD`（非空）
   - `JWT_SECRET`（**≥ 32 字符**）
   - `BOOTSTRAP_ADMIN_USERNAME` / `BOOTSTRAP_ADMIN_PASSWORD`（密码 **≥ 12 字符**，生产首次启动轮换默认超管）
4. 放置**正式 TLS 证书**（见下文「正式证书」）。
5. 启动：`docker compose -f docker/docker-compose.yml --env-file docker/.env up -d`。
6. 访问 `https://<域名>`，使用 bootstrap 后的管理员账号登录，确认后可将 bootstrap 环境变量从 `.env` 移除并重启 app。

### Bootstrap 管理员

生产 profile 下，若库内仍是种子默认 `admin` / `admin@123`，**必须**设置：

```bash
BOOTSTRAP_ADMIN_USERNAME=...
BOOTSTRAP_ADMIN_PASSWORD=...   # ≥12 字符
```

`AdminBootstrapRunner` 会在首次启动时轮换用户名与密码；未设置将拒绝启动。轮换成功后建议从 `.env` 删除这两项，避免明文长期驻留。

### 正式证书

- 生产必须使用机构/公网 CA 签发的证书，将 `server.crt`、`server.key` 放入 `docker/certs/`（见 `docker/certs/README.md`）。
- **禁止**用自签证书对外服务；自签仅限本地联调。
- 证书到期前轮换：替换文件后 `docker compose ... restart nginx`，并验证 `https://` 与浏览器信任链。

## 升级版本

1. 拉取新代码并重新打包：`./mvnw -DskipTests package`。
2. 备份数据库（见下文），确认 `.sql.gz` 非空且 `.sha256` 存在。
3. 记录当前镜像 tag / Git SHA，便于回滚。
4. 重建并滚动应用：`docker compose -f docker/docker-compose.yml build app && docker compose -f docker/docker-compose.yml up -d app`。
5. 检查：`docker compose -f docker/docker-compose.yml logs -f app`，确认 Flyway 迁移成功且无 ERROR。
6. 冒烟：登录、今日课表、签到各一条。

## 回滚步骤

**应用 / 镜像回滚**（无 DB 破坏性迁移时）：

1. `docker compose -f docker/docker-compose.yml stop app`
2. 将 `APP_VERSION`（或 image tag）改回上一已知良好版本，或 checkout 上一 Git tag 后重新 `build app`。
3. `docker compose -f docker/docker-compose.yml up -d app`
4. 验证健康检查与核心业务路径。

**含失败迁移 / 数据损坏时的 DB 回滚**：

1. `docker compose -f docker/docker-compose.yml stop app`（停止写流量）。
2. 选用升级前备份：`CONFIRM_RESTORE=YES APP_STOPPED=YES DB_PASSWORD='***' ./scripts/restore-mysql.sh /backups/eduze-....sql.gz`
3. 将应用镜像回退到与该备份匹配的版本。
4. `up -d app`，冒烟验证后恢复流量。

> Flyway 已成功的 forward-only 迁移通常不能“倒迁”；依赖备份恢复 + 旧应用版本对齐。

## 备份与恢复

生产 compose 已挂载 `../backups:/backups`（mysql / app）。MySQL/Redis **不映射**宿主机端口，仅 compose 内网；备份可在宿主机对已挂载卷操作，或经 `docker compose exec` 进入容器。

**备份**（保留最近 30 天；校验 gzip 并写 `.sha256`）：

```bash
DB_PASSWORD='***' BACKUP_DIR=./backups DB_HOST=127.0.0.1 ./scripts/backup-mysql.sh
# 或在可访问 mysql 服务的网络中：
# docker compose -f docker/docker-compose.yml exec -e DB_PASSWORD -e DB_HOST=127.0.0.1 mysql ...
```

**恢复**（必须显式确认；建议先停 app）：

```bash
docker compose -f docker/docker-compose.yml stop app
CONFIRM_RESTORE=YES APP_STOPPED=YES DB_PASSWORD='***' \
  ./scripts/restore-mysql.sh ./backups/eduze-2026-05-19-0300.sql.gz
docker compose -f docker/docker-compose.yml start app
```

定时任务示例见 `scripts/cron.example`。

## 查看日志

- 应用：`docker compose -f docker/docker-compose.yml logs -f app`
- Nginx：`docker compose -f docker/docker-compose.yml logs -f nginx`
- 宿主机挂载：`./logs/`

## 轮换 JWT Secret

1. 在 `docker/.env` 中设置新 `JWT_SECRET`（仍须 ≥ 32 字符）。
2. 重启应用：`docker compose -f docker/docker-compose.yml up -d app`。
3. 所有已签发 Token 在过期前仍有效；登出黑名单条目随旧 Secret 失效。建议在维护窗口通知全员重新登录。

## 新增校区与超管账号

- **校区**：使用超级管理员登录 → 设置 → 校区 → 新建。
- **超管**：在数据库为用户分配 `SUPER_ADMIN` 角色（`t_user_role.role_id = 1`），或通过已有超管在「账号 / 角色」模块操作（阶段 D 完整 UI）。

## 本地开发依赖

仅启动 MySQL + Redis（开发 compose **仍映射**端口，便于本机 `spring-boot:run`）：

```bash
docker compose -f docker/docker-compose.dev.yml --env-file docker/.env.example up -d
./mvnw spring-boot:run
cd web && pnpm dev
```
