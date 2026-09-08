# 美术平台实施记录

依据：本任务中用户批准的 M0–M6 微服务、PC 与小程序实施计划。

## 已确认边界

自营多校区；可重建数据库但保留旧库备份；固定班多课次主题；教师直接发布家庭课效；公开展览另行授权审核；Compose 独立服务数据库；Java 17 / Boot 3.5；原生 TypeScript 小程序；私有 OSS；阿里黄山版规约及项目架构门禁。

## 交付跟踪

- [x] M0：单仓库 Maven 聚合、八个服务独立数据库/镜像、质量门禁、Outbox/Inbox、入口隔离。
- [x] M1：身份/组织/权限与教务迁移，家庭关系、课表、名单、请假、签到、课时冲正及重复/并发回归。
- [x] M2：模板/课件固定版本、学期计划、班级多课次主题、私有媒体上传封存和签名接口。
- [x] M3：过程与成品、合作作品、草稿并发校验、课次记录、固定课效发布、PC 与老师小程序代码。
- [x] M4 开发交付：家庭邀请与确认、家长端、真实微信适配器、站内消息、订阅意愿和投递状态分离。
- [ ] M4 正式首发验收：真实 AppID/模板、OSS、HTTPS、微信开发者工具及 iOS/Android 真机验收。
- [x] M5 开发交付：成长报告/作品集、授权公开展览、试听与线索、活动、渠道、推荐记录和续费待办。
- [x] M6 基础代码交付：商品、订单、支付验签/解密、权益补偿、退款冻结、实物履约/售后和对账。
- [ ] M6 正式商户验收：真实付款、退款、回调、账单与商户配置验证。

M5/M6 代码已包含，但按原计划作为后续业务版本发布。复杂库存、备料、教师课时费是后续子版本，未交付。READY 媒体永久自动清理还需业务所有者冻结协议，当前提供安全候选与审计并阻止危险删除。开发完成不等于正式上线。

实测结果见 [开发验收记录](../../operations/platform-acceptance-2026-09-08.md)，运行步骤见 [运行手册](../../operations/platform-runbook.md)。

## 基础接口约定

响应：`{code:0,message:"OK",data:...,traceId:"..."}`。错误返回匹配 HTTP 状态及安全错误信息。所有新接口 ID 为字符串，时间为 ISO offset/UTC 时间。

新服务共享基础包 `com.eduze.platform.runtime`（模块 `platform-runtime`），仅提供技术能力：

- `Actor(String userId,String tenantId,Set<String> branchIds,Set<String> roles,Set<String> permissions)`；`isStaff()` / `isSuperAdmin()` / `requireBranch(String)` / `requirePermission(String)`。
- `Actors.current()` 返回当前验证身份；无身份抛 401。不使用客户端头构造身份。
- `ApiResponse.ok(T)`，`PlatformException(int status,String message)`。
- `InternalClient.get(String service,String path,Class<T>)` / `post(String service,String path,Object body,Class<T>)`：向配置服务地址调用，携带原始用户 Bearer 及内部服务凭证。不自动重试写操作。
- `Outbox.enqueue(String target,String type,String tenantId,String branchId,String aggregateId,Object payload)` 与业务事务同库。接收方 `/internal/events` 使用唯一事件 ID 去重并同事务处理。
- `EventEnvelope(String eventId,String type,int version,String tenantId,String branchId,String aggregateId,Instant occurredAt,String traceId,JsonNode payload)`。
- Spring Boot 新服务扫描 `com.eduze.platform.runtime` 与自身包；运行时身份校验使用 identity `POST /internal/identity/introspect`，入参 `{token:"..."}`，响应为原始 Actor JSON；无效会话 401，不可用 503，拒绝访问。
- 内部调用凭证：`X-Service-Name` / `X-Service-Token`，接收端按调用方配置核验；不向公网暴露 `/internal`。

`GET /internal/academic/students/{id}/access` 返回 `{allowed:boolean,branchId:string}`，教务根据转发的真实用户会话校验家庭或员工关系；非授权 403。批量学生/老师/校区接口由归属服务提供。

服务项目：`services/{identity,academic,teaching,portfolio,media,notification,engagement,commerce}-service`。父 POM `com.eduze:eduze-platform:1.0.0`，公共基础库 `com.eduze:platform-runtime:1.0.0`。

旧 API 路由由身份/教务/教学/作品归属服务兼容；只读兼容代码不能定义远端拥有的数据表。旧根 src 已归档到 legacy/monolith/src，退出聚合构建，数据库初始化由各服务独立 Flyway 执行。

## 初始验证

- 原工程 Maven 单元测试命令完成（退出码 0）。
- 原 PC：26 个测试文件，81 个测试通过。
- 工作树 `.worktrees/art-platform`，分支 `codex/art-platform`。
