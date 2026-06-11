# EduZE Manage 运维手册

投产前请完成 [release-gate 检查清单](release-gate/README.md)（84 项 Blocker 全 PASS + 签字）。

## 首次部署

1. 安装 Docker 与 Docker Compose v2。
2. 构建前端并打包 Jar：
   ```bash
   ./mvnw -DskipTests package
   ```
3. 复制环境变量：`cp docker/.env.example docker/.env`，修改 `DB_PASSWORD`、`JWT_SECRET`。
4. 生成 TLS 证书（见 `docker/certs/README.md`）。
5. 启动：`docker compose -f docker/docker-compose.yml --env-file docker/.env up -d`。
6. 访问 `https://localhost`，默认账号 `admin` / `admin@123`（首次登录后请改密）。

## 升级版本

1. 拉取新代码并重新打包：`./mvnw -DskipTests package`。
2. 备份数据库（见下文）。
3. 重建并滚动应用：`docker compose -f docker/docker-compose.yml build app && docker compose -f docker/docker-compose.yml up -d app`。
4. 检查：`docker compose -f docker/docker-compose.yml logs -f app`，确认 Flyway 迁移成功且无 ERROR。

## 备份与恢复

**备份**（保留最近 30 天）：

```bash
DB_PASSWORD='***' ./scripts/backup-mysql.sh
```

Docker 环境可将 `BACKUP_DIR` 挂载到宿主机，例如 `-v ./backups:/backups`。

**恢复**：

```bash
DB_PASSWORD='***' ./scripts/restore-mysql.sh /backups/eduze-2026-05-19-0300.sql.gz
```

定时任务示例见 `scripts/cron.example`。

## 查看日志

- 应用：`docker compose -f docker/docker-compose.yml logs -f app`
- Nginx：`docker compose -f docker/docker-compose.yml logs -f nginx`
- 宿主机挂载：`./logs/`

## 轮换 JWT Secret

1. 在 `docker/.env` 中设置新 `JWT_SECRET`。
2. 重启应用：`docker compose -f docker/docker-compose.yml up -d app`。
3. 所有已签发 Token 在过期前仍有效；登出黑名单条目随旧 Secret 失效。建议在维护窗口通知全员重新登录。

## 新增校区与超管账号

- **校区**：使用超级管理员登录 → 设置 → 校区 → 新建。
- **超管**：在数据库为用户分配 `SUPER_ADMIN` 角色（`t_user_role.role_id = 1`），或通过已有超管在「账号 / 角色」模块操作（阶段 D 完整 UI）。

## 本地开发依赖

仅启动 MySQL + Redis：

```bash
docker compose -f docker/docker-compose.dev.yml --env-file docker/.env.example up -d
./mvnw spring-boot:run
cd web && pnpm dev
```
