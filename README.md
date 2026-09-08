# EduZE 美术画室平台

Java 17 / Spring Boot 3.5.16 多模块微服务，React + TypeScript PC 管理端，以及原生 TypeScript 微信小程序。机构、教务、教学、作品、媒体、通知、招生、交易各自拥有数据库和部署镜像。

## 开发启动

```bash
python3 scripts/platform-env.py  # 首次生成私有本地配置，拒绝覆盖已有文件
./mvnw package
pnpm --dir web install --frozen-lockfile
pnpm --dir web build
node miniapp/build.mjs
docker compose --env-file docker/platform/.env.dev -f docker/platform/compose.yml up -d --build --wait
```

PC 入口默认 http://localhost:18880。初始化管理员由私有配置中的 BOOTSTRAP_ADMIN_USERNAME / BOOTSTRAP_ADMIN_PASSWORD 指定，没有固定默认密码。微信、OSS、支付正式凭证需按运维文档配置；开发文件存储只在开发环境启用。

## 验证

```bash
./scripts/verify-platform.sh
```

该入口执行服务边界、独立数据库配置、Java 规范门禁、可执行包一致性、OpenAPI 契约、后端测试、PC lint/测试/构建与小程序 TypeScript 编译。Testcontainers 测试需要 Docker。正式接入与真机验收单独记录，不以模拟测试替代。

## 文档

- [部署、备份与恢复](docs/operations/platform-runbook.md)
- [实施与验收记录](docs/socrates/plans/2026-09-08-platform-execution.md)
- 各服务的 API.md / README.md 提供业务接口和权限说明。
- [原单体归档](legacy/monolith/README.md) 保留拆分前实现与回归基线；不参与新平台构建。

正式部署必须使用 docker/platform/compose.prod.yml 覆盖配置，提供 HTTPS 证书与真实外部适配器配置。Compose 是单机部署方案。
