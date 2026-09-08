# 微服务平台运行说明

此平台使用独立 Compose 项目 `eduze-platform` 和新数据卷。旧单体数据库不迁移、不清空；第一次初始化不会复用原 `mysql_data`。

## 开发启动

1. `python3 scripts/platform-env.py` 生成不进入 Git 的 `docker/platform/.env.dev`，文件权限 0600。已存在时拒绝覆盖。
2. `./mvnw package` 构建独立服务 Jar；`pnpm --dir web install --frozen-lockfile && pnpm --dir web build` 构建 PC。
3. `docker compose --env-file docker/platform/.env.dev -f docker/platform/compose.yml up -d --build --wait`。
4. 访问 `http://localhost:18880`，管理员账号信息在私有环境文件中。不要复制口令到日志、工单或代码。
5. `node miniapp/build.mjs` 后使用微信开发者工具导入 `miniapp/`。开发环境文件上传是明确的本地适配；微信登录没有模拟成功后门。

服务数据库仅开放 Compose 内网，各账号只拥有所属数据库；身份和教务使用不同 Redis ACL 账号与 key 范围。不能跨服务查库排查业务；使用所属服务接口或对应账号查询。

## 正式配置

使用私有正式环境文件，配置画室域名、可信 HTTPS 证书、微信 AppID/密钥、通知模板，以及 OSS endpoint/bucket/凭证。正式环境用 `compose.yml` 加 `compose.prod.yml`，禁止使用开发存储。

OSS Bucket 保持私有，上传与读取分开签名。允许的浏览器来源和微信上传／下载域名需要按画室实际域名配置。建议为 `staging/` 对象前缀设置一天后清理的生命周期；正式归档对象不得套用该规则。业务数据库保存媒体元数据与引用，OSS 备份应启用版本保留策略；备份范围同时覆盖数据库和媒体。

微信发送模板配置放在 `eduze.wechat.templates` 下，键为 `LESSON_REMINDER`、`LESSON_CHANGED`、`LEAVE_RESULT`、`PORTFOLIO_PUBLISHED`；每项包含 `template-id` 和 `fields`（微信字段名映射到 `title`／`body`）。实际模板应匹配合法类目、长度与订阅权限。未配置或未订阅会记录 SKIPPED，不显示微信发送成功。发送结果未知不自动重放，需核实后处理。

正式启动示例：

```sh
docker compose --env-file docker/platform/.env.prod -f docker/platform/compose.yml -f docker/platform/compose.prod.yml up -d
```

## 升级与回退

每个服务独立镜像版本，先验证迁移与接口兼容性，再更新目标服务；`compose up -d --no-deps <service>` 不意味着零停机。单机 Compose 不提供高可用承诺。

数据库采用向前兼容迁移。回退应用前检查其能否理解当前 schema 与事件版本；禁止使用旧备份覆盖上线后新业务数据。需要数据修复时停止对应写入，保留当前快照，并以可审计的补偿记录处理。

## 观测与恢复

各服务内部 `/actuator/health` 和 `/actuator/prometheus` 不由公网网关暴露。关注错误率、数据库池、Outbox DEAD/PENDING/SENDING、通知 FAILED 和等待时长。日志带 traceId，网关不记录 URL 查询串，避免输出媒体签名。

每个数据库独立备份并验证恢复到临时库。恢复演练必须核对课程名单、课时包余额／流水、课效版本和媒体引用，并确保已消费事件不会再次产生业务效果。发送到外部平台的通知不能通过还原数据库撤回。

## 外部验收边界

实际微信登录、拍摄、播放、订阅消息、OSS 正式访问，以及 iOS／Android 真机体验必须用画室配置验收。自动化假提供者仅存在测试中。发布说明需列出真实完成的检查，不能将单元测试或浏览器接口模拟称为正式端到端验收。

## 可重复验收命令

```sh
./scripts/verify-platform.sh
./scripts/security-platform.sh
python3 scripts/verify-database-isolation.py
python3 scripts/verify-jdbc-timezone.py
python3 scripts/export-platform-contracts.py
pnpm --dir packages/contracts generate
pnpm --dir packages/contracts check
python3 scripts/platform-smoke.py
python3 scripts/platform-load.py
```

