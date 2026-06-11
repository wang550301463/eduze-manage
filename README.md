# EduZE Manage

教育机构管理系统 —— 单仓库 Spring Boot 3.2.5 + Java 17（后端） + React 18 + Vite 6（前端 `web/`），生产环境单 Jar 同包部署。

## 技术栈

后端：Spring Boot 3.2.5、MyBatis-Plus、MySQL 8、Redis 7、Spring Security + JWT、Flyway。前端：React 18、Vite 6、TypeScript、Tailwind CSS、Radix UI、cmdk。

## 本地启动

```bash
# 可选：仅数据库
docker compose -f docker/docker-compose.dev.yml --env-file docker/.env.example up -d

./mvnw spring-boot:run
curl -s http://localhost:8080/actuator/health

cd web && pnpm install && pnpm dev
# http://localhost:5173  （API 代理到 8080）
```

默认账号：`admin` / `admin@123`

## Docker 生产部署

```bash
./mvnw -DskipTests package
cp docker/.env.example docker/.env   # 编辑密码与 JWT_SECRET
# 在 docker/certs/ 放置 server.crt / server.key
docker compose -f docker/docker-compose.yml --env-file docker/.env up -d
```

详见 [docs/operations/runbook.md](docs/operations/runbook.md)。

## 备份

```bash
DB_PASSWORD='***' ./scripts/backup-mysql.sh
DB_PASSWORD='***' ./scripts/restore-mysql.sh /backups/eduze-YYYY-MM-DD-HHMM.sql.gz
```

## 测试

```bash
./mvnw test                    # 后端单元测试
./mvnw verify                  # 含 Testcontainers 集成测试（*IT.java，需 Docker）
cd web && pnpm test            # 前端单元测试
cd web && pnpm e2e             # Playwright 冒烟（需后端运行）
./scripts/e2e-bootstrap.sh     # 可选：自动起依赖并跑 E2E
```

## 文档

- 设计：`docs/socrates/specs/2026-05-09-eduze-manage-design.md`
- 实施计划：`docs/socrates/plans/2026-05-09-eduze-manage-phase1-plan.md`
- 运维：`docs/operations/runbook.md`
- 投产检查：`docs/operations/release-gate/`（设计见 `docs/socrates/specs/2026-06-11-release-gate-design.md`）
