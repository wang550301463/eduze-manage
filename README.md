# EduZE Manage

教育机构管理系统 —— 单仓库 Spring Boot 3.2.5 + Java 17（后端） + React 18 + Vite 6（前端 `web/`），生产环境单 Jar 同包部署。

## 技术栈

后端：Spring Boot 3.2.5、MyBatis-Plus、MySQL 8、Redis 7、Spring Security + JWT、Lombok、MapStruct、Hutool、FastJSON2。前端：React 18、Vite 6、TypeScript、Tailwind CSS、Radix UI（详见 `docs/socrates/specs/`）。

## 本地启动

```bash
./mvnw spring-boot:run
curl -s http://localhost:8080/actuator/health
# {"status":"UP"}
```

## 文档

- 设计：`docs/socrates/specs/2026-05-09-eduze-manage-design.md`
- 实施计划：`docs/socrates/plans/2026-05-09-eduze-manage-phase1-plan.md`