`platform-smoke.py` 仅允许本机开发环境，会创建带 E2E 标签的两个校区、两个班和学生数据；检验多课次发布、重复发布、余额不变、家庭草稿隔离、媒体访问、站内通知和撤权。不会伪造微信登录或支付。压测脚本只使用这些开发数据及合成图片，结果写入 `target/platform-load.json`。不得将本机数字作为生产容量承诺。

OpenAPI 通过内部凭证读取 `/internal/openapi`，公网入口拒绝该路径。八份 schema 是 TypeScript 生成来源；`packages/contracts/README.md` 记录组合类型及动态响应的边界。修改 DTO 后重新导出并生成，CI 检查契约漂移。可执行 Jar 检查验证其嵌入的公共库与当前构建完全一致，避免增量构建携带旧依赖。

## 备份与恢复命令

```sh
python3 scripts/platform-backup.py --quiesce
python3 scripts/platform-restore-check.py backups/platform/<输出的备份目录>
```

`--quiesce` 暂停当前运行的八个业务服务，形成跨库一致恢复点，备份后恢复原先运行的服务。检查 manifest 的 complete、各文件 SHA256 和恢复报告的逐表行数。未加此参数的在线备份只有各库独立快照，不能声称是平台级一致恢复点。恢复检查仅使用随机命名的临时容器，不连接现有业务库；原生产数据不覆盖。数据库恢复后仍须执行业务一致性检查，确认名单、课时余额/流水、发布版本和事件消费效果。

媒体文件单独备份；开发存储卷不能替代 OSS。生产 OSS 需配置版本保留或备份，并实际演练误删恢复，当前代码验收不包含正式 Bucket 恢复证明。

## 单服务发布与事件处置

设置对应的 `IDENTITY_IMAGE_TAG`、`ACADEMIC_IMAGE_TAG` 等独立版本，执行 `compose up -d --no-deps --wait <service>`。网关使用 Docker DNS 动态解析，服务更换地址后可恢复寻址；请求可能在重启窗口失败，客户端应显示可重试状态。回退时改回已验证兼容数据库/事件版本的镜像标签，不执行旧备份覆盖。

```sh
python3 scripts/platform-outbox.py portfolio
python3 scripts/platform-outbox.py portfolio --retry <event-id> --reason '已核实并修复失败原因'
```

只重排 DEAD 事件，保持原事件 ID 和原始内容，消费端继续去重；操作写入本地受限审计文件。不能将外部投递结果未知的通知直接重发。告警应覆盖 DEAD、最老待投递时长及通知积压。Prometheus 提供 `eduze_outbox_events`、`eduze_outbox_oldest_seconds`、连接池与 JVM 指标；开发压测记录采样结果。

## 媒体清理边界

过期未完成上传、失败上传及已封存文件的 staging 副本可以清理。READY 原件不会仅因暂时没有异步引用就被物理删除；管理接口提供候选、版本校验和审计，DELETE 在尚无业务所有者冻结协议时明确返回 `BLOCKED_OWNER_PROTOCOL_REQUIRED`。永久删除 READY 原件的自动化协议仍是后续工作。可访问内容使用不超过五分钟的签名，撤权后停止新签名，不能收回已下载内容。

## 微信支付正式配置

`WECHAT_PAY_APP_ID`、`WECHAT_PAY_MCH_ID`、商户证书序列号、API V3 密钥、平台公钥/证书序列号和回调 HTTPS 基址必须匹配真实商户。私钥和平台公钥放在私有 `docker/platform/payment-keys/`，以只读方式挂载到 `/run/payment-keys`；设置最小读取权限并确认容器 UID 10001 能读取所需文件，禁止把私钥写入镜像。配置文件中的路径使用容器路径。代码包含签名、验签和回调解密；未配置时明确失败，不返回模拟付款成功。

退款冻结阶段遇到尚未到账的教务权益时保留相同退款号等待补偿；确实无法发放权益的失败需要运营核实并完成可审计处理，不能绕过冻结直接退款。支付成功、权益完成及退款结果分别展示。

JDBC `connectionTimeZone=Asia/Shanghai` 必须同时启用 `forceConnectionTimeZoneToSession=true`，JVM 使用 `-Duser.timezone=Asia/Shanghai`，保持 SQL 当前时间、Instant 和既有课表 LocalDateTime 读取一致。单独指定驱动时区而不设置会话会造成事件延迟八小时。运行校验程序只创建连接级临时表。
