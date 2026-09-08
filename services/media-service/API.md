# Media API (M2)

媒体上传和业务访问均通过网关 `/api/v1`，内部接口仅接受已验证的服务身份。文件元数据保存媒体 ID 和对象键，不保存临时签名 URL。

## 上传与读取

- `POST /api/v1/media/uploads`: `{branchId,purpose,fileName,contentType,size}`，用途为 `ARTWORK|COURSEWARE|AUDIO`，返回 `{id,uploadUrl,method,headers,expiresAt}`，上传凭证有效期 15 分钟。
- `POST /api/v1/media/uploads/{id}/complete`: 原上传者确认。服务端复制 staging 到独立随机 sealed 键，检查大小、类型和文件签名后才置为 READY。旧 PUT 凭证不能覆盖 sealed 文件。重复确认 READY 是幂等操作。
- `GET /internal/media/{id}/usable`: 租户、员工和校区校验后返回可用性；READY 文件会原子更新最近使用时间及审核版本。
- `POST /internal/media/access`: 业务服务在执行实时资源授权后传入 `{mediaIds}`。
- `POST /internal/media/public-access`: portfolio 传入 `{tenantId,publicationId,mediaIds}`；要求 publication 的媒体引用已经到达 media。

访问接口返回 `{items:[{id,url,expiresAt,contentType,thumbnailUrl?}]}`。`url` 始终读取原文件；仅图片返回 `thumbnailUrl`。OSS 图片缩略图签名包含 480px resize、自动旋转和 WebP 转换，两个链接均在 5 分钟后过期。音频/PDF不请求图片处理。localdev 缩略图链接允许回退到原图。签名链接本身的后续读取不会刷新数据库时间，但每次业务签发链接会刷新。

业务提交后的 `teaching.media-references`、`portfolio.media-references` 等事件通过 Inbox 更新引用；每个业务所有者以 revision 防止旧事件覆盖新引用，重试失败不会丢失既有引用。引用事件及同步关联也刷新媒体审核版本。

## 安全清理与人工复核

定时清理每批至多 50 条，默认每小时执行。凭证过期超过一天的 PENDING/FAILED staging 可实际删除；READY 的旧 staging 也可独立删除，sealed 原件始终保留。清理与确认使用同一元数据行锁，并在获得锁之后重新检查状态及引用，避免候选查询与确认并发导致删除 sealed。对象删除失败会使本批数据库事务回滚；存储删除需要具备幂等语义，下一轮可以重试。

READY 文件超过 30 天未调用 usable、签发访问链接或收到引用，且 media 当前无引用时，才列为**人工复核候选**。候选仍然可读、可复用，不是可删除证明：业务 Outbox 可能延迟，历史发布内容可能尚未同步引用。系统不根据年龄或空引用集合自动删除 READY 文件。

- `GET /api/v1/media/cleanup/candidates`：最多 100 个候选，返回 `[{id,branchId,fileName,version,lastAccessedAt}]`。需要 `media:manage` 权限（SUPER_ADMIN可用），按租户及校区限定访问。
- `POST /api/v1/media/cleanup/{id}/review`：请求 `{expectedVersion,decision,reason}`，`decision` 为 `RETAIN` 或 `DELETE`，reason 必填且最多 1000 字。权限同上。
- `RETAIN` 返回 `{id,decision:"RETAIN",deleted:false,version}`，刷新最近使用时间并递增版本，30 天内不再列为候选。
- `DELETE` **不会删除文件**，返回 `{id,decision:"BLOCKED_OWNER_PROTOCOL_REQUIRED",deleted:false,version}`，并保留文件 30 天后再复核。客户端必须检查 `deleted` 和 `decision`，不能把 HTTP 200 解读为物理删除成功。
- 文件已被使用、引用到达或版本已变化时返回 409，要求刷新候选。成功复核写入 `media_cleanup_audit`，包含操作者、租户、媒体 ID、提交版本、决定、理由和时间。

要开放 READY 永久删除，后续必须建立所有业务所有者的引用确认/冻结协议，并阻止复用与新业务提交，覆盖未投递 Outbox 和正在进行的事务；单次查询队列为空或无引用不足以安全删除。当前接口明确保留这条安全边界。

V3 迁移只为现有 PENDING/FAILED 的已知 staging 键补充清理信息。迁移前已确认文件若没有保留 staging 键，不猜测并删除潜在对象；其历史 staging 残留需要独立核对。

## 本地验证

`mvn -pl services/media-service test` 使用 H2 与模拟 ObjectStore 检查缩略图/原图、音频不转换、签名参数、旧引用顺序、staging 清理、确认并发、双事务复用与审核版本冲突及删除阻断。OSS 签名测试仅离线生成 URL，不执行上传、复制或删除真实 OSS 对象。
